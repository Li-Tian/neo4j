package neo.network.p2p;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.testkit.TestActorRef;
import akka.testkit.TestKit;
import neo.MyNeoSystem;
import neo.NeoSystem;
import neo.UInt160;
import neo.UInt256;
import neo.Utils;
import neo.cryptography.BloomFilter;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.csharp.Ushort;
import neo.io.SerializeHelper;
import neo.io.actors.Idle;
import neo.ledger.Blockchain;
import neo.ledger.MyBlockchain2;
import neo.ledger.RelayResultReason;
import neo.log.tr.TR;
import neo.network.p2p.payloads.AddrPayload;
import neo.network.p2p.payloads.Block;
import neo.network.p2p.payloads.CoinReference;
import neo.network.p2p.payloads.ConsensusPayload;
import neo.network.p2p.payloads.ContractTransaction;
import neo.network.p2p.payloads.FilterAddPayload;
import neo.network.p2p.payloads.FilterLoadPayload;
import neo.network.p2p.payloads.GetBlocksPayload;
import neo.network.p2p.payloads.Header;
import neo.network.p2p.payloads.HeadersPayload;
import neo.network.p2p.payloads.InvPayload;
import neo.network.p2p.payloads.InventoryType;
import neo.network.p2p.payloads.MinerTransaction;
import neo.network.p2p.payloads.NetworkAddressWithTime;
import neo.network.p2p.payloads.Transaction;
import neo.network.p2p.payloads.TransactionAttribute;
import neo.network.p2p.payloads.TransactionOutput;
import neo.network.p2p.payloads.VersionPayload;
import neo.network.p2p.payloads.Witness;
import neo.persistence.AbstractLeveldbTest;
import neo.persistence.Snapshot;
import neo.persistence.Store;
import neo.vm.OpCode;
import scala.concurrent.duration.FiniteDuration;

public class ProtocolHandlerTest extends AbstractLeveldbTest {

    private static NeoSystem neoSystem;
    private static TestKit testKit;
    private static ActorRef protocolHandler;
    private static ActorRef relBlockChain;

    public static class MyActor extends AbstractActor {
        private NeoSystem system;

        public MyActor(NeoSystem system) {
            this.system = system;
        }

        public void initProtocol() {
            //  protocolHandler = TestActorRef.create(system.actorSystem, ProtocolHandler.props(system));
            protocolHandler = context().actorOf(ProtocolHandler.props(system));
        }

        public static Props props(NeoSystem system) {
            return Props.create(MyActor.class, system);
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder().matchAny(o -> {
                testKit.testActor().forward(o, context());
            }).build();
        }
    }

    // 过滤 header.verify 方法，不进行脚本检测
    public static class MyHeader extends Header {
        @Override
        public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
            return new UInt160[0];
        }

        @Override
        public Witness[] getWitnesses() {
            return new Witness[0];
        }

