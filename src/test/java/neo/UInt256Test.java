package neo;

import org.junit.Assert;
import org.junit.Test;


public class UInt256Test {

    // 0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01 byte 反转
    private final static byte[] BYTES = ByteHelper.reverse(ByteHelper.hexToBytes("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01"));

    @Test
    public void parse() {
        System.out.println(ByteHelper.toHexString(BYTES));
        UInt256 uInt256 = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");

        Assert.assertArrayEquals(BYTES, uInt256.toArray());
        Assert.assertEquals("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01", uInt256.toString());

        uInt256 = UInt256.parse("a400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        Assert.assertArrayEquals(BYTES, uInt256.toArray());
        Assert.assertEquals("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01", uInt256.toString());
    }

    @Test
    public void tryParse() {
        UInt256 uInt256 = new UInt256();
        boolean success = UInt256.tryParse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01", uInt256);
        Assert.assertTrue(success);
        Assert.assertEquals("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01", uInt256.toString());
        Assert.assertArrayEquals(BYTES, uInt256.toArray());

        success = UInt256.tryParse("a400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01", uInt256);
        Assert.assertTrue(success);
        Assert.assertEquals("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01", uInt256.toString());
        Assert.assertArrayEquals(BYTES, uInt256.toArray());

        uInt256 = uInt256.clone();
        Assert.assertEquals("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01", uInt256.toString());
        Assert.assertArrayEquals(BYTES, uInt256.toArray());
    }

}