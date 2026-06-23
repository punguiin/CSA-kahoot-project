package kahoot.protocol;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Arrays;

public final class PacketEncoder {

    public byte[] encodePacket(Packet pkt, MessageCipher cipher) throws GeneralSecurityException {
        byte[] plaintext = encodeMessage(pkt.getMessage());
        byte[] ciphertext = cipher.encrypt(plaintext);
        int wLen = ciphertext.length;

        ByteBuffer buffer = ByteBuffer.allocate(Packet.HEADER_LEN + wLen + Packet.TAIL_CRC_LEN);
        buffer.put(pkt.getMagic());
        buffer.put(pkt.getSrc());
        buffer.putLong(pkt.getPktId());
        buffer.putInt(wLen);

        short headCrc = Crc16.calculateCrc(Arrays.copyOfRange(buffer.array(), 0, 14));
        buffer.putShort(headCrc);

        buffer.put(ciphertext);

        short tailCrc = Crc16.calculateCrc(ciphertext);
        buffer.putShort(tailCrc);

        return buffer.array();
    }

    private byte[] encodeMessage(Message msg) {
        byte[] payload = msg.getPayload();
        ByteBuffer buffer = ByteBuffer.allocate(8 + payload.length);
        buffer.putInt(msg.getCType());
        buffer.putInt(msg.getConnId());
        buffer.put(payload);
        return buffer.array();
    }
}
