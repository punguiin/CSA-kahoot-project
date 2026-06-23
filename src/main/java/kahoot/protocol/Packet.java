package kahoot.protocol;

public final class Packet {

    public static final byte MAGIC = 0x13;

    public static final int HEADER_LEN = 16;

    public static final int W_LEN_OFFSET = 10;

    public static final int TAIL_CRC_LEN = 2;

    private final byte magic = MAGIC;
    private final byte src;
    private final long pktId;
    private final int wLen;
    private final Message message;

    public Packet(byte magic, byte src, long pktId, int wLen, Message message) {
        if (magic != this.magic) {
            throw new IllegalArgumentException("Bad start byte: expected 0x13, got " + magic);
        }
        this.src = src;
        this.pktId = pktId;
        this.wLen = wLen;
        this.message = message;
    }

    public byte getMagic() {
        return magic;
    }

    public byte getSrc() {
        return src;
    }

    public long getPktId() {
        return pktId;
    }

    public int getWLen() {
        return wLen;
    }

    public Message getMessage() {
        return message;
    }
}
