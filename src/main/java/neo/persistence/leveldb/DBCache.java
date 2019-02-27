package neo.persistence.leveldb;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.ReadOptions;
import org.iq80.leveldb.WriteBatch;

import java.util.Collection;
import java.util.Map;

import neo.io.ICloneable;
import neo.io.ISerializable;
import neo.io.caching.DataCache;

public class DBCache<TKey extends ISerializable, TValue extends ICloneable<TValue> & ISerializable> extends DataCache<TKey, TValue> {

    private final DB db;
    private final ReadOptions options;
    private final WriteBatch batch;
    private final byte prefix;

    public DBCache(DB db, ReadOptions options, WriteBatch batch, byte prefix) {
        this.db = db;
        this.options = options;
        this.batch = batch;
        this.prefix = prefix;
    }

    @Override
    protected TValue getInternal(TKey key) {
        return null;
    }

    @Override
    protected void addInternal(TKey key, TValue value) {

    }

    @Override
    protected TValue tryGetInternal(TKey key) {
        return null;
    }

    @Override
    protected void updateInternal(TKey key, TValue value) {

    }

    @Override
    public void deleteInternal(TKey key) {

    }

    @Override
    protected Collection<Map.Entry<TKey, TValue>> findInternal(byte[] keyPrefix) {
        return null;
    }
}
