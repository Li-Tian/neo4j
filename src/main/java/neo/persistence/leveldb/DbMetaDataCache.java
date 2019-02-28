package neo.persistence.leveldb;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.ReadOptions;
import org.iq80.leveldb.WriteBatch;

import java.util.function.Supplier;

import neo.io.ICloneable;
import neo.io.ISerializable;
import neo.io.SerializeHelper;
import neo.io.caching.MetaDataCache;

public class DbMetaDataCache<T extends ISerializable & ICloneable<T>> extends MetaDataCache<T> {

    private final DB db;
    private final ReadOptions options;
    private final WriteBatch batch;
    private final byte prefix;
    private final Supplier<T> valueGenerator;

    protected DbMetaDataCache(DB db, ReadOptions options, WriteBatch batch, byte prefix, Supplier<T> valueGenerator) {
        super(valueGenerator);

        this.db = db;
        this.options = options;
        this.batch = batch;
        this.prefix = prefix;
        this.valueGenerator = valueGenerator;
    }

    @Override
    protected void addInternal(T item) {
        byte[] bytes = new byte[]{prefix};
        batch.put(bytes, SerializeHelper.toBytes(item));
    }

    @Override
    protected T tryGetInternal() {
        byte[] bytes = new byte[]{prefix};
        byte[] value = db.get(bytes, options);
        return SerializeHelper.parse(valueGenerator, value);
    }

    @Override
    protected void updateInternal(T item) {
        addInternal(item);
    }
}
