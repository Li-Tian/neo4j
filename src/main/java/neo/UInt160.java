package neo;

import neo.cryptography.Crypto;
import neo.csharp.BitConverter;
import neo.log.tr.TR;

/**
 * This class stores a 160 bit unsigned int, represented as a 20-byte little-endian byte array
 */
public class UInt160 extends UIntBase implements Cloneable {

    public static final UInt160 Zero = new UInt160();

    /**
     * The empty constructor stores a null byte array
     */
    public UInt160() {
        super(20, null);
    }

    /**
     * Base constructor receives the intended number of bytes and a byte array. If byte array is
     * null, it's automatically initialized with given size.
     */
    public UInt160(byte[] value) {
        super(20, value);
    }

    /**
     * Method Parse receives a big-endian hex string and stores as a UInt160 little-endian 20-bytes
     * array. eg: Parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01") should create UInt160
     * 01ff00ff00ff00ff00ff00ff00ff00ff00ff00a4
     */
    public static UInt160 parse(String s) {
        TR.enter();

        if (s == null) {
            throw new NullPointerException();
        }

        if (s.startsWith("0x")) {
            s = s.substring(2);
        }
        if (s.length() != 40) {
            throw new IllegalArgumentException();
        }

        byte[] bytes = BitConverter.hexToBytes(s);
        bytes = BitConverter.reverse(bytes);
        return TR.exit(new UInt160(bytes));
    }

    /**
     * 从script 脚本中，获取 scripthash
     *
     * @param script 脚本字节数组
     * @return UInt160 脚本hash
     */
    public static UInt160 parseToScriptHash(byte[] script) {
        return new UInt160(Crypto.Default.hash160(script));
    }

    /**
     * Try to parse string into UInt256
     *
     * @param s          The string is Hex decimal format in BIG ENDIAN
     * @param out_result out parameter. The value will be changed as output
     * @return true if successfully parsed.
     */
    public static boolean tryParse(String s, UInt160 out_result) {
        TR.enter();

        try {
            UInt160 v = parse(s);
            out_result.dataBytes = v.dataBytes;
            return TR.exit(true);
        } catch (Exception e) {
            return TR.exit(false);
        }
    }

    @Override
    protected UInt160 clone() {
        TR.enter();

        byte[] tmp = new byte[dataBytes.length];
        System.arraycopy(dataBytes, 0, tmp, 0, dataBytes.length);
        return TR.exit(new UInt160(tmp));
    }

}
