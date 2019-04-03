package neo.ledger;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;

import akka.testkit.TestActorRef;
import akka.testkit.TestKit;
import neo.MyNeoSystem;
import neo.NeoSystem;
import neo.UInt160;
import neo.UInt256;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.csharp.Ushort;
import neo.io.actors.Idle;
import neo.network.p2p.LocalNode;
import neo.network.p2p.MyLocalNode;
import neo.network.p2p.MyTaskManager;
import neo.network.p2p.TaskManager;
import neo.network.p2p.payloads.Block;
import neo.network.p2p.payloads.CoinReference;
import neo.network.p2p.payloads.Header;
import neo.network.p2p.payloads.IssueTransaction;
import neo.network.p2p.payloads.MinerTransaction;
import neo.network.p2p.payloads.Transaction;
import neo.network.p2p.payloads.TransactionAttribute;
import neo.network.p2p.payloads.TransactionOutput;
import neo.network.p2p.payloads.Witness;
import neo.persistence.AbstractLeveldbTest;
import neo.smartcontract.Contract;
import neo.vm.OpCode;

import static neo.ledger.Blockchain.GoverningToken;
import static neo.ledger.Blockchain.StandbyValidators;
import static neo.ledger.Blockchain.UtilityToken;

public class BlockChainTest extends AbstractLeveldbTest {
    private static NeoSystem neoSystem;
    private static TestKit testKit;
    private static MyBlockchain2 blockchain;

    @BeforeClass
    public static void setUp() throws IOException {
        AbstractLeveldbTest.setUp(BlockChainTest.class.getSimpleName());
        neoSystem = new MyNeoSystem(store, self -> {
            testKit = new TestKit(self.actorSystem);

            // Synchronous Unit Testing with TestActorRef
            self.blockchain = TestActorRef.create(self.actorSystem, MyBlockchain2.props(self, store, testKit.testActor()));
            self.localNode = TestActorRef.create(self.actorSystem, MyLocalNode.props(self, testKit.testActor()));
            self.taskManager = TestActorRef.create(self.actorSystem, MyTaskManager.props(self, testKit.testActor()));
            self.consensus = TestActorRef.create(self.actorSystem, MyConsensusService.props(self, testKit.testActor()));

            blockchain = ((TestActorRef<MyBlockchain2>) self.blockchain).underlyingActor();
        });
    }

    @AfterClass
    public static void tearDown() throws IOException {
        AbstractLeveldbTest.tearDown(BlockChainTest.class.getSimpleName());
    }

    public static final Block testBlock1 = new Block() {
        {
            prevHash = UInt256.Zero;
            timestamp = new Uint(Long.toString(new Date("Jul 15 15:08:50 UTC 2016").getTime() / 1000));
            index = Uint.ONE;
            consensusData = new Ulong(323435);
            nextConsensus = UInt160.parse("0x4b5acd30ba7ec77199561afa0bbd49b5e94517da");
            witness = new Witness() {
                {
                    invocationScript = new byte[0];
                    verificationScript = new byte[]{OpCode.PUSHT.getCode()};
                }
            };
            transactions = new Transaction[]{
                    new MinerTransaction() {
                        {
                            nonce = new Uint(2083236893);
                            attributes = new TransactionAttribute[0];
                            inputs = new CoinReference

                                    [0];
                            outputs = new TransactionOutput[0];
                            witnesses = new Witness[0];
                        }
                    },
                    GoverningToken,
                    UtilityToken,
                    new IssueTransaction() {
                        {
                            attributes = new TransactionAttribute[0];
                            inputs = new CoinReference[0];
                            outputs = new TransactionOutput[]{
                                    new TransactionOutput() {
                                        {
                                            assetId = GoverningToken.hash();
                                            value = GoverningToken.amount;
                                            scriptHash = UInt160.parseToScriptHash(Contract.createMultiSigRedeemScript(StandbyValidators.length / 2 + 1, StandbyValidators));
                                        }
                                    }
                            };
                            witnesses = new Witness[]{
                                    new Witness() {
                                        {
                                            invocationScript = new byte[0];
                                            verificationScript = new byte[]{OpCode.PUSHT.getCode()};
                                        }
                                    }
                            };
                        }
                    }
            };
        }
    };

