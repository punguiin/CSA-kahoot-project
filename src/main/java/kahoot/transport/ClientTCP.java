package kahoot.transport;

import kahoot.protocol.Message;
import kahoot.protocol.MessageCipher;
import kahoot.protocol.MessageType;
import kahoot.protocol.Packet;
import kahoot.protocol.PacketDecoder;
import kahoot.protocol.PacketEncoder;
import kahoot.protocol.PayloadCodec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.function.Consumer;

public final class ClientTCP {

    private final String host;
    private final int port;
    private final byte src;
    private final MessageCipher cipher;
    private final Consumer<Packet> onPacket;

    private final PacketEncoder encoder = new PacketEncoder();
    private final PacketDecoder decoder = new PacketDecoder();

    private volatile boolean running;
    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private Thread reader;
    private long pktId = 0;

    public ClientTCP(String host, int port, byte src, MessageCipher cipher, Consumer<Packet> onPacket) {
        this.host = host;
        this.port = port;
        this.src = src;
        this.cipher = cipher;
        this.onPacket = onPacket;
    }

    public void connect() throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 2000);
        in = socket.getInputStream();
        out = socket.getOutputStream();
        running = true;
        reader = new Thread(this::readLoop, "client-" + src + "-reader");
        reader.setDaemon(true);
        reader.start();
    }

    public void close() {
        running = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    public synchronized void send(MessageType type, String json) {
        try {
            Message msg = new Message(type.code(), 0, PayloadCodec.bytes(json));
            Packet pkt = new Packet(Packet.MAGIC, src, ++pktId, 0, msg);
            Frames.write(out, encoder.encodePacket(pkt, cipher));
        } catch (IOException | GeneralSecurityException e) {
            System.err.println("ClientTCP[" + src + "]: send failed — " + e.getMessage());
        }
    }

    private void readLoop() {
        try {
            byte[] frame;
            while (running && (frame = Frames.read(in)) != null) {
                try {
                    onPacket.accept(decoder.decodePacket(frame, cipher));
                } catch (IllegalArgumentException | GeneralSecurityException e) {
                    System.err.println("ClientTCP[" + src + "]: bad frame — " + e.getMessage());
                }
            }
        } catch (IOException e) {

        }
    }
}