        public static Header[] copy(Header[] headers) {
            Header[] newHeaders = new Header[headers.length];

            for (int i = 0; i < headers.length; i++) {
                newHeaders[i] = Utils.copyFromSerialize(headers[i], MyHeader::new);
            }
            return newHeaders;
        }
    }

    // 过滤 MyBlock.verify 方法，不进行脚本检测
    public static class MyBlock extends Block {
        @Override
        public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
            return new UInt160[0];
        }

        @Override
        public Witness[] getWitnesses() {
            return new Witness[0];
        }

        @Override
        public Header getHeader() {
            neo.log.notr.TR.enter();
            MyHeader header = new MyHeader();
            header.version = this.version;
            header.prevHash = this.prevHash;
            header.merkleRoot = this.merkleRoot;
            header.timestamp = this.timestamp;
            header.index = this.index;
            header.consensusData = this.consensusData;
            header.nextConsensus = this.nextConsensus;
            header.witness = this.witness;
            return neo.log.notr.TR.exit(header);
        }
    }

    // Blockchain 代理方法，将header 全部专成 myheader 跳过脚本检查，才能添加通过
    public static class BlockchainProxy extends AbstractActor {

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(Header[].class, headers -> relBlockChain.forward(MyHeader.copy(headers), context()))
                    .matchAny(o -> relBlockChain.forward(o, context()))
                    .build();
        }

        public static Props props() {
            return Props.create(BlockchainProxy.class);
        }
    }


    @BeforeClass
    public static void setUp() throws IOException {
        AbstractLeveldbTest.setUp();

        neoSystem = new MyNeoSystem(store, self -> {
            testKit = new TestKit(self.actorSystem);

            // Synchronous Unit Testing with TestActorRef
            relBlockChain = TestActorRef.create(self.actorSystem, MyBlockchain2.props(self, store, testKit.testActor()));
            self.blockchain = TestActorRef.create(self.actorSystem, BlockchainProxy.props());
            self.localNode = TestActorRef.create(self.actorSystem, MyLocalNode.props(self, testKit.testActor()));
            self.taskManager = TestActorRef.create(self.actorSystem, MyTaskManager.props(self, testKit.testActor()));
            self.consensus = null;
            TestActorRef<MyActor> myActorTestActorRef = TestActorRef.create(self.actorSystem, MyActor.props(self));
            myActorTestActorRef.underlyingActor().initProtocol();
            //   protocolHandler = TestActorRef.create(self.actorSystem, ProtocolHandler.props(self));
        });
    }

    @AfterClass
    public static void tearDown() throws IOException {
        AbstractLeveldbTest.tearDown();
    }

    @Test
    public void testEvent() {
        // case 1: test Version event
//        testKit.expectMsgClass(Idle.class);

        VersionPayload versionPayload = new VersionPayload() {{
            version = Uint.ZERO;
            services = Ulong.ZERO;
            timestamp = new Uint(Long.toString(new Date("Jul 16 15:08:21 UTC 2016").getTime() / 1000));
            port = new Ushort(8080);
            nonce = Uint.ZERO;
            userAgent = "neo_java#1.0";
            startHeight = Uint.ONE;
            relay = true;
        }};
        Message message = Message.create("version", versionPayload);
        protocolHandler.tell(message, testKit.testActor());
        ProtocolHandler.SetVersion setVersion = testKit.expectMsgClass(ProtocolHandler.SetVersion.class);
        Assert.assertEquals(setVersion.version.version, versionPayload.version);
        Assert.assertEquals(setVersion.version.services, versionPayload.services);
        Assert.assertEquals(setVersion.version.timestamp, versionPayload.timestamp);
        Assert.assertEquals(setVersion.version.port, versionPayload.port);
        Assert.assertEquals(setVersion.version.nonce, versionPayload.nonce);
        Assert.assertEquals(setVersion.version.userAgent, versionPayload.userAgent);
        Assert.assertEquals(setVersion.version.startHeight, versionPayload.startHeight);
        Assert.assertEquals(setVersion.version.relay, versionPayload.relay);


        // case 2: test Verack event
        message = Message.create("verack");
        protocolHandler.tell(message, testKit.testActor());
        testKit.expectMsgClass(ProtocolHandler.SetVerack.class);


        // case 3: test addr event
        AddrPayload addrPayload = new AddrPayload() {{
            addressList = new NetworkAddressWithTime[]{
                    NetworkAddressWithTime.create(new InetSocketAddress("localhost", 8080), Ulong.ZERO, new Uint(192884848))
            };
        }};
        message = Message.create("addr", addrPayload);
        protocolHandler.tell(message, testKit.testActor());
        Peer.Peers peers = testKit.expectMsgClass(Peer.Peers.class);
        Assert.assertEquals(1, peers.endPoints.size());
        for (InetSocketAddress socketAddress : peers.endPoints) {
            Assert.assertEquals(8080, socketAddress.getPort());
            Assert.assertEquals(true, socketAddress.getAddress().isLoopbackAddress());
//            Assert.assertEquals("0:0:0:0:0:0:7f00:1", socketAddress.getHostName());// 127.0.0.1
        }


        // case 4: test block event
        Block block1 = new MyBlock() {
            {
                prevHash = Blockchain.GenesisBlock.hash();
                timestamp = new Uint(Long.toString(new Date("Jul 15 15:08:21 UTC 2018").getTime() / 1000));
                index = new Uint(1);
                consensusData = new Ulong(2083236894); //向比特币致敬
                nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                witness = new Witness() {
                    {
                        invocationScript = new byte[0];
                        verificationScript = new byte[]{OpCode.PUSHT.getCode()};
                    }
                };
                transactions = new Transaction[]{
                        new MinerTransaction() {
                            {
                                nonce = new Uint(2083236891);
                                attributes = new TransactionAttribute[0];
                                inputs = new CoinReference[0];
                                outputs = new TransactionOutput[0];
                                witnesses = new Witness[0];
                            }
                        }
                };
            }
        };
        block1.rebuildMerkleRoot();
        message = Message.create("block", block1);
        protocolHandler.tell(message, testKit.testActor());

        TaskManager.TaskCompleted taskCompleted = testKit.expectMsgClass(TaskManager.TaskCompleted.class);
        Assert.assertEquals(block1.hash(), taskCompleted.hash);

        LocalNode.Relay relay = testKit.expectMsgClass(LocalNode.Relay.class);
        Assert.assertEquals(block1.hash(), relay.inventory.hash());


        // case 5: test consensus event
        ConsensusPayload consensusPayload = new ConsensusPayload() {{
            version = Uint.ZERO;
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            blockIndex = new Uint(10);
            validatorIndex = new Ushort(1);
            timestamp = new Uint(10238328);
            data = new byte[]{0x00, 0x00, 0x00, 0x00};
            witness = new Witness() {{
                invocationScript = new byte[]{OpCode.PUSHT.getCode()};
                verificationScript = new byte[]{OpCode.PUSHT.getCode()};
            }};
        }};
        message = Message.create("consensus", consensusPayload);
        protocolHandler.tell(message, testKit.testActor());

        taskCompleted = testKit.expectMsgClass(TaskManager.TaskCompleted.class);
        Assert.assertEquals(consensusPayload.hash(), taskCompleted.hash);

        relay = testKit.expectMsgClass(LocalNode.Relay.class);
        Assert.assertEquals(consensusPayload.hash(), relay.inventory.hash());


        // case 6: test filteradd event
        FilterAddPayload filterAddPayload = new FilterAddPayload() {{
            data = new byte[]{0x00, 0x01, 0x02, 0x03};
        }};
        message = Message.create("filteradd", filterAddPayload);
        protocolHandler.tell(message, testKit.testActor());
        testKit.expectNoMessage();


        // case 8: test filterload event
        BloomFilter bf = new BloomFilter(1024, 16, new Uint(127));
        FilterLoadPayload filterLoadPayload = FilterLoadPayload.create(bf);
        message = Message.create("filterload", filterLoadPayload);
        protocolHandler.tell(message, testKit.testActor());
        ProtocolHandler.SetFilter setFilter = testKit.expectMsgClass(ProtocolHandler.SetFilter.class);
        Assert.assertEquals(bf.getTweak(), setFilter.filter.getTweak());
        Assert.assertEquals(bf.getK(), setFilter.filter.getK());
        Assert.assertEquals(bf.getM(), setFilter.filter.getM());


        // case 7: test filterclear event
        message = Message.create("filterclear");
        protocolHandler.tell(message, testKit.testActor());
        setFilter = testKit.expectMsgClass(ProtocolHandler.SetFilter.class);
        Assert.assertNull(setFilter.filter);


        // case 9: test getaddr event
        message = Message.create("getaddr");
        protocolHandler.tell(message, testKit.testActor());
        testKit.expectNoMessage(); // as there is no remote node


        // case 10: test getblocks event
        // add block1 in the blockchain
        neoSystem.blockchain.tell(block1, testKit.testActor());
        LocalNode.RelayDirectly relayDirectly = testKit.expectMsgClass(LocalNode.RelayDirectly.class);
        Assert.assertEquals(block1.hash(), relayDirectly.inventory.hash());
        testKit.expectMsg(RelayResultReason.Succeed);

        GetBlocksPayload getBlocksPayload = GetBlocksPayload.create(Blockchain.GenesisBlock.hash());
        message = Message.create("getblocks", getBlocksPayload);
        protocolHandler.tell(message, testKit.testActor());
        message = testKit.expectMsgClass(Message.class);
        InvPayload invPayload = SerializeHelper.parse(InvPayload::new, message.payload);
        Assert.assertEquals(InventoryType.Block, invPayload.type);
        Assert.assertEquals(1, invPayload.hashes.length);
        Assert.assertEquals(block1.hash(), invPayload.hashes[0]);


        // case 11: test getdata event
        invPayload = InvPayload.create(InventoryType.Block, new UInt256[]{block1.hash()});
        message = Message.create("getdata", invPayload);
        protocolHandler.tell(message, testKit.testActor());
        message = testKit.expectMsgClass(Message.class);
        Assert.assertEquals("block", message.command);
        Assert.assertArrayEquals(SerializeHelper.toBytes(block1), message.payload);
        Block tmp = SerializeHelper.parse(Block::new, message.payload);
        Assert.assertEquals(block1.hash(), tmp.hash());


        // case 12: test getheaders event
        getBlocksPayload = GetBlocksPayload.create(Blockchain.GenesisBlock.hash());
        message = Message.create("getheaders", getBlocksPayload);
        protocolHandler.tell(message, testKit.testActor());
        message = testKit.expectMsgClass(FiniteDuration.apply(20, TimeUnit.SECONDS), Message.class);
        Assert.assertEquals("headers", message.command);
        HeadersPayload headersPayload = SerializeHelper.parse(HeadersPayload::new, message.payload);
        Assert.assertEquals(1, headersPayload.headers.length);
        Assert.assertEquals(block1.getHeader().hash(), headersPayload.headers[0].hash());

        // case 13: test headers event
        // add block2
        Block block2 = new MyBlock() {
            {
                prevHash = block1.hash();
                timestamp = new Uint(Long.toString(new Date("Jul 16 15:08:21 UTC 2018").getTime() / 1000));
                index = new Uint(2);
                consensusData = new Ulong(2083236894); //向比特币致敬
                nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                witness = new Witness() {
                    {
                        invocationScript = new byte[0];
                        verificationScript = new byte[]{OpCode.PUSHT.getCode()};
                    }
                };
                transactions = new Transaction[]{
                        new MinerTransaction() {
                            {
                                nonce = new Uint(2083236993);
                                attributes = new TransactionAttribute[0];
                                inputs = new CoinReference[0];
                                outputs = new TransactionOutput[0];
                                witnesses = new Witness[0];
                            }
                        }
                };
            }
        };
        block2.rebuildMerkleRoot();

        headersPayload = HeadersPayload.create(Collections.singleton(block2.getHeader()));

        System.err.println("blcok2 verify header: " + block2.getHeader().verify(Blockchain.singleton().getSnapshot()) + " class " + block2.getHeader().getClass());

        message = Message.create("headers", headersPayload);
        protocolHandler.tell(message, testKit.testActor());
        testKit.expectMsgClass(TaskManager.HeaderTaskCompleted.class);
        UInt256 hash2 = Blockchain.singleton().getBlockHash(new Uint(2));
        Assert.assertEquals(block2.hash(), hash2);


        // case 14: test inv event
        invPayload = InvPayload.create(InventoryType.Block, new UInt256[]{block1.hash(), block2.hash()});
        message = Message.create("inv", invPayload);
        protocolHandler.tell(message, testKit.testActor());
        TaskManager.NewTasks newTasks = testKit.expectMsgClass(TaskManager.NewTasks.class);
        Assert.assertEquals(InventoryType.Block, newTasks.payload.type);
        Assert.assertEquals(1, newTasks.payload.hashes.length);
        Assert.assertEquals(block2.hash(), newTasks.payload.hashes[0]);


        // case 15: test mempool event
        ContractTransaction contractTransaction = new ContractTransaction();
        Blockchain.singleton().getMemPool().tryAdd(contractTransaction.hash(), contractTransaction);
        protocolHandler.tell(Message.create("mempool"), testKit.testActor());
        message = testKit.expectMsgClass(Message.class);
        invPayload = SerializeHelper.parse(InvPayload::new, message.payload);
        Assert.assertEquals(InventoryType.Tx, invPayload.type);
        Assert.assertEquals(1, invPayload.hashes.length);
        Assert.assertEquals(contractTransaction.hash(), invPayload.hashes[0]);


        // case 16: test tx event
        message = Message.create("tx", contractTransaction);
        protocolHandler.tell(message, testKit.testActor());
        taskCompleted = testKit.expectMsgClass(TaskManager.TaskCompleted.class);
        Assert.assertEquals(contractTransaction.hash(), taskCompleted.hash);
        testKit.expectMsgClass(LocalNode.Relay.class);

        // case 17: test alert... event
        protocolHandler.tell(Message.create("merkleblock"), testKit.testActor());
        testKit.expectNoMessage();
    }


}