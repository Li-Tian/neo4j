package neo.network.p2p;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.io.Inet;
import akka.io.Tcp;
import akka.io.TcpMessage;
import akka.io.TcpSO;
import akka.testkit.TestActorRef;
import akka.testkit.TestKit;
import neo.MyNeoSystem;
import neo.NeoSystem;
import neo.exception.InvalidOperationException;
import neo.io.SerializeHelper;
import neo.io.actors.Idle;
import neo.ledger.MyBlockchain;
import neo.ledger.RelayResultReason;
import neo.log.tr.TR;
import neo.network.p2p.payloads.ContractTransaction;
import neo.persistence.AbstractLeveldbTest;

public class LocalNodeTest extends AbstractLeveldbTest {

    private static NeoSystem neoSystem;
    private static TestKit testKit;
    private static TestActorRef<MyLocalNode2> localNodeRef;
    private static MyLocalNode2 localNode;

    @BeforeClass
    public static void setUp() throws IOException {
        AbstractLeveldbTest.setUp(LocalNodeTest.class.getSimpleName());

        neoSystem = new MyNeoSystem(store, self -> {
            testKit = new TestKit(self.actorSystem);

            // Synchronous Unit Testing with TestActorRef
            self.blockchain = TestActorRef.create(self.actorSystem, MyBlockchain.props(self, store, testKit.testActor()));
            localNodeRef = TestActorRef.create(self.actorSystem, MyLocalNode2.props(self));
            localNode = localNodeRef.underlyingActor();
            self.localNode = localNodeRef;
            self.taskManager = TestActorRef.create(self.actorSystem, MyTaskManager.props(self, testKit.testActor()));
            self.consensus = TestActorRef.create(self.actorSystem, MyConsensus.props());
        });

        testKit.expectMsgClass(Idle.class);
    }

    @AfterClass
    public static void tearDown() throws IOException {
        AbstractLeveldbTest.tearDown(LocalNodeTest.class.getSimpleName());
    }

    @Test
    public void getConnectedCount() {
        InetSocketAddress remoteAddr = new InetSocketAddress("localhost", 8090);
        InetSocketAddress localAddr = new InetSocketAddress("localhost", 8081);
        Props props = RemoteNode.props(neoSystem, testKit.testActor(), remoteAddr, localAddr);
        TestActorRef<RemoteNode> remoteNodeRef = TestActorRef.create(neoSystem.actorSystem, props);
//        testKit.expectMsgClass(Idle.class);
        Tcp.Write writeCmd = testKit.expectMsgClass(Tcp.Write.class);
        Message message = SerializeHelper.parse(Message::new, writeCmd.data().toArray());
        Assert.assertEquals("version", message.command);

        localNode.registerRemoteNode(remoteNodeRef.underlyingActor());
        Assert.assertEquals(1, localNode.getConnectedCount());
        localNode.unregisterRemoteNode(remoteNodeRef.underlyingActor());
        Assert.assertEquals(0, localNode.getRemoteNodes().size());
        Assert.assertEquals(0, localNode.getConnectedCount());

        // clear all data
        localNode.clear();
    }

    @Test
    public void getUnconnectedCount() {
        // test Peer.Peers event
        Peer.Peers peers = new Peer.Peers() {{
            endPoints = new ArrayList<InetSocketAddress>(2) {{
                add(new InetSocketAddress(8082));
                add(new InetSocketAddress(8083));
            }};
        }};
        int unconnectedCount = localNode.getUnconnectedCount();
        localNodeRef.tell(peers, testKit.testActor());
        HashSet<InetSocketAddress> socketSet = localNodeRef.underlyingActor().unconnectedPeers;
        boolean has8082 = false, has8083 = false;
        for (InetSocketAddress socketAddress : socketSet) {
            if (socketAddress.getPort() == 8082) {
                has8082 = true;
            }
            if (socketAddress.getPort() == 8083) {
                has8083 = true;
            }
        }
        Assert.assertEquals(true, has8082 && has8083);

        Assert.assertEquals(2 + unconnectedCount, localNode.getUnconnectedCount());
        Assert.assertEquals(2 + unconnectedCount, localNode.getUnconnectedPeers().size());

        // clear all data
        localNode.clear();
    }

