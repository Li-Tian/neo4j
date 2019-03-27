package neo.persistence.leveldb;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;

import neo.persistence.Snapshot;
import neo.persistence.SnapshotTest;


public class DbSnapshotTest extends SnapshotTest {

    private final static String LEVELDB_TEST_PATH = "Chain_test_snapshot";
    private static LevelDBStore store;

    @Override
    protected Snapshot init() {
        return store.getSnapshot();
    }

    @BeforeClass
    public static void setUp() {
        SnapshotTest.setUp();
        try {
            store = new LevelDBStore(LEVELDB_TEST_PATH);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Before
    public void before() throws IOException {
        super.before();
    }

    @AfterClass
    public static void after() throws IOException {
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