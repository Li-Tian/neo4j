package neo;

import neo.log.tr.TR;

/**
 * This class stores a 256 bit unsigned int, represented as a 32-byte little-endian byte array
 */
public class UInt256 extends UIntBase implements Cloneable{

    public static final UInt256 Zero = new UInt256();

    /**
     * The empty constructor stores a null byte array
     */
    public UInt256() {
        super(32, null);
    }

    /**
     * Base constructor receives the intended number of bytes and a byte array. If byte array is
     * null, it's automatically initialized with given size.
     */
    public UInt256(byte[] value) {
        super(32, value);
    }

    /**
     * Method Parse receives a big-endian hex string and stores as a UInt256 little-endian 32-bytes
     * array Example: Parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01")
     * should create UInt256 01ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00a4
     */
    public static UInt256 parse(String s) {
        TR.enter();
        if (s == null) {
            throw new NullPointerException();
        }

        if (s.startsWith("0x")) {
            s = s.substring(2);
        }
        if (s.length() != 64) {
            throw new IllegalArgumentException();
        }

        byte[] bytes = ByteHelper.hexToBytes(s);
        bytes = ByteHelper.reverse(bytes);
        return TR.exit(new UInt256(bytes));
    }

    /**
     * Try to parse string into UInt256
     *
     * @param s          The string is Hex decimal format in BIG ENDIAN
     * @param out_result out parameter. The value will be changed as output
     * @return true if successfully parsed.
     */
    public static boolean tryParse(String s, UInt256 out_result) {
        TR.enter();
        try {
            UInt256 v = parse(s);
            out_result.dataBytes = v.dataBytes;
            return TR.exit(true);
        } catch (Exception e) {
            return TR.exit(false);
        }
    }

    @Override
    protected UInt256 clone() {
        TR.enter();

        byte[] tmp = new byte[dataBytes.length];
        System.arraycopy(dataBytes, 0, tmp, 0, dataBytes.length);
        return TR.exit(new UInt256(tmp));
    }

}
