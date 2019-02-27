package neo;

import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import neo.csharp.Out;
import neo.io.BinaryReader;
import neo.io.BinaryWriter;

import static org.junit.Assert.*;

public class UIntBaseTest {

    @Test
    public void size() {
        Assert.assertEquals(4, UInt32.Zero.size());
        Assert.assertEquals(20, UInt160.Zero.size());
        Assert.assertEquals(32, UInt256.Zero.size());

        Assert.assertEquals(0, UInt32.Zero.hashCode());
        Assert.assertEquals(3, UInt32.parse("0x00000003").hashCode());
    }

    @Test
    public void equals() {
        UInt32 a = UInt32.parse("0x00000001");
        UInt32 b = UInt32.parse("0x00000002");
        UInt32 c = UInt32.parse("0x00000001");

        Assert.assertEquals("0x00000001", a.toString());
        Assert.assertFalse(a.equals(b));
        Assert.assertTrue(a.equals(c));
    }

    @Test
    public void compareTo() {
        UInt32 a = UInt32.parse("0x00000001");
        UInt32 b = UInt32.parse("0x00000002");
        UInt32 c = UInt32.parse("0x00000003");
        UInt32 d = UInt32.parse("0x00000004");

        Assert.assertEquals(-1, a.compareTo(b));
        Assert.assertEquals(1, c.compareTo(b));
        Assert.assertEquals(0, c.compareTo(c));
        Assert.assertEquals(3, d.compareTo(a));

        UInt160 aa = UInt160.parse("0x0000000000000000000000000000000000000001");
        UInt160 bb = UInt160.parse("0x0000000000000000000000000000000000000002");
        UInt160 cc = UInt160.parse("0x0000000000000000000000000000000000000003");
        UInt160 dd = UInt160.parse("0x0000000000000000000000000000000000000004");

        Assert.assertEquals(0, a.compareTo(aa));
        Assert.assertEquals(0, b.compareTo(bb));
        Assert.assertEquals(0, c.compareTo(cc));

        Assert.assertEquals(-1, aa.compareTo(bb));
        Assert.assertEquals(1, cc.compareTo(bb));
        Assert.assertEquals(0, cc.compareTo(cc));
        Assert.assertEquals(3, dd.compareTo(aa));
    }

    @Test
    public void parse() {
        UIntBase uIntBase = UIntBase.parse("0x01020304");
        Assert.assertEquals("0x01020304", uIntBase.toString());
        Assert.assertArrayEquals(new byte[]{0x04, 0x03, 0x02, 0x01}, uIntBase.toArray());
    }

    @Test
    public void toArray() {
        UIntBase uIntBase = UIntBase.parse("0x01020304");
        Assert.assertEquals("0x01020304", uIntBase.toString());
        Assert.assertArrayEquals(new byte[]{0x04, 0x03, 0x02, 0x01}, uIntBase.toArray());
    }

    @Test
    public void tryParse() {
        Out<UIntBase> out = new Out<>();
        boolean success = UIntBase.tryParse("0x01020304", out);

        Assert.assertTrue(success);

        UIntBase uIntBase = out.get();
        Assert.assertEquals("0x01020304", uIntBase.toString());
        Assert.assertArrayEquals(new byte[]{0x04, 0x03, 0x02, 0x01}, uIntBase.toArray());
    }

    @Test
    public void serialize() {
        UInt32 uInt32 = UInt32.parse("0x01020304");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1024);
        try {
            uInt32.serialize(new BinaryWriter(outputStream));
            byte[] byteArray = outputStream.toByteArray();
            Assert.assertArrayEquals(new byte[]{0x04, 0x03, 0x02, 0x01}, byteArray);
        } catch (IOException e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void deserialize() {
        UInt32 uInt32 = UInt32.parse("0x01020304");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1024);
        try {
            uInt32.serialize(new BinaryWriter(outputStream));
            byte[] byteArray = outputStream.toByteArray();

            UInt32 newUint = new UInt32();
            newUint.deserialize(new BinaryReader(new ByteArrayInputStream(byteArray)));
            Assert.assertEquals("0x01020304", newUint.toString());

            Assert.assertTrue(newUint.equals(uInt32));
            Assert.assertEquals(0, newUint.compareTo(uInt32));
        } catch (IOException e) {
            Assert.assertTrue(false);
        }
    }
}