    @Test
    public void needMorePeers() {
        InetSocketAddress localNodeAddr = new InetSocketAddress(8081);
        InetSocketAddress remoteNodeAddr = new InetSocketAddress(8086);
        Tcp.Connected connected = new Tcp.Connected(remoteNodeAddr, localNodeAddr);
        localNodeRef.tell(connected, testKit.testActor());
//        testKit.expectMsgClass(Idle.class);
        testKit.expectMsgClass(Tcp.Register.class);

        Peer.Timer timer = new Peer.Timer();
        localNodeRef.tell(timer, testKit.testActor());
        Message message = testKit.expectMsgClass(Message.class);
        Assert.assertEquals("getaddr", message.command);
        testKit.expectNoMessage(); // it will open a seed connection

        // clear all data
        localNode.clear();
    }


    @Test
    public void testEvent() {
        // test Peer.Start event
        Peer.Start start = new Peer.Start() {{
            port = 8081;
            minDesiredConnections = 1;
            maxConnections = 2;
        }};
        localNodeRef.tell(start, testKit.testActor());
        Tcp.Bind bind = testKit.expectMsgClass(Tcp.Bind.class);
        Assert.assertEquals(8081, bind.localAddress().getPort());


        // test timer event
        Peer.Timer timer = new Peer.Timer();
        localNodeRef.tell(timer, testKit.testActor());
        testKit.expectMsgClass(Tcp.Connect.class); // it will open a seed connection


        // test Peer.Peers event
        Peer.Peers peers = new Peer.Peers() {{
            endPoints = new ArrayList<InetSocketAddress>(2) {{
                add(new InetSocketAddress(8082));
                add(new InetSocketAddress(8083));
            }};
        }};
        localNodeRef.tell(peers, testKit.testActor());
        HashSet<InetSocketAddress> socketSet = localNodeRef.underlyingActor().unconnectedPeers;
        boolean has8082 = false, has8083 = false;
        for (InetSocketAddress socketAddress : socketSet) {
            if (socketAddress.getPort() == 8082) {
                has8082 = true;
            }
            if (socketAddress.getPort() == 8083) {
                has8083 = true;
            }
        }
        Assert.assertEquals(true, has8082 && has8083);


        // test Peer.Connect event
        Peer.Connect connect = new Peer.Connect() {{
            endPoint = new InetSocketAddress("localhost", 8084);
            isTrusted = true;
        }};
        localNodeRef.tell(connect, testKit.testActor());
        testKit.expectMsgClass(Tcp.Connect.class);


        // test Tcp.Connected  event
        InetSocketAddress localNodeAddr = new InetSocketAddress(8081);
        InetSocketAddress remoteNodeAddr = new InetSocketAddress(8086);
        Tcp.Connected connected = new Tcp.Connected(remoteNodeAddr, localNodeAddr);
        localNodeRef.tell(connected, testKit.testActor());
        testKit.expectMsgClass(Tcp.Register.class);


        // test Message event
        Message message = Message.create("getaddr");
        localNodeRef.tell(message, testKit.testActor());
        testKit.expectMsgClass(Message.class);


        // test Relay event
        LocalNode.Relay relay = new LocalNode.Relay(new ContractTransaction());
        localNodeRef.tell(relay, testKit.testActor());
        testKit.expectMsgClass(ContractTransaction.class);// Myconsensus
        testKit.expectMsgClass(ContractTransaction.class);// blockchain


        // test RelayDirectly event
        LocalNode.RelayDirectly relayDirectly = new LocalNode.RelayDirectly(new ContractTransaction()) ;
        localNodeRef.tell(relayDirectly, testKit.testActor());
        testKit.expectMsgClass(Idle.class); // MyRemoteNode
        testKit.expectMsgClass(RemoteNode.Relay.class); // MyRemoteNode


        // test SendDirectly event
        LocalNode.SendDirectly sendDirectly = new LocalNode.SendDirectly(new ContractTransaction());
        localNodeRef.tell(sendDirectly, testKit.testActor());
        testKit.expectMsgClass(ContractTransaction.class);


        // test RelayResultReason event
        localNodeRef.tell(RelayResultReason.Succeed, testKit.testActor());
        testKit.expectNoMessage();


        // test Tcp.Bound event
        Tcp.Bound bound = new Tcp.Bound(new InetSocketAddress("localhost", 8088));
        localNodeRef.tell(bound, testKit.testActor());
        testKit.expectNoMessage();

        // test commandFailed
        remoteNodeAddr = new InetSocketAddress(8086);
        int connectingPeersSize = localNode.getConnectingPeersSize();
        Tcp.CommandFailed commandFailed = new Tcp.CommandFailed(TcpMessage.connect(remoteNodeAddr));
        localNodeRef.tell(commandFailed, testKit.testActor());
        testKit.expectNoMessage();
        Assert.assertEquals(connectingPeersSize, localNode.getConnectingPeersSize());


        // test Terminated event
        // need to research the akka terminate
//        localNode.addConnectedPeers(remoteNodeAddr);
//        int connectedPeers = localNode.getConnectedPeers();
//        Terminated terminated = new Terminated(testKit.testActor(), true, true);
//        localNodeRef.tell(terminated, testKit.testActor());
//        testKit.expectNoMessage();// add twice?
//        Assert.assertEquals(2, connectedPeers);
//        Assert.assertEquals(connectedPeers - 1, localNode.getConnectedPeers());

        // clear data
        localNode.clear();

        ActorRef tcpManager = Tcp.get(localNode.context().system()).manager();
        tcpManager.tell(TcpMessage.unbind(), localNode.self());
    }

