package neo.io.caching;

import org.junit.Assert;
import org.junit.Test;

import neo.UInt256;


public class CloneMetaCacheTest {

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


    @Test
    public void commit() {
        TestMetaDataCache metaDataCache = new TestMetaDataCache();
        CloneMetaCache<TransactionDemo> cloneMetaCache = new CloneMetaCache<>(metaDataCache);
        cloneMetaCache.get();
        cloneMetaCache.commit();
        TransactionDemo t = cloneMetaCache.get();
        Assert.assertNull(t);
    }

    @Test
    public void createSnapshot() {
        TestMetaDataCache metaDataCache = new TestMetaDataCache();
        MetaDataCache<TransactionDemo> cache = metaDataCache.createSnapshot();
        Assert.assertNotNull(cache);
    }

    @Test
    public void get() {
        TestMetaDataCache metaDataCache = new TestMetaDataCache();
        CloneMetaCache<TransactionDemo> cloneMetaCache = new CloneMetaCache<>(metaDataCache);
        cloneMetaCache.get();
        cloneMetaCache.commit();
        TransactionDemo t = cloneMetaCache.get();
        Assert.assertNull(t);
    }

    @Test
    public void getAndChange() {
        TestMetaDataCache metaDataCache = new TestMetaDataCache();
        CloneMetaCache<TransactionDemo> cloneMetaCache = new CloneMetaCache<>(metaDataCache);
        cloneMetaCache.get();
        cloneMetaCache.commit();
        TransactionDemo t = cloneMetaCache.get();
        Assert.assertNull(t);

        metaDataCache = new TestMetaDataCache(() -> t1);
        cloneMetaCache = (CloneMetaCache<TransactionDemo>) metaDataCache.createSnapshot();
        cloneMetaCache.get();
        cloneMetaCache.commit();
        t = cloneMetaCache.getAndChange();
        cloneMetaCache.commit();

        Assert.assertEquals(t1.hash().toString(), t.hash().toString());
    }
}