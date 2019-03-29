package neo.consensus;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Date;

import akka.actor.ActorRef;
import akka.testkit.TestActorRef;
import akka.testkit.TestKit;
import neo.Fixed8;
import neo.MyNeoSystem;
import neo.NeoSystem;
import neo.TimeProvider;
import neo.UInt160;
import neo.UInt256;
import neo.Wallets.KeyPair;
import neo.Wallets.WalletAccount;
import neo.cryptography.ecc.ECPoint;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.csharp.Ushort;
import neo.io.SerializeHelper;
import neo.ledger.Blockchain;
import neo.ledger.MyBlockchain2;
import neo.ledger.RelayResultReason;
import neo.ledger.ValidatorState;
import neo.network.p2p.LocalNode;
import neo.network.p2p.MyLocalNode;
import neo.network.p2p.MyTaskManager;
import neo.network.p2p.TaskManager;
import neo.network.p2p.payloads.Block;
import neo.network.p2p.payloads.CoinReference;
import neo.network.p2p.payloads.ConsensusPayload;
import neo.network.p2p.payloads.ContractTransaction;
import neo.network.p2p.payloads.IVerifiable;
import neo.network.p2p.payloads.InventoryType;
import neo.network.p2p.payloads.MinerTransaction;
import neo.network.p2p.payloads.Transaction;
import neo.network.p2p.payloads.TransactionAttribute;
import neo.network.p2p.payloads.TransactionAttributeUsage;
import neo.network.p2p.payloads.TransactionOutput;
import neo.network.p2p.payloads.Witness;
import neo.persistence.AbstractBlockchainTest;
import neo.persistence.AbstractLeveldbTest;
import neo.persistence.Snapshot;
import neo.vm.OpCode;
import scala.PartialFunction;


public class ConsensusServiceTest extends AbstractBlockchainTest {

    private static MyWallet wallet;
    private static ConsensusContext context;
    private static NeoSystem neoSystem;
    private static TestKit testKit;
    private static Snapshot snapshot;
    private static MyBlockchain2 blockchain2;
    private static ConsensusContextTest.MyBlock block1;
    private static ConsensusContextTest.MyBlock block2;
    private static ConsensusService consensusService;

    // 总共14个方法
    @BeforeClass
    public static void setUp() throws IOException {
        AbstractBlockchainTest.setUp();
        initOnce();
    }

    @AfterClass
    public static void tearDown() throws IOException {
        AbstractLeveldbTest.tearDown();
    }

