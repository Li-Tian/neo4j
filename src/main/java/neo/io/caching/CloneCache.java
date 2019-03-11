package neo.io.caching;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.AbstractMap.SimpleEntry;

import neo.io.ICloneable;
import neo.csharp.io.ISerializable;
import neo.log.notr.TR;

public class CloneCache<TKey extends ISerializable, TValue extends ICloneable<TValue> & ISerializable> extends DataCache<TKey, TValue> {

    private DataCache<TKey, TValue> innerCache;

    public CloneCache(DataCache<TKey, TValue> innerCache) {
        this.innerCache = innerCache;
    }

    @Override
    protected TValue getInternal(TKey key) {
        TR.enter();

        TValue value = innerCache.get(key);
        if (value == null) {
            return null;
        }
        return TR.exit(value.copy());
    }

    @Override
    protected void addInternal(TKey key, TValue value) {
        TR.enter();
        innerCache.add(key, value);
        TR.exit();
    }

    @Override
    protected TValue tryGetInternal(TKey key) {
        TR.enter();

        TValue value = innerCache.tryGet(key);
        if (value == null) {
            return TR.exit(null);
        }
        return TR.exit(value.copy());
    }

    @Override
    protected void updateInternal(TKey key, TValue value) {
        TR.enter();
        innerCache.getAndChange(key).fromReplica(value);
        TR.exit();
    }

    @Override
    public void deleteInternal(TKey key) {
        TR.enter();
        innerCache.delete(key);
        TR.exit();
    }

    @Override
    protected Collection<Map.Entry<TKey, TValue>> findInternal(byte[] keyPrefix) {
        TR.enter();

        Collection<Map.Entry<TKey, TValue>> collection = new ArrayList<>();

        for (Map.Entry<TKey, TValue> entry : innerCache.find(keyPrefix)) {
            collection.add(new SimpleEntry<>(entry.getKey(), entry.getValue().copy()));
        }
        return TR.exit(collection);
    }

}

