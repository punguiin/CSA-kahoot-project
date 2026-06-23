package kahoot.protocol;

import kahoot.game.GameAction;
import kahoot.game.GameActionType;
import kahoot.model.Answer;
import kahoot.model.Question;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProtocolTest {

    private final PacketEncoder encoder = new PacketEncoder();
    private final PacketDecoder decoder = new PacketDecoder();
    private final MessageCipher cipher = new IdentityCipher();

    private Packet encodeThenDecode(Packet pkt) throws Exception {
        return decoder.decodePacket(encoder.encodePacket(pkt, cipher), cipher);
    }

    @Test
    void shouldRoundTripPacketThroughEncodeDecode() throws Exception {
        byte[] payload = "{\"pin\":\"012345\",\"nickname\":\"alice\"}".getBytes(StandardCharsets.UTF_8);
        Packet original = new Packet(Packet.MAGIC, (byte) 7, 42L,
                0, new Message(MessageType.REQ_JOIN_ROOM.code(), 99, payload));

        Packet decoded = encodeThenDecode(original);

        assertThat(decoded.getSrc()).isEqualTo((byte) 7);
        assertThat(decoded.getPktId()).isEqualTo(42L);
        assertThat(decoded.getMessage().getCType()).isEqualTo(MessageType.REQ_JOIN_ROOM.code());
        assertThat(decoded.getMessage().getConnId()).isEqualTo(99);
        assertThat(decoded.getMessage().getPayload()).isEqualTo(payload);
    }

    @Test
    void shouldRoundTripEveryMessageType() throws Exception {
        for (MessageType type : MessageType.values()) {
            Packet pkt = new Packet(Packet.MAGIC, (byte) 1, 1L,
                    0, new Message(type.code(), 0, "{}".getBytes(StandardCharsets.UTF_8)));
            Packet decoded = encodeThenDecode(pkt);
            assertThat(MessageType.fromCode(decoded.getMessage().getCType())).isEqualTo(type);
        }
    }

    @Test
    void shouldRejectTamperedHeader() throws Exception {
        Packet pkt = new Packet(Packet.MAGIC, (byte) 1, 1L,
                0, new Message(MessageType.REQ_START_QUIZ.code(), 0, "{}".getBytes()));
        byte[] frame = encoder.encodePacket(pkt, cipher);

        frame[5] ^= 0xFF;

        assertThatThrownBy(() -> decoder.decodePacket(frame, cipher))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Head CRC mismatch");
    }

    @Test
    void shouldRejectTamperedPayload() throws Exception {
        byte[] payload = "{\"answerId\":3}".getBytes(StandardCharsets.UTF_8);
        Packet pkt = new Packet(Packet.MAGIC, (byte) 1, 1L,
                0, new Message(MessageType.REQ_SUBMIT_ANSWER.code(), 0, payload));
        byte[] frame = encoder.encodePacket(pkt, cipher);

        frame[Packet.HEADER_LEN + 1] ^= 0xFF;

        assertThatThrownBy(() -> decoder.decodePacket(frame, cipher))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tail CRC mismatch");
    }

    @Test
    void jsonShouldRoundTripNestedShapes() {
        String json = Json.Writer.object()
                .str("pin", "012345")
                .num("index", 2)
                .bool("done", false)
                .raw("answers", Json.array(List.of(
                        Json.Writer.object().num("id", 1).str("text", "Pa\"r\\is").end(),
                        Json.Writer.object().num("id", 2).str("text", "Rome").end())))
                .end();

        Map<String, Object> parsed = Json.parseObject(json);

        assertThat(parsed.get("pin")).isEqualTo("012345");
        assertThat(parsed.get("index")).isEqualTo(2L);
        assertThat(parsed.get("done")).isEqualTo(false);
        @SuppressWarnings("unchecked")
        List<Object> answers = (List<Object>) parsed.get("answers");
        assertThat(answers).hasSize(2);
        @SuppressWarnings("unchecked")
        Map<String, Object> first = (Map<String, Object>) answers.get(0);
        assertThat(first.get("text")).isEqualTo("Pa\"r\\is");
    }

    @Test
    void jsonShouldRejectMalformedInput() {
        assertThatThrownBy(() -> Json.parse("{\"pin\": }"))
                .isInstanceOf(Json.JsonException.class);
    }

    @Test
    void questionPayloadMustNeverLeakIsCorrect() {
        Question q = new Question(5, 1, "Capital of France?", 20, List.of(
                new Answer(42, 5, "Paris", true),
                new Answer(43, 5, "Rome", false),
                new Answer(44, 5, "Berlin", false)));

        String json = PayloadCodec.question("012345", 0, q);

        assertThat(json).doesNotContain("isCorrect").doesNotContain("correct").doesNotContain("true");
        assertThat(json).contains("Paris").contains("\"id\":42");

        Map<String, Object> parsed = Json.parseObject(json);
        @SuppressWarnings("unchecked")
        List<Object> answers = (List<Object>) parsed.get("answers");
        assertThat(answers).hasSize(3);
        @SuppressWarnings("unchecked")
        Map<String, Object> paris = (Map<String, Object>) answers.get(0);
        assertThat(paris).containsOnlyKeys("id", "text");
    }

    @Test
    void toActionShouldBuildJoinFromWire() {
        GameAction action = PayloadCodec.toAction(MessageType.REQ_JOIN_ROOM,
                Map.of("pin", "012345", "nickname", "alice"), null, null);

        assertThat(action.getType()).isEqualTo(GameActionType.JOIN_ROOM);
        assertThat(action.getPin()).isEqualTo("012345");
        assertThat(action.getNickname()).isEqualTo("alice");
    }

    @Test
    void toActionShouldUseBoundIdentityForSubmit() {
        GameAction action = PayloadCodec.toAction(MessageType.REQ_SUBMIT_ANSWER,
                Map.of("answerId", 7L), "012345", "alice");

        assertThat(action.getType()).isEqualTo(GameActionType.SUBMIT_ANSWER);
        assertThat(action.getPin()).isEqualTo("012345");
        assertThat(action.getNickname()).isEqualTo("alice");
        assertThat(action.getAnswerId()).isEqualTo(7);
    }
}
