package neo.network.p2p;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.TestActorRef;
import akka.testkit.TestKit;
import neo.MyNeoSystem;
import neo.NeoSystem;
import neo.UInt160;
import neo.UInt256;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.csharp.Ushort;
import neo.io.SerializeHelper;
import neo.ledger.Blockchain;
import neo.ledger.MyBlockchain2;
import neo.ledger.RelayResultReason;
import neo.log.tr.TR;
import neo.network.p2p.payloads.Block;
import neo.network.p2p.payloads.CoinReference;
import neo.network.p2p.payloads.GetBlocksPayload;
import neo.network.p2p.payloads.Header;
import neo.network.p2p.payloads.InvPayload;
import neo.network.p2p.payloads.InventoryType;
import neo.network.p2p.payloads.MinerTransaction;
import neo.network.p2p.payloads.Transaction;
import neo.network.p2p.payloads.TransactionAttribute;
import neo.network.p2p.payloads.TransactionOutput;
import neo.network.p2p.payloads.VersionPayload;
import neo.network.p2p.payloads.Witness;
import neo.persistence.AbstractLeveldbTest;
import neo.persistence.Snapshot;
import neo.vm.OpCode;


public class TaskManagerTest extends AbstractLeveldbTest {

    private static NeoSystem neoSystem;
    private static TestKit testKit;


    @BeforeClass
    public static void setUp() throws IOException {
        AbstractLeveldbTest.setUp(TaskManagerTest.class.getSimpleName());
        neoSystem = new MyNeoSystem(store, self -> {
            testKit = new TestKit(self.actorSystem);

            // Synchronous Unit Testing with TestActorRef
            self.blockchain = TestActorRef.create(self.actorSystem, MyBlockchain2.props(self, store, testKit.testActor()));
            self.localNode = TestActorRef.create(self.actorSystem, MyLocalNode.props(self, testKit.testActor()));
            self.taskManager = TestActorRef.create(self.actorSystem, TaskManager.props(self));
            self.consensus = null;
        });
    }

    @AfterClass
    public static void tearDown() throws IOException {
        AbstractLeveldbTest.tearDown(TaskManagerTest.class.getSimpleName());
    }


    public class MyBlock extends Block {
        @Override
        public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
            return new UInt160[0];
        }

