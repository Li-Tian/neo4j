package scenario;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.testkit.TestActorRef;
import akka.testkit.TestKit;
import neo.Fixed8;
import neo.MyNeoSystem;
import neo.NeoSystem;
import neo.UInt160;
import neo.consensus.ConsensusContext;
import neo.consensus.ConsensusService;
import neo.cryptography.ecc.ECPoint;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.ledger.Blockchain;
import neo.ledger.MyBlockchain2;
import neo.ledger.RelayResultReason;
import neo.ledger.ValidatorState;
import neo.network.p2p.LocalNode;
import neo.network.p2p.MyTaskManager;
import neo.network.p2p.payloads.Block;
import neo.network.p2p.payloads.CoinReference;
import neo.network.p2p.payloads.ConsensusPayload;
import neo.network.p2p.payloads.IInventory;
import neo.network.p2p.payloads.MinerTransaction;
import neo.network.p2p.payloads.Transaction;
import neo.network.p2p.payloads.TransactionAttribute;
import neo.network.p2p.payloads.TransactionOutput;
import neo.network.p2p.payloads.Witness;
import neo.persistence.AbstractBlockchainTest;
import neo.persistence.Snapshot;
import neo.vm.OpCode;
import neo.wallets.Wallet;
import neo.wallets.WalletAccount;
import scala.concurrent.Future;

/**
 * consensus mechanism test
 *
 * Simulation environment: 7 consensus nodes, and need 2/3 * 7 = 5 nodes to sign the proposal
 * block.
 */
public class ConsensusMechanismTest extends AbstractBlockchainTest {

    protected static TestKit testKit;
    protected static NeoSystem neoSystem;
    private static Snapshot snapshot;

    private static ActorRef cnode1;
    private static ActorRef cnode2;
    private static ActorRef cnode3;
    private static ActorRef cnode4;
    private static ActorRef cnode5;
    private static ActorRef cnode6;
    private static ActorRef cnode7;

    private static ActorRef blockchainSubscriber;

    private static boolean stopTest = false;

    @BeforeClass
    public static void setUp() throws IOException {
        AbstractBlockchainTest.setUp(InvocationTxWithContractTest.class.getSimpleName());

        init();
    }

    @AfterClass
    public static void tearDown() throws IOException {
        AbstractBlockchainTest.tearDown(InvocationTxWithContractTest.class.getSimpleName());

        // free resource
    }


    private static void init() {
        neoSystem = new MyNeoSystem(store, self -> {
            testKit = new TestKit(self.actorSystem);

            // Synchronous Unit Testing with TestActorRef
            self.blockchain = TestActorRef.create(self.actorSystem, MyBlockchain2.props(self, store, testKit.testActor()));
            self.localNode = TestActorRef.create(self.actorSystem, MyLocalNode.props(self));
            self.taskManager = TestActorRef.create(self.actorSystem, MyTaskManager.props(self, testKit.testActor()));
            self.consensus = null;
        });
        blockchainSubscriber = TestActorRef.create(neoSystem.actorSystem, BlockChainSubscriber.props());
        snapshot = Blockchain.singleton().getSnapshot();
    }

