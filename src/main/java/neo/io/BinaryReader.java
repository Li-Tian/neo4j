package neo.io;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.csharp.Ushort;
import neo.log.tr.TR;

import static neo.Helper.UTF_8;
import static neo.io.BinaryWriter.GROUP_SIZE;

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
            TR.warn(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public boolean readBoolean() {
        try {
            return inputStream.read() != 0;
        } catch (IOException e) {
            TR.warn(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 正常返回值是 0 到 255，这样设计的原因是从 int 转 byte 不存在二义性。反过来则存在二义性，很容易出bug.
     * 如果遇到 EOF，则抛出RuntimeException异常
     *
     * @return 0 到 255
     */
    public int readByte() {
        try {
            int value = inputStream.read();
            if (value < 0) {
                throw new RuntimeException("readByte() meets EOF");
            }
            return value;
        } catch (IOException e) {
            TR.warn(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public byte[] readFully(int length) {
        byte[] bytes = new byte[length];
        readFully(bytes);
        return bytes;
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
            TR.warn(e.getMessage());
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

    public float readFloat() {
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
            return new String(buf, UTF_8);
        } catch (IOException e) {
            TR.warn(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public <T extends ISerializable> T readSerializable(Supplier<T> generator) {
        T obj = generator.get();
        obj.deserialize(this);
        return obj;
    }

    public <T extends ISerializable> T[] readSerializableArray(IntFunction<T[]> arrayGen, Supplier<T> objGen) {
        return readSerializableArray(arrayGen, objGen, 0x1000000);
    }

    public <T extends ISerializable> T[] readSerializableArray(IntFunction<T[]> arrayGen, Supplier<T> objGen, int max/* = 0x1000000 */) {
        int length = readVarInt(new Ulong(max)).intValue();
        T[] array = arrayGen.apply(length);
        for (int i = 0; i < array.length; i++) {
            array[i] = objGen.get();
            array[i].deserialize(this);
        }
        return array;
    }

    public byte[] readBytesWithGrouping() {
        ByteArrayOutputStream tempStream = new ByteArrayOutputStream();
        int padding = 0;
        byte[] buf = new byte[GROUP_SIZE];
        do {
            readFully(buf);
            padding = readByte();
            if (padding > GROUP_SIZE) {
                TR.error("format error %d", padding);
                throw new RuntimeException("format error");
            }
            int count = GROUP_SIZE - padding;
            if (count > 0) {
                tempStream.write(buf, 0, count);
            }
        } while (padding == 0);
        return tempStream.toByteArray();
    }

    public String readFixedString(int length) {
        byte[] data = readFully(length);
        // LINQ START
        //return Encoding.UTF8.GetString(data.TakeWhile(p => p != 0).ToArray());
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (byte b : data) {
            if (b != 0) {
                stream.write(b);
            } else {
                break;
            }
        }
        return new String(stream.toByteArray(), UTF_8);
        // LINQ END
    }

    public byte[] readVarBytes() {
        return readVarBytes(0x1000000);//16777216
    }

    public byte[] readVarBytes(int max) {
        TR.enter();
        Ulong length = readVarInt(new Ulong(max));
        return TR.exit(readFully(length.intValue()));
    }

    public Ulong readVarInt() {
        return readVarInt(Ulong.MAX_VALUE_2);
    }

    public Ulong readVarInt(Ulong max) {
        TR.enter();
        Ulong value = null;
        try {
            value = new Ulong(BitConverter.decodeVarInt(inputStream));
        } catch (IOException e) {
            TR.warn(e.getMessage());
            TR.exit();
            throw new RuntimeException(e);
        }
        if (value.compareTo(max) > 0) {
            throw new RuntimeException("VarInt out of specified range : " + value);
        }
        return TR.exit(value);
    }

    public String readVarString() {
        return readVarString(0x1000000);
    }

    public String readVarString(int max/* = 0x1000000*/) {
        byte[] buf = readVarBytes(max);
        return new String(buf, UTF_8);
    }

}
