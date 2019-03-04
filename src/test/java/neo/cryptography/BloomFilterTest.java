package neo.cryptography;

import org.junit.Test;

import neo.csharp.BitConverter;
import neo.csharp.Uint;

import static org.junit.Assert.*;

public class BloomFilterTest {

    byte[] element1 = {1, 2, 3, 4, 5 };
    byte[] element2 = {6, 7, 8, 9, 10 };

    String bitStr1 = "0000000000000000000000000000000000000000000000000100000000000000400100000000000400000400000000000004000000000000000000200000000a08000000000000000000000008000000400000000000000040001000000000000000000000000000000000020080000000000000000000000000000000000000";
    String bitStr2 = "0080000004000000000000000024000020000000000000000100000000800000400100000000000600002400000000000004000000000000000080200000000a08000000000000000000000008000000400000100800000040001000000002000000000000000000000000020080000000000000000000000084200800000000";

    @Test
    public void add() {
        BloomFilter bf = new BloomFilter(1024, 16, new Uint(127));
        bf.add(element1);
        byte[] bits = new byte[128];
        bf.getBits(bits);
        byte[] expected1 = BitConverter.hexToBytes(bitStr1);
        assertArrayEquals(expected1, bits);
        assertTrue(bf.check(element1));
        BloomFilter bf2 = new BloomFilter(1024, 16, new Uint(127), bits);
        assertEquals(bf.getM(), bf2.getM());
        assertEquals(bf.getK(), bf2.getK());
        assertEquals(bf.getTweak(), bf2.getTweak());
        byte[] copy = new byte[128];
        byte[] copy2 = new byte[128];
        bf.getBits(copy);
        bf2.getBits(copy2);
        assertArrayEquals(copy, copy2);
        assertTrue(bf.check(element1));
        assertTrue(bf2.check(element1));
        bf.add(element2);
        bf.getBits(bits);
        byte[] expected2 = BitConverter.hexToBytes(bitStr2);
        assertArrayEquals(expected2, bits);
        assertTrue(bf.check(element1));
        assertTrue(bf.check(element2));
    }

    @Test
    public void check() {
        byte[] bits = BitConverter.hexToBytes(bitStr1);
        assertEquals(128, bits.length);
        BloomFilter bf = new BloomFilter(1024, 16, new Uint(127), bits);
        assertTrue(bf.check(element1));
        bits = BitConverter.hexToBytes(bitStr2);
        bf = new BloomFilter(1024, 16, new Uint(127), bits);
        assertTrue(bf.check(element1));
        assertTrue(bf.check(element2));
    }

    @Test
    public void getBits() {
        byte[] bits = BitConverter.hexToBytes(bitStr1);
        assertEquals(128, bits.length);
        BloomFilter bf = new BloomFilter(1024, 16, new Uint(127), bits);
        byte[] copy = new byte[128];
        bf.getBits(copy);
        assertArrayEquals(bits, copy);
    }

}