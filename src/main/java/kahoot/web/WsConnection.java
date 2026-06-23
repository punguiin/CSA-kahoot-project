package kahoot.web;

import kahoot.protocol.MessageCipher;
import kahoot.protocol.Packet;
import kahoot.protocol.PacketDecoder;
import kahoot.protocol.PacketEncoder;
import kahoot.transport.ClientConnection.InboundHandler;
import kahoot.transport.OutboundSink;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntConsumer;

public final class WsConnection implements OutboundSink {

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

    public WsConnection(int connId, Socket socket, MessageCipher cipher,
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
        WsCodec.sendHandshake(out, WsCodec.handshakeKey(in));
        Thread reader = new Thread(this::readLoop, "ws-" + connId + "-reader");
        Thread writer = new Thread(this::writeLoop, "ws-" + connId + "-writer");
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
            System.err.println("WsConnection[" + connId + "]: outbound queue full, dropping client");
            close();
            return false;
        }
        return true;
    }

    private void readLoop() {
        ByteArrayOutputStream message = new ByteArrayOutputStream();
        try {
            WsCodec.Frame frame;
            while ((frame = WsCodec.readFrame(in)) != null) {
                switch (frame.opcode()) {
                    case WsCodec.OP_CLOSE -> {
                        return;
                    }
                    case WsCodec.OP_PING -> WsCodec.writeFrame(out, WsCodec.OP_PONG, frame.payload());
                    case WsCodec.OP_PONG -> {
                    }
                    default -> {
                        message.write(frame.payload());
                        if (frame.fin()) {
                            byte[] packetBytes = message.toByteArray();
                            message.reset();
                            dispatch(packetBytes);
                        }
                    }
                }
            }
        } catch (IOException e) {

        } finally {
            close();
        }
    }

    private void dispatch(byte[] packetBytes) {
        try {
            onPacket.onPacket(connId, decoder.decodePacket(packetBytes, cipher));
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            System.err.println("WsConnection[" + connId + "]: bad packet — " + e.getMessage());
        }
    }

    private void writeLoop() {
        try {
            while (true) {
                Packet pkt = outbound.take();
                if (pkt == POISON) {
                    break;
                }
                WsCodec.writeFrame(out, WsCodec.OP_BINARY, encoder.encodePacket(pkt, cipher));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException | GeneralSecurityException e) {
            System.err.println("WsConnection[" + connId + "]: write failed — " + e.getMessage());
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
