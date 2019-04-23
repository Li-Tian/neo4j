package scenario;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.testkit.TestActorRef;
import akka.testkit.TestKit;
import neo.AVMParser;
import neo.Fixed8;
import neo.MyNeoSystem;
import neo.NeoSystem;
import neo.UInt160;
import neo.Utils;
import neo.cryptography.Crypto;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.ledger.ApplicationExecutionResult;
import neo.ledger.Blockchain;
import neo.ledger.ContractPropertyState;
import neo.ledger.ContractState;
import neo.ledger.MyBlockchain2;
import neo.ledger.RelayResultReason;
import neo.ledger.StorageItem;
import neo.ledger.StorageKey;
import neo.network.p2p.LocalNode;
import neo.network.p2p.MyLocalNode;
import neo.network.p2p.MyTaskManager;
import neo.network.p2p.payloads.Block;
import neo.network.p2p.payloads.CoinReference;
import neo.network.p2p.payloads.IVerifiable;
import neo.network.p2p.payloads.InvocationTransaction;
import neo.network.p2p.payloads.MinerTransaction;
import neo.network.p2p.payloads.Transaction;
import neo.network.p2p.payloads.TransactionAttribute;
import neo.network.p2p.payloads.TransactionAttributeUsage;
import neo.network.p2p.payloads.TransactionOutput;
import neo.network.p2p.payloads.Witness;
import neo.persistence.AbstractBlockchainTest;
import neo.persistence.Snapshot;
import neo.smartcontract.ApplicationEngine;
import neo.smartcontract.ContractParameterType;
import neo.smartcontract.NotifyEventArgs;
import neo.smartcontract.TriggerType;
import neo.vm.OpCode;
import neo.vm.ScriptBuilder;
import neo.vm.StackItem;
import neo.vm.Types.Array;
import neo.vm.VMState;
import neo.wallets.KeyPair;

/**
 * 一笔完整的NEP5智能合约调用测试: 创建，部署，转账，查询， 升级，销毁
 */
public class InvocationTxWithContractTest extends AbstractBlockchainTest {

    private final static String privateKey = "f72b8fab85fdcc1bdd20b107e5da1ab4713487bc88fc53b5b134f5eddeaa1a19";
    private final static String publicKey = "031f64da8a38e6c1e5423a72ddd6d4fc4a777abe537e5cb5aa0425685cda8e063b";
    private final static KeyPair keypair = new KeyPair(BitConverter.hexToBytes(privateKey));
    private final static UInt160 owner = UInt160.parseToScriptHash(neo.smartcontract.Contract.createSignatureRedeemScript(keypair.publicKey));

    private static final String testFile1 = "nep5contract0.avm";
    private static final String testFile2 = "nep5contract.avm";

    private static final UInt160 scriptHash1 = UInt160.parseToScriptHash(loadAvm());
    private static final UInt160 scriptHash2 = UInt160.parseToScriptHash(loadAvm2());

    protected static TestKit testKit;
    protected static MyBlockchain2 blockchain;
    protected static NeoSystem neoSystem;


    @BeforeClass
    public static void setUp() throws IOException {
        AbstractBlockchainTest.setUp(InvocationTxWithContractTest.class.getSimpleName());

        init();
    }

    @AfterClass
    public static void tearDown() throws IOException {
        AbstractBlockchainTest.tearDown(InvocationTxWithContractTest.class.getSimpleName());
    }


    private static void init() {
        neoSystem = new MyNeoSystem(store, self -> {
            testKit = new TestKit(self.actorSystem);

            // Synchronous Unit Testing with TestActorRef
            self.blockchain = TestActorRef.create(self.actorSystem, MyBlockchain2.props(self, store, testKit.testActor()));
            self.localNode = TestActorRef.create(self.actorSystem, MyLocalNode.props(self, testKit.testActor()));
            self.taskManager = TestActorRef.create(self.actorSystem, MyTaskManager.props(self, testKit.testActor()));
            self.consensus = null;
        });
        blockchain = (MyBlockchain2) MyBlockchain2.singleton();

    }


