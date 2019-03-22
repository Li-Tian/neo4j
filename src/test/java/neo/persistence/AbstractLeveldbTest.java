package neo.persistence;


import java.io.File;
import java.io.IOException;

import neo.log.tr.TR;
import neo.persistence.leveldb.LevelDBStore;

public class AbstractLeveldbTest {

    protected final static String LEVELDB_TEST_PATH = "Chain_test";
    protected static LevelDBStore store;

    public static void setUp() throws IOException {
        TR.debug("----  AbstractLeveldbTest setup......");

        File file = new File(LEVELDB_TEST_PATH);
        if (file.exists()) {
            for (File subFile : file.listFiles()) {
                subFile.delete();
            }
            file.delete();
        }

        store = new LevelDBStore(LEVELDB_TEST_PATH);
    }

    public static void tearDown() throws IOException {
        TR.debug("----  AbstractLeveldbTest tearDown......");

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
