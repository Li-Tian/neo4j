package neo.persistence.leveldb;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
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
}
