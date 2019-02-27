package neo;

import neo.log.tr.TR;

/**
 * Byte 相关的辅助操作方法
 */
public class ByteHelper {

    public static String toHexString(byte[] value) {
        TR.enter();
        StringBuilder sb = new StringBuilder();
        for (byte b : value) {
            int v = Byte.toUnsignedInt(b);
            sb.append(Integer.toHexString(v >>> 4));
            sb.append(Integer.toHexString(v & 0x0f));
        }
        return TR.exit(sb.toString());
    }

    public static byte[] reverse(byte[] v) {
        TR.enter();
        byte[] result = new byte[v.length];
        for (int i = 0; i < v.length; i++) {
            result[i] = v[v.length - i - 1];
        }
        return TR.exit(result);
    }

    public static byte[] hexToBytes(String value) {
        TR.enter();

        if (value == null || value.length() == 0) {
            return new byte[0];
        }

        if (value.startsWith("0x")) {
            value = value.substring(2);
        }

        if (value.length() % 2 == 1) {
            throw new IllegalArgumentException();
        }
        byte[] result = new byte[value.length() / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16);
        }
        return TR.exit(result);
    }

    public static byte[] addBytes(byte[] data1, byte[] data2) {
        TR.enter();

        byte[] data3 = new byte[data1.length + data2.length];
        System.arraycopy(data1, 0, data3, 0, data1.length);
        System.arraycopy(data2, 0, data3, data1.length, data2.length);
        return TR.exit(data3);
    }

}

