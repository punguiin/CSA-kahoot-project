package kahoot.net;

import kahoot.game.GameAction;
import kahoot.game.GameResult;
import kahoot.game.GameService;
import kahoot.game.GameSession;
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

        if (type == MessageType.REQ_REJOIN) {
            handleRejoin(connId, reqPktId, json);
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

        GameResult result = switch (type) {
            case REQ_START_QUIZ -> gameService.startGame(identity.pin());
            case REQ_SUBMIT_ANSWER -> gameService.submitAndAdvance(
                    identity.pin(), identity.nickname(), action.getAnswerId());
            default -> gameService.executeAction(action);
        };
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
            case REQ_SUBMIT_ANSWER -> {
                reply(connId, reqPktId, MessageType.ANSWER_RESULT,
                        PayloadCodec.answerResult(result.getAnswerResult()));

                if (result.getState() == GameState.FINISHED) {
                    sendTo(connId, MessageType.GAME_FINISHED,
                            PayloadCodec.leaderboard(action.getPin(), GameState.FINISHED, result.getLeaderboard()));
                } else if (result.getCurrentQuestion() != null) {
                    int index = gameStateManager.getSession(action.getPin())
                            .map(s -> s.progressOf(action.getNickname()))
                            .orElse(0);
                    sendTo(connId, MessageType.QUESTION,
                            PayloadCodec.question(action.getPin(), index, result.getCurrentQuestion()));
                }
            }
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

    private void handleRejoin(int connId, long reqPktId, Map<String, Object> json) {
        Object pinObj = json.get("pin");
        if (!(pinObj instanceof String pin)) {
            sendError(connId, reqPktId, "Missing pin");
            return;
        }
        GameSession session = gameStateManager.getSession(pin).orElse(null);
        if (session == null) {
            sendError(connId, reqPktId, "Room not found: " + pin);
            return;
        }

        Object nickObj = json.get("nickname");
        if (nickObj instanceof String nickname && !nickname.isBlank()) {
            boolean known = session.getPlayers().stream()
                    .anyMatch(p -> p.getNickname().equalsIgnoreCase(nickname));
            if (!known) {
                sendError(connId, reqPktId, "No active player '" + nickname + "' to rejoin");
                return;
            }
            registry.bindPlayer(connId, pin, nickname);
            reply(connId, reqPktId, MessageType.JOIN_ACCEPTED,
                    PayloadCodec.joinAccepted(pin, nickname, session.getState()));
            replayPlayerState(connId, session, nickname);
        } else {
            registry.bindHost(connId, pin);
            reply(connId, reqPktId, MessageType.ROOM_CREATED, PayloadCodec.roomCreated(pin));
            replayHostState(connId, session);
        }
    }

    private void replayPlayerState(int connId, GameSession session, String nickname) {
        String pin = session.getPin();
        if (session.getState() == GameState.LOBBY) {
            sendTo(connId, MessageType.PLAYER_JOINED, PayloadCodec.playerJoined(pin, session.getPlayers(), ""));
        } else if (session.isPlayerDone(nickname)) {
            sendTo(connId, MessageType.GAME_FINISHED,
                    PayloadCodec.leaderboard(pin, GameState.FINISHED, session.getLeaderboard()));
        } else {
            session.currentQuestionForPlayer(nickname).ifPresent(q -> sendTo(connId, MessageType.QUESTION,
                    PayloadCodec.question(pin, session.progressOf(nickname), q)));
        }
    }

    private void replayHostState(int connId, GameSession session) {
        String pin = session.getPin();
        if (session.getState() == GameState.LOBBY) {
            sendTo(connId, MessageType.PLAYER_JOINED, PayloadCodec.playerJoined(pin, session.getPlayers(), ""));
        } else {
            sendTo(connId, MessageType.LEADERBOARD,
                    PayloadCodec.leaderboard(pin, session.getState(), session.getLeaderboard()));
        }
    }

    public void endRoom(String pin) {
        broadcast(pin, MessageType.ROOM_CLOSED,
                PayloadCodec.error("Сесію завершено адміністратором"));
        gameStateManager.removeSession(pin);
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

    private void sendTo(int connId, MessageType type, String json) {
        registry.sendTo(connId, build(type, connId, eventPktId.incrementAndGet(), json));
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
