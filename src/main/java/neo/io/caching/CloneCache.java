package neo.io.caching;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.AbstractMap.SimpleEntry;

import neo.io.ICloneable;
import neo.io.ISerializable;

public class CloneCache<TKey extends ISerializable, TValue extends  ICloneable<TValue> & ISerializable> extends DataCache<TKey, TValue> {

    private DataCache<TKey, TValue> innerCache;

    public CloneCache(DataCache<TKey, TValue> innerCache) {
        this.innerCache = innerCache;
    }

    @Override
    protected TValue getInternal(TKey key) {
        return innerCache.get(key).copy();
    }

    @Override
    protected void addInternal(TKey key, TValue value) {
        innerCache.add(key, value);
    }

    @Override
    protected TValue tryGetInternal(TKey key) {
        TValue value = innerCache.tryGet(key);
        if (value == null) {
            return null;
        }
        return value.copy();
    }

    @Override
    protected void updateInternal(TKey key, TValue value) {
        innerCache.getAndChange(key).fromReplica(value);
    }

    @Override
    public void deleteInternal(TKey key) {
        innerCache.delete(key);
    }

    @Override
    protected Collection<Map.Entry<TKey, TValue>> findInternal(byte[] keyPrefix) {
        Collection<Map.Entry<TKey, TValue>> collection = new ArrayList<>();

        for (Map.Entry<TKey, TValue> entry : innerCache.find(keyPrefix)) {
            collection.add(new SimpleEntry<>(entry.getKey(), entry.getValue().copy()));
        }
        return collection;
    }
}

