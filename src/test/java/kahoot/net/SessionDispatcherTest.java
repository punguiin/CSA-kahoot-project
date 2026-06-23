package kahoot.net;

import kahoot.db.DatabaseConnection;
import kahoot.db.GameHistoryDAOImpl;
import kahoot.db.QuizDAO;
import kahoot.db.QuizDAOImpl;
import kahoot.game.GameService;
import kahoot.game.GameStateManager;
import kahoot.model.Answer;
import kahoot.model.Question;
import kahoot.model.Quiz;
import kahoot.protocol.Message;
import kahoot.protocol.MessageType;
import kahoot.protocol.Packet;
import kahoot.protocol.PayloadCodec;
import kahoot.transport.OutboundSink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SessionDispatcherTest {

    private static final class RecordingSink implements OutboundSink {
        final int connId;
        final List<Packet> received = new ArrayList<>();

        RecordingSink(int connId) {
            this.connId = connId;
        }

        @Override public int connId() {
            return connId;
        }

        @Override public boolean isOpen() {
            return true;
        }

        @Override public boolean send(Packet packet) {
            received.add(packet);
            return true;
        }

        List<MessageType> types() {
            return received.stream().map(p -> MessageType.fromCode(p.getMessage().getCType())).toList();
        }

        Packet last() {
            return received.get(received.size() - 1);
        }
    }

    private SessionDispatcher dispatcher;
    private ConnectionRegistry registry;
    private QuizDAO quizDAO;
    private int quizId;
    private int correctAnswerId;

    private RecordingSink host;
    private RecordingSink alice;
    private RecordingSink bob;

    @BeforeEach
    void setUp() {
        Connection connection = new DatabaseConnection(":memory:").getConnection();
        quizDAO = new QuizDAOImpl(connection);
        GameStateManager gsm = new GameStateManager();
        GameService gameService = new GameService(gsm, quizDAO, new GameHistoryDAOImpl(connection));
        registry = new ConnectionRegistry();
        dispatcher = new SessionDispatcher(gameService, gsm, registry);

        quizId = quizDAO.insert(buildQuiz());
        correctAnswerId = quizDAO.findById(quizId).get().getQuestions().get(0).getAnswers().stream()
                .filter(Answer::isCorrect).findFirst().get().getId();

        host = register(1);
        alice = register(2);
        bob = register(3);
    }

    private Quiz buildQuiz() {
        Question question = new Question(0, "Capital of France?", 10);
        question.setAnswers(List.of(
                new Answer(0, "Paris", true),
                new Answer(0, "Berlin", false),
                new Answer(0, "Madrid", false)));
        Quiz quiz = new Quiz("Geography", "Quick geography quiz", 1);
        quiz.setQuestions(List.of(question));
        return quiz;
    }

    private RecordingSink register(int connId) {
        RecordingSink sink = new RecordingSink(connId);
        registry.register(sink);
        return sink;
    }

    private void request(int connId, MessageType type, String json) {
        Message msg = new Message(type.code(), 0, PayloadCodec.bytes(json));
        dispatcher.onPacket(connId, new Packet(Packet.MAGIC, (byte) connId, 1L, 0, msg));
    }

    private static Map<String, Object> payloadOf(Packet pkt) {
        return PayloadCodec.read(pkt.getMessage().getPayload());
    }

    private String createRoomAndJoinEveryone() {
        request(1, MessageType.REQ_CREATE_ROOM, "{\"quizId\":" + quizId + "}");
        String pin = (String) payloadOf(host.last()).get("pin");
        request(2, MessageType.REQ_JOIN_ROOM, "{\"pin\":\"" + pin + "\",\"nickname\":\"alice\"}");
        request(3, MessageType.REQ_JOIN_ROOM, "{\"pin\":\"" + pin + "\",\"nickname\":\"bob\"}");
        return pin;
    }

    @Test
    void createRoomRepliesOnlyToHost() {
        request(1, MessageType.REQ_CREATE_ROOM, "{\"quizId\":" + quizId + "}");

        assertThat(host.types()).containsExactly(MessageType.ROOM_CREATED);
        assertThat(alice.received).isEmpty();
        assertThat(bob.received).isEmpty();
        assertThat(payloadOf(host.last()).get("pin")).asString().hasSize(6);
    }

    @Test
    void joinBroadcastsRosterToWholeRoom() {
        request(1, MessageType.REQ_CREATE_ROOM, "{\"quizId\":" + quizId + "}");
        String pin = (String) payloadOf(host.last()).get("pin");

        request(2, MessageType.REQ_JOIN_ROOM, "{\"pin\":\"" + pin + "\",\"nickname\":\"alice\"}");

        assertThat(alice.types()).containsExactly(MessageType.JOIN_ACCEPTED, MessageType.PLAYER_JOINED);
        assertThat(host.types()).containsExactly(MessageType.ROOM_CREATED, MessageType.PLAYER_JOINED);

        @SuppressWarnings("unchecked")
        List<Object> roster = (List<Object>) payloadOf(host.last()).get("players");
        assertThat(roster).hasSize(1);
    }

    @Test
    void startQuizPushesQuestionToPlayersWhoNeverAsked() {
        String pin = createRoomAndJoinEveryone();

        request(1, MessageType.REQ_START_QUIZ, "{}");

        assertThat(alice.types()).contains(MessageType.QUESTION);
        assertThat(bob.types()).contains(MessageType.QUESTION);
        assertThat(host.types()).contains(MessageType.QUESTION);

        String questionJson = new String(alice.last().getMessage().getPayload());
        assertThat(questionJson).doesNotContain("isCorrect");
    }

    @Test
    void submitAnswerRepliesOnlyToTheAnsweringPlayer() {
        String pin = createRoomAndJoinEveryone();
        request(1, MessageType.REQ_START_QUIZ, "{}");
        int hostBefore = host.received.size();
        int bobBefore = bob.received.size();

        request(2, MessageType.REQ_SUBMIT_ANSWER, "{\"answerId\":" + correctAnswerId + "}");

        assertThat(alice.last().getMessage().getCType()).isEqualTo(MessageType.ANSWER_RESULT.code());
        assertThat(payloadOf(alice.last()).get("correct")).isEqualTo(true);

        assertThat(host.received).hasSize(hostBefore);
        assertThat(bob.received).hasSize(bobBefore);
    }

    @Test
    void endRoundThenFinishBroadcastsToRoom() {
        String pin = createRoomAndJoinEveryone();
        request(1, MessageType.REQ_START_QUIZ, "{}");

        request(1, MessageType.REQ_END_ROUND, "{}");
        assertThat(alice.types()).contains(MessageType.LEADERBOARD);
        assertThat(bob.types()).contains(MessageType.LEADERBOARD);

        request(1, MessageType.REQ_NEXT_QUESTION, "{}");
        assertThat(alice.types()).contains(MessageType.GAME_FINISHED);
        assertThat(host.types()).contains(MessageType.GAME_FINISHED);
    }

    @Test
    void playerMayNotStartTheQuiz() {
        String pin = createRoomAndJoinEveryone();

        request(2, MessageType.REQ_START_QUIZ, "{}");

        assertThat(alice.last().getMessage().getCType()).isEqualTo(MessageType.ERROR.code());
        assertThat(payloadOf(alice.last()).get("message")).asString().contains("host");
    }

    @Test
    void joiningUnknownRoomErrorsOnlyTheRequester() {
        request(2, MessageType.REQ_JOIN_ROOM, "{\"pin\":\"000000\",\"nickname\":\"ghost\"}");

        assertThat(alice.types()).containsExactly(MessageType.ERROR);
        assertThat(host.received).isEmpty();
        assertThat(bob.received).isEmpty();
    }

    @Test
    void disconnectBroadcastsPlayerLeftToRest() {
        String pin = createRoomAndJoinEveryone();
        int hostBefore = host.received.size();

        dispatcher.onDisconnect(2);

        assertThat(host.received).hasSizeGreaterThan(hostBefore);
        assertThat(host.last().getMessage().getCType()).isEqualTo(MessageType.PLAYER_LEFT.code());
        assertThat(payloadOf(host.last()).get("left")).isEqualTo("alice");
    }
}
