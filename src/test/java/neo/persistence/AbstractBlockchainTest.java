package neo.persistence;


import java.io.IOException;
import java.util.Random;

import akka.testkit.TestActorRef;
import akka.testkit.TestKit;
import neo.MyNeoSystem;
import neo.NeoSystem;
import neo.ledger.MyBlockchain;
import neo.network.p2p.MyLocalNode;
import neo.network.p2p.MyTaskManager;

public abstract class AbstractBlockchainTest extends AbstractLeveldbTest {

    protected static TestKit testKit;
    protected static MyBlockchain blockchain;
    protected static NeoSystem neoSystem;

    /**
     * init leveldb and blockchain
     *
     * @param leveldbName leveldb file name
     */
    public static void setUp(String leveldbName) throws IOException {
        AbstractLeveldbTest.setUp(leveldbName);
        init();
    }

    private static void init() {
        neoSystem = new MyNeoSystem(store, self -> {
            testKit = new TestKit(self.actorSystem);

            // Synchronous Unit Testing with TestActorRef
            self.blockchain = TestActorRef.create(self.actorSystem, MyBlockchain.props(self, store, testKit.testActor()));
            self.localNode = TestActorRef.create(self.actorSystem, MyLocalNode.props(self, testKit.testActor()));
            self.taskManager = TestActorRef.create(self.actorSystem, MyTaskManager.props(self, testKit.testActor()));
            self.consensus = null;
        });

        blockchain = (MyBlockchain) MyBlockchain.singleton();
    }

    /**
     * tear down levedb
     *
     * @param leveldbName leveldb file name
     */
    public static void tearDown(String leveldbName) throws IOException {
        AbstractLeveldbTest.tearDown(leveldbName);

        MyLocalNode.instance.closeTimer();
    }

}