    static class MyLocalNode2 extends LocalNode {

        public MyLocalNode2(NeoSystem system) {
            super(system);
        }

        public static Props props(NeoSystem system) {
            return Props.create(MyLocalNode2.class, system);
        }

        // 减少单例的的冲突
        @Override
        protected void init() {
            singleton = this;
        }

        @Override
        public void initOnlyOnce() {
            // redirect all the message to TestActor
            tcpManager = testKit.testActor();
        }

        @Override
        protected Props protocolProps(ActorRef connection, InetSocketAddress remote, InetSocketAddress local) {
            return MyRemoteNode.props();
        }

        public int getConnectingPeersSize() {
            return connectedPeers.size();
        }

        public int getConnectedPeers() {
            return connectedPeers.size();
        }

        public void clear() {
            for (ActorRef actorRef : connectedPeers.keySet()) {
                context().unwatch(actorRef);
                context().stop(actorRef);
            }
            connectedPeers.clear();
            connectedAddresses.clear();
            unconnectedPeers.clear();
            connectingPeers.clear();
        }


        @Override
        protected void onStart(int port, int minDesiredConnections, int maxConnections) {
            this.listenerPort = port;
            this.minDesiredConnections = minDesiredConnections;
            this.maxConnections = maxConnections;

//            ActorSystem system = context().system();
//            timer = system.scheduler()
//                    .schedule(Duration.ZERO,
//                            Duration.ofMillis(5000),
//                            context().self(),
//                            new Timer(),
//                            system.dispatcher(),
//                            ActorRef.noSender());

            if (port > 0) {
                Collection<Inet.SocketOption> options = Collections.singletonList(TcpSO.reuseAddress(true));
                InetSocketAddress address = new InetSocketAddress("localhost", port);
                Tcp.Command command = TcpMessage.bind(self(), address, 100, options, false);
                tcpManager.tell(command, self());
            }
        }
    }

    static class MyRemoteNode extends AbstractActor {

        @Override
        public Receive createReceive() {
            // forward event to testActor
            return receiveBuilder().matchAny(o -> {
                TR.debug("MyRemoteNode received msg: " + o);
                testKit.testActor().forward(o, context());
            }).build();
        }

        public static Props props() {
            return Props.create(MyRemoteNode.class);
        }
    }

    static class MyConsensus extends AbstractActor {

        @Override
        public Receive createReceive() {
            // forward event to testActor
            return receiveBuilder().matchAny(o -> {
                TR.debug("MyConsensus received msg: " + o);
                testKit.testActor().forward(o, context());
            }).build();
        }

        public static Props props() {
            return Props.create(MyConsensus.class);
        }
    }
}