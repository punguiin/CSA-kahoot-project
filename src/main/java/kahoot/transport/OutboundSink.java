package kahoot.transport;

import kahoot.protocol.Packet;

public interface OutboundSink {

    int connId();

    boolean isOpen();

    boolean send(Packet packet);
}
