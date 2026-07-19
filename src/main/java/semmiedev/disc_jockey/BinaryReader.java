package semmiedev.disc_jockey;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class BinaryReader implements AutoCloseable {
    private static final int MAX_STRING_BYTES = 1 << 20;

    private final InputStream in;
    private final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);

    public BinaryReader(InputStream in) {
        this.in = in;
    }

    public int readInt() throws IOException {
        return buffer.clear().put(readBytes(Integer.BYTES)).rewind().getInt();
    }

    public long readUInt() throws IOException {
        return readInt() & 0xFFFFFFFFL;
    }

    public int readUShort() throws IOException {
        return readShort() & 0xFFFF;
    }

    public short readShort() throws IOException {
        return buffer.clear().put(readBytes(Short.BYTES)).rewind().getShort();
    }

    public String readString() throws IOException {
        int length = readInt();
        if (length < 0 || length > MAX_STRING_BYTES) {
            throw new IOException("Invalid NBS string length: " + length);
        }
        return new String(readBytes(length), StandardCharsets.UTF_8);
    }

    public float readFloat() throws IOException {
        return buffer.clear().put(readBytes(Float.BYTES)).rewind().getFloat();
    }

    public byte readByte() throws IOException {
        int b = in.read();
        if (b < 0) throw new EOFException();
        return (byte)(b);
    }

    public int readUnsignedByte() throws IOException {
        return Byte.toUnsignedInt(readByte());
    }

    public byte[] readBytes(int length) throws IOException {
        byte[] bytes = in.readNBytes(length);
        if (bytes.length != length) throw new EOFException();
        return bytes;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
