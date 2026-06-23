package kahoot.transport;

import kahoot.protocol.MessageCipher;
import kahoot.protocol.Packet;
import kahoot.protocol.PacketDecoder;
import kahoot.protocol.PacketEncoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntConsumer;

public final class ClientConnection implements OutboundSink {

    public interface InboundHandler {
        void onPacket(int connId, Packet packet);
    }

    private static final int OUTBOUND_CAPACITY = 256;

    private static final Packet POISON = new Packet(Packet.MAGIC, (byte) 0, 0L, 0, null);

    private final int connId;
    private final Socket socket;
    private final MessageCipher cipher;
    private final InboundHandler onPacket;
    private final IntConsumer onClose;

    private final PacketEncoder encoder = new PacketEncoder();
    private final PacketDecoder decoder = new PacketDecoder();
    private final BlockingQueue<Packet> outbound = new LinkedBlockingQueue<>(OUTBOUND_CAPACITY);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private InputStream in;
    private OutputStream out;
    private Thread reader;
    private Thread writer;

    public ClientConnection(int connId, Socket socket, MessageCipher cipher,
                            InboundHandler onPacket, IntConsumer onClose) {
        this.connId = connId;
        this.socket = socket;
        this.cipher = cipher;
        this.onPacket = onPacket;
        this.onClose = onClose;
    }

    public void start() throws IOException {
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
        reader = new Thread(this::readLoop, "conn-" + connId + "-reader");
        writer = new Thread(this::writeLoop, "conn-" + connId + "-writer");
        reader.setDaemon(true);
        writer.setDaemon(true);
        reader.start();
        writer.start();
    }

    @Override
    public int connId() {
        return connId;
    }

    @Override
    public boolean isOpen() {
        return !closed.get();
    }

    @Override
    public boolean send(Packet packet) {
        if (closed.get()) {
            return false;
        }
        if (!outbound.offer(packet)) {

            System.err.println("ClientConnection[" + connId + "]: outbound queue full, dropping client");
            close();
            return false;
        }
        return true;
    }

    private void readLoop() {
        try {
            byte[] frame;
            while ((frame = Frames.read(in)) != null) {
                try {
                    Packet pkt = decoder.decodePacket(frame, cipher);
                    onPacket.onPacket(connId, pkt);
                } catch (IllegalArgumentException | GeneralSecurityException e) {

                    System.err.println("ClientConnection[" + connId + "]: bad frame — " + e.getMessage());
                }
            }
        } catch (IOException e) {

        } finally {
            close();
        }
    }

    private void writeLoop() {
        try {
            while (true) {
                Packet pkt = outbound.take();
                if (pkt == POISON) {
                    break;
                }
                Frames.write(out, encoder.encodePacket(pkt, cipher));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException | GeneralSecurityException e) {
            System.err.println("ClientConnection[" + connId + "]: write failed — " + e.getMessage());
        } finally {
            close();
        }
    }

    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        outbound.offer(POISON);
        try {
            socket.close();
        } catch (IOException ignored) {
        }
        if (onClose != null) {
            onClose.accept(connId);
        }
    }
}
