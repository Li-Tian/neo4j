package neo;

import neo.csharp.BitConverter;
import neo.log.notr.TR;

/**
 * This class stores a 32 bit unsigned int, represented as a 4-byte little-endian byte array
 *
 * @notice Be careful when use Zero, can not modify the byte array, especially the deserialize
 * method, it's better to create a object to deserialize.
 */
public class UInt32 extends UIntBase implements Cloneable {

    public static final UInt32 Zero = new UInt32();

    /**
     * The empty constructor stores a null byte array
     */
    public UInt32() {
        super(4, null);
    }

    /**
     * Base constructor receives the intended number of bytes and a byte array. If byte array is
     * null, it's automatically initialized with given size.
     */
    public UInt32(byte[] value) {
        super(4, value);
    }


    /**
     * Method Parse receives a big-endian hex string and stores as a UInt32 little-endian 4-bytes
     * array Example: Parse("0xff00ff01") should create UInt32 01ff00ff
     */
    public static UInt32 parse(String s) {
        TR.enter();

        if (s == null) {
            throw new NullPointerException();
        }

        if (s.startsWith("0x")) {
            s = s.substring(2);
        }
        if (s.length() != 8) {
            throw new IllegalArgumentException();
        }

        byte[] bytes = BitConverter.hexToBytes(s);
        bytes = BitConverter.reverse(bytes);
        return TR.exit(new UInt32(bytes));
    }


    /**
     * Try to parse string into UInt256
     *
     * @param s          The string is Hex decimal format in BIG ENDIAN
     * @param out_result out parameter. The value will be changed as output
     * @return true if successfully parsed.
     */
    public static boolean tryParse(String s, UInt32 out_result) {
        TR.enter();
        try {
            UInt32 v = parse(s);
            out_result.dataBytes = v.dataBytes;
            return TR.exit(true);
        } catch (Exception e) {
            return TR.exit(false);
        }
    }

    @Override
    protected UInt32 clone() {
        TR.enter();

        byte[] tmp = new byte[dataBytes.length];
        System.arraycopy(dataBytes, 0, tmp, 0, dataBytes.length);
        return TR.exit(new UInt32(tmp));
    }

    @Override
    public String toString() {
        TR.enter();
        if (dataBytes == null || dataBytes.length <= 0) {
            return TR.exit(null);
        }
        return TR.exit("0x" + BitConverter.toHexString(BitConverter.reverse(dataBytes)));
    }
}
