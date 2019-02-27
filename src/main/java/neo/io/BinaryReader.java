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

/**
 * 从C#移植过来的数据写入包装类。 与 java.io.DataInput 的不同在于它使用 little endian
 */

public class BinaryReader {

    private InputStream inputStream;

    public BinaryReader(InputStream stream) {
        inputStream = stream;
    }

    public void close() throws IOException {
        inputStream.close();
    }

    public boolean readBoolean() throws IOException {
        return inputStream.read() != 0;
    }

    public byte readByte() throws IOException {
        return (byte) inputStream.read();
    }

    public void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    public void readFully(byte[] b, int off, int len) throws IOException {
        int readLen = inputStream.read(b, off, len);
        if (readLen < len) {
            throw new EOFException();
        }
    }

    public char readChar() throws IOException {
        byte[] temp = new byte[2];
        readFully(temp);
        return BitConverter.toChar(temp);
    }

    public double readDouble() throws IOException {
        byte[] temp = new byte[8];
        readFully(temp);
        return BitConverter.toDouble(temp);
    }

    public double readFloat() throws IOException {
        byte[] temp = new byte[4];
        readFully(temp);
        return BitConverter.toFloat(temp);
    }

    public int readInt() throws IOException {
        byte[] temp = new byte[4];
        readFully(temp);
        return BitConverter.toInt(temp);
    }

    public long readLong() throws IOException {
        byte[] temp = new byte[8];
        readFully(temp);
        return BitConverter.toLong(temp);
    }

    public short readShort() throws IOException {
        byte[] temp = new byte[2];
        readFully(temp);
        return BitConverter.toShort(temp);
    }

    public Ushort readUshort() throws IOException {
        byte[] temp = new byte[2];
        readFully(temp);
        return BitConverter.toUshort(temp);
    }

    public Uint readUint() throws IOException {
        byte[] temp = new byte[4];
        readFully(temp);
        return BitConverter.toUint(temp);
    }

    public Ulong readUlong() throws IOException {
        byte[] temp = new byte[8];
        readFully(temp);
        return BitConverter.toUlong(temp);
    }

    public String readUTF() throws IOException {
        int length = BitConverter.decode7BitEncodedInt(inputStream);
        byte[] buf = new byte[length];
        inputStream.read(buf);
        return new String(buf, "UTF-8");
    }

    public <T extends ISerializable> T readSerializable(Supplier<T> generator) throws IOException {
        T obj = generator.get();
        obj.deserialize(this);
        return obj;
    }
}
