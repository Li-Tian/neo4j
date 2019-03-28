package neo.consensus;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import akka.testkit.TestActorRef;
import akka.testkit.TestKit;
import neo.Fixed8;
import neo.MyNeoSystem;
import neo.NeoSystem;
import neo.UInt160;
import neo.UInt256;
import neo.Wallets.SQLite.Account;
import neo.Wallets.WalletAccount;
import neo.cryptography.ecc.ECPoint;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.exception.FormatException;
import neo.io.SerializeHelper;
import neo.ledger.Blockchain;
import neo.ledger.MyBlockchain2;
import neo.ledger.RelayResultReason;
import neo.ledger.ValidatorState;
import neo.network.p2p.LocalNode;
import neo.network.p2p.MyLocalNode;
import neo.network.p2p.TaskManager;
import neo.network.p2p.payloads.Block;
import neo.network.p2p.payloads.CoinReference;
import neo.network.p2p.payloads.ConsensusPayload;
import neo.network.p2p.payloads.Header;
import neo.network.p2p.payloads.MinerTransaction;
import neo.network.p2p.payloads.Transaction;
import neo.network.p2p.payloads.TransactionAttribute;
import neo.network.p2p.payloads.TransactionOutput;
import neo.network.p2p.payloads.Witness;
import neo.persistence.AbstractBlockchainTest;
import neo.persistence.AbstractLeveldbTest;
import neo.persistence.Snapshot;
import neo.smartcontract.Contract;
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
        // 构建 validators
        // 直接从db构建，跳过 blockchian.persist 环节
        snapshot = Blockchain.singleton().getSnapshot();
        WalletAccount account = wallet.getAccounts().iterator().next();
        ValidatorState validatorState = new ValidatorState() {{
            publicKey = account.getKey().publicKey;
            registered = true;
            votes = Fixed8.fromDecimal(BigDecimal.valueOf(100000));
        }};
        snapshot.getValidators().add(validatorState.publicKey, validatorState);
        snapshot.commit();


        // 区块数据
        // init leveldb data
        UInt160 consensusAddress = Blockchain.singleton().getConsensusAddress(snapshot.getValidatorPubkeys());
        block1 = new MyBlock() {
            {
                prevHash = Blockchain.GenesisBlock.hash();
                timestamp = new Uint(Long.toString(new Date("Jul 15 15:08:21 UTC 2018").getTime() / 1000));
                index = new Uint(1);
                consensusData = new Ulong(2083236894); //向比特币致敬
                nextConsensus = consensusAddress;
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
                nextConsensus = consensusAddress;
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
        context.fill();

        Assert.assertTrue(context.transactionExists(block1.transactions[0].hash()));
        Assert.assertTrue(context.transactionExists(block2.transactions[0].hash()));
        Assert.assertFalse(context.transactionExists(context.transactionHashes[0]));
    }

    @Test
    public void verifyTransaction() {
        context.fill();

        boolean result = context.verifyTransaction(context.transactions.get(context.transactionHashes[0]));
        Assert.assertTrue(result);
    }

    @Test
    public void changeView() {
        Uint oldPrimary = context.primaryIndex;
        boolean hasSignature = (context.state.value() & ConsensusState.SignatureSent.value()) != 0 ? true : false;
        context.changeView((byte) 0x01);

        Assert.assertEquals(hasSignature, context.state.hasFlag(ConsensusState.SignatureSent));
        Assert.assertEquals(0x01, context.viewNumber);
        Assert.assertEquals(oldPrimary.intValue() - 1, context.primaryIndex.intValue());
        Assert.assertEquals(0x01, context.expectedView[context.myIndex]);
    }

    @Test
    public void createBlock() {
        context.fill();
        byte[] signature = new byte[64];
        Arrays.fill(signature, (byte) 0x01);

        for (int i = 0; i < context.getM(); i++) {
            context.signatures[i] = signature;
        }
        Block block = context.createBlock();
        Assert.assertEquals(1, block.getWitnesses().length);
        Assert.assertEquals(1, block.transactions.length);
    }

    @Test
    public void makeHeader() {
        context.fill();
        Block header = context.makeHeader();
        Assert.assertEquals(context.VERSION, header.version);
        Assert.assertEquals(context.prevHash, header.prevHash);
        Assert.assertEquals(context.blockIndex, header.index);
        Assert.assertEquals(context.nextConsensus, header.nextConsensus);
        Assert.assertEquals(context.transactionHashes.length, header.transactions.length);
    }

    @Test
    public void signHeader() {
        context.fill();
        Block header = context.makeHeader();
        header.consensusData = Ulong.ZERO;
        context.signHeader();
        Assert.assertNotNull(context.signatures[context.myIndex]);
    }

    @Test
    public void makePrepareRequest() {
        context.fill();
        context.signHeader();

        ConsensusPayload payload = context.makePrepareRequest();
        PrepareRequest request = SerializeHelper.parse(PrepareRequest::new, payload.data);
        Assert.assertEquals(context.nonce, request.nonce);
        Assert.assertEquals(context.nextConsensus, request.nextConsensus);
        Assert.assertArrayEquals(context.transactionHashes, request.transactionHashes);
        Assert.assertArrayEquals(context.signatures[context.myIndex], request.signature);
        Assert.assertEquals(context.prevHash, payload.prevHash);
        Assert.assertEquals(context.blockIndex, payload.blockIndex);
        Assert.assertEquals(context.myIndex, payload.validatorIndex.intValue());
        Assert.assertEquals(context.timestamp, payload.timestamp);
    }

    @Test(expected = FormatException.class)
    public void makeChangeView() {
        context.fill();
        context.expectedView[context.myIndex] = 0x01;
        ConsensusPayload payload = context.makeChangeView();
        ChangeView changeView = SerializeHelper.parse(ChangeView::new, payload.data);
        Assert.assertEquals(0x00, changeView.viewNumber);
        Assert.assertEquals(context.expectedView[context.myIndex], changeView.newViewNumber);

        context.expectedView[context.myIndex] = 0x00;
        payload = context.makeChangeView();
        SerializeHelper.parse(ChangeView::new, payload.data);
    }

    @Test
    public void makePrepareResponse() {
        context.fill();

        byte[] signature = new byte[64];
        Arrays.fill(signature, (byte) 0x01);

        ConsensusPayload payload = context.makePrepareResponse(signature);
        PrepareResponse response = SerializeHelper.parse(PrepareResponse::new, payload.data);
        Assert.assertArrayEquals(signature, response.signature);
    }

    @Test
    public void getPrimaryIndex() {
        Uint primary = context.getPrimaryIndex((byte) 0x00);
        Assert.assertEquals(new Uint(3), primary);
    }

    @Test
    public void reset() {
        context.reset();

        int myIndex = context.myIndex;
        ECPoint[] validatorPoints = context.validators;

        WalletAccount account = wallet.getAccounts().iterator().next();
        boolean foundPublicKey = false;
        for (int i = 0; i < validatorPoints.length; i++) {
            if (validatorPoints[i].compareTo(account.getKey().publicKey) == 0) {
                Assert.assertEquals(i, myIndex);
                foundPublicKey = true;
                break;
            }
        }
        Assert.assertTrue(foundPublicKey);
    }

    @Test
    public void fill() {
        context.fill();
        UInt256 mxHash = context.transactionHashes[0];
        MinerTransaction mx = (MinerTransaction) context.transactions.get(mxHash);
        Assert.assertNotNull(mx);
        Assert.assertEquals(0, mx.outputs.length);
        UInt160 nextConsensus = Blockchain.getConsensusAddress(snapshot.getValidators(Collections.singleton(mx)));
        Assert.assertEquals(context.nextConsensus, nextConsensus);
    }

    @Test
    public void verifyRequest() {
        context.fill();
        Assert.assertFalse(context.verifyRequest());
        context.state = context.state.or(ConsensusState.RequestReceived);
        Assert.assertTrue(context.verifyRequest());
    }
}