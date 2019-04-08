package neo.persistence.leveldb;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.ReadOptions;
import org.iq80.leveldb.WriteBatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import neo.csharp.BitConverter;
import neo.log.notr.TR;

/**
 * leveldb helper
 */
public class DBHelper {

    /**
     * query by key prefix
     *
     * @param db        leveldb
     * @param keyPrefix key prefix
     * @param generator value generator eg: (keyBytes, valueBytes) -> item
     * @param <R>       value type
     * @return Collection<R>, empty set will be return if not found
     */
    public static <R> Collection<R> find(DB db, byte[] keyPrefix, BiFunction<byte[], byte[], R> generator) {
        TR.enter();
        DBIterator iterator = db.iterator();
        iterator.seek(keyPrefix);
        ArrayList<R> list = new ArrayList<>();
        while (iterator.hasNext()) {
            Map.Entry<byte[], byte[]> entry = iterator.next();

            byte[] tmpKeyBytes = entry.getKey();
            byte[] valueBytes = entry.getValue();

            if (!BitConverter.startWith(tmpKeyBytes, keyPrefix)) {
                break;
            }
            list.add(generator.apply(tmpKeyBytes, valueBytes));
        }
        return TR.exit(list);
    }


    /**
     * query object and for each invoke the acceptor
     *
     * @param db       leveldb
     * @param prefix   prefix
     * @param acceptor object handler
     */
    public static void findForEach(DB db, byte prefix, BiConsumer<byte[], byte[]> acceptor) {
        TR.enter();
        findForEach(db, prefix, new byte[0], acceptor);
        TR.exit();
    }


    /**
     * query object and for each invoke the acceptor
     *
     * @param db       leveldb
     * @param prefix   prefix
     * @param key      key
     * @param acceptor object handler
     */
    public static void findForEach(DB db, byte prefix, byte[] key, BiConsumer<byte[], byte[]> acceptor) {
        TR.enter();
        byte[] keyPrefix = BitConverter.merge(prefix, key);
        DBIterator iterator = db.iterator();
        iterator.seek(keyPrefix);

        while (iterator.hasNext()) {
            Map.Entry<byte[], byte[]> entry = iterator.next();

            byte[] tmpKeyBytes = entry.getKey();
            byte[] valueBytes = entry.getValue();

            if (!BitConverter.startWith(tmpKeyBytes, keyPrefix)) {
                break;
            }
            acceptor.accept(tmpKeyBytes, valueBytes);
        }
        TR.exit();
    }


    /**
     * query by key prefix
     *
     * @param db        leveldb
     * @param prefix    prefix
     * @param key       key
     * @param generator value generator eg: (keyBytes, valueBytes) -> item
     * @param <R>       value type
     * @return Collection<R>, empty set will be return if not found
     */
    public static <R> Collection<R> find(DB db, byte prefix, byte[] key, BiFunction<byte[], byte[], R> generator) {
        byte[] keyBytes = BitConverter.merge(prefix, key);
        return find(db, keyBytes, generator);
    }

    /**
     * query by key prefix
     *
     * @param db        leveldb
     * @param prefix    prefix
     * @param generator value generator eg: (keyBytes, valueBytes) -> item
     * @param <R>       value type
     * @return Collection<R>, empty set will be return if not found
     */
    public static <R> Collection<R> find(DB db, byte prefix, BiFunction<byte[], byte[], R> generator) {
        return find(db, new byte[prefix], generator);
    }

    /**
     * get from leveldb
     *
     * @param db        leveldb
     * @param prefix    prefix
     * @param key       key
     * @param options   read options
     * @param generator alue generator eg: (keyBytes, valueBytes) -> item
     * @param <R>       value type
     * @return R
     */
    public static <R> R get(DB db, byte prefix, byte[] key, ReadOptions options, BiFunction<byte[], byte[], R> generator) {
        byte[] keyBytes = BitConverter.merge(prefix, key);
        byte[] valueBytes = db.get(keyBytes, options);
        return generator.apply(keyBytes, valueBytes);
    }

    /**
     * get from leveldb
     *
     * @param db      leveldb
     * @param prefix  prefix
     * @param key     key
     * @param options read options
     * @return R
     */
    public static byte[] get(DB db, byte prefix, byte[] key, ReadOptions options) {
        byte[] keyBytes = BitConverter.merge(prefix, key);
        return db.get(keyBytes, options);
    }

    /**
     * get from leveldb
     *
     * @param db     leveldb
     * @param prefix prefix
     * @param key    key
     * @return R
     */
    public static byte[] get(DB db, byte prefix, byte[] key) {
        byte[] keyBytes = BitConverter.merge(prefix, key);
        return db.get(keyBytes);
    }

    /**
     * get from leveldb
     *
     * @param db     leveldb
     * @param prefix prefix
     * @return R
     */
    public static byte[] get(DB db, byte prefix) {
        return db.get(new byte[]{prefix});
    }

    /**
     * batch put operation
     *
     * @param batch  leveldb batch
     * @param prefix prefix
     * @param key    key
     * @param value  value
     */
    public static void batchPut(WriteBatch batch, byte prefix, byte[] key, byte[] value) {
        byte[] keyBytes = BitConverter.merge(prefix, key);
        batch.put(keyBytes, value);
    }

    /**
     * batch put operation
     *
     * @param batch  leveldb batch
     * @param prefix prefix
     * @param value  value
     */
    public static void batchPut(WriteBatch batch, byte prefix, byte[] value) {
        batch.put(new byte[]{prefix}, value);
    }


    /**
     * batch delete operation
     *
     * @param batch  leveldb batch
     * @param prefix prefix
     * @param key    key
     */
    public static void batchDelete(WriteBatch batch, byte prefix, byte[] key) {
        byte[] keyBytes = BitConverter.merge(prefix, key);
        batch.delete(keyBytes);
    }


}
