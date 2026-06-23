package kahoot.protocol;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Arrays;

public final class PacketDecoder {

    public Packet decodePacket(byte[] arr, MessageCipher cipher) throws GeneralSecurityException {
        ByteBuffer buffer = ByteBuffer.wrap(arr);
        byte magic = buffer.get(0);
        byte src = buffer.get(1);
        long pktId = buffer.getLong(2);
        int wLen = buffer.getInt(10);
        short headCrcWire = buffer.getShort(14);

        short headCrcCalc = Crc16.calculateCrc(Arrays.copyOfRange(arr, 0, 14));
        if (headCrcCalc != headCrcWire) {
            throw new IllegalArgumentException(String.format(
                    "Head CRC mismatch: wire=0x%04X, computed=0x%04X",
                    headCrcWire & 0xFFFF, headCrcCalc & 0xFFFF));
        }

        byte[] ciphertext = new byte[wLen];
        buffer.position(Packet.HEADER_LEN);
        buffer.get(ciphertext, 0, wLen);

        short tailCrcWire = buffer.getShort(Packet.HEADER_LEN + wLen);
        short tailCrcCalc = Crc16.calculateCrc(ciphertext);
        if (tailCrcCalc != tailCrcWire) {
            throw new IllegalArgumentException(String.format(
                    "Tail CRC mismatch: wire=0x%04X, computed=0x%04X",
                    tailCrcWire & 0xFFFF, tailCrcCalc & 0xFFFF));
        }

        byte[] plaintext = cipher.decrypt(ciphertext);
        Message msg = decodeMessage(plaintext);

        return new Packet(magic, src, pktId, wLen, msg);
    }

    private Message decodeMessage(byte[] plaintext) {
        ByteBuffer buffer = ByteBuffer.wrap(plaintext);
        int cType = buffer.getInt(0);
        int connId = buffer.getInt(4);
        byte[] payload = new byte[plaintext.length - 8];
        buffer.position(8);
        buffer.get(payload, 0, payload.length);
        return new Message(cType, connId, payload);
    }
}
