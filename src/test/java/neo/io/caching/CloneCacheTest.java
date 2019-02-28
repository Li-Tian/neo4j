package neo.io.caching;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.Map;

import neo.UInt256;
import neo.exception.KeyNotFoundException;

public class CloneCacheTest {

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


    @Test()
    public void get() {
        CloneCache<UInt256, TransactionDemo> cloneCache = new CloneCache<UInt256, TransactionDemo>(new DataCacheTest<>());
        cloneCache.add(t1.hash(), t1);

        TransactionDemo t = cloneCache.get(t1.hash());
        Assert.assertEquals(t1, t);
        t = cloneCache.get(t2.hash());
        Assert.assertNull(t);
    }

    @Test
    public void add() {
        CloneCache<UInt256, TransactionDemo> cloneCache = new CloneCache<UInt256, TransactionDemo>(new DataCacheTest<>());
        cloneCache.add(t1.hash(), t1);
        cloneCache.add(t2.hash(), t2);

        TransactionDemo t = cloneCache.get(t1.hash());
        Assert.assertEquals(t1, t);
        t = cloneCache.get(t2.hash());
        Assert.assertEquals(t2, t);
    }

    @Test
    public void commit() {
        DataCacheTest<UInt256, TransactionDemo> TestDataCache = new DataCacheTest<>();
//        CloneCache<UInt256, TransactionDemo> cloneCache = new CloneCache<UInt256, TransactionDemo>(DataCacheTest);
        TestDataCache.add(t1.hash(), t1);
        TestDataCache.add(t2.hash(), t2);
        TestDataCache.add(t3.hash(), t3);
        TestDataCache.add(t4.hash(), t4);

        Assert.assertTrue(TestDataCache.getOriginCache().isEmpty());
        TestDataCache.commit();
        Assert.assertEquals(4, TestDataCache.getOriginCache().size());
        TestDataCache.delete(t2.hash());
        TestDataCache.commit();
        Assert.assertEquals(3, TestDataCache.getOriginCache().size());
    }

    @Test
    public void createSnapshot() {
    }

    @Test(expected = KeyNotFoundException.class)
    public void delete() {
        DataCacheTest<UInt256, TransactionDemo> TestDataCache = new DataCacheTest<>();
//        CloneCache<UInt256, TransactionDemo> cloneCache = new CloneCache<UInt256, TransactionDemo>(DataCacheTest);
        TestDataCache.add(t1.hash(), t1);
        TestDataCache.add(t2.hash(), t2);

        TransactionDemo t = TestDataCache.get(t1.hash());
        Assert.assertEquals(t1, t);
        t = TestDataCache.get(t2.hash());
        Assert.assertEquals(t2, t);
        TestDataCache.commit();
        TestDataCache.delete(t2.hash());
        TestDataCache.get(t2.hash());
    }

    @Test
    public void deleteWhere() {
        DataCacheTest<UInt256, TransactionDemo> TestDataCache = new DataCacheTest<>();
        CloneCache<UInt256, TransactionDemo> cloneCache = new CloneCache<UInt256, TransactionDemo>(TestDataCache);
        cloneCache.add(t1.hash(), t1);
        cloneCache.add(t2.hash(), t2);
        cloneCache.add(t3.hash(), t3);
        cloneCache.add(t4.hash(), t4);

        cloneCache.deleteWhere((key, value) -> key.compareTo(t3.hash()) <= 0);
        cloneCache.commit();

        TransactionDemo t = cloneCache.get(t1.hash());
        Assert.assertNull(t);
        t = cloneCache.get(t2.hash());
        Assert.assertNull(t);
        t = cloneCache.get(t3.hash());
        Assert.assertNull(t);
        t = cloneCache.get(t4.hash());
        Assert.assertEquals(t4, t);
    }

