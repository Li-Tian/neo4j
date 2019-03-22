package neo.persistence;


import java.io.IOException;

import akka.testkit.TestActorRef;
import akka.testkit.TestKit;
import neo.MyNeoSystem;
import neo.NeoSystem;
import neo.ledger.MyBlockchain;
import neo.log.tr.TR;
import neo.network.p2p.MyLocalNode;
import neo.network.p2p.MyTaskManager;

public abstract class AbstractBlockchainTest extends AbstractLeveldbTest {

    protected static TestKit testKit;
    protected static MyBlockchain blockchain;
    protected static NeoSystem neoSystem;

    public static void setUp() throws IOException {
        TR.debug("----  AbstractBlockchainTest setup......");
        AbstractLeveldbTest.setUp();

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

    public static void tearDown() throws IOException {
        TR.debug("----  AbstractBlockchainTest tearDown......");
        AbstractLeveldbTest.tearDown();
        //TestKit.shutdownActorSystem(neoSystem.actorSystem, Duration.create(0, TimeUnit.SECONDS), true);
    }
}


