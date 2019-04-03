package neo.persistence;


import java.io.File;
import java.io.IOException;

import neo.log.tr.TR;
import neo.persistence.leveldb.LevelDBStore;

public class AbstractLeveldbTest {

    protected static LevelDBStore store;

    /**
     * init leveldb
     *
     * @param leveldbName leveldb file name
     */
    public static void setUp(String leveldbName) throws IOException {
        String leveldbPath = AbstractLeveldbTest.class.getClassLoader().getResource("").getPath() + leveldbName + "_leveldb";
        File file = new File(leveldbPath);
        if (file.exists()) {
            for (File subFile : file.listFiles()) {
                subFile.delete();
            }
            file.delete();
        }

        store = new LevelDBStore(leveldbPath);
    }


    /**
     * tear down levedb
     *
     * @param leveldbName leveldb file name
     */
    public static void tearDown(String leveldbName) throws IOException {
        String leveldbPath = AbstractLeveldbTest.class.getClassLoader().getResource("").getPath() + leveldbName + "_leveldb";
        store.close();

        // free leveldb file
        File file = new File(leveldbPath);
        if (file.exists()) {
            for (File subFile : file.listFiles()) {
                subFile.delete();
            }
            file.delete();
        }
    }

}