    @Test
    public void smartContract() throws InterruptedException {
        // prepare data
        // register BlockchainSubscribler
        TestActorRef<BlockchainSubscribler> subscriblerRef = TestActorRef.create(neoSystem.actorSystem, BlockchainSubscribler.props());
        BlockchainSubscribler subscribler = subscriblerRef.underlyingActor();

        // register blockchain
        subscriblerRef.tell(new BlockchainSubscribler.Register(), ActorRef.noSender());


        byte[] script = neo.smartcontract.Contract.createSignatureRedeemScript(keypair.publicKey);
        UInt160 owner = UInt160.parseToScriptHash(script);

        // check
        // print script
//        AVMParser.printScriptOpCode(loadAvm());

        // S1: create smart contract
        // init leveldb data
        Block block1 = new MyBlock() {
            {
                prevHash = Blockchain.GenesisBlock.hash();
                timestamp = new Uint(Long.toString(new Date("Jul 15 15:08:21 UTC 2018").getTime() / 1000));
                index = new Uint(1);
                consensusData = new Ulong(2083236894); //向比特币致敬
                nextConsensus = Blockchain.GenesisBlock.nextConsensus;
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
                        },
                        new InvocationTransaction() {{
                            script = createScript();                    // create contract
                            gas = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                        }}
                };
            }
        };
        block1.rebuildMerkleRoot();

        neoSystem.blockchain.tell(block1, testKit.testActor());
        LocalNode.RelayDirectly relayDirectly = testKit.expectMsgClass(LocalNode.RelayDirectly.class);
        Assert.assertNotNull(relayDirectly);
        Assert.assertEquals(block1.hash(), relayDirectly.inventory.hash());
        RelayResultReason resultReason = testKit.expectMsgClass(RelayResultReason.class);
        Assert.assertEquals(RelayResultReason.Succeed, resultReason);


        // check vm state
        Assert.assertNotNull(subscribler.executed);
        Assert.assertEquals(1, subscribler.executed.executionResults.length);
        ApplicationExecutionResult result = subscribler.executed.executionResults[0];
        Assert.assertEquals(UInt160.parseToScriptHash(createScript()), result.scriptHash);
        Assert.assertEquals(true, result.vmState.hasFlag(VMState.HALT));
        Assert.assertEquals(true, result.vmState.hasFlag(VMState.BREAK));
        Assert.assertEquals(Fixed8.fromDecimal(BigDecimal.valueOf(1000)), result.gasConsumed);


        // check contract in leveldb
        ContractState contractState = blockchain.getSnapshot().getContracts().get(scriptHash1);
        Assert.assertNotNull(contractState);

        Assert.assertArrayEquals(loadAvm(), contractState.script);
        Assert.assertEquals(ContractParameterType.String.value(), contractState.parameterList[0].value());
        Assert.assertEquals(ContractParameterType.Array.value(), contractState.parameterList[1].value());
        Assert.assertEquals(ContractParameterType.Boolean.value(), contractState.returnType.value());
        Assert.assertEquals(ContractPropertyState.HasStorage.or(ContractPropertyState.HasDynamicInvoke).value(), contractState.contractProperties.value());
        Assert.assertEquals("test", contractState.name);
        Assert.assertEquals("1.0", contractState.codeVersion);
        Assert.assertEquals("luc", contractState.author);
        Assert.assertEquals("luchuan@neo.org", contractState.email);

        subscribler.completed = null;
        subscribler.executed = null;


        // S2: deploy smart contract
        InvocationTransaction deployTx = new InvocationTransaction() {{
            script = deployScript(scriptHash1);
            gas = Fixed8.fromDecimal(BigDecimal.valueOf(10));
        }};


        Block block2 = new MyBlock() {
            {
                prevHash = block1.hash();
                timestamp = new Uint(Long.toString(new Date("Jul 15 18:08:21 UTC 2018").getTime() / 1000));
                index = new Uint(2);
                consensusData = new Ulong(2083236898); //向比特币致敬
                nextConsensus = block1.nextConsensus;
                witness = new Witness() {
                    {
                        invocationScript = new byte[0];
                        verificationScript = new byte[]{OpCode.PUSHT.getCode()};
                    }
                };
                transactions = new Transaction[]{
                        new MinerTransaction() {
                            {
                                nonce = new Uint(2083236892);
                                attributes = new TransactionAttribute[0];
                                inputs = new CoinReference[0];
                                outputs = new TransactionOutput[0];
                                witnesses = new Witness[0];
                            }
                        },
                        deployTx
                };
            }
        };
        block2.rebuildMerkleRoot();

