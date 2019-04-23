package neo.network.p2p;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;

import akka.actor.AbstractActor;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.io.Tcp;
import akka.io.TcpMessage;
import akka.testkit.TestActorRef;
import akka.testkit.TestKit;
import akka.util.ByteString;
import neo.MyNeoSystem;
import neo.NeoSystem;
import neo.cryptography.BloomFilter;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.io.SerializeHelper;
import neo.ledger.MyBlockchain2;
import neo.log.tr.TR;
import neo.network.p2p.payloads.ContractTransaction;
import neo.network.p2p.payloads.InvPayload;
import neo.network.p2p.payloads.InventoryType;
import neo.network.p2p.payloads.Transaction;
import neo.network.p2p.payloads.VersionPayload;
import neo.persistence.AbstractLeveldbTest;

public class RemoteNodeTest extends AbstractLeveldbTest {

    private static NeoSystem neoSystem;
    private static TestKit testKit;
    private static TestActorRef<RemoteNode> remoteNodeRef;
    private static RemoteNode remoteNode;
    private static InetSocketAddress localAddr = new InetSocketAddress("127.0.0.1", 8083);
    private static InetSocketAddress remoteAddr = new InetSocketAddress("127.0.0.1", 8081);


    public static class MyTcp extends AbstractActor {

        public MyTcp() {
        }

        public static Props props() {
            return Props.create(MyTcp.class);
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder().matchAny(o -> {
                testKit.testActor().forward(o, context());
            }).build();
        }
    }


    @BeforeClass
    public static void setUp() throws IOException {
        AbstractLeveldbTest.setUp(RemoteNodeTest.class.getSimpleName());

        neoSystem = new MyNeoSystem(store, self -> {
            testKit = new TestKit(self.actorSystem);

            // Synchronous Unit Testing with TestActorRef
            self.blockchain = TestActorRef.create(self.actorSystem, MyBlockchain2.props(self, store, testKit.testActor()));
            self.localNode = TestActorRef.create(self.actorSystem, MyLocalNode.props(self, testKit.testActor()));
            self.taskManager = TestActorRef.create(self.actorSystem, MyTaskManager.props(self, testKit.testActor()));
            self.consensus = null;

            TestActorRef<MyTcp> myTcpTestActorRef = TestActorRef.create(self.actorSystem, MyTcp.props());
            remoteNodeRef = TestActorRef.create(self.actorSystem, RemoteNode.props(self, myTcpTestActorRef, remoteAddr, localAddr));
            remoteNode = remoteNodeRef.underlyingActor();
        });
    }

    @AfterClass
    public static void tearDown() throws IOException {
        AbstractLeveldbTest.tearDown(RemoteNodeTest.class.getSimpleName());
    }

    @Test
    public void getListener() {
        InetSocketAddress inetSocketAddress = remoteNode.getListener();
        Assert.assertEquals(8081, inetSocketAddress.getPort());
        Assert.assertTrue(inetSocketAddress.getAddress().isLoopbackAddress());
    }

    @Test
    public void getListenerPort() {
        Assert.assertEquals(8081, remoteNode.getListenerPort());
    }

    @Test
    public void supervisorStrategy() {
        SupervisorStrategy supervisorStrategy = remoteNode.supervisorStrategy();
        Assert.assertTrue(supervisorStrategy instanceof OneForOneStrategy);
        OneForOneStrategy oneForOneStrategy = (OneForOneStrategy) supervisorStrategy;
        Assert.assertEquals(10, oneForOneStrategy.maxNrOfRetries());
        Assert.assertEquals(Duration.ofMinutes(1).toMillis(), oneForOneStrategy.withinTimeRange().toMillis());
    }


