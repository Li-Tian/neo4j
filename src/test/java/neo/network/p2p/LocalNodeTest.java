package neo.network.p2p;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import akka.testkit.TestActorRef;
import akka.testkit.TestKit;
import neo.MyNeoSystem;
import neo.NeoSystem;
import neo.ledger.Blockchain;
import neo.ledger.MyBlockchain2;
import neo.persistence.AbstractLeveldbTest;

import static org.junit.Assert.*;

public class LocalNodeTest extends AbstractLeveldbTest {

    private static NeoSystem neoSystem;
    private static TestKit testKit;

    @BeforeClass
    public static void setUp() throws IOException {
        AbstractLeveldbTest.setUp();

        neoSystem = new MyNeoSystem(store, self -> {
            testKit = new TestKit(self.actorSystem);

            // Synchronous Unit Testing with TestActorRef
            self.blockchain = TestActorRef.create(self.actorSystem, MyBlockchain2.props(self, store, testKit.testActor()));
            self.localNode = TestActorRef.create(self.actorSystem, MyLocalNode.props(self, testKit.testActor()));
            self.taskManager = TestActorRef.create(self.actorSystem, MyTaskManager.props(self, testKit.testActor()));
            self.consensus = null;

            TestActorRef<RemoteNodeTest.MyTcp> myTcpTestActorRef = TestActorRef.create(self.actorSystem, RemoteNodeTest.MyTcp.props());
//            remoteNodeRef = TestActorRef.create(self.actorSystem, RemoteNode.props(self, myTcpTestActorRef, remoteAddr, localAddr));
//            remoteNode = remoteNodeRef.underlyingActor();
        });
    }

    @AfterClass
    public static void tearDown() throws IOException {
        AbstractLeveldbTest.tearDown();
    }

    @Test
    public void getConnectedCount() {
    }

    @Test
    public void getUnconnectedCount() {
    }

    @Test
    public void getRemoteNodes() {
    }

    @Test
    public void registerRemoteNode() {
    }

    @Test
    public void unregisterRemoteNode() {
    }

    @Test
    public void getUnconnectedPeers() {
    }

    @Test
    public void needMorePeers() {
    }


    @Test
    public void testEvent() {

    }
}