//        AVMParser.printScriptOpCode(deployScript(scriptHash1));

        neoSystem.blockchain.tell(block2, testKit.testActor());
        relayDirectly = testKit.expectMsgClass(LocalNode.RelayDirectly.class);
        Assert.assertNotNull(relayDirectly);
        Assert.assertEquals(block2.hash(), relayDirectly.inventory.hash());
        resultReason = testKit.expectMsgClass(RelayResultReason.class);
        Assert.assertEquals(RelayResultReason.Succeed, resultReason);

        // check vm state
        Assert.assertNotNull(subscribler.executed);
        Assert.assertEquals(1, subscribler.executed.executionResults.length);
        result = subscribler.executed.executionResults[0];
        Assert.assertEquals(UInt160.parseToScriptHash(deployTx.script), result.scriptHash);
        Assert.assertEquals(true, result.vmState.hasFlag(VMState.HALT));
        Assert.assertEquals(true, result.vmState.hasFlag(VMState.BREAK));
//        Assert.assertEquals(Fixed8.fromDecimal(BigDecimal.valueOf(10)), result.gasConsumed);

        // check storage
        StorageKey storageKey = new StorageKey();
        storageKey.scriptHash = scriptHash1;
        storageKey.key = "totalSupply".getBytes();
        StorageItem storageItem = blockchain.getSnapshot().getStorages().get(storageKey);
        Assert.assertNotNull(storageItem);
        Assert.assertArrayEquals(BigInteger.valueOf(100000000L).multiply(BigInteger.valueOf(100000000L)).toByteArray(), BitConverter.reverse(storageItem.value));

        storageKey.scriptHash = scriptHash1;
        storageKey.key = owner.toArray();
        storageItem = blockchain.getSnapshot().getStorages().get(storageKey);
        Assert.assertNotNull(storageItem);
        Assert.assertArrayEquals(BigInteger.valueOf(100000000L).multiply(BigInteger.valueOf(100000000L)).toByteArray(), BitConverter.reverse(storageItem.value));


        // S3: transfer smart contract
        UInt160 from = owner;
        UInt160 to = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        BigInteger amount = BigInteger.valueOf(10000);
        InvocationTransaction transferTx = new InvocationTransaction() {{
            script = transferScript(scriptHash1, from, to, amount);
            gas = Fixed8.fromDecimal(BigDecimal.valueOf(10));
            attributes = new TransactionAttribute[]{
                    new TransactionAttribute() {{ // add the sender address in the attributes for verification.
                        usage = TransactionAttributeUsage.Script;
                        data = owner.toArray();
                    }}
            };
        }};

        // add signature
        byte[] signature = Crypto.Default.sign(IVerifiable.getHashData(transferTx), keypair.privateKey, keypair.publicKey.getEncoded(true));
        ScriptBuilder sb = new ScriptBuilder();
        neo.vm.Helper.emitPush(sb, signature);
        transferTx.witnesses = new Witness[]{
                new Witness() {{
                    invocationScript = sb.toArray();
                    verificationScript = neo.smartcontract.Contract.createSignatureRedeemScript(keypair.publicKey);
                }}
        };


        Block block3 = new MyBlock() {
            {
                prevHash = block2.hash();
                timestamp = new Uint(Long.toString(new Date("Jul 15 18:08:29 UTC 2018").getTime() / 1000));
                index = new Uint(3);
                consensusData = new Ulong(2083236899); //向比特币致敬
                nextConsensus = block1.nextConsensus;
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
                                inputs = new CoinReference[0];
                                outputs = new TransactionOutput[0];
                                witnesses = new Witness[0];
                            }
                        },
                        transferTx
                };
            }
        };
        block3.rebuildMerkleRoot();

