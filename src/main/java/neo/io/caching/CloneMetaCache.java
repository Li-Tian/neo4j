package neo.io.caching;

import neo.io.ICloneable;
import neo.io.ISerializable;

public class CloneMetaCache<T extends ICloneable<T> & ISerializable> extends MetaDataCache<T> {

    private MetaDataCache<T> innerCache;

    protected CloneMetaCache(MetaDataCache<T> innerCache) {
        super(null);
        this.innerCache = innerCache;
    }

    @Override
    protected void addInternal(T item) {

    }

    @Override
    protected T tryGetInternal() {
        return innerCache.get().copy();
    }

    @Override
    protected void updateInternal(T item) {
        innerCache.getAndChange().fromReplica(item);
    }
}
