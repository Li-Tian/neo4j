package neo.persistence.leveldb;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.ReadOptions;
import org.iq80.leveldb.WriteBatch;

import neo.function.FuncVoid2T;
import neo.io.ICloneable;
import neo.io.ISerializable;
import neo.io.caching.MetaDataCache;

public class DbMetaDataCache<T extends ISerializable & ICloneable<T>> extends MetaDataCache<T> {

    private final DB db;
    private final ReadOptions options;
    private final WriteBatch batch;
    private final byte prefix;

    protected DbMetaDataCache(DB db, ReadOptions options, WriteBatch batch, byte prefix, FuncVoid2T<T> factory) {
        super(factory);

        this.db = db;
        this.options = options;
        this.batch = batch;
        this.prefix = prefix;
    }

    @Override
    protected void addInternal(T item) {
        //
    }

    @Override
    protected T tryGetInternal() {
        return null;
    }

    @Override
    protected void updateInternal(T item) {

    }
}
