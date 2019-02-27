package neo.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.csharp.Ushort;
import neo.log.tr.TR;

/**
 * 从C#移植过来的数据写入包装类。 与 java.io.DataInput 的不同在于它使用 little endian
 */

public class BinaryReader {

    private InputStream inputStream;

    public BinaryReader(InputStream stream) {
        inputStream = stream;
    }

    public void close() {
        try {
            inputStream.close();
        } catch (IOException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    public boolean readBoolean() {
        try {
            return inputStream.read() != 0;
        } catch (IOException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    public byte readByte() {
        try {
            return (byte) inputStream.read();
        } catch (IOException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    public void readFully(byte[] b) {
        readFully(b, 0, b.length);
    }

    public void readFully(byte[] b, int off, int len) {
        int readLen = 0;
        try {
            readLen = inputStream.read(b, off, len);
            if (readLen < len) {
                throw new EOFException();
            }
        } catch (IOException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    public char readChar() {
        byte[] temp = new byte[2];
        readFully(temp);
        return BitConverter.toChar(temp);
    }

    public double readDouble() {
        byte[] temp = new byte[8];
        readFully(temp);
        return BitConverter.toDouble(temp);
    }

    public double readFloat() {
        byte[] temp = new byte[4];
        readFully(temp);
        return BitConverter.toFloat(temp);
    }

    public int readInt() {
        byte[] temp = new byte[4];
        readFully(temp);
        return BitConverter.toInt(temp);
    }

    public long readLong() {
        byte[] temp = new byte[8];
        readFully(temp);
        return BitConverter.toLong(temp);
    }

    public short readShort() {
        byte[] temp = new byte[2];
        readFully(temp);
        return BitConverter.toShort(temp);
    }

    public Ushort readUshort() {
        byte[] temp = new byte[2];
        readFully(temp);
        return BitConverter.toUshort(temp);
    }

    public Uint readUint() {
        byte[] temp = new byte[4];
        readFully(temp);
        return BitConverter.toUint(temp);
    }

    public Ulong readUlong() {
        byte[] temp = new byte[8];
        readFully(temp);
        return BitConverter.toUlong(temp);
    }

    public String readUTF() {
        try {
            int length = BitConverter.decode7BitEncodedInt(inputStream);
            byte[] buf = new byte[length];
            inputStream.read(buf);
            return new String(buf, "UTF-8");
        } catch (IOException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    public <T extends ISerializable> T readSerializable(Supplier<T> generator) {
        T obj = generator.get();
        try {
            obj.deserialize(this);
            return obj;
        } catch (IOException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }
}
