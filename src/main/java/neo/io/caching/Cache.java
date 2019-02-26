package neo.io.caching;

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import neo.csharp.Out;
import neo.exception.KeyNotFoundException;

public abstract class Cache<TKey, TValue> implements Collection<TValue> {

    protected class CacheItem {
        public TKey key;
        public TValue value;
        public Date date;

        public CacheItem(TKey key, TValue value) {
            this.key = key;
            this.value = value;
            this.date = new Date();
        }
    }

    protected ConcurrentHashMap<TKey, CacheItem> map = new ConcurrentHashMap<>();
    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private final int maxCapacity;

    public Cache(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    protected abstract void onAccess(CacheItem item);

    protected abstract TKey getKeyForItem(TValue item);


    /**
     * get value by key
     *
     * @throws KeyNotFoundException if key is not exist in Cache.
     */
    public TValue get(TKey key) {
        if (!map.containsKey(key)) {
            throw new KeyNotFoundException();
        }

        CacheItem item = map.get(key);
        onAccess(item);
        return item.value;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        TValue value = (TValue) o;
        TKey key = getKeyForItem(value);
        return map.contains(key);
    }

    public boolean containsKey(TKey key) {
        return map.containsKey(key);
    }

    public boolean containsValue(TValue value) {
        TKey key = getKeyForItem(value);
        return map.containsKey(key);
    }

    @Override
    public Iterator<TValue> iterator() {
        return map.values().stream().map(o -> o.value).iterator();
    }

    @Override
    public Object[] toArray() {
        return map.values().stream().map(o -> o.value).toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        Object[] items = map.values().toArray();
        for (int i = 0; i < items.length && i < a.length; i++) {
            CacheItem cacheItem = (CacheItem) items[i];
            a[i] = (T) cacheItem.value;
        }
        return a;
    }

    @Override
    public boolean add(TValue value) {
        readWriteLock.writeLock().lock();

        TKey key = getKeyForItem(value);
        if (map.contains(key)) {
            CacheItem item = map.get(key);
            onAccess(item);
        } else {
            if (map.size() >= maxCapacity) {
                freeCapacity(map.size() - maxCapacity + 1);
            }
            map.put(key, new CacheItem(key, value));
        }
        readWriteLock.writeLock().unlock();
        return true;
    }

    private void freeCapacity(int size) {
        if (size <= 0) return;
        // remove too old items.
        /*
        C#
        foreach (CacheItem item_del in InnerDictionary.Values.AsParallel()
       .OrderBy(p => p.Time)
       .Take(InnerDictionary.Count - max_capacity + 1))
        {
            RemoveInternal(item_del);
        }
        */
        map.values().stream()
                .sorted(Comparator.comparing(o -> o.date))
                .limit(size)
                .forEach(o -> map.remove(o.key));
    }


    @Override
    public boolean remove(Object o) {
        TValue value = (TValue) o;
        TKey key = getKeyForItem(value);
        map.remove(key);
        return true;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object obj : c) {
            TValue value = (TValue) obj;
            TKey key = getKeyForItem(value);
            if (!map.containsKey(key)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends TValue> c) {
        readWriteLock.writeLock().lock();

        if (map.size() + c.size() > maxCapacity) {
            freeCapacity(map.size() + c.size() - maxCapacity);
        }
        for (TValue v : c) {
            add(v);
        }

        readWriteLock.writeLock().unlock();
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        for (Object obj : c) {
            TValue value = (TValue) c;
            TKey key = getKeyForItem(value);
            map.remove(key);
        }
        return true;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        HashSet<TKey> set = new HashSet<>(c.size());
        for (Object obj : c) {
            TValue value = (TValue) c;
            TKey key = getKeyForItem(value);
            if (key != null) {
                set.add(key);
            }
        }
        for (TKey key : map.keySet()) {
            if (!set.contains(key)) {
                map.remove(key);
            }
        }
        return true;
    }

    @Override
    public void clear() {
        map.clear();
    }

    public boolean tryGet(TKey key, Out<TValue> out) {
        if (map.containsKey(key)) {
            CacheItem item = map.get(key);
            onAccess(item);
            out.set(item.value);
            return true;
        }
        return false;
    }


}
