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
import neo.io.ISerializable;
import neo.io.SerializeHelper;
import neo.io.caching.DataCache;

public class DBCache<TKey extends ISerializable, TValue extends ICloneable<TValue> & ISerializable> extends DataCache<TKey, TValue> {

    private final DB db;
    private final ReadOptions options;
    private final WriteBatch batch;
    private final byte prefix;
    private final Supplier<TKey> keyGenerator;
    private final Supplier<TValue> valueGenerator;

    public DBCache(DB db, ReadOptions options, WriteBatch batch, byte prefix, Supplier<TKey> keyGenerator, Supplier<TValue> valueGenerator) {
        this.db = db;
        this.options = options;
        this.batch = batch;
        this.prefix = prefix;
        this.keyGenerator = keyGenerator;
        this.valueGenerator = valueGenerator;
    }

    @Override
    protected TValue getInternal(TKey key) {
        byte[] bytes = BitConverter.merge(prefix, SerializeHelper.toBytes(key));
        byte[] value = db.get(bytes, options);
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
        DBIterator iterator = db.iterator();
        iterator.seek(keyPrefix);
        ArrayList<Map.Entry<TKey, TValue>> list = new ArrayList<>();

        while (iterator.hasNext()) {
            Map.Entry<byte[], byte[]> entry = iterator.next();
            TKey key = SerializeHelper.parse(keyGenerator, entry.getValue());
            TValue value = SerializeHelper.parse(valueGenerator, entry.getValue());
            list.add(new AbstractMap.SimpleEntry<>(key, value));
        }
        return list;
    }
}