    public static final MyBlock testBlock2 = new MyBlock() {
        {
            prevHash = UInt256.Zero;
            timestamp = new Uint(Long.toString(new Date("Jul 15 15:08:55 UTC 2016").getTime() / 1000));
            index = new Uint(3);
            consensusData = new Ulong(323433455);
            nextConsensus = Blockchain.getConsensusAddress(StandbyValidators);
            witness = new Witness() {
                {
                    invocationScript = new byte[0];
                    verificationScript = new byte[]{OpCode.PUSHT.getCode()};
                }
            };
            transactions = new Transaction[]{
                    new MinerTransaction() {
                        {
                            nonce = new Uint(2083236893);
                            attributes = new TransactionAttribute[0];
                            inputs = new CoinReference

                                    [0];
                            outputs = new TransactionOutput[0];
                            witnesses = new Witness[0];
                        }
                    },
                    GoverningToken,
                    UtilityToken,
                    new IssueTransaction() {
                        {
                            attributes = new TransactionAttribute[0];
                            inputs = new CoinReference[0];
                            outputs = new TransactionOutput[]{
                                    new TransactionOutput() {
                                        {
                                            assetId = GoverningToken.hash();
                                            value = GoverningToken.amount;
                                            scriptHash = UInt160.parseToScriptHash(Contract.createMultiSigRedeemScript(StandbyValidators.length / 2 + 1, StandbyValidators));
                                        }
                                    }
                            };
                            witnesses = new Witness[]{
                                    new Witness() {
                                        {
                                            invocationScript = new byte[0];
                                            verificationScript = new byte[]{OpCode.PUSHT.getCode()};
                                        }
                                    }
                            };
                        }
                    }
            };
        }
    };