    @Test
    public void find() {
        DataCacheTest<UInt256, TransactionDemo> TestDataCache = new DataCacheTest<>();
        CloneCache<UInt256, TransactionDemo> cloneCache = new CloneCache<UInt256, TransactionDemo>(TestDataCache);
        cloneCache.add(t1.hash(), t1);
        cloneCache.add(t2.hash(), t2);
        cloneCache.add(t3.hash(), t3);
        cloneCache.add(t4.hash(), t4);

        Collection<Map.Entry<UInt256, TransactionDemo>> collection = cloneCache.find("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00".getBytes());
        Assert.assertEquals(4, collection.size());
    }

    @Test
    public void getChangeSet() {
        DataCacheTest<UInt256, TransactionDemo> TestDataCache = new DataCacheTest<>();
        CloneCache<UInt256, TransactionDemo> cloneCache = new CloneCache<UInt256, TransactionDemo>(TestDataCache);
        cloneCache.add(t1.hash(), t1);
        cloneCache.add(t2.hash(), t2);
        cloneCache.add(t3.hash(), t3);
        cloneCache.add(t4.hash(), t4);

        Collection<DataCache<UInt256, TransactionDemo>.Trackable> collection = cloneCache.getChangeSet();
        Assert.assertEquals(4, collection.size());
        cloneCache.commit();
        collection = cloneCache.getChangeSet();
        Assert.assertEquals(0, collection.size());
    }

    @Test
    public void getAndChange() {
        DataCacheTest<UInt256, TransactionDemo> TestDataCache = new DataCacheTest<>();
        CloneCache<UInt256, TransactionDemo> cloneCache = new CloneCache<UInt256, TransactionDemo>(TestDataCache);
        cloneCache.add(t1.hash(), t1);
        cloneCache.add(t2.hash(), t2);

        TransactionDemo t = cloneCache.getAndChange(t1.hash());
        Assert.assertEquals(t1, t);
        cloneCache.getAndChange(t3.hash(), () -> t2);
        t = cloneCache.getAndChange(t3.hash());
        Assert.assertEquals(t2, t);
    }

    @Test(expected = KeyNotFoundException.class)
    public void getAndChange1() {
        DataCacheTest<UInt256, TransactionDemo> TestDataCache = new DataCacheTest<>();
        CloneCache<UInt256, TransactionDemo> cloneCache = new CloneCache<UInt256, TransactionDemo>(TestDataCache);
        cloneCache.add(t1.hash(), t1);
        cloneCache.add(t2.hash(), t2);

        TransactionDemo t = cloneCache.getAndChange(t1.hash());
        Assert.assertEquals(t1, t);
        cloneCache.getAndChange(t3.hash());
    }

    @Test
    public void getOrAdd() {
        DataCacheTest<UInt256, TransactionDemo> TestDataCache = new DataCacheTest<>();
        CloneCache<UInt256, TransactionDemo> cloneCache = new CloneCache<UInt256, TransactionDemo>(TestDataCache);
        cloneCache.add(t1.hash(), t1);
        cloneCache.add(t2.hash(), t2);

        TransactionDemo t = cloneCache.getAndChange(t1.hash());
        Assert.assertEquals(t1, t);
        cloneCache.getOrAdd(t3.hash(), () -> t3);
        t = cloneCache.getAndChange(t3.hash());
        Assert.assertEquals(t3, t);
    }

    @Test
    public void tryGet() {
        DataCacheTest<UInt256, TransactionDemo> TestDataCache = new DataCacheTest<>();
        CloneCache<UInt256, TransactionDemo> cloneCache = new CloneCache<>(TestDataCache);
        cloneCache.add(t1.hash(), t1);
        cloneCache.add(t2.hash(), t2);
        cloneCache.commit();

        TransactionDemo t = cloneCache.tryGet(t1.hash());
        Assert.assertEquals(t1, t);
        t = cloneCache.tryGet(t3.hash());
        Assert.assertNull(t);

        DataCache<UInt256, TransactionDemo> cache = TestDataCache.createSnapshot();
        Assert.assertNotNull(cache);

        cloneCache.delete(t3.hash());
        cloneCache.delete(t2.hash());
        cloneCache.add(t2.hash(), t3);
        cloneCache.commit();
        cloneCache.delete(t1.hash());
        cloneCache.commit();
        t = cloneCache.tryGet(t3.hash());
        Assert.assertNull(t);
    }
}