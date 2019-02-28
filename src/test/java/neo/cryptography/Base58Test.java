package neo.cryptography;

import org.junit.Assert;
import org.junit.Test;

import neo.csharp.BitConverter;

import static org.junit.Assert.*;

public class Base58Test {

    private byte[] dataArray = {
            (byte) 0x17, (byte) 0xad, (byte) 0x5c, (byte) 0xac, (byte) 0x59, (byte) 0x6a,
            (byte) 0x1e, (byte) 0xf6, (byte) 0xc1, (byte) 0x8a, (byte) 0xc1, (byte) 0x74,
            (byte) 0x6d, (byte) 0xfd, (byte) 0x30, (byte) 0x4f, (byte) 0x93, (byte) 0x96,
            (byte) 0x43, (byte) 0x54, (byte) 0xb5, (byte) 0x78, (byte) 0xa5, (byte) 0x83,
            (byte) 0x22};

    private String dataStr = "AXaXZjZGA3qhQRTCsyG5uFKr9HeShgVhTF";

    @Test
    public void decode() {
        assertArrayEquals(dataArray, Base58.decode(dataStr));
    }

    @Test
    public void encode() {
        assertEquals(dataStr, Base58.encode(dataArray));
    }

    @Test
    public void encodeWithSha256Check() {
        byte[] input = BitConverter.hexToBytes("0xa8e04dcd1c0531835fb83237a946ae5213cf3f8d");
        Assert.assertEquals("GPwE8v5H7YqmcWzQABBP5qGCpQt5EH31X", Base58.encodeWithSha256Check(input));
    }


    @Test
    public void decodeWithSha256Check() {
        byte[] input = BitConverter.hexToBytes("0xa8e04dcd1c0531835fb83237a946ae5213cf3f8d");
        String dest = Base58.encodeWithSha256Check(input);
        byte[] bytes = Base58.decodeWithSha256Check(dest);
        Assert.assertArrayEquals(input, bytes);
    }
}