package kahoot.net;

import kahoot.game.GameAction;
import kahoot.game.GameResult;
import kahoot.game.GameService;
import kahoot.game.GameState;
import kahoot.game.GameStateManager;
import kahoot.game.Player;
import kahoot.net.ConnectionRegistry.Identity;
import kahoot.net.ConnectionRegistry.Role;
import kahoot.protocol.Message;
import kahoot.protocol.MessageType;
import kahoot.protocol.Packet;
import kahoot.protocol.PayloadCodec;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class SessionDispatcher {

    private static final byte SERVER_SRC = 0x00;

    private final GameService gameService;
    private final GameStateManager gameStateManager;
    private final ConnectionRegistry registry;
    private final AtomicLong eventPktId = new AtomicLong(0);

    public SessionDispatcher(GameService gameService, GameStateManager gameStateManager,
                             ConnectionRegistry registry) {
        this.gameService = gameService;
        this.gameStateManager = gameStateManager;
        this.registry = registry;
    }

    public void onPacket(int connId, Packet pkt) {
        long reqPktId = pkt.getPktId();

        MessageType type;
        try {
            type = MessageType.fromCode(pkt.getMessage().getCType());
        } catch (IllegalArgumentException e) {
            sendError(connId, reqPktId, "Unknown message type");
            return;
        }
        if (!type.isRequest()) {
            sendError(connId, reqPktId, "Not a request: " + type);
            return;
        }

        Map<String, Object> json;
        try {
            json = PayloadCodec.read(pkt.getMessage().getPayload());
        } catch (RuntimeException e) {
            sendError(connId, reqPktId, "Malformed payload: " + e.getMessage());
            return;
        }

        Identity identity = registry.identityOf(connId);
        if (requiresBinding(type) && identity == null) {
            sendError(connId, reqPktId, "You have not joined a room");
            return;
        }
        if (requiresHost(type) && (identity == null || identity.role() != Role.HOST)) {
            sendError(connId, reqPktId, "Only the host may do that");
            return;
        }

        GameAction action;
        try {
            String pin = identity != null ? identity.pin() : null;
            String nick = identity != null ? identity.nickname() : null;
            action = PayloadCodec.toAction(type, json, pin, nick);
        } catch (RuntimeException e) {
            sendError(connId, reqPktId, e.getMessage());
            return;
        }

        GameResult result = gameService.executeAction(action);
        if (!result.isSuccess()) {
            sendError(connId, reqPktId, result.getMessage());
            return;
        }

        dispatch(connId, reqPktId, type, action, result);
    }

    private void dispatch(int connId, long reqPktId, MessageType type, GameAction action, GameResult result) {
        switch (type) {
            case REQ_CREATE_ROOM -> {
                registry.bindHost(connId, result.getPin());
                reply(connId, reqPktId, MessageType.ROOM_CREATED, PayloadCodec.roomCreated(result.getPin()));
            }
            case REQ_JOIN_ROOM -> {
                String pin = result.getPin();
                String nick = action.getNickname();
                registry.bindPlayer(connId, pin, nick);
                reply(connId, reqPktId, MessageType.JOIN_ACCEPTED,
                        PayloadCodec.joinAccepted(pin, nick, result.getState()));

                broadcast(pin, MessageType.PLAYER_JOINED,
                        PayloadCodec.playerJoined(pin, roster(pin), nick));
            }
            case REQ_START_QUIZ -> pushQuestion(action.getPin(), result);
            case REQ_SUBMIT_ANSWER -> reply(connId, reqPktId, MessageType.ANSWER_RESULT,
                    PayloadCodec.answerResult(result.getAnswerResult()));
            case REQ_NEXT_QUESTION -> {
                String pin = action.getPin();
                if (result.getState() == GameState.FINISHED) {
                    broadcast(pin, MessageType.GAME_FINISHED,
                            PayloadCodec.leaderboard(pin, GameState.FINISHED, result.getLeaderboard()));
                } else {
                    pushQuestion(pin, result);
                }
            }
            case REQ_END_ROUND -> {
                String pin = action.getPin();
                broadcast(pin, MessageType.LEADERBOARD,
                        PayloadCodec.leaderboard(pin, result.getState(), result.getLeaderboard()));
            }
            case REQ_GET_LEADERBOARD -> reply(connId, reqPktId, MessageType.LEADERBOARD,
                    PayloadCodec.leaderboard(action.getPin(), result.getState(), result.getLeaderboard()));
            default -> sendError(connId, reqPktId, "Unhandled request: " + type);
        }
    }

    public void onDisconnect(int connId) {
        Identity identity = registry.unregister(connId);
        if (identity == null) {
            return;
        }
        String who = identity.role() == Role.HOST ? "host" : identity.nickname();
        broadcast(identity.pin(), MessageType.PLAYER_LEFT,
                PayloadCodec.playerLeft(identity.pin(), roster(identity.pin()), who));
    }

    private void pushQuestion(String pin, GameResult result) {
        int index = gameStateManager.getSession(pin)
                .map(s -> s.getCurrentQuestionIndex())
                .orElse(0);
        broadcast(pin, MessageType.QUESTION, PayloadCodec.question(pin, index, result.getCurrentQuestion()));
    }

    private List<Player> roster(String pin) {
        return gameStateManager.getSession(pin)
                .map(s -> s.getPlayers())
                .orElse(List.of());
    }

    private void reply(int connId, long pktId, MessageType type, String json) {
        registry.sendTo(connId, build(type, connId, pktId, json));
    }

    private void broadcast(String pin, MessageType type, String json) {
        registry.broadcast(pin, build(type, 0, eventPktId.incrementAndGet(), json), -1);
    }

    private void sendError(int connId, long pktId, String message) {
        registry.sendTo(connId, build(MessageType.ERROR, connId, pktId, PayloadCodec.error(message)));
    }

    private Packet build(MessageType type, int connId, long pktId, String json) {
        Message msg = new Message(type.code(), connId, PayloadCodec.bytes(json));
        return new Packet(Packet.MAGIC, SERVER_SRC, pktId, 0, msg);
    }

    private static boolean requiresBinding(MessageType type) {
        return switch (type) {
            case REQ_START_QUIZ, REQ_SUBMIT_ANSWER, REQ_NEXT_QUESTION,
                 REQ_END_ROUND, REQ_GET_LEADERBOARD -> true;
            default -> false;
        };
    }

    private static boolean requiresHost(MessageType type) {
        return switch (type) {
            case REQ_START_QUIZ, REQ_NEXT_QUESTION, REQ_END_ROUND -> true;
            default -> false;
        };
    }
}
