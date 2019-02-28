package neo.persistence.leveldb;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.ReadOptions;
import org.iq80.leveldb.WriteBatch;

import java.io.IOException;

import neo.persistence.Snapshot;

public class DbSnapshot extends Snapshot {

    private final DB db;
    private final org.iq80.leveldb.Snapshot snapshot;
    private final WriteBatch writeBatch;

    public DbSnapshot(DB db) {
        this.db = db;
        this.snapshot = db.getSnapshot();
        this.writeBatch = db.createWriteBatch();

        ReadOptions readOptions = new ReadOptions();
        readOptions.fillCache(false);
        readOptions.snapshot(this.snapshot);
    }

    @Override
    public void commit() {
        super.commit();
        db.write(writeBatch);

        // 各种db cache 赋值
    }

    public void close() throws IOException {
        snapshot.close();
    }

}
