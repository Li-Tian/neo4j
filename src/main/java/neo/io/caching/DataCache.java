package neo.io.caching;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import neo.exception.KeyAlreadyExistException;
import neo.exception.KeyNotFoundException;
import neo.function.FuncAB2T;
import neo.function.FuncVoid2T;
import neo.io.ICloneable;
import neo.io.ISerializable;

import static neo.io.caching.TrackState.ADDED;
import static neo.io.caching.TrackState.CHANGED;
import static neo.io.caching.TrackState.DELETED;
import static neo.io.caching.TrackState.NONE;

public abstract class DataCache<TKey extends ISerializable, TValue extends  ICloneable<TValue> & ISerializable> {

    public class Trackable {
        public TKey key;
        public TValue item;
        public TrackState state;

        public Trackable() {
        }

        public Trackable(TKey key, TValue value, TrackState state) {
            this.key = key;
            this.item = value;
            this.state = state;
        }
    }

    private final ConcurrentHashMap<TKey, Trackable> map = new ConcurrentHashMap<>();

    public TValue get(TKey key) {
        if (map.containsKey(key)) {
            Trackable trackable = map.get(key);
            if (trackable.state == DELETED) {
                throw new KeyNotFoundException();
            }
            return trackable.item;
        } else {
            Trackable trackable = new Trackable(key, getInternal(key), NONE);
            map.put(key, trackable);
            return trackable.item;
        }
    }

    public void add(TKey key, TValue value) {
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
    }


    public void commit() {
        for (Trackable trackable : map.values()) {
            switch (trackable.state) {
                case ADDED:
                    addInternal(trackable.key, trackable.item);
                case CHANGED:
                    updateInternal(trackable.key, trackable.item);
                case DELETED:
                    deleteInternal(trackable.key);
                default:
                    ;
            }
        }
    }

    public DataCache<TKey, TValue> CreateSnapshot() {
        return new CloneCache<>(this);
    }

    public void delete(TKey key) {
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
    }


    public void deleteWhere(FuncAB2T<TKey, TValue, Boolean> predicate) {
        /*
        C# deleteWhere
        foreach(Trackable trackable in dictionary
                .Where(p = > p.Value.State != Deleted && predicate(p.Key, p.Value.Item)).Select(p = > p.Value))
        trackable.State = Deleted;
        */
        for (Map.Entry<TKey, Trackable> entry : map.entrySet()) {
            if (predicate.gen(entry.getKey(), entry.getValue().item)) {
                map.remove(entry.getKey());
            }
        }
    }

    public Collection<Map.Entry<TKey, TValue>> find(byte[] keyPrefix) {
        Collection<Map.Entry<TKey, TValue>> results = new ArrayList<>();

        Collection<Map.Entry<TKey, TValue>> c = findInternal(keyPrefix);
        for (Map.Entry<TKey, TValue> entry : c) {
            if (!map.containsKey(entry.getKey())) {
                results.add(entry);
            }
        }

        for (Map.Entry<TKey, Trackable> entry : map.entrySet()) {
            Trackable trackable = entry.getValue();
            if (trackable.state != DELETED && (keyPrefix != null && entry.getKey().toString().startsWith(new String(keyPrefix)))) {
                results.add(new AbstractMap.SimpleEntry<>(entry.getKey(), trackable.item));
            }
        }
        return results;
    }


    public Collection<Trackable> getChangeSet() {
        /*
        C# code
        foreach(Trackable trackable in dictionary.Values.Where(p = > p.State != None))
        yield return trackable;
        */
        return map.values().stream().filter(p -> p.state != NONE).collect(Collectors.toList());
    }


    public TValue getAndChange(TKey key) {
        return getAndChange(key, null);
    }

    public TValue getAndChange(TKey key, FuncVoid2T<TValue> factory) {
        Trackable trackable;
        if (map.containsKey(key)) {
            trackable = map.get(key);
            if (trackable.state == DELETED) {
                if (factory == null) {
                    throw new KeyNotFoundException();
                }
                trackable.item = factory.gen();
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
                trackable.item = factory.gen();
                trackable.state = ADDED;
            }
            map.put(key, trackable);
        }
        return trackable.item;
    }

    public TValue getOrAdd(TKey key, FuncVoid2T<TValue> factory) {
        Trackable trackable;
        if (map.containsKey(key)) {
            trackable = map.get(key);
            if (trackable.state == DELETED) {
                trackable.item = factory.gen();
                trackable.state = CHANGED;
            }
        } else {
            trackable = new Trackable(key, tryGetInternal(key), NONE);
            if (trackable.item == null) {
                trackable.item = factory.gen();
                trackable.state = ADDED;
            }
            map.put(key, trackable);
        }
        return trackable.item;
    }

    public TValue tryGet(TKey key) {
        if (map.containsKey(key)) {
            Trackable trackable = map.get(key);

            if (trackable.state == DELETED) {
                return null;
            }
            return trackable.item;
        }

        TValue value = tryGetInternal(key);
        if (value == null) {
            return null;
        }
        map.put(key, new Trackable(key, value, NONE));
        return value;
    }

    protected abstract TValue getInternal(TKey key);

    protected abstract void addInternal(TKey key, TValue value);

    protected abstract TValue tryGetInternal(TKey key);

    protected abstract void updateInternal(TKey key, TValue value);

    public abstract void deleteInternal(TKey key);

    protected abstract Collection<Map.Entry<TKey, TValue>> findInternal(byte[] keyPrefix);
}
