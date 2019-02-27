package neo.io;

import java.io.IOException;
import java.io.OutputStream;

import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.csharp.Ushort;

/**
 * 从C#移植过来的数据写入包装类。 与 java.io.DataOutput 的不同在于它使用 little endian
 */
public class BinaryWriter {

    private OutputStream outputStream;

    public BinaryWriter(OutputStream stream) {
        outputStream = stream;
    }

    public void close() throws IOException {
        outputStream.close();
    }

    public void write(byte[] b) throws IOException {
        outputStream.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        outputStream.write(b, off, len);
    }

    public void writeBoolean(boolean v) throws IOException {
        outputStream.write(BitConverter.getBytes(v));
    }

    public void writeByte(byte v) throws IOException {
        outputStream.write(v);
    }

    public void writeChar(char v) throws IOException {
        outputStream.write(BitConverter.getBytes(v));
    }

    public void writeDouble(double v) throws IOException {
        outputStream.write(BitConverter.getBytes(v));
    }

    public void writeFloat(float v) throws IOException {
        outputStream.write(BitConverter.getBytes(v));
    }

    public void writeInt(int v) throws IOException {
        outputStream.write(BitConverter.getBytes(v));
    }

    public void writeLong(long v) throws IOException {
        outputStream.write(BitConverter.getBytes(v));
    }

    public void writeShort(short v) throws IOException {
        outputStream.write(BitConverter.getBytes(v));
    }

    public void writeUshort(Ushort v) throws IOException {
        outputStream.write(BitConverter.getBytes(v));
    }

    public void writeUint(Uint v) throws IOException {
        outputStream.write(BitConverter.getBytes(v));
    }

    public void writeUlong(Ulong v) throws IOException {
        outputStream.write(BitConverter.getBytes(v));
    }

    public void writeUTF(String s) throws IOException {
        byte[] data = s.getBytes("UTF-8");
        byte[] length = BitConverter.get7BitEncodedBytes(data.length);
        outputStream.write(length);
        outputStream.write(data);
    }

    public void writeSerializable(ISerializable ser) throws IOException {
        ser.serialize(this);
    }
}
