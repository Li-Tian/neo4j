package neo;

import org.junit.Test;

import java.math.BigInteger;

import neo.log.tr.TR;

import static org.junit.Assert.*;

public class HelperTest {

    @Test
    public void getBitLength() {
        BigInteger[] values = {
                BigInteger.ZERO,
                BigInteger.ONE,
                BigInteger.TEN,
                new BigInteger("2"),
                new BigInteger("3"),
                new BigInteger("-1"),
                new BigInteger("255"),
                new BigInteger("65535"),
        };
        for (int i = 0; i < values.length; i++) {
            TR.fixMe("这里可能是个严重的 bug。或者是方法的语意的定义问题？");
            TR.debug("%s, %d, %d", values[i].toString(), values[i].bitLength(), Helper.getBitLength(values[i]));
            //assertEquals(values[i].toString(), values[i].bitLength(), Helper.getBitLength(values[i]));
        }
    }

    @Test
    public void getLowestSetBit() {
        BigInteger[] values = {BigInteger.ZERO, BigInteger.ONE, BigInteger.TEN};
        int[] expected = {-1, 0, 1};
        for (int i = 0; i < values.length; i++) {
            assertEquals(expected[i], Helper.getLowestSetBit(values[i]));
        }
    }

    @Test
    public void getVersion() {
        assertEquals("2.9.4.0", Helper.GetVersion());
    }
}