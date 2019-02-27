package neo.io.caching;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import neo.io.ICloneable;
import neo.io.ISerializable;

public class TestDataCache<TKey extends ISerializable, TValue extends ICloneable<TValue> & ISerializable> extends DataCache<TKey, TValue> {

    private HashMap<TKey, TValue> cache = new HashMap<>();

    public HashMap<TKey, TValue> getOriginCache(){
        return cache;
    }

    @Override
    protected TValue getInternal(TKey key) {
        return cache.get(key);
    }

    @Override
    protected void addInternal(TKey key, TValue value) {
        cache.put(key, value);
    }

    @Override
    protected TValue tryGetInternal(TKey key) {
        return cache.get(key);
    }

    @Override
    protected void updateInternal(TKey key, TValue value) {
        cache.put(key, value);
    }

    @Override
    public void deleteInternal(TKey key) {
        cache.remove(key);
    }

    @Override
    protected Collection<Map.Entry<TKey, TValue>> findInternal(byte[] keyPrefix) {
        ArrayList<Map.Entry<TKey,TValue>> collection = new ArrayList<>();
        for (Map.Entry<TKey, TValue> entry: cache.entrySet()){
            if (entry.getKey().toString().startsWith(new String(keyPrefix))){
                collection.add(entry);
            }
        }
        return collection;
    }
}
