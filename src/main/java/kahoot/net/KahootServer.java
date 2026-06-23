package kahoot.net;

import kahoot.protocol.MessageCipher;
import kahoot.transport.ServerTCP;

import java.io.IOException;

public final class KahootServer {

    private final ServerTCP server;

    public KahootServer(int port, MessageCipher cipher, ConnectionRegistry registry,
                        SessionDispatcher dispatcher) {
        this.server = new ServerTCP(
                port,
                cipher,
                registry::register,
                dispatcher::onPacket,
                dispatcher::onDisconnect);
    }

    public void start() throws IOException {
        server.start();
    }

    public void stop() {
        server.stop();
    }

    public void join() throws InterruptedException {
        server.join();
    }

    public int port() {
        return server.port();
    }
}
