package kahoot.transport;

import kahoot.protocol.MessageCipher;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public final class ServerTCP {

    private final int port;
    private final MessageCipher cipher;
    private final Consumer<OutboundSink> onConnect;
    private final ClientConnection.InboundHandler onPacket;
    private final IntConsumer onDisconnect;

    private final AtomicInteger connIds = new AtomicInteger(0);
    private final Set<ClientConnection> connections = ConcurrentHashMap.newKeySet();

    private volatile boolean running;
    private ServerSocket serverSocket;
    private Thread acceptThread;

    public ServerTCP(int port, MessageCipher cipher, Consumer<OutboundSink> onConnect,
                     ClientConnection.InboundHandler onPacket, IntConsumer onDisconnect) {
        this.port = port;
        this.cipher = cipher;
        this.onConnect = onConnect;
        this.onPacket = onPacket;
        this.onDisconnect = onDisconnect;
    }

    public int port() {
        return serverSocket != null ? serverSocket.getLocalPort() : port;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(port));
        running = true;
        acceptThread = new Thread(this::acceptLoop, "tcp-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
        System.out.println("ServerTCP: listening on " + serverSocket.getLocalPort());
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        for (ClientConnection c : connections) {
            c.close();
        }
    }

    public void join() throws InterruptedException {
        if (acceptThread != null) {
            acceptThread.join();
        }
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                int id = connIds.incrementAndGet();
                ClientConnection conn = new ClientConnection(id, socket, cipher, onPacket, this::handleClose);
                connections.add(conn);
                onConnect.accept(conn);
                conn.start();
                System.out.println("ServerTCP: client " + id + " connected " + socket.getRemoteSocketAddress());
            } catch (IOException e) {
                if (running) {
                    System.err.println("ServerTCP: accept failed — " + e.getMessage());
                }
            }
        }
    }

    private void handleClose(int connId) {
        connections.removeIf(c -> c.connId() == connId);
        if (onDisconnect != null) {
            onDisconnect.accept(connId);
        }
        System.out.println("ServerTCP: client " + connId + " disconnected");
    }
}