//        AVMParser.printScriptOpCode(deployScript(scriptHash1));

        neoSystem.blockchain.tell(block3, testKit.testActor());
        relayDirectly = testKit.expectMsgClass(LocalNode.RelayDirectly.class);
        Assert.assertNotNull(relayDirectly);
        Assert.assertEquals(block3.hash(), relayDirectly.inventory.hash());
        resultReason = testKit.expectMsgClass(RelayResultReason.class);
        Assert.assertEquals(RelayResultReason.Succeed, resultReason);

        // check vm state
        Assert.assertNotNull(subscribler.executed);
        Assert.assertEquals(1, subscribler.executed.executionResults.length);
        result = subscribler.executed.executionResults[0];
        Assert.assertEquals(UInt160.parseToScriptHash(transferTx.script), result.scriptHash);
        Assert.assertEquals(true, result.vmState.hasFlag(VMState.HALT));
        Assert.assertEquals(true, result.vmState.hasFlag(VMState.BREAK));
        Assert.assertEquals(1, result.stack.length);
        Assert.assertEquals(true, result.stack[0].getBoolean());
        Assert.assertEquals(1, result.notifications.length);
        NotifyEventArgs eventArgs = result.notifications[0];
        neo.vm.Types.Array array = (Array) eventArgs.state;
        Assert.assertEquals(4, array.getCount());

        // TODO the first is ?
//        System.out.println(BitConverter.toHexString(array.getArrayItem(0).getByteArray()));
        Assert.assertArrayEquals(from.toArray(), array.getArrayItem(1).getByteArray());
        Assert.assertArrayEquals(to.toArray(), array.getArrayItem(2).getByteArray());
        Assert.assertEquals(10000, array.getArrayItem(3).getBigInteger().intValue());
//        Assert.assertEquals(Fixed8.fromDecimal(BigDecimal.valueOf(10)), result.gasConsumed);

        // 查询余额
        storageKey.scriptHash = scriptHash1;
        storageKey.key = from.toArray();
        storageItem = blockchain.getSnapshot().getStorages().get(storageKey);
        Assert.assertNotNull(storageItem);
//        Assert.assertArrayEquals(BigInteger.valueOf(100000000L)
//                .multiply(BigInteger.valueOf(100000000L))
//                .subtract(BigInteger.valueOf(10000L)).toByteArray(), BitConverter.reverse(storageItem.value));

        storageKey.scriptHash = scriptHash1;
        storageKey.key = to.toArray();
        storageItem = blockchain.getSnapshot().getStorages().get(storageKey);
        Assert.assertNotNull(storageItem);
        Assert.assertEquals(BigInteger.valueOf(10000L), new BigInteger(storageItem.value));


        // S4: balanceOf
        InvocationTransaction balanceOfTx = new InvocationTransaction() {{
            script = balanceOfScript(scriptHash1, to);
            gas = Fixed8.fromDecimal(BigDecimal.valueOf(10));
        }};

        Block block4 = new MyBlock() {
            {
                prevHash = block3.hash();
                timestamp = new Uint(Long.toString(new Date("Jul 15 19:08:21 UTC 2018").getTime() / 1000));
                index = new Uint(4);
                consensusData = new Ulong(2083236998); //向比特币致敬
                nextConsensus = block3.nextConsensus;
                witness = new Witness() {
                    {
                        invocationScript = new byte[0];
                        verificationScript = new byte[]{OpCode.PUSHT.getCode()};
                    }
                };
                transactions = new Transaction[]{
                        new MinerTransaction() {
                            {
                                nonce = new Uint(2083246892);
                                attributes = new TransactionAttribute[0];
                                inputs = new CoinReference[0];
                                outputs = new TransactionOutput[0];
                                witnesses = new Witness[0];
                            }
                        },
                        balanceOfTx
                };
            }
        };
        block4.rebuildMerkleRoot();

