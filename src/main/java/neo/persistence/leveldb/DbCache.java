package neo.persistence.leveldb;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.ReadOptions;
import org.iq80.leveldb.WriteBatch;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

import neo.csharp.BitConverter;
import neo.io.ICloneable;
import neo.csharp.io.ISerializable;
import neo.io.SerializeHelper;
import neo.io.caching.DataCache;

/**
 * Table cache
 *
 * @param <TKey>   table prefix
 * @param <TValue> column value type
 */
public class DbCache<TKey extends ISerializable, TValue extends ICloneable<TValue> & ISerializable> extends DataCache<TKey, TValue> {

    private final DB db;
    private final ReadOptions options;
    private final WriteBatch batch;
    private final byte prefix;
    private final Supplier<TKey> keyGenerator;
    private final Supplier<TValue> valueGenerator;

    /**
     * DBCache constructor
     *
     * @param db             leveldb
     * @param options        read options
     * @param batch          write batch
     * @param prefix         table prefix
     * @param keyGenerator   key generator
     * @param valueGenerator value generator
     */
    public DbCache(DB db, ReadOptions options, WriteBatch batch, byte prefix, Supplier<TKey> keyGenerator, Supplier<TValue> valueGenerator) {
        this.db = db;
        this.options = options;
        this.batch = batch;
        this.prefix = prefix;
        this.keyGenerator = keyGenerator;
        this.valueGenerator = valueGenerator;
    }

    /**
     * get from internal
     *
     * @param key query key
     * @return TValue
     */
    @Override
    protected TValue getInternal(TKey key) {
        byte[] bytes = BitConverter.merge(prefix, SerializeHelper.toBytes(key));
        byte[] value = options == null ? db.get(bytes) : db.get(bytes, options);

        if (value == null || value.length == 0) {
            return null;
        }
        return SerializeHelper.parse(valueGenerator, value);
    }

    @Override
    protected void addInternal(TKey key, TValue value) {
        byte[] bytes = BitConverter.merge(prefix, SerializeHelper.toBytes(key));
        batch.put(bytes, SerializeHelper.toBytes(value));
    }

    @Override
    protected TValue tryGetInternal(TKey key) {
        return getInternal(key);
    }

    @Override
    protected void updateInternal(TKey key, TValue value) {
        addInternal(key, value);
    }

    @Override
    public void deleteInternal(TKey key) {
        byte[] bytes = BitConverter.merge(prefix, SerializeHelper.toBytes(key));
        db.delete(bytes);
    }

    @Override
    protected Collection<Map.Entry<TKey, TValue>> findInternal(byte[] keyPrefix) {
        byte[] keyBytes = BitConverter.merge(prefix, keyPrefix);

        // C# code
        //  db.Find(options, SliceBuilder.Begin(prefix).Add(key_prefix),
        //            (k, v) => new KeyValuePair<TKey, TValue>(k.ToArray().AsSerializable<TKey>(1),
        //            v.ToArray().AsSerializable<TValue>()));
        // be careful with the prefix!
        return DBHelper.find(db, keyBytes, (key, value) ->
                new AbstractMap.SimpleEntry<>(SerializeHelper.parse(keyGenerator, key, 1),
                        SerializeHelper.parse(valueGenerator, value)));
    }
}
