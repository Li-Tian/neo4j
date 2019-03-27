package neo.consensus;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;

import akka.testkit.TestActorRef;
import akka.testkit.TestKit;
import neo.Fixed8;
import neo.MyNeoSystem;
import neo.NeoSystem;
import neo.UInt160;
import neo.Wallets.SQLite.Account;
import neo.Wallets.WalletAccount;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.ledger.Blockchain;
import neo.ledger.MyBlockchain2;
import neo.ledger.RelayResultReason;
import neo.ledger.ValidatorState;
import neo.network.p2p.LocalNode;
import neo.network.p2p.MyLocalNode;
import neo.network.p2p.TaskManager;
import neo.network.p2p.payloads.Block;
import neo.network.p2p.payloads.CoinReference;
import neo.network.p2p.payloads.Header;
import neo.network.p2p.payloads.MinerTransaction;
import neo.network.p2p.payloads.Transaction;
import neo.network.p2p.payloads.TransactionAttribute;
import neo.network.p2p.payloads.TransactionOutput;
import neo.network.p2p.payloads.Witness;
import neo.persistence.AbstractBlockchainTest;
import neo.persistence.AbstractLeveldbTest;
import neo.persistence.Snapshot;
import neo.vm.OpCode;

import static org.junit.Assert.*;

public class ConsensusContextTest extends AbstractBlockchainTest {

    private static MyWallet wallet;
    private static ConsensusContext context;
    private static NeoSystem neoSystem;
    private static TestKit testKit;
    private static Snapshot snapshot;
    private static MyBlockchain2 blockchain2;
    private static MyBlock block1;
    private static MyBlock block2;


    @BeforeClass
    public static void setUp() throws IOException {
        AbstractBlockchainTest.setUp();
        initOnce();
    }

    // 屏蔽掉验证模块的验证，等那边完善
    public static class MyBlock extends Block {
        @Override
        public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
            return new UInt160[0];
        }

        @Override
        public Witness[] getWitnesses() {
            return new Witness[0];
        }
    }


    private static void initOnce() {
        wallet = new MyWallet();
        context = new ConsensusContext(wallet);
        neoSystem = new MyNeoSystem(store, self -> {
            testKit = new TestKit(self.actorSystem);

            // Synchronous Unit Testing with TestActorRef
            self.blockchain = TestActorRef.create(self.actorSystem, MyBlockchain2.props(self, store, testKit.testActor()));
            self.localNode = TestActorRef.create(self.actorSystem, MyLocalNode.props(self, testKit.testActor()));
            self.taskManager = TestActorRef.create(self.actorSystem, TaskManager.props(self));
            self.consensus = null;
        });

        // TODO 初始化 leveldb 数据
        // 区块数据
        // validators 数据准备
        // init leveldb data
        block1 = new MyBlock() {
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
        block2 = new MyBlock() {
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

        // 构建 validators
        // 直接从db构建，跳过 blockchian.persist 环节
        WalletAccount account = wallet.getAccounts().iterator().next();
        ValidatorState validatorState = new ValidatorState() {{
            publicKey = account.getKey().publicKey;
            registered = true;
            votes = Fixed8.fromDecimal(BigDecimal.valueOf(100000));
        }};
        snapshot = Blockchain.singleton().getSnapshot();

        System.err.println("privateKey: " + BitConverter.toHexString(account.getKey().privateKey));

        snapshot.getValidators().add(validatorState.publicKey, validatorState);
        snapshot.commit();

        // init consensus context
        context.reset();
    }

    @AfterClass
    public static void tearDown() throws IOException {
        AbstractLeveldbTest.tearDown();
    }


    @Test
    public void getM() {
        Assert.assertEquals(5, context.getM());
    }

    @Test
    public void getPrevHeader() {
        Header header = context.getPrevHeader();
        Assert.assertEquals(block2.prevHash, header.prevHash);
        Assert.assertEquals(block2.getHeader().hash(), header.hash());
    }

    @Test
    public void transactionExists() {

    }

    @Test
    public void verifyTransaction() {
    }

    @Test
    public void changeView() {
    }

    @Test
    public void createBlock() {
    }

    @Test
    public void makeHeader() {
    }

    @Test
    public void signHeader() {
    }

    @Test
    public void makePrepareRequest() {
    }

    @Test
    public void makeChangeView() {
    }

    @Test
    public void makePrepareResponse() {
    }

    @Test
    public void getPrimaryIndex() {
    }

    @Test
    public void reset() {
    }

    @Test
    public void fill() {
    }

    @Test
    public void verifyRequest() {
    }
}