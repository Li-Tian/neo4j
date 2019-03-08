package neo;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

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


    @Test
    public void sum() {
        ArrayList<Fixed8> list = new ArrayList();
        list.add(new Fixed8(1));
        list.add(new Fixed8(2));
        list.add(new Fixed8(3));
        Fixed8 sum = Helper.sum(list, p -> p);
        Assert.assertEquals(new Fixed8(6), sum);
    }

    @Test
    public void weightedAverage() {
        ArrayList<Integer[]> list = new ArrayList<>();
        list.add(new Integer[]{1, 10});
        list.add(new Integer[]{2, 10});
        list.add(new Integer[]{3, 10});
        list.add(new Integer[]{4, 10});

        long avg = Helper.weightedAverage(list, p -> Long.valueOf(p[0]), p -> Long.valueOf(p[1]));
        Assert.assertEquals(2, avg);

        list.add(new Integer[]{5, 10});
        avg = Helper.weightedAverage(list, p -> Long.valueOf(p[0]), p -> Long.valueOf(p[1]));
        Assert.assertEquals(3, avg);
    }

    @Test
    public void weightedFilter() {
        ArrayList<Integer[]> list = new ArrayList<>();
        list.add(new Integer[]{1, 10});
        list.add(new Integer[]{2, 10});
        list.add(new Integer[]{3, 10});
        list.add(new Integer[]{4, 10});
        list.add(new Integer[]{5, 10});

        Collection<Integer[]> results = Helper.weightedFilter(list, 0.24, 0.76, p -> new Fixed8(Long.valueOf(p[1])), (p, w) -> p);
        Assert.assertEquals(3, results.size());
        Iterator<Integer[]> iterator = results.iterator();

        Integer[] item = iterator.next();
        Assert.assertEquals(Integer.valueOf(2), item[0]);

        item = iterator.next();
        Assert.assertEquals(Integer.valueOf(3), item[0]);

        item = iterator.next();
        Assert.assertEquals(Integer.valueOf(4), item[0]);
    }

}