    @Test
    public void testEvent() {
        //case Register
        neoSystem.blockchain.tell(new Blockchain.Register(), testKit.testActor());
        testKit.expectNoMessage();

        //case Import
        neoSystem.blockchain.tell(blockchain.new Import() {{
            blocks = new Block[0];
        }}, testKit.testActor());
        testKit.expectMsgClass(Blockchain.ImportCompleted.class);

        neoSystem.blockchain.tell(blockchain.new Import() {{
            blocks = new Block[]{new Block() {{
                index = Uint.ZERO;
            }}};
        }}, testKit.testActor());
        testKit.expectMsgClass(Blockchain.ImportCompleted.class);

        Block block = testBlock1;
        block.rebuildMerkleRoot();
        neoSystem.blockchain.tell(blockchain.new Import() {{
            blocks = new Block[]{block};
        }}, testKit.testActor());
        testKit.expectMsgClass(Blockchain.PersistCompleted.class);
        testKit.expectMsgClass(Blockchain.PersistCompleted.class);
        testKit.expectMsgClass(Blockchain.ImportCompleted.class);
        blockchain = ((TestActorRef<MyBlockchain2>) neoSystem.blockchain).underlyingActor();
        Assert.assertEquals(true, blockchain.headerIndex.contains(block.hash()));
        Assert.assertEquals(block.hash(), store.getSnapshot().getHeaderHashIndex().get().hash);
        Assert.assertEquals(block.index, store.getSnapshot().getHeaderHashIndex().get().index);
        Assert.assertEquals(true, store.getSnapshot().getBlocks().tryGet(block.hash()) != null);
        Assert.assertEquals(block.hash(), store.getSnapshot().getBlockHashIndex().get().hash);
        Assert.assertEquals(block.index, store.getSnapshot().getBlockHashIndex().get().index);
        //Invalid Block
        neoSystem.blockchain.tell(blockchain.new Import() {{
            blocks = new Block[]{new Block() {{
                index = blockchain.height().add(new Uint(2));
            }}};
        }}, testKit.testActor());
        testKit.expectNoMessage();

        //Case HeaderArray
        Header oldHeader = blockchain.getSnapshot().getHeader(blockchain.getHeight());
        MyHeader header = new MyHeader();
        header.prevHash = oldHeader.hash();
        header.timestamp = new Uint(Long.toString(new Date("Jul 15 15:08:51 UTC 2016").getTime() / 1000));
        header.index = oldHeader.index.add(Uint.ONE);
        header.consensusData = oldHeader.consensusData;
        header.nextConsensus = oldHeader.nextConsensus;
        header.merkleRoot = oldHeader.merkleRoot;
        header.witness = oldHeader.witness;
        neoSystem.blockchain.tell(new Header[]{header}, testKit.testActor());
        testKit.expectMsgClass(TaskManager.HeaderTaskCompleted.class);
        blockchain = ((TestActorRef<MyBlockchain2>) neoSystem.blockchain).underlyingActor();
        Assert.assertEquals(true, blockchain.headerIndex.contains(header.hash()));
        Assert.assertEquals(header.trim().hash(), blockchain.getSnapshot().getBlocks().get(header.hash()).trimmedBlock.hash());
        Assert.assertEquals(header.hash(), blockchain.getSnapshot().getHeaderHashIndex().get().hash);
        Assert.assertEquals(header.index, blockchain.getSnapshot().getHeaderHashIndex().get().index);

        //Case Block
        //1, Block already exist
        neoSystem.blockchain.tell(new Block() {
            {
                index = Uint.ZERO;
            }
        }, testKit.testActor());
        testKit.expectMsg(RelayResultReason.AlreadyExists);
        //2, Invalid Block
        testBlock2.index = new Uint(2);
        testBlock2.prevHash = testBlock1.hash();
        testBlock2.rebuildMerkleRoot();
        neoSystem.blockchain.tell(testBlock2, testKit.testActor());
        testKit.expectMsg(RelayResultReason.Invalid);
        //3, Block UnableToVerify
        testBlock2.index = new Uint(10);
        testBlock2.prevHash = testBlock1.hash();
        testBlock2.rebuildMerkleRoot();
        neoSystem.blockchain.tell(testBlock2, testKit.testActor());
        testKit.expectMsg(RelayResultReason.UnableToVerify);
        //4, New Block
        testBlock2.prevHash = testBlock1.hash();
        testBlock2.index = new Uint(3);
        testBlock2.rebuildMerkleRoot();
        neoSystem.blockchain.tell(testBlock2, testKit.testActor());
        testKit.expectMsgClass(LocalNode.RelayDirectly.class);
        testKit.expectMsg(RelayResultReason.Succeed);
        blockchain = ((TestActorRef<MyBlockchain2>) neoSystem.blockchain).underlyingActor();
        Assert.assertEquals(true, blockchain.headerIndex.contains(testBlock2.hash()));
        Assert.assertEquals(testBlock2.hash(), store.getSnapshot().getHeaderHashIndex().get().hash);
        Assert.assertEquals(testBlock2.index, store.getSnapshot().getHeaderHashIndex().get().index);
        Assert.assertEquals(true, store.getSnapshot().getBlocks().tryGet(testBlock2.hash()) != null);

        //Case Transaction
        //Invalid Transaction
        neoSystem.blockchain.tell(Blockchain.GenesisBlock.transactions[0], testKit.testActor());
        testKit.expectMsg(RelayResultReason.Invalid);
        //AlreadyExist Transaction
        neoSystem.blockchain.tell(Blockchain.GenesisBlock.transactions[1], testKit.testActor());
        testKit.expectMsg(RelayResultReason.AlreadyExists);
        //Successful Transaction
        MyTransaction transaction = new MyTransaction();
        neoSystem.blockchain.tell(transaction, testKit.testActor());
        testKit.expectMsgClass(LocalNode.RelayDirectly.class);
        testKit.expectMsg(RelayResultReason.Succeed);
        Assert.assertEquals(true, blockchain.getMemPool().containsKey(transaction.hash()));

        //Case ConsensusPayload
        MyConsensusPayload payload = new MyConsensusPayload() {{
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
        neoSystem.blockchain.tell(payload, testKit.testActor());
        testKit.expectMsgClass(MyConsensusPayload.class);
        testKit.expectMsgClass(LocalNode.RelayDirectly.class);
        testKit.expectMsg(RelayResultReason.Succeed);
        Assert.assertEquals(true, blockchain.relayCache.contains(payload));

        //Case Idle
        neoSystem.blockchain.tell(Idle.instance(), testKit.testActor());
        testKit.expectNoMessage();
    }
}