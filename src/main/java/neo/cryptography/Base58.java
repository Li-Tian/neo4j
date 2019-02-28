package neo.cryptography;

import java.math.BigInteger;
import java.util.Arrays;

import neo.csharp.BitConverter;
import neo.log.notr.TR;

public class Base58 {
    public static final String ALPHABET =
            "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final BigInteger BASE = BigInteger.valueOf(ALPHABET.length());

    /**
     * @param input 要解码的字符串
     * @return 返回解码后的字节数组
     * @Author:doubi.liu
     * @description:对字符串做base58解码
     * @date:2018/10/11
     */
    public static byte[] decode(String input) {
        TR.enter();
        BigInteger bi = BigInteger.ZERO;
        for (int i = 0; i < input.length(); i++) {
            int index = ALPHABET.indexOf(input.charAt(i));
            if (index == -1) {
                TR.exit();
                throw new IllegalArgumentException();
            }
            bi = bi.multiply(BASE).add(BigInteger.valueOf(index));
        }
        byte[] bytes = bi.toByteArray();
        boolean stripSignByte = bytes.length > 1 && bytes[0] == 0 && bytes[1] < 0;
        int leadingZeros = 0;
        for (; leadingZeros < input.length()
                && input.charAt(leadingZeros) == ALPHABET.charAt(0); leadingZeros++) {
            // just to count leading zeros.
        }
        byte[] tmp = new byte[bytes.length - (stripSignByte ? 1 : 0) + leadingZeros];
        System.arraycopy(bytes, stripSignByte ? 1 : 0, tmp, leadingZeros,
                tmp.length - leadingZeros);
        TR.exit();
        return tmp;
    }

    /**
     * @param input 要编码的字节数组
     * @return 返回编码后的字符串
     * @Author:doubi.liu
     * @description:对数据做base58编码
     * @date:2018/10/11
     */
    public static String encode(byte[] input) {
        TR.enter();
        BigInteger value = new BigInteger(1, input);
        StringBuilder sb = new StringBuilder();
        while (value.compareTo(BASE) >= 0) {
            BigInteger[] r = value.divideAndRemainder(BASE);
            sb.insert(0, ALPHABET.charAt(r[1].intValue()));
            value = r[0];
        }
        sb.insert(0, ALPHABET.charAt(value.intValue()));
        for (byte b : input) {
            if (b == 0) {
                sb.insert(0, ALPHABET.charAt(0));
            } else {
                break;
            }
        }
        return TR.exit(sb.toString());
    }

    /**
     * base58 带 sha256 校验码的编码
     */
    public static String encodeWithSha256Check(byte[] data) {
        try {
            byte[] checksum = Crypto.Default.sha256(Crypto.Default.sha256(data));
            byte[] buffer = new byte[data.length + 4];

            System.arraycopy(data, 0, buffer, 0, data.length);
            System.arraycopy(checksum, 0, buffer, data.length, 4);
            return encode(buffer);
        } catch (Exception e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * decode checked base58 string
     */
    public static byte[] decodeWithSha256Check(String input) {
        byte[] buffer = decode(input);
        if (buffer.length < 4) {
            throw new IllegalArgumentException();
        }

        byte[] data = BitConverter.subBytes(buffer, 0, buffer.length - 4);
        try {
            byte[] checksum = Crypto.Default.sha256(Crypto.Default.sha256(data));
            checksum = BitConverter.subBytes(checksum, 0, 4);// take 4 bytes.
            byte[] originCheckSum = BitConverter.subBytes(buffer, buffer.length - 4, buffer.length);

            if (!Arrays.equals(checksum, originCheckSum)) {
                throw new IllegalArgumentException();
            }
            return data;
        } catch (Exception e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }
}
