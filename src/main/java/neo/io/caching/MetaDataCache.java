package neo.io.caching;

import neo.function.FuncVoid2T;
import neo.io.ICloneable;
import neo.io.ISerializable;

public abstract class MetaDataCache<T extends ISerializable & ICloneable<T>> {

    private T item;
    private TrackState state;
    private final FuncVoid2T<T> factory;

    protected abstract void addInternal(T item);

    protected abstract T tryGetInternal();

    protected abstract void updateInternal(T item);

    protected MetaDataCache(FuncVoid2T<T> factory) {
        this.factory = factory;
    }

    public void commit() {
        switch (state) {
            case ADDED:
                addInternal(item);
                break;
            case CHANGED:
                updateInternal(item);
                break;
        }
    }

    public MetaDataCache<T> CreateSnapshot() {
        return new CloneMetaCache<T>(this);
    }

    public T get() {
        if (item == null) {
            item = tryGetInternal();
        }
        if (item == null) {
            item = factory == null ? null : factory.gen();
            state = TrackState.ADDED;
        }
        return item;
    }

    public T getAndChange() {
        T item = get();
        if (state == TrackState.NONE)
            state = TrackState.CHANGED;
        return item;
    }

}
