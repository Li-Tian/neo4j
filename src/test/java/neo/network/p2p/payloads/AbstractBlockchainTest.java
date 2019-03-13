package neo.network.p2p.payloads;

import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.IOException;

import akka.actor.ActorSystem;
import akka.actor.Props;
import neo.persistence.leveldb.BlockchainDemo;
import neo.persistence.leveldb.LevelDBStore;

public abstract class AbstractBlockchainTest {
    
    protected final static String LEVELDB_TEST_PATH = "Chain_test";

    protected LevelDBStore store;
    protected BlockchainDemo blockchainDemo;

    @Before
    public void before() throws IOException {
        store = new LevelDBStore(LEVELDB_TEST_PATH);

        ActorSystem system = ActorSystem.create("neosystem");
        system.actorOf(Props.create(BlockchainDemo.class, store));
        blockchainDemo = (BlockchainDemo) BlockchainDemo.singleton();
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
