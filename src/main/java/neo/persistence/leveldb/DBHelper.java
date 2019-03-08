package neo.persistence.leveldb;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import neo.csharp.BitConverter;
import neo.function.FuncAB2T;

/**
 * leveldb helper
 */
public class DBHelper {

    /**
     * query by key prefix
     *
     * @param db        leveldb
     * @param keyPrefix key prefix
     * @param generator value generator
     * @param <R>       value type
     * @return Collection<R>, empty set will be return if not found
     */
    public static <R> Collection<R> find(DB db, byte[] keyPrefix, FuncAB2T<byte[], byte[], R> generator) {
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
            list.add(generator.get(tmpKeyBytes, valueBytes));
        }
        return list;
    }

}
