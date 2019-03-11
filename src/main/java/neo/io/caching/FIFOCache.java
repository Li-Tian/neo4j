package neo.io.caching;

import neo.log.notr.TR;

public abstract class FIFOCache<TKey, TValue> extends Cache<TKey, TValue> {

    public FIFOCache(int maxCapacity) {
        super(maxCapacity);
    }

    @Override
    protected void onAccess(CacheItem item) {
        TR.enter();
        TR.exit();
    }
}