//        AVMParser.printScriptOpCode(balanceOfTx.script);

        neoSystem.blockchain.tell(block4, testKit.testActor());
        relayDirectly = testKit.expectMsgClass(LocalNode.RelayDirectly.class);
        Assert.assertNotNull(relayDirectly);
        Assert.assertEquals(block4.hash(), relayDirectly.inventory.hash());
        resultReason = testKit.expectMsgClass(RelayResultReason.class);
        Assert.assertEquals(RelayResultReason.Succeed, resultReason);

        // check vm state
        Assert.assertNotNull(subscribler.executed);
        Assert.assertEquals(1, subscribler.executed.executionResults.length);
        result = subscribler.executed.executionResults[0];
        Assert.assertEquals(UInt160.parseToScriptHash(balanceOfTx.script), result.scriptHash);
        Assert.assertEquals(true, result.vmState.hasFlag(VMState.HALT));
        Assert.assertEquals(true, result.vmState.hasFlag(VMState.BREAK));
//        Assert.assertEquals(Fixed8.fromDecimal(BigDecimal.valueOf(10)), result.gasConsumed);

        // check balanceOf result
        Assert.assertEquals(1, result.stack.length);
        Assert.assertEquals(10000, result.stack[0].getBigInteger().intValue());


        // S5: contract migration
        InvocationTransaction migrateTx = new InvocationTransaction() {{
            script = migrateScript(scriptHash1);
            gas = Fixed8.fromDecimal(BigDecimal.valueOf(10000));
        }};

        Block block5 = new MyBlock() {
            {
                prevHash = block4.hash();
                timestamp = new Uint(Long.toString(new Date("Jul 15 19:08:22 UTC 2018").getTime() / 1000));
                index = new Uint(5);
                consensusData = new Ulong(2083236999); //向比特币致敬
                nextConsensus = block4.nextConsensus;
                witness = new Witness() {
                    {
                        invocationScript = new byte[0];
                        verificationScript = new byte[]{OpCode.PUSHT.getCode()};
                    }
                };
                transactions = new Transaction[]{
                        new MinerTransaction() {
                            {
                                nonce = new Uint(2083246892);
                                attributes = new TransactionAttribute[0];
                                inputs = new CoinReference[0];
                                outputs = new TransactionOutput[0];
                                witnesses = new Witness[0];
                            }
                        },
                        migrateTx
                };
            }
        };
        block5.rebuildMerkleRoot();

//        AVMParser.printScriptOpCode(balanceOfTx.script);

        neoSystem.blockchain.tell(block5, testKit.testActor());
        relayDirectly = testKit.expectMsgClass(LocalNode.RelayDirectly.class);
        Assert.assertNotNull(relayDirectly);
        Assert.assertEquals(block5.hash(), relayDirectly.inventory.hash());
        resultReason = testKit.expectMsgClass(RelayResultReason.class);
        Assert.assertEquals(RelayResultReason.Succeed, resultReason);

        // check vm state
        Assert.assertNotNull(subscribler.executed);
        Assert.assertEquals(1, subscribler.executed.executionResults.length);
        result = subscribler.executed.executionResults[0];
        Assert.assertEquals(UInt160.parseToScriptHash(migrateTx.script), result.scriptHash);
        Assert.assertEquals(true, result.vmState.hasFlag(VMState.HALT));
        Assert.assertEquals(true, result.vmState.hasFlag(VMState.BREAK));
//        Assert.assertEquals(Fixed8.fromDecimal(BigDecimal.valueOf(10)), result.gasConsumed);

        // check storage
        // 查询余额
        storageKey.scriptHash = scriptHash1;
        storageKey.key = from.toArray();
        storageItem = blockchain.getSnapshot().getStorages().get(storageKey);
        Assert.assertNull(storageItem);

        storageKey.scriptHash = scriptHash1;
        storageKey.key = to.toArray();
        storageItem = blockchain.getSnapshot().getStorages().get(storageKey);
        Assert.assertNull(storageItem);

        // the old contract was deleted
        ContractState contractState1 = blockchain.getSnapshot().getContracts().get(scriptHash1);
        Assert.assertNull(contractState1);

        storageKey.scriptHash = scriptHash2;
        storageKey.key = from.toArray();
        storageItem = blockchain.getSnapshot().getStorages().get(storageKey);
        Assert.assertNotNull(storageItem);
