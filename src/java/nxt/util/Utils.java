package nxt.util;

public class Utils {

    public static byte setBit(byte v, byte pos) {
        return (byte) (v | (1 << pos));
    }

    public static byte unsetBit(byte v, byte pos) {
        return (byte) (v & ~(1 << pos));
    }

    public static int getBit(int v, int position) {
        return (v >> position) & 1;
    }

}
