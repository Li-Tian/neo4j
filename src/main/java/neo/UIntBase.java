package neo;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import neo.csharp.Out;
import neo.io.ISerializable;

/**
 * Base class for little-endian unsigned integers. Two classes inherit from this: UInt160 and
 * UInt256. Only basic comparison/serialization are proposed for these classes. For arithmetic
 * purposes, use BigInteger class.
 */
public abstract class UIntBase implements ISerializable, Comparable<UIntBase> {

    /**
     * Storing unsigned int in a little-endian byte array.
     */
    protected byte[] dataBytes;


    /**
     * Base constructor receives the intended number of bytes and a byte array. If byte array is
     * null, it's automatically initialized with given size.
     */
    protected UIntBase(int bytes, byte[] value) {
        if (value == null) {
            this.dataBytes = new byte[bytes];
            return;
        }
        if (value.length != bytes) {
            throw new IllegalArgumentException();
        }

        this.dataBytes = value;
    }

    /**
     * Number of bytes of the unsigned int. Currently, inherited classes use 20-bytes (UInt160) or
     * 32-bytes (UInt256), 4 -bytes (UInt32)
     */
    @Override
    public int size() {
        return dataBytes.length;
    }

    /**
     * Method Serialize writes the data_bytes array into a BinaryWriter object
     */
    @Override
    public void serialize(OutputStream writer) throws IOException {
        writer.write(dataBytes);
    }

    /**
     * Deserialize function reads the expected size in bytes from the given BinaryReader and stores
     * in data_bytes array.
     */
    @Override
    public void deserialize(InputStream reader) throws IOException {
        reader.read(dataBytes, 0, dataBytes.length);
    }

    /**
     * Method Equals returns true if objects are equal, false otherwise If null is passed as
     * parameter, this method returns false. If it's a self-reference, it returns true.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final UIntBase uIntBase = (UIntBase) o;
        return Arrays.equals(dataBytes, uIntBase.dataBytes);
    }

    /**
     * Method GetHashCode returns a 32-bit int representing a hash code, composed of the first 4
     * bytes.
     */
    @Override
    public int hashCode() {
        if (dataBytes == null || dataBytes.length == 0) {
            return 0;
        }
        return ByteBuffer.wrap(dataBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    /**
     * Method CompareTo returns 1 if this UIntBase is bigger than other UIntBase; -1 if it's
     * smaller; 0 if it's equals. eg: assume this is 01ff00ff00ff00ff00ff00ff00ff00ff00ff00a4,
     * this.CompareTo(02ff00ff00ff00ff00ff00ff00ff00ff00ff00a3) returns 1
     */
    @Override
    public int compareTo(UIntBase other) {
        byte[] x = this.dataBytes;
        byte[] y = other.dataBytes;

        int i = x.length - 1, j = y.length - 1;

        // 包含比较 UInt32, UInt160, UInt256  之间的大小
        while (i > j) {
            int r = Byte.toUnsignedInt(x[i]);
            if (r > 0) return r;
            i--;
        }

        while (j > i) {
            int r = Byte.toUnsignedInt(y[j]);
            if (r > 0) return -r;
            j--;
        }

        for (; i >= 0 && j >= 0; i--, j--) {
            int r = Byte.toUnsignedInt(x[i]) - Byte.toUnsignedInt(y[j]);
            if (r != 0) {
                return r;
            }
        }
        return 0;
    }


    /**
     * Method ToString returns a big-endian string starting by "0x" representing the little-endian
     * unsigned int Example: if this is storing 20-bytes 01ff00ff00ff00ff00ff00ff00ff00ff00ff00a4,
     * ToString() should return "0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01"
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("0x");

        if (dataBytes == null || dataBytes.length <= 0) {
            return null;
        }
        return "0x" + ByteHelper.toHexString(ByteHelper.reverse(dataBytes));
    }

    /**
     * Method Parse receives a big-endian hex string and stores as a UInt160 or UInt256
     * little-endian byte array Example: Parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01") should
     * create UInt160 01ff00ff00ff00ff00ff00ff00ff00ff00ff00a4
     */
    public static UIntBase parse(String s) {
        if (s.length() == 8 || s.length() == 10) {
            return UInt32.parse(s);
        } else if (s.length() == 40 || s.length() == 42)
            return UInt160.parse(s);
        else if (s.length() == 64 || s.length() == 66)
            return UInt256.parse(s);
        else {
            throw new IllegalArgumentException(String.format("%s cannot convert to UIntBase.class", s));
        }
    }

    /**
     * Method ToArray() returns the byte array data_bytes, which stores the little-endian unsigned
     * int
     */
    public byte[] toArray() {
        return dataBytes;
    }

    /**
     * Method TryParse tries to parse a big-endian hex string and stores it as a UInt160 or UInt256
     * little-endian bytes array Example: TryParse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01",
     * result) should create result UInt160 01ff00ff00ff00ff00ff00ff00ff00ff00ff00a4
     */
    public static boolean tryParse(String s, Out<UIntBase> result) {
        int size = 0;

        if (s.length() == 8 || s.length() == 10) {
            size = 4;
        } else if (s.length() == 40 || s.length() == 42) {
            size = 20;
        } else if (s.length() == 64 || s.length() == 66) {
            size = 32;
        } else {
            size = 0;
        }

        if (size == 4) {
            UInt32 uInt32 = new UInt32();
            if (UInt32.tryParse(s, uInt32)) {
                result.set(uInt32);
                return true;
            }
        } else if (size == 20) {
            UInt160 uInt160 = new UInt160();
            if (UInt160.tryParse(s, uInt160)) {
                result.set(uInt160);
                return true;
            }
        } else if (size == 32) {
            UInt256 uInt256 = new UInt256();
            if (UInt256.tryParse(s, uInt256)) {
                result.set(uInt256);
                return true;
            }
        }
        return false;
    }

}
