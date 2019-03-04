package neo;

import org.junit.Assert;
import org.junit.Test;

import neo.csharp.BitConverter;


public class UInt160Test {

    // 0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01  的反转byte
    private final static byte[] BYTES = new byte[]{0x01, (byte) 0xff, 0x00, (byte) 0xff, 0x00, (byte) 0xff, 0x00,
            (byte) 0xff, 0x00, (byte) 0xff, 0x00, (byte) 0xff, 0x00, (byte) 0xff, 0x00,
            (byte) 0xff, 0x00, (byte) 0xff, 0x00, (byte) 0xa4};

    @Test
    public void parse() {
        System.out.println(BitConverter.toHexString(BYTES));
        UInt160 uInt160 = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");

        Assert.assertArrayEquals(BYTES, uInt160.toArray());
        Assert.assertEquals("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01", uInt160.toString());

        uInt160 = UInt160.parse("a400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        Assert.assertArrayEquals(BYTES, uInt160.toArray());
        Assert.assertEquals("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01", uInt160.toString());
    }

    @Test
    public void tryParse() {
        UInt160 uInt160 = new UInt160();
        boolean success = UInt160.tryParse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01", uInt160);
        Assert.assertTrue(success);
        Assert.assertEquals("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01", uInt160.toString());
        Assert.assertArrayEquals(BYTES, uInt160.toArray());

        success = UInt160.tryParse("a400ff00ff00ff00ff00ff00ff00ff00ff00ff01", uInt160);
        Assert.assertTrue(success);
        Assert.assertEquals("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01", uInt160.toString());
        Assert.assertArrayEquals(BYTES, uInt160.toArray());

        uInt160 = uInt160.clone();
        Assert.assertEquals("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01", uInt160.toString());
        Assert.assertArrayEquals(BYTES, uInt160.toArray());
    }

    @Test
    public void parseScriptHash() {
        byte[] script = BitConverter.hexToBytes("21031f64da8a38e6c1e5423a72ddd6d4fc4a777abe537e5cb5aa0425685cda8e063bac");
        UInt160 uInt160 = UInt160.parseToScriptHash(script);
        Assert.assertEquals("0xa8e04dcd1c0531835fb83237a946ae5213cf3f8d", uInt160.toString());

    }

    @Test
    public void toAddress() {
        UInt160 uInt160 = new UInt160();
        UInt160.tryParse("0xa8e04dcd1c0531835fb83237a946ae5213cf3f8d", uInt160);
        Assert.assertEquals("AUejN4mLdGA8Yyhmj1NwqURwE3FrMjZ4e9", uInt160.toAddress());
    }
}