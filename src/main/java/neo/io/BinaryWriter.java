package neo.io;

import java.io.IOException;
import java.io.OutputStream;

import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.csharp.Ushort;
import neo.log.tr.TR;

/**
 * 从C#移植过来的数据写入包装类。 与 java.io.DataOutput 的不同在于它使用 little endian
 */
public class BinaryWriter {

    private OutputStream outputStream;

    public BinaryWriter(OutputStream stream) {
        outputStream = stream;
    }

    public void close() {
        try {
            outputStream.close();
        } catch (IOException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    public void write(byte[] b) {
        try {
            outputStream.write(b);
        } catch (IOException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    public void write(byte[] b, int off, int len) {
        try {
            outputStream.write(b, off, len);
        } catch (IOException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    public void writeBoolean(boolean v) {
        try {
            outputStream.write(BitConverter.getBytes(v));
        } catch (IOException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    public void writeByte(byte v) {
        try {
            outputStream.write(v);
        } catch (IOException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    public void writeChar(char v) {
        try {
            outputStream.write(BitConverter.getBytes(v));
        } catch (IOException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    public void writeDouble(double v) {
        try {
            outputStream.write(BitConverter.getBytes(v));
        } catch (IOException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    public void writeFloat(float v) {
        try {
            outputStream.write(BitConverter.getBytes(v));
        } catch (IOException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    public void writeInt(int v) {
        try {
            outputStream.write(BitConverter.getBytes(v));
        } catch (IOException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    public void writeLong(long v) {
        try {
            outputStream.write(BitConverter.getBytes(v));
        } catch (IOException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    public void writeShort(short v) {
        try {
            outputStream.write(BitConverter.getBytes(v));
        } catch (IOException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    public void writeUshort(Ushort v) {
        try {
            outputStream.write(BitConverter.getBytes(v));
        } catch (IOException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    public void writeUint(Uint v) {
        try {
            outputStream.write(BitConverter.getBytes(v));
        } catch (IOException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    public void writeUlong(Ulong v) {
        try {
            outputStream.write(BitConverter.getBytes(v));
        } catch (IOException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    public void writeUTF(String s) {
        try {
            byte[] data = s.getBytes("UTF-8");
            byte[] length = BitConverter.get7BitEncodedBytes(data.length);
            outputStream.write(length);
            outputStream.write(data);
        } catch (IOException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    public void writeSerializable(ISerializable ser) throws IOException {
        ser.serialize(this);
    }
}
