package bftsmart.demo.incident;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class CanonicalEncoder {

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    public void writeByte(int value) {
        output.write(value & 0xff);
    }

    public void writeInt(int value) {
        output.write((value >>> 24) & 0xff);
        output.write((value >>> 16) & 0xff);
        output.write((value >>> 8) & 0xff);
        output.write(value & 0xff);
    }

    public void writeLong(long value) {
        output.write((int) ((value >>> 56) & 0xff));
        output.write((int) ((value >>> 48) & 0xff));
        output.write((int) ((value >>> 40) & 0xff));
        output.write((int) ((value >>> 32) & 0xff));
        output.write((int) ((value >>> 24) & 0xff));
        output.write((int) ((value >>> 16) & 0xff));
        output.write((int) ((value >>> 8) & 0xff));
        output.write((int) (value & 0xff));
    }

    public void writeString(String value) {
        byte[] bytes = value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
        writeInt(bytes.length);
        writeRawBytes(bytes);
    }

    public void writeByteArray(byte[] value) {
        byte[] bytes = value == null ? new byte[0] : value;
        writeInt(bytes.length);
        writeRawBytes(bytes);
    }

    public void writeRawBytes(byte[] value) {
        byte[] bytes = value == null ? new byte[0] : value;
        output.write(bytes, 0, bytes.length);
    }

    public void writeObject(CanonicalWritable writable) {
        if (writable == null) {
            throw new IllegalArgumentException("writable must not be null");
        }
        writable.encode(this);
    }

    public <T extends CanonicalWritable> void writeList(List<T> values) {
        if (values == null) {
            writeInt(0);
            return;
        }
        writeInt(values.size());
        for (T value : values) {
            writeObject(value);
        }
    }

    public byte[] toByteArray() {
        return output.toByteArray();
    }

    public static byte[] encode(CanonicalWritable writable) {
        CanonicalEncoder encoder = new CanonicalEncoder();
        writable.encode(encoder);
        return encoder.toByteArray();
    }
}