    @Test
    public void consensus() throws Exception {
        // prepare data
        //  初始化 leveldb 数据
        // 构建 validators
        // 直接从db构建，跳过 blockchian.persist 环节
        // 修改数据库中 投票表情况， 设置为当前七个用户的地址
        MyWallet3 wallet1 = new MyWallet3(1);
        MyWallet3 wallet2 = new MyWallet3(1);
        MyWallet3 wallet3 = new MyWallet3(1);
        MyWallet3 wallet4 = new MyWallet3(1);
        MyWallet3 wallet5 = new MyWallet3(1);
        MyWallet3 wallet6 = new MyWallet3(1);
        MyWallet3 wallet7 = new MyWallet3(1);

        Collection<WalletAccount> accounts = Arrays.asList(wallet1.getDefaultAccount(),
                wallet2.getDefaultAccount(),
                wallet3.getDefaultAccount(),
                wallet4.getDefaultAccount(),
                wallet5.getDefaultAccount(),
                wallet6.getDefaultAccount(),
                wallet7.getDefaultAccount());

        // register 7 validators in leveldb
        ArrayList<ECPoint> ecPoints = new ArrayList<>();
        for (WalletAccount account : accounts) {
            Fixed8 myVote = Fixed8.fromDecimal(BigDecimal.valueOf(100000));
            ValidatorState validatorState = new ValidatorState() {{
                publicKey = account.getKey().publicKey;
                registered = true;
                votes = myVote;
            }};
            snapshot.getValidators().add(validatorState.publicKey, validatorState);
            ecPoints.add(account.getKey().publicKey);
        }

        // change
        int m = ecPoints.size() - (ecPoints.size() - 1) / 3;
        UInt160 newNextConsensus = UInt160.parseToScriptHash(neo.smartcontract.Contract.createMultiSigRedeemScript(m, ecPoints));
        snapshot.commit();

        // persist a new block with the new `nextconsensus` which binding our validators.
        Block block1 = new MyBlock() {
            {
                prevHash = Blockchain.GenesisBlock.hash();
                timestamp = new Uint(Long.toString(new Date("Jul 15 15:08:21 UTC 2018").getTime() / 1000));
                index = new Uint(1);
                consensusData = new Ulong(2083236894); //向比特币致敬
                nextConsensus = newNextConsensus;
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

        neoSystem.blockchain.tell(block1, testKit.testActor());
        RelayResultReason resultReason = testKit.expectMsgClass(RelayResultReason.class);
        Assert.assertEquals(RelayResultReason.Succeed, resultReason);

        // register the blockchainSubscriber
        blockchainSubscriber.tell(new Blockchain.Register(), ActorRef.noSender());


        // check
        // create 7 consensus service for simulating the cnodes
        ConsensusContext context1 = new ConsensusContext(wallet1);
        ConsensusContext context2 = new ConsensusContext(wallet2);
        ConsensusContext context3 = new ConsensusContext(wallet3);
        ConsensusContext context4 = new ConsensusContext(wallet4);
        ConsensusContext context5 = new ConsensusContext(wallet5);
        ConsensusContext context6 = new ConsensusContext(wallet6);
        ConsensusContext context7 = new ConsensusContext(wallet7);

        cnode1 = neoSystem.actorSystem.actorOf(MyConsensusSerivce.props(neoSystem.localNode, neoSystem.taskManager, context1));
        cnode2 = neoSystem.actorSystem.actorOf(MyConsensusSerivce.props(neoSystem.localNode, neoSystem.taskManager, context2));
        cnode3 = neoSystem.actorSystem.actorOf(MyConsensusSerivce.props(neoSystem.localNode, neoSystem.taskManager, context3));
        cnode4 = neoSystem.actorSystem.actorOf(MyConsensusSerivce.props(neoSystem.localNode, neoSystem.taskManager, context4));
        cnode5 = neoSystem.actorSystem.actorOf(MyConsensusSerivce.props(neoSystem.localNode, neoSystem.taskManager, context5));
        cnode6 = neoSystem.actorSystem.actorOf(MyConsensusSerivce.props(neoSystem.localNode, neoSystem.taskManager, context6));
        cnode7 = neoSystem.actorSystem.actorOf(MyConsensusSerivce.props(neoSystem.localNode, neoSystem.taskManager, context7));

        neoSystem.consensus = cnode1;

        // start the consensus
        cnode1.tell(new ConsensusService.Start(), ActorRef.noSender());
        cnode2.tell(new ConsensusService.Start(), ActorRef.noSender());
        cnode3.tell(new ConsensusService.Start(), ActorRef.noSender());
        cnode4.tell(new ConsensusService.Start(), ActorRef.noSender());
        cnode5.tell(new ConsensusService.Start(), ActorRef.noSender());
        cnode6.tell(new ConsensusService.Start(), ActorRef.noSender());
        cnode7.tell(new ConsensusService.Start(), ActorRef.noSender());

        while (!stopTest) {
            Thread.sleep(1000 * 1);
        }

        for (ConsensusService service: MyConsensusSerivce.list){
            service.closeTimer();
        }
    }

    public static class  MyConsensusSerivce extends ConsensusService{
        private  static ArrayList<ConsensusService> list = new ArrayList<>(10);

        public MyConsensusSerivce(ActorRef localNode, ActorRef taskManager, ConsensusContext context) {
            super(localNode, taskManager, context);
            list.add(this);
        }

        public static Props props(ActorRef localNode, ActorRef taskManager, Wallet wallet) {
            return Props.create(MyConsensusSerivce.class, localNode, taskManager, wallet)
                    .withMailbox("consensus-service-mailbox");
        }

    }

    public static class MyLocalNode extends LocalNode {

        public MyLocalNode(NeoSystem system) {
            super(system);
        }

        @Override
        protected void init() {
            singleton = this;
        }

        public static Props props(NeoSystem system) {
            return Props.create(MyLocalNode.class, system);
        }

        @Override
        public AbstractActor.Receive createReceive() {
            return receiveBuilder()
                    .matchAny(obj -> {
                        if (obj instanceof SendDirectly) {
                            SendDirectly sendDirectly = (SendDirectly) obj;
                            IInventory inventory = sendDirectly.inventory;
                            if (inventory instanceof ConsensusPayload) {

                                ConsensusPayload consensusPayload = (ConsensusPayload) inventory;

                                cnode1.tell(consensusPayload, ActorRef.noSender());
                                cnode2.tell(consensusPayload, ActorRef.noSender());
                                cnode3.tell(consensusPayload, ActorRef.noSender());
                                cnode4.tell(consensusPayload, ActorRef.noSender());
                                cnode5.tell(consensusPayload, ActorRef.noSender());
                                cnode6.tell(consensusPayload, ActorRef.noSender());
                                cnode7.tell(consensusPayload, ActorRef.noSender());
                            }
                        } else if (obj instanceof LocalNode.Relay) {
                            LocalNode.Relay relay = (Relay) obj;
                            if (relay.inventory instanceof Block) {
                                Block block = (Block) relay.inventory;
                                System.out.println("--- A new proposal block was accepted, then broadcast for all the nodes, block height: " + block.index.intValue());
                                neoSystem.blockchain.tell(relay.inventory, self());
                            }
                        }
                    })
                    .build();
        }
    }


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

    // subscribe the new block event
    public static class BlockChainSubscriber extends AbstractActor {

        public int blockStartHeight = 2;

        public static Props props() {
            return Props.create(BlockChainSubscriber.class);
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(Blockchain.PersistCompleted.class, persistCompleted -> {
                        // check the block height
                        Assert.assertEquals(blockStartHeight++, persistCompleted.block.index.intValue());
                        System.out.println("--- persist a new block, height: " + persistCompleted.block.index.intValue());

                        cnode1.tell(persistCompleted, ActorRef.noSender());
                        cnode2.tell(persistCompleted, ActorRef.noSender());
                        cnode3.tell(persistCompleted, ActorRef.noSender());
                        cnode4.tell(persistCompleted, ActorRef.noSender());
                        cnode5.tell(persistCompleted, ActorRef.noSender());
                        cnode6.tell(persistCompleted, ActorRef.noSender());
                        cnode7.tell(persistCompleted, ActorRef.noSender());

                        // stop the test
                        stopTest = blockStartHeight >= 4;
                    })
                    .match(Blockchain.Register.class, register -> neoSystem.blockchain.tell(register, self()))
                    .build();
        }
    }


}