    private static void initOnce() {
        wallet = new MyWallet();
        context = new ConsensusContext(wallet);
        neoSystem = new MyNeoSystem(store, self -> {
            testKit = new TestKit(self.actorSystem);

            // Synchronous Unit Testing with TestActorRef
            self.blockchain = TestActorRef.create(self.actorSystem, MyBlockchain2.props(self, store, testKit.testActor()));
            self.localNode = TestActorRef.create(self.actorSystem, MyLocalNode.props(self, testKit.testActor()));
            self.taskManager = TestActorRef.create(self.actorSystem, MyTaskManager.props(self, testKit.testActor()));
            self.consensus = null;
        });


        //  初始化 leveldb 数据
        // 构建 validators
        // 直接从db构建，跳过 blockchian.persist 环节
        snapshot = Blockchain.singleton().getSnapshot();
        int i = 0;
        for (WalletAccount account : wallet.getAccounts()) {
            Fixed8 myVote =  Fixed8.fromDecimal(BigDecimal.valueOf(100000 - i * 100));
            ValidatorState validatorState = new ValidatorState() {{
                publicKey = account.getKey().publicKey;
                registered = true;
                votes = myVote;
            }};
            snapshot.getValidators().add(validatorState.publicKey, validatorState);
            i++;
        }
        snapshot.commit();


        // 区块数据
        // init leveldb data
        UInt160 consensusAddress = Blockchain.singleton().getConsensusAddress(snapshot.getValidatorPubkeys());
        block1 = new ConsensusContextTest.MyBlock() {
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

        block2 = new ConsensusContextTest.MyBlock() {
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


        // start consensus
        TestActorRef<ConsensusService> consensusServiceTestActorRef = TestActorRef.create(neoSystem.actorSystem, ConsensusService.props(neoSystem.localNode, neoSystem.taskManager, context));
        neoSystem.consensus = consensusServiceTestActorRef;
        consensusService = consensusServiceTestActorRef.underlyingActor();
        neoSystem.consensus.tell(new ConsensusService.Start(), ActorRef.noSender());
        testKit.expectNoMessage();

        // context 进行了更新，这里需要处理下
        Assert.assertEquals(0, context.viewNumber);
        if (context.myIndex == context.primaryIndex.intValue()) {
            Assert.assertTrue(context.state.hasFlag(ConsensusState.Primary));
        } else {
            Assert.assertTrue(context.state.hasFlag(ConsensusState.Backup));
        }
        testKit.ignoreMsg(partialFunction);  // ignore timer
    }

    // 忽略共识的超时消息，减少对测试消息的干扰
    private static PartialFunction partialFunction = new PartialFunction<Object, Boolean>() {

        @Override
        public Boolean apply(Object v1) {
            return v1 instanceof ConsensusService.Timer;
        }

        @Override
        public boolean isDefinedAt(Object x) {
            return false;
        }
    };

    @Test
    public void testEvent() {
        // test SetViewNumber event
        int primaryIndex = context.primaryIndex.intValue();

        ConsensusService.SetViewNumber setViewNumber = new ConsensusService.SetViewNumber() {{
            viewNumber = 0x01;
        }};
        neoSystem.consensus.tell(setViewNumber, testKit.testActor());
        testKit.expectNoMessage();
        Assert.assertEquals(3, context.blockIndex.intValue());
        Assert.assertEquals(0x01, context.viewNumber);
        Assert.assertEquals(primaryIndex - 1, context.primaryIndex.intValue());


        // test Blockchain.PersistCompleted.
        // 发送 block3 更新，共识模块的数据将会被更新
        ConsensusContextTest.MyBlock block3 = new ConsensusContextTest.MyBlock() {
            {
                prevHash = block2.hash();
                timestamp = new Uint(Long.toString(new Date("Jul 16 16:08:21 UTC 2018").getTime() / 1000));
                index = new Uint(3);
                consensusData = new Ulong(2083236994); //向比特币致敬
                nextConsensus = block2.nextConsensus;
                witness = new Witness() {
                    {
                        invocationScript = new byte[0];
                        verificationScript = new byte[]{OpCode.PUSHT.getCode()};
                    }
                };
                transactions = new Transaction[]{
                        new MinerTransaction() {
                            {
                                nonce = new Uint(2083238593);
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


        neoSystem.blockchain.tell(block3, testKit.testActor());
        LocalNode.RelayDirectly relayDirectly = testKit.expectMsgClass(LocalNode.RelayDirectly.class);
        Assert.assertNotNull(relayDirectly);
        Assert.assertEquals(block3.hash(), relayDirectly.inventory.hash());

        RelayResultReason resultReason1 = testKit.expectMsg(RelayResultReason.Succeed);
        Assert.assertEquals(RelayResultReason.Succeed, resultReason1);

        //  那么，进入下一个区块的共识过程
        Assert.assertEquals(4, context.blockIndex.intValue());
        Assert.assertEquals(0x00, context.viewNumber);
        Assert.assertEquals(4 % context.validators.length, context.primaryIndex.intValue());


        // test Transaction.class event
        MyContractTransaction contractTransaction = new MyContractTransaction() {{
            attributes = new TransactionAttribute[]{
                    new TransactionAttribute() {{
                        usage = TransactionAttributeUsage.Remark1;
                        data = "hello world".getBytes();
                    }}
            };
        }};

        Blockchain.singleton().getMemPool().tryAdd(contractTransaction.hash(), contractTransaction);
        context.state = context.state.or(ConsensusState.RequestReceived);
        context.fill();
        context.transactions.remove(contractTransaction.hash());
        neoSystem.consensus.tell(contractTransaction, testKit.testActor());
        // ConsensusService --- LocalNode.SendDirectly(signature) --> LocalNode(TestActor proxy)
        // 交易收齐之后，就会发送Signature到外面
        LocalNode.SendDirectly sendDirectly = testKit.expectMsgClass(LocalNode.SendDirectly.class);
        ConsensusPayload payload = (ConsensusPayload) sendDirectly.inventory;
        PrepareResponse response = SerializeHelper.parse(PrepareResponse::new, payload.data);
        Assert.assertNotNull(response);


        // test ConsensusPayload event: case 1: ChangeView,  2, PrepareRequest, 3. PrepareResponse
        // case 1: ChangeView msg
        ChangeView changeView = new ChangeView(){{
            viewNumber  = 0x00;
            newViewNumber = 0x01;
        }};
       final byte[] tmp = SerializeHelper.toBytes(changeView);
        ConsensusPayload consensusPayload = new ConsensusPayload(){{
            version = ConsensusContext.VERSION;
            validatorIndex = new Ushort((context.myIndex +1)% context.validators.length);
            prevHash = context.prevHash;
            blockIndex = context.blockIndex;
           data = tmp;
        }};
        neoSystem.consensus.tell(consensusPayload, testKit.testActor());
        testKit.expectNoMessage();
        Assert.assertEquals(0x01, context.expectedView[consensusPayload.validatorIndex.intValue()]);

        // case 2: PrepareRequest (from Primary node)
        // 确定出 Primary
        context.reset();
        if (context.primaryIndex.intValue() == context.myIndex){
            context.state = context.state.or(ConsensusState.Primary);
        }else{
            context.state = context.state.or(ConsensusState.Backup);
        }

        ECPoint primaryPoint = context.validators[context.primaryIndex.intValue()];
        KeyPair primaryKeyPair = null;
        for (WalletAccount account: wallet.getAccounts()){
            if (account.getKey().publicKey.compareTo(primaryPoint) == 0){
                primaryKeyPair = account.getKey();
                break;
            }
        }

        MinerTransaction mx1 = new MinerTransaction(){{
            nonce = getNonce().uintValue();
        }};
        ContractTransaction tx1 = new ContractTransaction(){{
            attributes = new TransactionAttribute[]{
                new TransactionAttribute(){{
                    usage = TransactionAttributeUsage.Remark1;
                    data = "test 123".getBytes();
                }}
            } ;
        }};

        PrepareRequest prepareRequest = new PrepareRequest(){{
            nonce = getNonce();
            nextConsensus = block3.nextConsensus;
            transactionHashes = new UInt256[]{mx1.hash(), tx1.hash()};
            minerTransaction = mx1;
        }};

        Block proposalBlock = new Block(){{
            version = ConsensusContext.VERSION;
            prevHash = context.prevHash;
            timestamp =new Uint((int) TimeProvider.current().utcNow().getTime());
            index = new Uint(4);
            consensusData = prepareRequest.nonce;
            nextConsensus = context.nextConsensus;
            witness = new Witness();
        }};
        proposalBlock.transactions = new Transaction[]{mx1, tx1};
        proposalBlock.rebuildMerkleRoot();
        prepareRequest.signature  = IVerifiable.sign(proposalBlock, primaryKeyPair);
        consensusPayload = new ConsensusPayload(){{
            version = ConsensusContext.VERSION;
            prevHash = context.prevHash;
            validatorIndex = new Ushort(context.primaryIndex.intValue());
            prevHash = context.prevHash;
            blockIndex = context.blockIndex;
            timestamp = proposalBlock.timestamp;
            data = SerializeHelper.toBytes(prepareRequest);
        }};

        neoSystem.consensus.tell(consensusPayload, testKit.testActor());
        // ConsensusService ---- (TaskManager.RestartTasks) ---> taskManager
        TaskManager.RestartTasks restartTasks = testKit.expectMsgClass(TaskManager.RestartTasks.class);
        Assert.assertNotNull(restartTasks.payload);
        Assert.assertEquals(InventoryType.Tx, restartTasks.payload.type);
        Assert.assertEquals(1, restartTasks.payload.hashes.length); // mx 已经附带在 payload中
        Assert.assertEquals(tx1.hash(), restartTasks.payload.hashes[0]);



        // case 3: PrepareResponse
        // 寻找一个 backup进行签名

         primaryPoint = context.validators[context.primaryIndex.intValue()];
        ECPoint myPoint = context.validators[context.myIndex];
        int backupIndex = 0;
        KeyPair backupKeyPair = null;
        for (WalletAccount account: wallet.getAccounts()){
            ECPoint publicKey = account.getKey().publicKey;
            if (publicKey.compareTo(primaryPoint) != 0 && publicKey.compareTo(myPoint) != 0){
                backupKeyPair = account.getKey();
                for (int i = 0; i < context.validators.length;i++){
                    if (publicKey.compareTo(context.validators[i]) == 0){
                        backupIndex = i;
                        break;
                    }
                }
                break;
            }
        }

        PrepareResponse prepareResponse = new PrepareResponse();
        prepareResponse.signature = IVerifiable.sign(proposalBlock, backupKeyPair);

        final  int backUpIdx = backupIndex;
        consensusPayload = new ConsensusPayload(){{
            prevHash = context.prevHash;
            version = ConsensusContext.VERSION;
            validatorIndex = new Ushort(backUpIdx);
            prevHash = context.prevHash;
            blockIndex = context.blockIndex;
            timestamp = proposalBlock.timestamp;
            data = SerializeHelper.toBytes(prepareResponse);
        }};
        neoSystem.consensus.tell(consensusPayload, testKit.testActor());
        testKit.expectNoMessage();
        Assert.assertArrayEquals(prepareResponse.signature, context.signatures[backupIndex]);


        // test Timer event
        ConsensusService.Timer timer = new ConsensusService.Timer(){{
            height = context.blockIndex;
            viewNumber = context.viewNumber;
        }};
        neoSystem.consensus.tell(timer, testKit.testActor());
        sendDirectly = testKit.expectMsgClass(LocalNode.SendDirectly.class);
        payload = (ConsensusPayload) sendDirectly.inventory;
        changeView = SerializeHelper.parse(ChangeView::new, payload.data);
        Assert.assertEquals(context.viewNumber , changeView.viewNumber);
        Assert.assertEquals(context.viewNumber + 1, changeView.newViewNumber);
    }

    private static Ulong getNonce(){
        byte[] myNonce = new byte[Ulong.BYTES];
        SecureRandom rand = new SecureRandom();
        rand.nextBytes(myNonce);
        return BitConverter.toUlong(myNonce);
    }

    // 过滤 交易检测时，对脚本的校验
    public static class MyContractTransaction extends ContractTransaction {
        @Override
        public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
            return new UInt160[0];
        }

        @Override
        public Witness[] getWitnesses() {
            return new Witness[0];
        }
    }
}