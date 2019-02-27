package neo.cryptography;

import org.junit.Test;

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
}