        @Override
        public Witness[] getWitnesses() {
            return new Witness[0];
        }
    }


    @Test
    public void testRegisterMsg() {
        // 1. prepare data

        // init leveldb data
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

        Block block3 = new MyBlock() {
            {
                prevHash = block2.hash();
                timestamp = new Uint(Long.toString(new Date("Jul 17 15:08:21 UTC 2018").getTime() / 1000));
                index = new Uint(3);
                consensusData = new Ulong(2083336894); //向比特币致敬
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
                                nonce = new Uint(2083236896);
                                attributes = new TransactionAttribute[0];
                                inputs = new CoinReference[0];
                                outputs = new TransactionOutput[0];
                                witnesses = new Witness[0];
                            }
                        }
                };
            }
        };
        block3.rebuildMerkleRoot();

        neoSystem.blockchain.tell(block1, testKit.testActor());
        LocalNode.RelayDirectly relayDirectly = testKit.expectMsgClass(LocalNode.RelayDirectly.class);
        Assert.assertNotNull(relayDirectly);
        Assert.assertEquals(block1.hash(), relayDirectly.inventory.hash());

        RelayResultReason resultReason1 = testKit.expectMsg(RelayResultReason.Succeed);
        Assert.assertEquals(RelayResultReason.Succeed, resultReason1);

        neoSystem.blockchain.tell(block2, testKit.testActor());
        relayDirectly = testKit.expectMsgClass(LocalNode.RelayDirectly.class);
        Assert.assertNotNull(relayDirectly);
        Assert.assertEquals(block2.hash(), relayDirectly.inventory.hash());
        RelayResultReason resultReason2 = testKit.expectMsgClass(RelayResultReason.class);
        Assert.assertEquals(RelayResultReason.Succeed, resultReason2);

        neoSystem.blockchain.tell(block3, testKit.testActor());
        relayDirectly = testKit.expectMsgClass(LocalNode.RelayDirectly.class);
        Assert.assertNotNull(relayDirectly);
        Assert.assertEquals(block3.hash(), relayDirectly.inventory.hash());
        RelayResultReason resultReason3 = testKit.expectMsgClass(RelayResultReason.class);
        TR.debug(resultReason3);
        Assert.assertNotNull(resultReason3);

        // 2. send message and check response
        // case 1: send getheaders message
        TaskManager.Register register = new TaskManager.Register() {{
            version = new VersionPayload() {{
                version = Uint.ZERO;
                services = Ulong.ZERO;
                timestamp = new Uint(128384844);
                port = new Ushort(8080);
                nonce = Uint.ZERO;
                userAgent = "#ksjld#120322";
                startHeight = new Uint(4);
                relay = false;
            }};
        }};
        neoSystem.taskManager.tell(register, testKit.testActor()); // trigger Register event

        Message message = testKit.expectMsgClass(Message.class);
        Assert.assertEquals("getheaders", message.command);
        GetBlocksPayload blocksPayload = SerializeHelper.parse(GetBlocksPayload::new, message.payload);
        Assert.assertNotNull(blocksPayload);
        Assert.assertEquals(block3.hash(), blocksPayload.hashStart[0]);


        // case 2: send getblocks message
        Block block4 = new Block() {
            {
                prevHash = block3.hash();
                timestamp = new Uint(Long.toString(new Date("Jul 18 15:08:21 UTC 2018").getTime() / 1000));
                index = new Uint(4);
                consensusData = new Ulong(2083336984); //向比特币致敬
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
                                nonce = new Uint(2083236896);
                                attributes = new TransactionAttribute[0];
                                inputs = new CoinReference[0];
                                outputs = new TransactionOutput[0];
                                witnesses = new Witness[0];
                            }
                        }
                };
            }
        };
        block4.rebuildMerkleRoot();

        Block block5 = new Block() {
            {
                prevHash = block4.hash();
                timestamp = new Uint(Long.toString(new Date("Jul 19 15:08:21 UTC 2018").getTime() / 1000));
                index = new Uint(5);
                consensusData = new Ulong(2083436894); //向比特币致敬
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
                                nonce = new Uint(2086236896);
                                attributes = new TransactionAttribute[0];
                                inputs = new CoinReference[0];
                                outputs = new TransactionOutput[0];
                                witnesses = new Witness[0];
                            }
                        }
                };
            }
        };
        block5.rebuildMerkleRoot();


        System.err.println(Blockchain.singleton().getHeight());
        System.err.println(Blockchain.singleton().getHeaderHeight());

        Header[] headers = new Header[]{block4.getHeader(), block5.getHeader()};
        neoSystem.blockchain.tell(headers, testKit.testActor()); // this will invoke taskManager.HeaderTaskCompleted
        message = testKit.expectMsgClass(Message.class);
        Assert.assertEquals("getheaders", message.command);
        blocksPayload = SerializeHelper.parse(GetBlocksPayload::new, message.payload);
        Assert.assertNotNull(blocksPayload);
        Assert.assertEquals(block3.hash(), blocksPayload.hashStart[0]);


        // case 3: test NewTasks event
        TaskManager.NewTasks newTasks = new TaskManager.NewTasks() {{
            payload = new InvPayload() {{
                type = InventoryType.Block;
                hashes = new UInt256[]{block4.hash(), block5.hash()};
            }};
        }};
        neoSystem.taskManager.tell(newTasks, testKit.testActor());
        message = testKit.expectMsgClass(Message.class);
        Assert.assertEquals("getdata", message.command);
        InvPayload invPayload = SerializeHelper.parse(InvPayload::new, message.payload);
        Assert.assertEquals(InventoryType.Block, invPayload.type);
        Assert.assertEquals(block4.hash(), invPayload.hashes[0]);
        Assert.assertEquals(block5.hash(), invPayload.hashes[1]);


        // case 4: test TaskCompleted event
        TaskManager.TaskCompleted completed = new TaskManager.TaskCompleted() {
            {
                hash = block4.hash();
            }
        };
        neoSystem.taskManager.tell(completed, testKit.testActor());
        testKit.expectNoMessage(); // as some block4, block5 are in the queue


        // case 5: test RestartTasks event
        TaskManager.RestartTasks restartTasks = new TaskManager.RestartTasks() {{
            payload = new InvPayload() {{
                type = InventoryType.Block;
                hashes = new UInt256[]{block4.hash(), block5.hash()};
            }};
        }};
        neoSystem.taskManager.tell(restartTasks, testKit.testActor()); // message send to LocalNode
        message = testKit.expectMsgClass(Message.class);
        Assert.assertEquals("getdata", message.command);
        invPayload = SerializeHelper.parse(InvPayload::new, message.payload);
        Assert.assertEquals(block4.hash(), invPayload.hashes[0]);
        Assert.assertEquals(block5.hash(), invPayload.hashes[1]);


        // case 6: test Timer event, it will sleep 1 minute for timeout.
//        TR.debug("it will sleep 1 minute for timeout");
//        Thread.sleep(Duration.ofMinutes(1).toMillis());
//        TaskManager.Timer timer = new TaskManager.Timer();
//        neoSystem.taskManager.tell(timer, testKit.testActor()); // message send to LocalNode
//        message = testKit.expectMsgClass(Message.class);
//        Assert.assertEquals("getblocks", message.command);
//        blocksPayload = SerializeHelper.parse(GetBlocksPayload::new, message.payload);
//        Assert.assertNotNull(blocksPayload);
//        Assert.assertEquals(block3.hash(), blocksPayload.hashStart[0]);
//        testKit.expectNoMessage();


        // case 7: test Terminated event, it will failed, maybe Terminated cannot be send.
//        Terminated terminated = new Terminated(testKit.testActor(), false, true);
//        neoSystem.taskManager.tell(terminated, testKit.testActor()); // message send to LocalNode
//        testKit.expectNoMessage();
    }

}