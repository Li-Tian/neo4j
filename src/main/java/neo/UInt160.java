package neo;

/**
 * This class stores a 160 bit unsigned int, represented as a 20-byte little-endian byte array
 */
public class UInt160 extends UIntBase {

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
        if (s == null) {
            throw new NullPointerException();
        }

        if (s.startsWith("0x")) {
            s = s.substring(2);
        }
        if (s.length() != 40) {
            throw new IllegalArgumentException();
        }

        byte[] bytes = ByteHelper.hexToBytes(s);
        bytes = ByteHelper.reverse(bytes);
        return new UInt160(bytes);
    }


    /**
     * Try to parse string into UInt256
     *
     * @param s          The string is Hex decimal format in BIG ENDIAN
     * @param out_result out parameter. The value will be changed as output
     * @return true if successfully parsed.
     */
    public static boolean tryParse(String s, UInt160 out_result) {
        try {
            UInt160 v = parse(s);
            out_result.dataBytes = v.dataBytes;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
