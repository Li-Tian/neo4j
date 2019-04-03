package neo.io.caching;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import neo.csharp.BitConverter;
import neo.exception.KeyAlreadyExistException;
import neo.exception.KeyNotFoundException;
import neo.io.ICloneable;
import neo.csharp.io.ISerializable;
import neo.io.SerializeHelper;
import neo.log.notr.TR;

import static neo.io.caching.TrackState.ADDED;
import static neo.io.caching.TrackState.CHANGED;
import static neo.io.caching.TrackState.DELETED;
import static neo.io.caching.TrackState.NONE;

public abstract class DataCache<TKey extends ISerializable, TValue extends ICloneable<TValue> & ISerializable> {

    public class Trackable {
        public TKey key;
        public TValue item;
        public TrackState state;

        public Trackable(TKey key, TValue value, TrackState state) {
            this.key = key;
            this.item = value;
            this.state = state;
        }
    }

    private final ConcurrentHashMap<TKey, Trackable> map = new ConcurrentHashMap<>();

    /**
     * get value by key
     *
     * @throws KeyNotFoundException if key is deleted in the datacache
     */
    public TValue get(TKey key) {
        TR.enter();

        Trackable trackable;
        if (map.containsKey(key)) {
            trackable = map.get(key);
            if (trackable.state == DELETED) {
                throw new KeyNotFoundException();
            }
        } else {
            trackable = new Trackable(key, getInternal(key), NONE);
            map.put(key, trackable);

        }
        return TR.exit(trackable.item);
    }

    public void add(TKey key, TValue value) {
        TR.enter();

        if (map.containsKey(key)) {
            Trackable trackable = map.get(key);
            if (trackable.state != DELETED) {
                throw new KeyAlreadyExistException();
            }
            trackable.item = value;
            trackable.state = CHANGED;
        } else {
            map.put(key, new Trackable(key, value, ADDED));
        }
        TR.exit();
    }


    public void commit() {
        TR.enter();

        for (Trackable trackable : map.values()) {
            switch (trackable.state) {
                case ADDED:
                    addInternal(trackable.key, trackable.item);
                    trackable.state = NONE; // C# 这里没有. 这里补加的，防止C#中的 add -> commit -> delete -> commit 失效
                    break;
                case CHANGED:
                    updateInternal(trackable.key, trackable.item);
                    trackable.state = NONE;// C# 这里没有. 这里补加的，防止C#中的 add -> commit -> delete -> commit 失效
                    break;
                case DELETED:
                    deleteInternal(trackable.key);
                    map.remove(trackable.key);
                    break;
                default:
                    break;
            }
        }
        TR.exit();
    }

    public DataCache<TKey, TValue> createSnapshot() {
        TR.enter();
        return TR.exit(new CloneCache<>(this));
    }

    public void delete(TKey key) {
        TR.enter();
        if (map.containsKey(key)) {
            Trackable trackable = map.get(key);
            if (trackable.state == ADDED) {
                map.remove(key);
            } else {
                trackable.state = DELETED;
            }
        } else {
            TValue item = tryGetInternal(key);
            if (item == null) {
                return;
            }
            map.put(key, new Trackable(key, item, DELETED));
        }
        TR.exit();
    }


    public void deleteWhere(BiPredicate<TKey, TValue> predicate) {
        TR.enter();

        /*
        C# deleteWhere
        foreach(Trackable trackable in dictionary
                .Where(p = > p.Value.State != Deleted && predicate(p.Key, p.Value.Item)).Select(p = > p.Value))
        trackable.State = Deleted;
        */
        for (Map.Entry<TKey, Trackable> entry : map.entrySet()) {
            if (predicate.test(entry.getKey(), entry.getValue().item)) {
                map.remove(entry.getKey());
            }
        }
        TR.exit();
    }

    public Collection<Map.Entry<TKey, TValue>> find() {
        return find(new byte[0]);
    }


    public Collection<Map.Entry<TKey, TValue>> find(byte[] keyPrefix) {
        TR.enter();

        Collection<Map.Entry<TKey, TValue>> results = new ArrayList<>();

        Collection<Map.Entry<TKey, TValue>> c = findInternal(keyPrefix);
        for (Map.Entry<TKey, TValue> entry : c) {
            if (!map.containsKey(entry.getKey())) {
                results.add(entry);
            }
        }

        for (Map.Entry<TKey, Trackable> entry : map.entrySet()) {
            Trackable trackable = entry.getValue();
            if (trackable.state != DELETED && (keyPrefix == null || BitConverter.startWith(SerializeHelper.toBytes(entry.getKey()), keyPrefix))) {
                results.add(new AbstractMap.SimpleEntry<>(entry.getKey(), trackable.item));
            }
        }
        return TR.exit(results);
    }


    public Collection<Trackable> getChangeSet() {
        TR.enter();

        /*
        C# code
        foreach(Trackable trackable in dictionary.Values.Where(p = > p.State != None))
        yield return trackable;
        */
        Collection<Trackable> collection = map.values().stream().filter(p -> p.state != NONE).collect(Collectors.toList());
        return TR.exit(collection);
    }


    public TValue getAndChange(TKey key) {
        TR.enter();

        return TR.exit(getAndChange(key, null));
    }

    /**
     * get value by key, and add a new one if not exist
     */
    public TValue getAndChange(TKey key, Supplier<TValue> factory) {
        TR.enter();

        Trackable trackable;
        if (map.containsKey(key)) {
            trackable = map.get(key);
            if (trackable.state == DELETED) {
                if (factory == null) {
                    throw new KeyNotFoundException();
                }
                trackable.item = factory.get();
                trackable.state = CHANGED;
            } else if (trackable.state == NONE) {
                trackable.state = CHANGED;
            }
        } else {
            trackable = new Trackable(key, tryGetInternal(key), CHANGED);
            if (trackable.item == null) {
                if (factory == null) {
                    throw new KeyNotFoundException();
                }
                trackable.item = factory.get();
                trackable.state = ADDED;
            }
            map.put(key, trackable);
        }
        return TR.exit(trackable.item);
    }

    public TValue getOrAdd(TKey key, Supplier<TValue> factory) {
        TR.enter();

        Trackable trackable;
        if (map.containsKey(key)) {
            trackable = map.get(key);
            if (trackable.state == DELETED) {
                trackable.item = factory.get();
                trackable.state = CHANGED;
            }
        } else {
            trackable = new Trackable(key, tryGetInternal(key), NONE);
            if (trackable.item == null) {
                trackable.item = factory.get();
                trackable.state = ADDED;
            }
            map.put(key, trackable);
        }
        return TR.exit(trackable.item);
    }

    public TValue tryGet(TKey key) {
        TR.enter();
        if (key == null) {
            return TR.exit(null);
        }

        if (map.containsKey(key)) {
            Trackable trackable = map.get(key);

            if (trackable.state == DELETED) {
                return TR.exit(null);
            }
            return TR.exit(trackable.item);
        }

        TValue value = tryGetInternal(key);
        if (value == null) {
            return TR.exit(null);
        }
        map.put(key, new Trackable(key, value, NONE));
        return TR.exit(value);
    }

    protected abstract TValue getInternal(TKey key);

    protected abstract void addInternal(TKey key, TValue value);

    protected abstract TValue tryGetInternal(TKey key);

    protected abstract void updateInternal(TKey key, TValue value);

    public abstract void deleteInternal(TKey key);

    protected abstract Collection<Map.Entry<TKey, TValue>> findInternal(byte[] keyPrefix);
}
