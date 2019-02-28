//package neo.io;
//
//import java.io.IOException;
//import java.io.OutputStream;
//
//import neo.csharp.BitConverter;
//import neo.csharp.Uint;
//import neo.csharp.Ulong;
//import neo.csharp.Ushort;
//import neo.log.notr.TR;
//
//import static neo.Helper.UTF_8;
//
///**
// * 从C#移植过来的数据写入包装类。 与 java.io.DataOutput 的不同在于它使用 little endian
// */
//public class BinaryWriter {
//
//    static final int GROUP_SIZE = 16;
//    private OutputStream outputStream;
//
//    public BinaryWriter(OutputStream stream) {
//        outputStream = stream;
//    }
//
//    public void close() {
//        try {
//            outputStream.close();
//        } catch (IOException e) {
//            TR.warn(e.getMessage());
//            throw new RuntimeException(e);
//        }
//    }
//
//    public void flush() {
//        try {
//            outputStream.flush();
//        } catch (IOException e) {
//            TR.warn(e.getMessage());
//            throw new RuntimeException(e);
//        }
//    }
//
//    /**
//     * 写入字节数组。数组的长度没有写入。因此如果没有长度信息无法还原。
//     *
//     * @param b 字节数组
//     */
//    public void write(byte[] b) {
//        try {
//            outputStream.write(b);
//        } catch (IOException e) {
//            TR.warn(e.getMessage());
//            throw new RuntimeException(e);
//        }
//    }
//
//    public void write(byte[] b, int off, int len) {
//        try {
//            outputStream.write(b, off, len);
//        } catch (IOException e) {
//            TR.warn(e.getMessage());
//            throw new RuntimeException(e);
//        }
//    }
//
//    public void writeBoolean(boolean v) {
//        try {
//            outputStream.write(BitConverter.getBytes(v));
//        } catch (IOException e) {
//            TR.warn(e.getMessage());
//            throw new RuntimeException(e);
//        }
//    }
//
//    public void writeByte(byte v) {
//        try {
//            outputStream.write(v);
//        } catch (IOException e) {
//            TR.warn(e.getMessage());
//            throw new RuntimeException(e);
//        }
//    }
//
//    public void writeChar(char v) {
//        try {
//            outputStream.write(BitConverter.getBytes(v));
//        } catch (IOException e) {
//            TR.warn(e.getMessage());
//            throw new RuntimeException(e);
//        }
//    }
//
//    public void writeDouble(double v) {
//        try {
//            outputStream.write(BitConverter.getBytes(v));
//        } catch (IOException e) {
//            TR.warn(e.getMessage());
//            throw new RuntimeException(e);
//        }
//    }
//
//    public void writeFloat(float v) {
//        try {
//            outputStream.write(BitConverter.getBytes(v));
//        } catch (IOException e) {
//            TR.warn(e.getMessage());
//            throw new RuntimeException(e);
//        }
//    }
//
//    public void writeInt(int v) {
//        try {
//            outputStream.write(BitConverter.getBytes(v));
//        } catch (IOException e) {
//            TR.warn(e.getMessage());
//            throw new RuntimeException(e);
//        }
//    }
//
//    public void writeLong(long v) {
//        try {
//            outputStream.write(BitConverter.getBytes(v));
//        } catch (IOException e) {
//            TR.warn(e.getMessage());
//            throw new RuntimeException(e);
//        }
//    }
//
//    public void writeShort(short v) {
//        try {
//            outputStream.write(BitConverter.getBytes(v));
//        } catch (IOException e) {
//            TR.warn(e.getMessage());
//            throw new RuntimeException(e);
//        }
//    }
//
//    public void writeUshort(Ushort v) {
//        try {
//            outputStream.write(BitConverter.getBytes(v));
//        } catch (IOException e) {
//            TR.warn(e.getMessage());
//            throw new RuntimeException(e);
//        }
//    }
//
//    public void writeUint(Uint v) {
//        try {
//            outputStream.write(BitConverter.getBytes(v));
//        } catch (IOException e) {
//            TR.warn(e.getMessage());
//            throw new RuntimeException(e);
//        }
//    }
//
//    public void writeUlong(Ulong v) {
//        try {
//            outputStream.write(BitConverter.getBytes(v));
//        } catch (IOException e) {
//            TR.warn(e.getMessage());
//            throw new RuntimeException(e);
//        }
//    }
//
//    public void writeUTF(String s) {
//        TR.enter();
//        try {
//            byte[] data = s.getBytes(UTF_8);
//            byte[] length = BitConverter.get7BitEncodedBytes(data.length);
//            outputStream.write(length);
//            outputStream.write(data);
//        } catch (IOException e) {
//            TR.warn(e.getMessage());
//            TR.exit();
//            throw new RuntimeException(e);
//        }
//        TR.exit();
//    }
//
//    public void writeSerializable(ISerializable ser) {
//        ser.serialize(this);
//    }
//
//    public <T extends ISerializable> void writeArray(T[] values) {
//        TR.enter();
//        writeVarInt(values.length);
//        for (int i = 0; i < values.length; i++) {
//            values[i].serialize(this);
//        }
//        TR.exit();
//    }
//
//    /**
//     * 分组写字节数组。每逢16字节写一个字节0。 如果剩余0到15字节，则补0至16字节，然后写补0的个数，最少补1个，最多补16个。
//     *
//     * @param value 字节数组
//     */
//    public void writeBytesWithGrouping(byte[] value) {
//        TR.enter();
//        try {
//            int index = 0;
//            int remain = value.length;
//            while (remain >= GROUP_SIZE) {
//                outputStream.write(value, index, GROUP_SIZE);
//                outputStream.write((byte) 0);
//                index += GROUP_SIZE;
//                remain -= GROUP_SIZE;
//            }
//            if (remain > 0) {
//                outputStream.write(value, index, remain);
//            }
//            int padding = GROUP_SIZE - remain;
//            for (int i = 0; i < padding; i++) {
//                outputStream.write((byte) 0);
//            }
//            outputStream.write((byte) padding);
//        } catch (IOException e) {
//            TR.warn(e.getMessage());
//            TR.exit();
//            throw new RuntimeException(e);
//        }
//        TR.exit();
//    }
//
//    /**
//     * 将字符串用UTF-8编码写入字节数据流。如果没有达到指定的长度，则在后面补0。
//     *
//     * @param value  字符串。如果UTF-8编码后长度超过指定长度则抛异常。
//     * @param length 指定长度。
//     */
//    public void writeFixedString(String value, int length) {
//        TR.enter();
//        if (value == null) {
//            TR.exit();
//            throw new NullPointerException("value");
//        }
//        if (value.length() > length) {
//            TR.exit();
//            throw new IllegalArgumentException();
//        }
//        try {
//            byte[] bytes = value.getBytes(UTF_8);
//            if (bytes.length > length) {
//                TR.exit();
//                throw new IllegalArgumentException();
//            }
//            outputStream.write(bytes);
//            if (bytes.length < length) {
//                outputStream.write(new byte[length - bytes.length]);
//            }
//        } catch (IOException e) {
//            TR.warn(e.getMessage());
//            TR.exit();
//            throw new RuntimeException(e);
//        }
//        TR.exit();
//    }
//
//    /**
//     * 写入字节数组。先用VarInt格式写书数组的长度，再写入数组的内容。
//     *
//     * @param value 字节数组
//     */
//    public void writeVarBytes(byte[] value) {
//        TR.enter();
//        writeVarInt(value.length);
//        try {
//            outputStream.write(value);
//        } catch (IOException e) {
//            TR.warn(e.getMessage());
//            TR.exit();
//            throw new RuntimeException(e);
//        }
//        TR.exit();
//    }
//
//    public void writeVarInt(long value) {
//        TR.enter();
//        if (value < 0) {
//            TR.exit();
//            throw new IllegalArgumentException("value out of range for VarInt");
//        }
//        try {
//            outputStream.write(BitConverter.getVarIntEncodedBytes(value));
//        } catch (IOException e) {
//            TR.warn(e.getMessage());
//            TR.exit();
//            throw new RuntimeException(e);
//        }
//        TR.exit();
//    }
//
//    public void writeVarString(String value) {
//        TR.enter();
//        writeVarBytes(value.getBytes(UTF_8));
//        TR.exit();
//    }
//}
