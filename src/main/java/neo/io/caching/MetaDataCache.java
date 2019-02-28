package neo.io.caching;

import java.util.function.Supplier;

import neo.io.ICloneable;
import neo.csharp.io.ISerializable;
import neo.log.tr.TR;

public abstract class MetaDataCache<T extends ISerializable & ICloneable<T>> {

    private T item;
    private TrackState state;
    private final Supplier<T> factory;

    protected abstract void addInternal(T item);

    protected abstract T tryGetInternal();

    protected abstract void updateInternal(T item);

    protected MetaDataCache(Supplier<T> factory) {
        this.factory = factory;
    }

    public void commit() {
        TR.enter();
        if (state == null) {
            return;
        }

        switch (state) {
            case ADDED:
                addInternal(item);
                state = TrackState.NONE;
                break;
            case CHANGED:
                updateInternal(item);
                state = TrackState.NONE;
                break;
            default:
                break;
        }
        TR.exit();
    }

    public MetaDataCache<T> createSnapshot() {
        TR.enter();
        return TR.exit(new CloneMetaCache<T>(this));
    }

    public T get() {
        TR.enter();
        if (item == null) {
            item = tryGetInternal();
        }
        if (item == null) {
            item = factory == null ? null : factory.get();
            state = TrackState.ADDED;
        }
        return TR.exit(item);
    }

    public T getAndChange() {
        TR.enter();
        T item = get();
        if (item != null && (state == null || state == TrackState.NONE)) {
            state = TrackState.CHANGED;
        }
        return TR.exit(item);
    }

}
