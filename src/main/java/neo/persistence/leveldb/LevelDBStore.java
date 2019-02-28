package neo.persistence.leveldb;

import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBFactory;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.ReadOptions;
import org.iq80.leveldb.WriteBatch;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import neo.Properties;
import neo.persistence.Snapshot;
import neo.persistence.Store;

public class LevelDBStore extends Store {

    private final DB db;
    private static final Charset CHARSET = Charset.forName("utf-8");

    public LevelDBStore(String path) throws IOException {
        DBFactory factory = new JniDBFactory();
        // 默认如果没有则创建
        Options options = new Options();
        options.createIfMissing(true);
        File file = new File(path);

        db = factory.open(file, options);
        byte[] keys = new byte[]{Prefixes.SYS_Version};
        byte[] versionBytes = db.get(keys);
        String version = new String(versionBytes);
        if (version.compareTo("2.9.1") >= 0) {
            return;
        }

        WriteBatch batch = db.createWriteBatch();
        ReadOptions readOptions = new ReadOptions();
        readOptions.fillCache(true);
        DBIterator iterator = db.iterator(readOptions);
        iterator.seekToFirst();
        while (iterator.hasNext()) {
            batch.delete(iterator.next().getKey());
        }
        iterator.close();
        db.put(keys, Properties.Default.version.getBytes(CHARSET));
        db.write(batch);
    }


    @Override
    public Snapshot GetSnapshot() {
        return null;
    }

    public void close() throws IOException {
        if (db != null) {
            db.close();
        }
    }
}
