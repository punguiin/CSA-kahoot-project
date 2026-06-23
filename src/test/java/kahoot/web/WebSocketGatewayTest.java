package kahoot.web;

import kahoot.db.DatabaseConnection;
import kahoot.db.GameHistoryDAOImpl;
import kahoot.db.QuizDAO;
import kahoot.db.QuizDAOImpl;
import kahoot.game.GameService;
import kahoot.game.GameStateManager;
import kahoot.model.Answer;
import kahoot.model.Question;
import kahoot.model.Quiz;
import kahoot.net.ConnectionRegistry;
import kahoot.net.SessionDispatcher;
import kahoot.protocol.IdentityCipher;
import kahoot.protocol.Message;
import kahoot.protocol.MessageCipher;
import kahoot.protocol.MessageType;
import kahoot.protocol.Packet;
import kahoot.protocol.PacketDecoder;
import kahoot.protocol.PacketEncoder;
import kahoot.protocol.PayloadCodec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketGatewayTest {

    private final MessageCipher cipher = new IdentityCipher();
    private final PacketEncoder encoder = new PacketEncoder();
    private final PacketDecoder decoder = new PacketDecoder();

    private WebSocketServer server;
    private QuizDAO quizDAO;
    private int quizId;

    private final class Client {
        final WebSocket ws;
        final BlockingQueue<Packet> received = new LinkedBlockingQueue<>();
        private long pktId = 0;

        Client(byte src) {
            this.ws = HttpClient.newHttpClient().newWebSocketBuilder()
                    .buildAsync(URI.create("ws://localhost:" + server.port()), new Listener(received))
                    .join();
            this.src = src;
        }

        private final byte src;

        void send(MessageType type, String json) {
            Message msg = new Message(type.code(), 0, PayloadCodec.bytes(json));
            try {
                byte[] bytes = encoder.encodePacket(new Packet(Packet.MAGIC, src, ++pktId, 0, msg), cipher);
                ws.sendBinary(ByteBuffer.wrap(bytes), true).join();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        Packet await(MessageType type) throws InterruptedException {
            long deadline = System.currentTimeMillis() + 3000;
            while (System.currentTimeMillis() < deadline) {
                Packet p = received.poll(deadline - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                if (p != null && p.getMessage().getCType() == type.code()) {
                    return p;
                }
            }
            throw new AssertionError("Timed out waiting for " + type);
        }
    }

    private final class Listener implements WebSocket.Listener {
        private final BlockingQueue<Packet> out;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        Listener(BlockingQueue<Packet> out) {
            this.out = out;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(Long.MAX_VALUE);
        }

        @Override
        public java.util.concurrent.CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            byte[] chunk = new byte[data.remaining()];
            data.get(chunk);
            buffer.writeBytes(chunk);
            if (last) {
                try {
                    out.add(decoder.decodePacket(buffer.toByteArray(), cipher));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                buffer.reset();
            }
            return null;
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        var connection = new DatabaseConnection(":memory:").getConnection();
        quizDAO = new QuizDAOImpl(connection);
        GameStateManager gsm = new GameStateManager();
        GameService gameService = new GameService(gsm, quizDAO, new GameHistoryDAOImpl(connection));
        ConnectionRegistry registry = new ConnectionRegistry();
        SessionDispatcher dispatcher = new SessionDispatcher(gameService, gsm, registry);

        quizId = quizDAO.insert(buildQuiz());

        server = new WebSocketServer(0, cipher, registry::register,
                dispatcher::onPacket, dispatcher::onDisconnect);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    private Quiz buildQuiz() {
        Question question = new Question(0, "Capital of France?", 10);
        question.setAnswers(List.of(
                new Answer(0, "Paris", true),
                new Answer(0, "Berlin", false)));
        Quiz quiz = new Quiz("Geography", "Quick geography quiz", 1);
        quiz.setQuestions(List.of(question));
        return quiz;
    }

    private static Map<String, Object> payloadOf(Packet pkt) {
        return PayloadCodec.read(pkt.getMessage().getPayload());
    }

    @Test
    void browserClientsPlayOverWebSocket() throws Exception {
        Client host = new Client((byte) 1);
        host.send(MessageType.REQ_CREATE_ROOM, "{\"quizId\":" + quizId + "}");
        String pin = (String) payloadOf(host.await(MessageType.ROOM_CREATED)).get("pin");
        assertThat(pin).hasSize(6);

        Client player = new Client((byte) 2);
        player.send(MessageType.REQ_JOIN_ROOM, "{\"pin\":\"" + pin + "\",\"nickname\":\"alice\"}");
        player.await(MessageType.JOIN_ACCEPTED);

        Packet joined = host.await(MessageType.PLAYER_JOINED);
        assertThat(payloadOf(joined).get("joined")).isEqualTo("alice");

        host.send(MessageType.REQ_START_QUIZ, "{}");
        Packet question = player.await(MessageType.QUESTION);
        assertThat(new String(question.getMessage().getPayload())).doesNotContain("isCorrect");
    }
}
