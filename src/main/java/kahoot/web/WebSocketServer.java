package kahoot.web;

import kahoot.protocol.MessageCipher;
import kahoot.transport.ClientConnection.InboundHandler;
import kahoot.transport.OutboundSink;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public final class WebSocketServer {

    private final int port;
    private final MessageCipher cipher;
    private final Consumer<OutboundSink> onConnect;
    private final InboundHandler onPacket;
    private final IntConsumer onDisconnect;

    private final AtomicInteger connIds = new AtomicInteger(0);
    private final Set<WsConnection> connections = ConcurrentHashMap.newKeySet();

    private volatile boolean running;
    private ServerSocket serverSocket;
    private Thread acceptThread;

    public WebSocketServer(int port, MessageCipher cipher, Consumer<OutboundSink> onConnect,
                           InboundHandler onPacket, IntConsumer onDisconnect) {
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
        acceptThread = new Thread(this::acceptLoop, "ws-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
        System.out.println("WebSocketServer: listening on " + serverSocket.getLocalPort());
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        for (WsConnection c : connections) {
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
                WsConnection conn = new WsConnection(id, socket, cipher, onPacket, this::handleClose);
                connections.add(conn);
                onConnect.accept(conn);
                conn.start();
                System.out.println("WebSocketServer: client " + id + " connected " + socket.getRemoteSocketAddress());
            } catch (IOException e) {
                if (running) {
                    System.err.println("WebSocketServer: accept failed — " + e.getMessage());
                }
            }
        }
    }

    private void handleClose(int connId) {
        connections.removeIf(c -> c.connId() == connId);
        if (onDisconnect != null) {
            onDisconnect.accept(connId);
        }
        System.out.println("WebSocketServer: client " + connId + " disconnected");
    }
}