    @Test
    public void testEvent() {
        testKit.expectMsgClass(Tcp.Command.class);
        // send version to the remotenode

        // case 1: test Tcp.Received event. The remotenode will received the version from the real remote node
        VersionPayload versionPayload = VersionPayload.create(8081, Uint.ZERO, "java_neo#1.0", new Uint(1));
        Message message = Message.create("version", versionPayload);
        byte[] bytes = SerializeHelper.toBytes(message);
        TR.debug("------------send RemoteNode: " + BitConverter.toHexString(bytes));
        Tcp.Received received = new Tcp.Received(ByteString.fromArray(bytes));
        remoteNodeRef.tell(received, testKit.testActor());
        testKit.expectMsgClass(Tcp.Command.class);

        // then the remote node send Verack to the real remote node
        ProtocolHandler.SetVerack setVerack = new ProtocolHandler.SetVerack();
        remoteNodeRef.tell(setVerack, testKit.testActor());
        TaskManager.Register register = testKit.expectMsgClass(TaskManager.Register.class);
        Assert.assertEquals(versionPayload.version, register.version.version);
        Assert.assertEquals(versionPayload.port, register.version.port);
        Assert.assertEquals(versionPayload.nonce, register.version.nonce);
        Assert.assertEquals(versionPayload.startHeight, register.version.startHeight);
        Assert.assertEquals(versionPayload.userAgent, register.version.userAgent);
        Assert.assertEquals(versionPayload.timestamp, register.version.timestamp);


        // case 2: test Ack event
        Connection.Ack ack = new Connection.Ack();
        remoteNodeRef.tell(ack, testKit.testActor());
        testKit.expectNoMessage();

        // case 3: test Message event
        ContractTransaction contractTransaction = new ContractTransaction();
        message = Message.create("tx", contractTransaction);
        remoteNodeRef.tell(message, testKit.testActor());
        testKit.expectMsgClass(Tcp.Command.class);
        // send ack back
        remoteNodeRef.tell(Connection.Ack.Instance, testKit.testActor());


        // case 5: test IInventory event, start to send Inventory to the real remote node
        remoteNodeRef.tell(contractTransaction, testKit.testActor());
        // then the tcp will send the real Message, it will forward to the testKit
        Tcp.Write tcpWrite = testKit.expectMsgClass(Tcp.Write.class);
        message = SerializeHelper.parse(Message::new, tcpWrite.data().toArray());
        Assert.assertEquals("tx", message.command);
        Transaction transaction = Transaction.deserializeFrom(message.payload);
        Assert.assertEquals(contractTransaction.hash(), transaction.hash());
        // then send ack back
        remoteNodeRef.tell(Connection.Ack.Instance, testKit.testActor());

        // case 6: test RemoteNode.Relay event
        RemoteNode.Relay relay = new RemoteNode.Relay() {{
            inventory = contractTransaction;
        }};
        remoteNodeRef.tell(relay, testKit.testActor());
        // tcp will received the message with invpayload
        Tcp.Write write = testKit.expectMsgClass(Tcp.Write.class);
        message = SerializeHelper.parse(Message::new, write.data().toArray());
        Assert.assertEquals("inv", message.command);
        InvPayload invPayload = SerializeHelper.parse(InvPayload::new, message.payload);
        Assert.assertEquals(InventoryType.Tx, invPayload.type);
        Assert.assertEquals(contractTransaction.hash(), invPayload.hashes[0]);
        // then send ack back
        remoteNodeRef.tell(Connection.Ack.Instance, testKit.testActor());


        // case 7: test ProtocolHandler.SetVersion event
        // ProtocolHandler.SetVersion setVersion =

        // case 8: test ProtocolHandler.SetVerack event

        // case 9: test ProtocolHandler.SetFilter event
        ProtocolHandler.SetFilter setFilter = new ProtocolHandler.SetFilter() {{
            filter = new BloomFilter(1024, 16, new Uint(127));
        }};
        remoteNodeRef.tell(setFilter, testKit.testActor());
        testKit.expectNoMessage();

        // case 1: test Timer case
        Connection.Timer timer = new Connection.Timer();
        remoteNodeRef.tell(timer, testKit.testActor());
        testKit.expectMsgClass(Tcp.Abort$.class);


        // case 10: test Tcp.ConnectionClosed case
        remoteNodeRef.tell(TcpMessage.close(), testKit.testActor());
        testKit.expectNoMessage();
    }
}