package neo.cryptography;

import org.junit.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

public class SCryptTest {
    @Test
    public void deriveKey() {
        try {
            byte[] result = new byte[64];
            result = SCrypt.deriveKey("HelloWorld".getBytes("utf-8"), "abcd".getBytes("utf-8"), 16384, 8, 8, 64);
            int[] expectedUnsigned = new int[] {214, 165, 226, 43, 75, 132, 119, 136, 44, 59, 1, 46, 89, 161, 197, 149, 136, 23, 47, 3, 83, 191, 113, 12, 203, 254, 218, 78, 60, 24, 103, 177, 107, 119, 77, 225, 31, 192, 188, 248, 176, 103, 174, 22, 57, 36, 219, 9, 142, 24, 235, 183, 194, 120, 118, 180, 224, 218, 64, 144, 47, 50, 109, 226};
            byte[] expected = new byte[expectedUnsigned.length];
            for (int i = 0; i < expected.length; i++) {
                expected[i] = (byte)expectedUnsigned[i];
            }
            Assert.assertArrayEquals(expected, result);
        }
        catch (UnsupportedEncodingException e) {}
    }
}