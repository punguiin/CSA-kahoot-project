package kahoot.transport;

import kahoot.protocol.Packet;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class Frames {

    private static final int MAX_W_LEN = 1 << 20;

    private Frames() {
    }

    public static void write(OutputStream out, byte[] frame) throws IOException {
        out.write(frame);
        out.flush();
    }

    public static byte[] read(InputStream in) throws IOException {
        int first = in.read();
        if (first < 0) {
            return null;
        }

        byte[] header = new byte[Packet.HEADER_LEN];
        header[0] = (byte) first;
        readFully(in, header, 1, Packet.HEADER_LEN - 1);

        int wLen = ((header[Packet.W_LEN_OFFSET] & 0xFF) << 24)
                | ((header[Packet.W_LEN_OFFSET + 1] & 0xFF) << 16)
                | ((header[Packet.W_LEN_OFFSET + 2] & 0xFF) << 8)
                | (header[Packet.W_LEN_OFFSET + 3] & 0xFF);
        if (wLen < 0 || wLen > MAX_W_LEN) {
            throw new IOException("Bad frame length: " + wLen);
        }

        byte[] frame = new byte[Packet.HEADER_LEN + wLen + Packet.TAIL_CRC_LEN];
        System.arraycopy(header, 0, frame, 0, Packet.HEADER_LEN);
        readFully(in, frame, Packet.HEADER_LEN, wLen + Packet.TAIL_CRC_LEN);
        return frame;
    }

    private static void readFully(InputStream in, byte[] buf, int off, int len) throws IOException {
        int read = 0;
        while (read < len) {
            int r = in.read(buf, off + read, len - read);
            if (r < 0) {
                throw new EOFException("Stream ended mid-frame");
            }
            read += r;
        }
    }
}
