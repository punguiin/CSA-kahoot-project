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
import kahoot.protocol.IdentityCipher;
import kahoot.protocol.MessageCipher;
import kahoot.protocol.MessageType;
import kahoot.protocol.Packet;
import kahoot.protocol.PayloadCodec;
import kahoot.transport.ClientTCP;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class KahootServerE2ETest {

    private static final class Recorder {
        final CopyOnWriteArrayList<Packet> received = new CopyOnWriteArrayList<>();

        void accept(Packet pkt) {
            received.add(pkt);
        }

        Packet await(MessageType type) {
            long deadline = System.currentTimeMillis() + 3000;
            while (System.currentTimeMillis() < deadline) {
                for (Packet p : received) {
                    if (p.getMessage().getCType() == type.code()) {
                        return p;
                    }
                }
                sleep(20);
            }
            fail("Timed out waiting for " + type);
            return null;
        }
    }

    private final MessageCipher cipher = new IdentityCipher();
    private KahootServer server;
    private QuizDAO quizDAO;
    private int quizId;
    private int correctAnswerId;
    private final List<ClientTCP> clients = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        var connection = new DatabaseConnection(":memory:").getConnection();
        quizDAO = new QuizDAOImpl(connection);
        GameStateManager gsm = new GameStateManager();
        GameService gameService = new GameService(gsm, quizDAO, new GameHistoryDAOImpl(connection));
        ConnectionRegistry registry = new ConnectionRegistry();
        SessionDispatcher dispatcher = new SessionDispatcher(gameService, gsm, registry);

        quizId = quizDAO.insert(buildQuiz());
        correctAnswerId = quizDAO.findById(quizId).get().getQuestions().get(0).getAnswers().stream()
                .filter(Answer::isCorrect).findFirst().get().getId();

        server = new KahootServer(0, cipher, registry, dispatcher);
        server.start();
    }

    @AfterEach
    void tearDown() {
        clients.forEach(ClientTCP::close);
        server.stop();
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

    private ClientTCP connect(byte src, Recorder recorder) throws Exception {
        ClientTCP client = new ClientTCP("localhost", server.port(), src, cipher, recorder::accept);
        client.connect();
        clients.add(client);
        return client;
    }

    private static Map<String, Object> payloadOf(Packet pkt) {
        return PayloadCodec.read(pkt.getMessage().getPayload());
    }

    @Test
    void playersReceivePushedEventsAcrossAFullGame() throws Exception {
        Recorder hostR = new Recorder();
        Recorder aliceR = new Recorder();
        Recorder bobR = new Recorder();

        ClientTCP host = connect((byte) 1, hostR);
        host.send(MessageType.REQ_CREATE_ROOM, "{\"quizId\":" + quizId + "}");
        String pin = (String) payloadOf(hostR.await(MessageType.ROOM_CREATED)).get("pin");
        assertThat(pin).hasSize(6);

        ClientTCP alice = connect((byte) 2, aliceR);
        ClientTCP bob = connect((byte) 3, bobR);
        alice.send(MessageType.REQ_JOIN_ROOM, "{\"pin\":\"" + pin + "\",\"nickname\":\"alice\"}");
        aliceR.await(MessageType.JOIN_ACCEPTED);
        bob.send(MessageType.REQ_JOIN_ROOM, "{\"pin\":\"" + pin + "\",\"nickname\":\"bob\"}");
        bobR.await(MessageType.JOIN_ACCEPTED);

        host.send(MessageType.REQ_START_QUIZ, "{}");
        Packet question = aliceR.await(MessageType.QUESTION);
        assertThat(new String(question.getMessage().getPayload())).doesNotContain("isCorrect");
        bobR.await(MessageType.QUESTION);

        alice.send(MessageType.REQ_SUBMIT_ANSWER, "{\"answerId\":" + correctAnswerId + "}");
        assertThat(payloadOf(aliceR.await(MessageType.ANSWER_RESULT)).get("correct")).isEqualTo(true);

        host.send(MessageType.REQ_END_ROUND, "{}");
        aliceR.await(MessageType.LEADERBOARD);
        bobR.await(MessageType.LEADERBOARD);

        host.send(MessageType.REQ_NEXT_QUESTION, "{}");
        aliceR.await(MessageType.GAME_FINISHED);
        bobR.await(MessageType.GAME_FINISHED);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