//        Assert.assertArrayEquals(BigInteger.valueOf(100000000L)
//                .multiply(BigInteger.valueOf(100000000L))
//                .subtract(BigInteger.valueOf(10000L)).toByteArray(), BitConverter.reverse(storageItem.value));

        storageKey.scriptHash = scriptHash2;
        storageKey.key = to.toArray();
        storageItem = blockchain.getSnapshot().getStorages().get(storageKey);
        Assert.assertNotNull(storageItem);
        Assert.assertEquals(BigInteger.valueOf(10000L), new BigInteger(storageItem.value));


        // S6: delete contract
        InvocationTransaction deleteTx = new InvocationTransaction() {{
            script = deleteScript(scriptHash2);
            gas = Fixed8.fromDecimal(BigDecimal.valueOf(10000));
        }};

        Block block6 = new MyBlock() {
            {
                prevHash = block5.hash();
                timestamp = new Uint(Long.toString(new Date("Jul 15 19:08:23 UTC 2018").getTime() / 1000));
                index = new Uint(6);
                consensusData = new Ulong(2083237999); //向比特币致敬
                nextConsensus = block5.nextConsensus;
                witness = new Witness() {
                    {
                        invocationScript = new byte[0];
                        verificationScript = new byte[]{OpCode.PUSHT.getCode()};
                    }
                };
                transactions = new Transaction[]{
                        new MinerTransaction() {
                            {
                                nonce = new Uint(2083246893);
                                attributes = new TransactionAttribute[0];
                                inputs = new CoinReference[0];
                                outputs = new TransactionOutput[0];
                                witnesses = new Witness[0];
                            }
                        },
                        deleteTx
                };
            }
        };
        block6.rebuildMerkleRoot();
//        AVMParser.printScriptOpCode(deleteTx.script);

        neoSystem.blockchain.tell(block6, testKit.testActor());
        relayDirectly = testKit.expectMsgClass(LocalNode.RelayDirectly.class);
        Assert.assertNotNull(relayDirectly);
        Assert.assertEquals(block6.hash(), relayDirectly.inventory.hash());
        resultReason = testKit.expectMsgClass(RelayResultReason.class);
        Assert.assertEquals(RelayResultReason.Succeed, resultReason);

        // check vm state
        Assert.assertNotNull(subscribler.executed);
        Assert.assertEquals(1, subscribler.executed.executionResults.length);
        result = subscribler.executed.executionResults[0];
        Assert.assertEquals(UInt160.parseToScriptHash(deleteTx.script), result.scriptHash);
        Assert.assertEquals(true, result.vmState.hasFlag(VMState.HALT));
        Assert.assertEquals(true, result.vmState.hasFlag(VMState.BREAK));
