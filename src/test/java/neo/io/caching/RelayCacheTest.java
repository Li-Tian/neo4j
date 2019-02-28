package neo.io.caching;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;

import neo.UInt256;
import neo.csharp.Out;
import neo.exception.KeyNotFoundException;
import neo.network.p2p.payloads.IInventory;

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
        RelayCache relayCache = new RelayCache(3);
        Assert.assertTrue(relayCache.isEmpty());
        relayCache.add(t1);
        Assert.assertFalse(relayCache.isEmpty());
    }

    @Test
    public void contains() {
        RelayCache relayCache = new RelayCache(3);
        relayCache.add(t1);
        relayCache.add(t2);
        relayCache.add(t3);
        relayCache.add(t4);

        Assert.assertFalse(relayCache.contains(t1));
        Assert.assertTrue(relayCache.contains(t2));
    }

    @Test
    public void containsKey() {
        RelayCache relayCache = new RelayCache(3);
        relayCache.add(t1);
        relayCache.add(t2);
        relayCache.add(t3);
        relayCache.add(t4);

        Assert.assertFalse(relayCache.containsKey(u1));
        Assert.assertTrue(relayCache.containsKey(u2));
    }

    @Test
    public void containsValue() {
        RelayCache relayCache = new RelayCache(3);
        relayCache.add(t1);
        relayCache.add(t2);
        relayCache.add(t3);
        relayCache.add(t4);

        Assert.assertFalse(relayCache.containsValue(t1));
        Assert.assertTrue(relayCache.containsValue(t2));
    }

    @Test
    public void iterator() {
        RelayCache relayCache = new RelayCache(3);
        relayCache.add(t1);
        relayCache.add(t2);
        relayCache.add(t3);
        relayCache.add(t4);

        Iterator<IInventory> iterator = relayCache.iterator();
        int i = 0;
        while (iterator.hasNext()){
            IInventory inventory = iterator.next();
            if (i == 0) {
                Assert.assertEquals(t2, inventory);
            }
            if (i == 1) {
                Assert.assertEquals(t3, inventory);
            }
            if (i == 2) {
                Assert.assertEquals(t4, inventory);
            }
            i++;
        }
        Assert.assertEquals(3, i);
    }

    @Test
    public void toArray() {
        RelayCache relayCache = new RelayCache(3);
        relayCache.add(t1);
        relayCache.add(t2);
        relayCache.add(t3);
        relayCache.add(t4);

        Object[] items = relayCache.toArray();
        Assert.assertEquals(3, items.length);
        Assert.assertEquals(t2, items[0]);
        Assert.assertEquals(t3, items[1]);
        Assert.assertEquals(t4, items[2]);
    }

    @Test
    public void toArray1() {
        RelayCache relayCache = new RelayCache(3);
        relayCache.add(t1);
        relayCache.add(t2);
        relayCache.add(t3);
        relayCache.add(t4);

        Object[] items = new Object[3];
        items = relayCache.toArray(items);

        Assert.assertEquals(3, items.length);
        Assert.assertEquals(t2, items[0]);
        Assert.assertEquals(t3, items[1]);
        Assert.assertEquals(t4, items[2]);
    }

    @Test
    public void add() {
        RelayCache relayCache = new RelayCache(3);
        relayCache.add(t1);
        relayCache.add(t2);

        Object obj = relayCache.get(u1);
        Assert.assertEquals(t1, obj);
    }

    @Test(expected = KeyNotFoundException.class)
    public void remove() {
        RelayCache relayCache = new RelayCache(3);
        relayCache.add(t1);
        relayCache.add(t2);

        Object obj = relayCache.get(u1);
        Assert.assertEquals(t1, obj);

        relayCache.remove(t1);

        Out<IInventory> out = new Out<>();
        boolean success = relayCache.tryGet(u1,out);
        Assert.assertFalse(success);
        Assert.assertNull(out.get());

        relayCache.get(u1);
    }

    @Test
    public void containsAll() {
        RelayCache relayCache = new RelayCache(3);
        relayCache.add(t1);
        relayCache.add(t2);
        relayCache.add(t3);
        relayCache.add(t4);

        ArrayList<IInventory> list = new ArrayList<>();
        list.add(t2);
        Assert.assertTrue(relayCache.containsAll(list));
        list.add(t3);
        Assert.assertTrue(relayCache.containsAll(list));
        list.add(t4);
        Assert.assertTrue(relayCache.containsAll(list));
        list.add(t1);
        Assert.assertFalse(relayCache.containsAll(list));
    }

    @Test
    public void addAll() {
        RelayCache relayCache = new RelayCache(3);
        ArrayList<IInventory> list = new ArrayList<>();
        list.add(t1);
        list.add(t2);
        list.add(t3);
        list.add(t4);
        relayCache.addAll(list);

        Assert.assertEquals(3, relayCache.size());
    }

    @Test
    public void removeAll() {
        RelayCache relayCache = new RelayCache(3);
        relayCache.add(t1);
        relayCache.add(t2);
        relayCache.add(t3);
        relayCache.add(t4);

        ArrayList<IInventory> list = new ArrayList<>();
        list.add(t1);
        list.add(t2);

        relayCache.removeAll(list);
        Assert.assertEquals(2, relayCache.size());
    }

    @Test
    public void retainAll() {
        RelayCache relayCache = new RelayCache(3);
        relayCache.add(t1);
        relayCache.add(t2);
        relayCache.add(t3);
        relayCache.add(t4);

        ArrayList<IInventory> list = new ArrayList<>();
        list.add(t1);
        list.add(t2);

        relayCache.retainAll(list);
        Assert.assertEquals(2, relayCache.size());

        list.add(t3);
        list.add(t4);
        relayCache.retainAll(list);
        Assert.assertEquals(3, relayCache.size());
    }

    @Test
    public void clear() {
        RelayCache relayCache = new RelayCache(3);
        relayCache.add(t1);
        relayCache.add(t2);
        relayCache.add(t3);
        relayCache.add(t4);

        relayCache.clear();
        Assert.assertTrue(relayCache.isEmpty());
    }

    @Test
    public void tryGet() {
        RelayCache relayCache = new RelayCache(3);
        relayCache.add(t1);
        relayCache.add(t2);
        relayCache.add(t3);
        relayCache.add(t4);

        Out<IInventory> out = new Out<>();
        boolean success = relayCache.tryGet(u1,out);
        Assert.assertFalse(success);
        Assert.assertNull(out.get());

        out = new Out<>();
        success = relayCache.tryGet(u2,out);
        Assert.assertTrue(success);
        Assert.assertNotNull(out.get());
        Assert.assertEquals(t2, out.get());
    }
}