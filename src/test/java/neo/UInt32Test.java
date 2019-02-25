package neo;

import org.junit.Assert;
import org.junit.Test;


public class UInt32Test {

    @Test
    public void parse() {
        UInt32 uInt32 = UInt32.parse("0x01020304");
        Assert.assertArrayEquals(new byte[]{0x04, 0x03, 0x02, 0x01}, uInt32.toArray());
        Assert.assertEquals("0x01020304", uInt32.toString());

        uInt32 = UInt32.parse("01020304");
        Assert.assertArrayEquals(new byte[]{0x04, 0x03, 0x02, 0x01}, uInt32.toArray());
        Assert.assertEquals("0x01020304", uInt32.toString());
    }

    @Test
    public void tryParse() {
        UInt32 uInt32 = new UInt32();
        boolean success = UInt32.tryParse("01020304", uInt32);
        Assert.assertTrue(success);
        Assert.assertEquals("0x01020304", uInt32.toString());
        Assert.assertArrayEquals(new byte[]{0x04, 0x03, 0x02, 0x01}, uInt32.toArray());

        success = UInt32.tryParse("01020304", uInt32);
        Assert.assertTrue(success);
        Assert.assertEquals("0x01020304", uInt32.toString());
        Assert.assertArrayEquals(new byte[]{0x04, 0x03, 0x02, 0x01}, uInt32.toArray());
    }
}