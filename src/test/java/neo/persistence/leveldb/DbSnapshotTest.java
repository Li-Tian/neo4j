package neo.persistence.leveldb;

import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.IOException;

import neo.persistence.Snapshot;
import neo.persistence.SnapshotTest;


public class DbSnapshotTest extends SnapshotTest {

    private final static String LEVELDB_TEST_PATH = "Chain_test";
    private LevelDBStore store;

    @Override
    protected Snapshot init() {
        return store.getSnapshot();
    }

    @Before
    public void before() throws IOException {
        store = new LevelDBStore(LEVELDB_TEST_PATH);
        super.before();
    }

    @After
    public void after() throws IOException {
        store.close();
        // free leveldb file
        File file = new File(LEVELDB_TEST_PATH);
        if (file.exists()) {
            for (File subFile : file.listFiles()) {
                subFile.delete();
            }
            file.delete();
        }
    }
}