package bftsmart.demo.incident;

public final class HexUtils {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private HexUtils() {
    }

    public static String toHex(byte[] value) {
        if (value == null) {
            return "";
        }
        char[] chars = new char[value.length * 2];
        for (int i = 0; i < value.length; i++) {
            int current = value[i] & 0xff;
            chars[i * 2] = HEX[current >>> 4];
            chars[i * 2 + 1] = HEX[current & 0x0f];
        }
        return new String(chars);
    }
}
