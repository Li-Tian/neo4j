package neo.io.caching;

import neo.io.ICloneable;
import neo.io.ISerializable;
import neo.log.tr.TR;

public class CloneMetaCache<T extends ICloneable<T> & ISerializable> extends MetaDataCache<T> {

    private MetaDataCache<T> innerCache;

    public CloneMetaCache(MetaDataCache<T> innerCache) {
        super(null);
        this.innerCache = innerCache;
    }

    @Override
    protected void addInternal(T item) {
        TR.enter();
        TR.exit();
    }

    @Override
    protected T tryGetInternal() {
        TR.enter();
        T t = innerCache.get();
        if (t == null) {
            return TR.exit(null);
        }

        return TR.exit(t.copy());
    }

    @Override
    protected void updateInternal(T item) {
        TR.enter();
        innerCache.getAndChange().fromReplica(item);
        TR.exit();
    }
}
