package neo.cryptography;

import org.junit.Test;

import neo.ByteHelper;
import neo.csharp.Uint;

import static org.junit.Assert.*;

public class Murmur3Test {

    @Test
    public void computeHash() {
        Murmur3 murmur3 = new Murmur3(Uint.ZERO);
        String input = "Hello World";
        String expected = "ce837619";
        byte[] result = murmur3.computeHash(input.getBytes());
        assertEquals(expected, ByteHelper.toHexString(result));
    }
}