package kahoot.web;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class WsCodec {

    public static final int OP_CONTINUATION = 0x0;
    public static final int OP_TEXT = 0x1;
    public static final int OP_BINARY = 0x2;
    public static final int OP_CLOSE = 0x8;
    public static final int OP_PING = 0x9;
    public static final int OP_PONG = 0xA;

    private static final String WS_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final long MAX_PAYLOAD = 1 << 20;

    private WsCodec() {
    }

    public record Frame(boolean fin, int opcode, byte[] payload) {
    }

    public static String handshakeKey(InputStream in) throws IOException {
        StringBuilder request = new StringBuilder();
        int prev = -1, cur;
        while ((cur = in.read()) != -1) {
            request.append((char) cur);
            if (prev == '\r' && cur == '\n' && request.length() >= 4
                    && request.substring(request.length() - 4).equals("\r\n\r\n")) {
                break;
            }
            prev = cur;
        }
        for (String line : request.toString().split("\r\n")) {
            int colon = line.indexOf(':');
            if (colon > 0 && line.substring(0, colon).trim().equalsIgnoreCase("Sec-WebSocket-Key")) {
                return line.substring(colon + 1).trim();
            }
        }
        return null;
    }

    public static void sendHandshake(OutputStream out, String key) throws IOException {
        String accept;
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] digest = sha1.digest((key + WS_GUID).getBytes(StandardCharsets.UTF_8));
            accept = Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-1 unavailable", e);
        }
        String response = "HTTP/1.1 101 Switching Protocols\r\n"
                + "Upgrade: websocket\r\n"
                + "Connection: Upgrade\r\n"
                + "Sec-WebSocket-Accept: " + accept + "\r\n\r\n";
        out.write(response.getBytes(StandardCharsets.US_ASCII));
        out.flush();
    }

    public static Frame readFrame(InputStream in) throws IOException {
        int b0 = in.read();
        if (b0 < 0) {
            return null;
        }
        boolean fin = (b0 & 0x80) != 0;
        int opcode = b0 & 0x0F;

        int b1 = readByte(in);
        boolean masked = (b1 & 0x80) != 0;
        long len = b1 & 0x7F;
        if (len == 126) {
            len = (readByte(in) << 8) | readByte(in);
        } else if (len == 127) {
            len = 0;
            for (int i = 0; i < 8; i++) {
                len = (len << 8) | readByte(in);
            }
        }
        if (len < 0 || len > MAX_PAYLOAD) {
            throw new IOException("WS frame too large: " + len);
        }

        byte[] mask = new byte[4];
        if (masked) {
            readFully(in, mask, 0, 4);
        }
        byte[] payload = new byte[(int) len];
        readFully(in, payload, 0, payload.length);
        if (masked) {
            for (int i = 0; i < payload.length; i++) {
                payload[i] ^= mask[i & 3];
            }
        }
        return new Frame(fin, opcode, payload);
    }

    public static void writeFrame(OutputStream out, int opcode, byte[] payload) throws IOException {
        ByteArrayOutputStream frame = new ByteArrayOutputStream();
        frame.write(0x80 | opcode);
        int len = payload.length;
        if (len < 126) {
            frame.write(len);
        } else if (len <= 0xFFFF) {
            frame.write(126);
            frame.write((len >> 8) & 0xFF);
            frame.write(len & 0xFF);
        } else {
            frame.write(127);
            for (int i = 7; i >= 0; i--) {
                frame.write((int) (((long) len >> (8 * i)) & 0xFF));
            }
        }
        frame.write(payload, 0, payload.length);
        synchronized (out) {
            out.write(frame.toByteArray());
            out.flush();
        }
    }

    private static int readByte(InputStream in) throws IOException {
        int b = in.read();
        if (b < 0) {
            throw new EOFException("WS stream ended");
        }
        return b;
    }

    private static void readFully(InputStream in, byte[] buf, int off, int len) throws IOException {
        int read = 0;
        while (read < len) {
            int r = in.read(buf, off + read, len - read);
            if (r < 0) {
                throw new EOFException("WS stream ended mid-frame");
            }
            read += r;
        }
    }
}
