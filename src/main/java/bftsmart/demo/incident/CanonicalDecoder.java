package bftsmart.demo.incident;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class CanonicalDecoder {

    private final byte[] input;
    private int offset;

    public CanonicalDecoder(byte[] input) {
        this.input = input == null ? new byte[0] : input;
        this.offset = 0;
    }

    public int readByte() {
        requireAvailable(1);
        return input[offset++] & 0xff;
    }

    public int readInt() {
        requireAvailable(4);
        int value = ((input[offset] & 0xff) << 24)
                | ((input[offset + 1] & 0xff) << 16)
                | ((input[offset + 2] & 0xff) << 8)
                | (input[offset + 3] & 0xff);
        offset += 4;
        return value;
    }

    public long readLong() {
        requireAvailable(8);
        long value = 0L;
        for (int i = 0; i < 8; i++) {
            value = (value << 8) | (input[offset + i] & 0xffL);
        }
        offset += 8;
        return value;
    }

    public String readString() {
        int length = readInt();
        requireAvailable(length);
        String value = new String(input, offset, length, StandardCharsets.UTF_8);
        offset += length;
        return value;
    }

    public byte[] readByteArray() {
        int length = readInt();
        requireAvailable(length);
        byte[] value = Arrays.copyOfRange(input, offset, offset + length);
        offset += length;
        return value;
    }

    public void requireFullyRead() {
        if (offset != input.length) {
            throw new IllegalArgumentException("Unexpected trailing bytes: " + (input.length - offset));
        }
    }

    private void requireAvailable(int length) {
        if (length < 0 || offset + length > input.length) {
            throw new IllegalArgumentException("Invalid canonical input");
        }
    }
}
