package neo.io.caching;

import org.junit.Assert;
import org.junit.Test;

import neo.UInt256;
import neo.exception.KeyNotFoundException;
import neo.network.p2p.payloads.TransactionDemo;

import static org.junit.Assert.*;

public class RelayCacheTest {

    private UInt256 u1 = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
    private UInt256 u2 = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
    private UInt256 u3 = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff03");
    private UInt256 u4 = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff04");
    private UInt256 u5 = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff05");

    private TransactionDemo t1 = new TransactionDemo(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01"));
    private TransactionDemo t2 = new TransactionDemo(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02"));
    private TransactionDemo t3 = new TransactionDemo(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff03"));
    private TransactionDemo t4 = new TransactionDemo(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff04"));
    private TransactionDemo t5 = new TransactionDemo(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff05"));

    @Test(expected = KeyNotFoundException.class)
    public void get() {
        RelayCache relayCache = new RelayCache(3);
        relayCache.add(t1);
        relayCache.add(t2);
        relayCache.add(t3);
        relayCache.add(t4);

        TransactionDemo t = (TransactionDemo) relayCache.get(u4);
        Assert.assertEquals(t4, t);

        Assert.assertEquals(3, relayCache.size());

        relayCache.get(u1);
    }

    @Test
    public void size() {
        RelayCache relayCache = new RelayCache(3);
        relayCache.add(t1);
        Assert.assertEquals(1, relayCache.size());
        relayCache.add(t2);
        Assert.assertEquals(2, relayCache.size());
        relayCache.add(t3);
        Assert.assertEquals(3, relayCache.size());
        relayCache.add(t4);
        Assert.assertEquals(3, relayCache.size());
    }

    @Test
    public void isEmpty() {

    }

    @Test
    public void contains() {

    }

    @Test
    public void containsKey() {

    }

    @Test
    public void containsValue() {

    }

    @Test
    public void iterator() {

    }

    @Test
    public void toArray() {

    }

    @Test
    public void toArray1() {
    }

    @Test
    public void add() {
    }

    @Test
    public void remove() {
    }

    @Test
    public void containsAll() {
    }

    @Test
    public void addAll() {
    }

    @Test
    public void removeAll() {
    }

    @Test
    public void retainAll() {
    }

    @Test
    public void clear() {

    }

    @Test
    public void tryGet() {

    }
}