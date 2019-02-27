package neo.io;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.csharp.Ushort;

import static org.junit.Assert.*;

public class BinaryReaderTest {

    @Test
    public void readBoolean() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BinaryWriter writer = new BinaryWriter(out);
            writer.writeBoolean(true);
            writer.writeBoolean(false);
            ByteArrayInputStream input = new ByteArrayInputStream(out.toByteArray());
            BinaryReader reader = new BinaryReader(input);
            assertEquals(true, reader.readBoolean());
            assertEquals(false, reader.readBoolean());
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void readByte() {
        byte[] data = {0, 1, -1, 127, -128};
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BinaryWriter writer = new BinaryWriter(out);
            for (byte b : data) {
                writer.writeByte(b);
            }
            ByteArrayInputStream input = new ByteArrayInputStream(out.toByteArray());
            BinaryReader reader = new BinaryReader(input);
            for (byte b : data) {
                assertEquals(b, reader.readByte());
            }
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void readChar() {
        char[] data = {0, 1, 127, 128, 255};
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BinaryWriter writer = new BinaryWriter(out);
            for (char b : data) {
                writer.writeChar(b);
            }
            ByteArrayInputStream input = new ByteArrayInputStream(out.toByteArray());
            BinaryReader reader = new BinaryReader(input);
            for (char b : data) {
                assertEquals(b, reader.readChar());
            }
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void readDouble() {
        double[] data = {0, 1, 1.1, -1, Double.MAX_VALUE, Double.MIN_VALUE};
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BinaryWriter writer = new BinaryWriter(out);
            for (double b : data) {
                writer.writeDouble(b);
            }
            ByteArrayInputStream input = new ByteArrayInputStream(out.toByteArray());
            BinaryReader reader = new BinaryReader(input);
            for (double b : data) {
                assertEquals(b, reader.readDouble(), 0.0);
            }
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void readFloat() {
        float[] data = {0.0F, 1.0F, 1.1F, -1.0F, Float.MAX_VALUE, Float.MIN_VALUE};
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BinaryWriter writer = new BinaryWriter(out);
            for (float b : data) {
                writer.writeFloat(b);
            }
            ByteArrayInputStream input = new ByteArrayInputStream(out.toByteArray());
            BinaryReader reader = new BinaryReader(input);
            for (float b : data) {
                assertEquals(b, reader.readFloat(), 0.0F);
            }
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void readInt() {
        int[] data = {0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE};
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BinaryWriter writer = new BinaryWriter(out);
            for (int b : data) {
                writer.writeInt(b);
            }
            ByteArrayInputStream input = new ByteArrayInputStream(out.toByteArray());
            BinaryReader reader = new BinaryReader(input);
            for (int b : data) {
                assertEquals(b, reader.readInt());
            }
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void readLong() {
        long[] data = {0, 1, -1, Long.MAX_VALUE, Long.MIN_VALUE};
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BinaryWriter writer = new BinaryWriter(out);
            for (long b : data) {
                writer.writeLong(b);
            }
            ByteArrayInputStream input = new ByteArrayInputStream(out.toByteArray());
            BinaryReader reader = new BinaryReader(input);
            for (long b : data) {
                assertEquals(b, reader.readLong());
            }
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void readShort() {
        short[] data = {0, 1, -1, Short.MAX_VALUE, Short.MIN_VALUE};
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BinaryWriter writer = new BinaryWriter(out);
            for (short b : data) {
                writer.writeInt(b);
            }
            ByteArrayInputStream input = new ByteArrayInputStream(out.toByteArray());
            BinaryReader reader = new BinaryReader(input);
            for (short b : data) {
                assertEquals(b, reader.readInt());
            }
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void readUshort() {
        Ushort[] data = {
                new Ushort(0),
                new Ushort(1),
                new Ushort(2),
                new Ushort(Ushort.MAX_VALUE),
                new Ushort(Ushort.MIN_VALUE)
        };
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BinaryWriter writer = new BinaryWriter(out);
            for (Ushort b : data) {
                writer.writeUshort(b);
            }
            ByteArrayInputStream input = new ByteArrayInputStream(out.toByteArray());
            BinaryReader reader = new BinaryReader(input);
            for (Ushort b : data) {
                assertEquals(b, reader.readUshort());
            }
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void readUint() {
        Uint[] data = {
                new Uint(0),
                new Uint(1),
                new Uint(-1),
                new Uint((int) Uint.MAX_VALUE),
                new Uint((int) Uint.MIN_VALUE)
        };
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BinaryWriter writer = new BinaryWriter(out);
            for (Uint b : data) {
                writer.writeUint(b);
            }
            ByteArrayInputStream input = new ByteArrayInputStream(out.toByteArray());
            BinaryReader reader = new BinaryReader(input);
            for (Uint b : data) {
                assertEquals(b, reader.readUint());
            }
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void readUlong() {
        Ulong[] data = {
                new Ulong(0),
                new Ulong(1),
                new Ulong(-1),
                new Ulong(Ulong.MAX_VALUE.longValue()),
                new Ulong(Ulong.MIN_VALUE.longValue())
        };
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BinaryWriter writer = new BinaryWriter(out);
            for (Ulong b : data) {
                writer.writeUlong(b);
            }
            ByteArrayInputStream input = new ByteArrayInputStream(out.toByteArray());
            BinaryReader reader = new BinaryReader(input);
            for (Ulong b : data) {
                assertEquals(b, reader.readUlong());
            }
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void readUTF() {
        byte[] buf = new byte[1024];
        String[] data = {
                "",
                "A",
                "中文",
                " ",
                " A中文 ",
                new String(buf)
        };
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BinaryWriter writer = new BinaryWriter(out);
            for (String b : data) {
                writer.writeUTF(b);
            }
            ByteArrayInputStream input = new ByteArrayInputStream(out.toByteArray());
            BinaryReader reader = new BinaryReader(input);
            for (String b : data) {
                assertEquals(b, reader.readUTF());
            }
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void readSerializable() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BinaryWriter writer = new BinaryWriter(out);
            DummySerializableTestObj dummy = new DummySerializableTestObj();
            dummy.value = 1;
            writer.writeSerializable(dummy);
            ByteArrayInputStream input = new ByteArrayInputStream(out.toByteArray());
            BinaryReader reader = new BinaryReader(input);
            assertEquals(dummy, reader.readSerializable(DummySerializableTestObj::new));
        } catch (IOException e) {
            fail();
        }
    }

    static class DummySerializableTestObj implements ISerializable {

        long value;

        @Override
        public int size() {
            return Long.SIZE;
        }

        @Override
        public void serialize(BinaryWriter writer) throws IOException {
            writer.writeLong(value);
        }

        @Override
        public void deserialize(BinaryReader reader) throws IOException {
            value = reader.readLong();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DummySerializableTestObj that = (DummySerializableTestObj) o;
            return value == that.value;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(value);
        }
    }
}