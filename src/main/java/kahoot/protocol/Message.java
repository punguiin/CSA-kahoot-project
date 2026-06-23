package kahoot.protocol;

public final class Message {

    private final int cType;
    private final int connId;
    private final byte[] payload;

    public Message(int cType, int connId, byte[] payload) {
        this.cType = cType;
        this.connId = connId;
        this.payload = payload;
    }

    public int getCType() {
        return cType;
    }

    public int getConnId() {
        return connId;
    }

    public byte[] getPayload() {
        return payload;
    }
}