//        Assert.assertEquals(Fixed8.fromDecimal(BigDecimal.valueOf(10)), result.gasConsumed);

        // check storage
        // 查询余额
        storageKey.scriptHash = scriptHash2;
        storageKey.key = from.toArray();
        storageItem = blockchain.getSnapshot().getStorages().get(storageKey);
        Assert.assertNull(storageItem);

        storageKey.scriptHash = scriptHash2;
        storageKey.key = to.toArray();
        storageItem = blockchain.getSnapshot().getStorages().get(storageKey);
        Assert.assertNull(storageItem);

        // contract was deleted
        ContractState contractState2 = blockchain.getSnapshot().getContracts().get(scriptHash2);
        Assert.assertNull(contractState2);

        // clear data
    }

    @Test
    public void nep5Contract() {
        InvocationTransaction tx_invocation = new InvocationTransaction() {{
            script = createScript();                    // create contract
            gas = Fixed8.fromDecimal(BigDecimal.valueOf(10000));
        }};
        ApplicationEngine engine = new ApplicationEngine(TriggerType.Application, tx_invocation, blockchain.getSnapshot().clone(), tx_invocation.gas);
        engine.loadScript(tx_invocation.script);

//        AVMParser.printScriptOpCode(tx_invocation.script);

        if (engine.execute2()) {
            engine.getService().commit();
        } else {
            System.out.println("executed failed");
        }
        ArrayList<StackItem> items = new ArrayList<>();
        for (int i = 0, n = engine.resultStack.getCount(); i < n; i++) {
            items.add(engine.resultStack.peek(i));
        }

        ApplicationExecutionResult result = new ApplicationExecutionResult() {
            {
                trigger = TriggerType.Application;
                scriptHash = UInt160.parseToScriptHash(tx_invocation.script);
                vmState = engine.state;
                gasConsumed = engine.getGasConsumed();
                stack = items.toArray(new StackItem[items.size()]);
                notifications = engine.getService().getNotifications().toArray(new NotifyEventArgs[engine.getService().getNotifications().size()]);
            }
        };

        Assert.assertEquals(true, result.vmState.hasFlag(VMState.HALT));
        Assert.assertEquals(true, result.vmState.hasFlag(VMState.BREAK));
    }


    private byte[] createScript() {
        byte[] script = loadAvm();
        byte[] parameter_list = new byte[]{
                ContractParameterType.String.value(),
                ContractParameterType.Array.value()
        };
        ContractParameterType return_type = ContractParameterType.Boolean;
        ContractPropertyState properties = ContractPropertyState.HasStorage.or(ContractPropertyState.HasDynamicInvoke);
        String name = "test";
        String version = "1.0";
        String author = "luc";
        String email = "luchuan@neo.org";
        String description = "test";

        ScriptBuilder sb = new ScriptBuilder();
        neo.vm.Helper.emitSysCall(sb, "Neo.Contract.Create", script, parameter_list, return_type, properties, name, version, author, email, description);
        return sb.toArray();
    }

    private byte[] deployScript(UInt160 scriptHash) {
        ScriptBuilder sb = new ScriptBuilder();
        neo.vm.Helper.emitAppCall(sb, scriptHash, "deploy");
        return sb.toArray();
    }

    private byte[] transferScript(UInt160 scriptHash, UInt160 from, UInt160 to, BigInteger amount) {
        ScriptBuilder sb = new ScriptBuilder();
        neo.vm.Helper.emitAppCall(sb, scriptHash, "transfer", from, to, amount);
        return sb.toArray();
    }

    private byte[] balanceOfScript(UInt160 scriptHash, UInt160 account) {
        ScriptBuilder sb = new ScriptBuilder();
        neo.vm.Helper.emitAppCall(sb, scriptHash, "balanceOf", account);
        return sb.toArray();
    }

    private byte[] migrateScript(UInt160 scriptHash) {
        byte[] script = loadAvm2();
        byte[] parameter_list = new byte[]{
                ContractParameterType.String.value(),
                ContractParameterType.Array.value()
        };
        ContractParameterType return_type = ContractParameterType.Boolean;
        ContractPropertyState properties = ContractPropertyState.HasStorage;
        String name = "test2";
        String version = "1.1";
        String author = "luc2";
        String email = "luchuan@neo.org";
        String description = "test2";

        ScriptBuilder sb = new ScriptBuilder();
        neo.vm.Helper.emitAppCall(sb, scriptHash, "migrate", script, parameter_list, return_type, properties, name, version, author, email, description);
        return sb.toArray();
    }

    private byte[] deleteScript(UInt160 scriptHash) {
        ScriptBuilder sb = new ScriptBuilder();
        neo.vm.Helper.emitAppCall(sb, scriptHash, "delete");
        return sb.toArray();
    }


    private static byte[] loadAvm() {
        String path = InvocationTxWithContractTest.class.getClassLoader().getResource("").getPath() + testFile1;
        try {
            return Utils.readContentFromFile(path);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static byte[] loadAvm2() {
        String path = InvocationTxWithContractTest.class.getClassLoader().getResource("").getPath() + testFile2;
        try {
            return Utils.readContentFromFile(path);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
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


    private static class BlockchainSubscribler extends AbstractActor {

        private Blockchain.ApplicationExecuted executed;
        private Blockchain.PersistCompleted completed;

        @Override
        public Receive createReceive() {
            return receiveBuilder().match(Blockchain.ApplicationExecuted.class, applicationExecuted -> this.executed = applicationExecuted)
                    .match(Blockchain.PersistCompleted.class, persistCompleted -> this.completed = persistCompleted)
                    .match(Register.class, register -> neoSystem.blockchain.tell(new Blockchain.Register(), self()))
                    .build();
        }

        private static class Register {
        }

        public static Props props() {
            return Props.create(BlockchainSubscribler.class);
        }
    }


}
