package neo.smartcontract;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import akka.testkit.TestActorRef;
import akka.testkit.TestKit;
import neo.Fixed8;
import neo.MyNeoSystem;
import neo.NeoSystem;
import neo.UInt160;
import neo.UInt256;
import neo.wallets.KeyPair;
import neo.cryptography.Crypto;
import neo.cryptography.ecc.ECC;
import neo.cryptography.ecc.ECPoint;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.csharp.Ushort;
import neo.csharp.io.BinaryWriter;
import neo.ledger.AccountState;
import neo.ledger.AssetState;
import neo.ledger.BlockState;
import neo.ledger.Blockchain;
import neo.ledger.CoinState;
import neo.ledger.ContractPropertyState;
import neo.ledger.ContractState;
import neo.ledger.HashIndexState;
import neo.ledger.MyBlockchain2;
import neo.ledger.StorageItem;
import neo.ledger.StorageKey;
import neo.ledger.TransactionState;
import neo.ledger.TrimmedBlock;
import neo.ledger.UnspentCoinState;
import neo.network.p2p.MyLocalNode;
import neo.network.p2p.MyTaskManager;
import neo.network.p2p.payloads.AssetType;
import neo.network.p2p.payloads.CoinReference;
import neo.network.p2p.payloads.ContractTransaction;
import neo.network.p2p.payloads.InvocationTransaction;
import neo.network.p2p.payloads.MinerTransaction;
import neo.network.p2p.payloads.TransactionAttribute;
import neo.network.p2p.payloads.TransactionAttributeUsage;
import neo.network.p2p.payloads.TransactionOutput;
import neo.network.p2p.payloads.Witness;
import neo.persistence.AbstractLeveldbTest;
import neo.persistence.Snapshot;
import neo.smartcontract.enumerators.IEnumerator;
import neo.smartcontract.enumerators.IteratorKeysWrapper;
import neo.smartcontract.enumerators.IteratorValuesWrapper;
import neo.smartcontract.iterators.ArrayWrapper;
import neo.smartcontract.iterators.IIterator;
import neo.smartcontract.iterators.StorageIterator;
import neo.vm.ExecutionEngine;
import neo.vm.StackItem;
import neo.vm.Types.Array;
import neo.vm.Types.InteropInterface;

public class NeoServiceTest extends AbstractLeveldbTest {

    private static NeoService neoService;
    private static Snapshot snapshot;
    private static MyBlockchain2 blockChain2;
    private static NeoSystem neoSystem;

    @BeforeClass
    public static void setUp() throws IOException {
        AbstractLeveldbTest.setUp(NeoServiceTest.class.getSimpleName());

        snapshot = store.getSnapshot();
        neoService = new NeoService(TriggerType.Application, snapshot);

        neoSystem = new MyNeoSystem(store, self -> {
            TestKit testKit = new TestKit(self.actorSystem);

            // Synchronous Unit Testing with TestActorRef
            TestActorRef<MyBlockchain2> blockchain2TestActorRef = TestActorRef.create(self.actorSystem, MyBlockchain2.props(self, store, testKit.testActor()));
            self.blockchain = blockchain2TestActorRef;
            blockChain2 = blockchain2TestActorRef.underlyingActor();
            self.localNode = TestActorRef.create(self.actorSystem, MyLocalNode.props(self, testKit.testActor()));
            self.taskManager = TestActorRef.create(self.actorSystem, MyTaskManager.props(self, testKit.testActor()));
            self.consensus = null;
        });
    }

    @AfterClass
    public static void tearDown() throws IOException {
        AbstractLeveldbTest.tearDown(NeoServiceTest.class.getSimpleName());
    }

    private ExecutionEngine getEngine() {
        InvocationTransaction invocationTransaction = new InvocationTransaction();
        ExecutionEngine engine = new ExecutionEngine(invocationTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);
        return engine;
    }

    @Test
    public void blockchainGetAccount() {
        ExecutionEngine engine = getEngine();
        UInt160 hash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(hash.toArray()));

        neoService.invoke("Neo.Blockchain.GetAccount".getBytes(), engine);

        InteropInterface<AccountState> stateInteropInterface = (InteropInterface<AccountState>) engine.getCurrentContext().evaluationStack.pop();
        AccountState accountState = stateInteropInterface.getInterface();
        Assert.assertEquals(hash, accountState.scriptHash);
    }

    @Test
    public void blockchainGetValidators() {
        ExecutionEngine engine = getEngine();
        neoService.invoke("Neo.Blockchain.GetValidators".getBytes(), engine);

        ECPoint[] pubkeys = snapshot.getValidatorPubkeys();
        neo.vm.Types.Array array = (neo.vm.Types.Array) engine.getCurrentContext().evaluationStack.pop();
        Assert.assertEquals(pubkeys.length, array.getCount());
        for (int i = 0; i < pubkeys.length; i++) {
            ECPoint ecPoint = pubkeys[i];
            StackItem itemm = array.getArrayItem(i);
            Assert.assertArrayEquals(ecPoint.getEncoded(true), itemm.getByteArray());
        }
    }

    @Test
    public void blockchainGetAsset() {
        // preparea data
        AssetState state = new AssetState();
        state.assetId = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        state.assetType = AssetType.Share;
        state.name = "Test";
        state.amount = new Fixed8(100000000);
        state.available = new Fixed8(100000000);
        state.precision = 0;
        state.fee = Fixed8.ZERO;
        state.feeAddress = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        state.owner = new ECPoint(ECC.getInfinityPoint());
        state.admin = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        state.issuer = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff03");
        state.expiration = new Uint(1000000);
        state.isFrozen = false;

        snapshot.getAssets().add(state.assetId, state);
        snapshot.commit();

        // check
        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(state.assetId.toArray()));

        neoService.invoke("Neo.Blockchain.GetAsset".getBytes(), engine);

        InteropInterface<AssetState> stateInteropInterface = (InteropInterface<AssetState>) engine.getCurrentContext().evaluationStack.pop();
        AssetState assetState = stateInteropInterface.getInterface();
        Assert.assertEquals(state.assetId, assetState.assetId);
        Assert.assertEquals(state.assetType, assetState.assetType);
        Assert.assertEquals(state.name, assetState.name);
        Assert.assertEquals(state.available, assetState.available);

        // clear data
        snapshot.getAssets().delete(state.assetId);
        snapshot.commit();
    }


    @Test
    public void headerGetVersion() {
        BlockState blockState = new BlockState();
        blockState.trimmedBlock = new TrimmedBlock();
        blockState.trimmedBlock.consensusData = new Ulong(10);
        blockState.trimmedBlock.version = new Uint(1);
        blockState.trimmedBlock.prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        blockState.trimmedBlock.timestamp = new Uint(1568489959);
        blockState.trimmedBlock.index = new Uint(10);
        blockState.trimmedBlock.nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.witness = new Witness();
        blockState.trimmedBlock.witness.verificationScript = new byte[]{0x01, 0x02};
        blockState.trimmedBlock.witness.invocationScript = new byte[]{0x3, 0x04};
        blockState.trimmedBlock.hashes = new UInt256[]{new MinerTransaction().hash()};
        blockState.systemFeeAmount = 10;

        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(blockState.trimmedBlock.getHeader()));

        neoService.invoke("Neo.Header.GetVersion".getBytes(), engine);

        Assert.assertEquals(1, engine.getCurrentContext().evaluationStack.pop().getBigInteger().intValue());
    }

    @Test
    public void headerGetMerkleRoot() {
        BlockState blockState = new BlockState();
        blockState.trimmedBlock = new TrimmedBlock();
        blockState.trimmedBlock.consensusData = new Ulong(10);
        blockState.trimmedBlock.version = new Uint(1);
        blockState.trimmedBlock.prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        blockState.trimmedBlock.timestamp = new Uint(1568489959);
        blockState.trimmedBlock.index = new Uint(10);
        blockState.trimmedBlock.nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.witness = new Witness();
        blockState.trimmedBlock.witness.verificationScript = new byte[]{0x01, 0x02};
        blockState.trimmedBlock.witness.invocationScript = new byte[]{0x3, 0x04};
        blockState.trimmedBlock.hashes = new UInt256[]{new MinerTransaction().hash()};
        blockState.systemFeeAmount = 10;

        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(blockState.trimmedBlock.getHeader()));

        neoService.invoke("Neo.Header.GetMerkleRoot".getBytes(), engine);

        Assert.assertEquals(blockState.trimmedBlock.merkleRoot.toArray(), engine.getCurrentContext().evaluationStack.pop().getByteArray());
    }

    @Test
    public void headerGetConsensusData() {
        BlockState blockState = new BlockState();
        blockState.trimmedBlock = new TrimmedBlock();
        blockState.trimmedBlock.consensusData = new Ulong(10);
        blockState.trimmedBlock.version = new Uint(1);
        blockState.trimmedBlock.prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        blockState.trimmedBlock.timestamp = new Uint(1568489959);
        blockState.trimmedBlock.index = new Uint(10);
        blockState.trimmedBlock.nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.witness = new Witness();
        blockState.trimmedBlock.witness.verificationScript = new byte[]{0x01, 0x02};
        blockState.trimmedBlock.witness.invocationScript = new byte[]{0x3, 0x04};
        blockState.trimmedBlock.hashes = new UInt256[]{new MinerTransaction().hash()};
        blockState.systemFeeAmount = 10;

        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(blockState.trimmedBlock.getHeader()));

        neoService.invoke("Neo.Header.GetConsensusData".getBytes(), engine);

        Assert.assertEquals(blockState.trimmedBlock.consensusData.intValue(), engine.getCurrentContext().evaluationStack.pop().getBigInteger().intValue());
    }

    @Test
    public void headerGetNextConsensus() {
        BlockState blockState = new BlockState();
        blockState.trimmedBlock = new TrimmedBlock();
        blockState.trimmedBlock.consensusData = new Ulong(10);
        blockState.trimmedBlock.version = new Uint(1);
        blockState.trimmedBlock.prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        blockState.trimmedBlock.timestamp = new Uint(1568489959);
        blockState.trimmedBlock.index = new Uint(10);
        blockState.trimmedBlock.nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.witness = new Witness();
        blockState.trimmedBlock.witness.verificationScript = new byte[]{0x01, 0x02};
        blockState.trimmedBlock.witness.invocationScript = new byte[]{0x3, 0x04};
        blockState.trimmedBlock.hashes = new UInt256[]{new MinerTransaction().hash()};
        blockState.systemFeeAmount = 10;

        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(blockState.trimmedBlock.getHeader()));

        neoService.invoke("Neo.Header.GetNextConsensus".getBytes(), engine);

        Assert.assertEquals(blockState.trimmedBlock.nextConsensus.toArray(), engine.getCurrentContext().evaluationStack.pop().getByteArray());
    }

    @Test
    public void transactionGetType() {
        ContractTransaction contractTransaction = new ContractTransaction();
        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(contractTransaction));

        neoService.invoke("Neo.Transaction.GetType".getBytes(), engine);

        Assert.assertEquals(contractTransaction.type.value(), engine.getCurrentContext().evaluationStack.pop().getBigInteger().intValue());
    }

    @Test
    public void transactionGetAttributes() {
        ContractTransaction contractTransaction = new ContractTransaction() {{
            attributes = new TransactionAttribute[]{
                    new TransactionAttribute() {{
                        usage = TransactionAttributeUsage.Remark1;
                        data = new byte[]{0x00, 0x01, 0x02, 0x03};
                    }}
            };
        }};
        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(contractTransaction));

        neoService.invoke("Neo.Transaction.GetAttributes".getBytes(), engine);

        neo.vm.Types.Array array = (Array) engine.getCurrentContext().evaluationStack.pop();
        Assert.assertEquals(1, array.getCount());
        InteropInterface<TransactionAttribute> attributeInteropInterface = (InteropInterface<TransactionAttribute>) array.getArrayItem(0);
        TransactionAttribute attr = attributeInteropInterface.getInterface();
        Assert.assertArrayEquals(contractTransaction.attributes[0].data, attr.data);
    }

    @Test
    public void transactionGetInputs() {
        ContractTransaction contractTransaction = new ContractTransaction() {{
            inputs = new CoinReference[]{
                    new CoinReference() {{
                        prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                        prevIndex = Ushort.ONE;
                    }}
            };
        }};
        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(contractTransaction));

        neoService.invoke("Neo.Transaction.GetInputs".getBytes(), engine);

        neo.vm.Types.Array array = (Array) engine.getCurrentContext().evaluationStack.pop();
        Assert.assertEquals(1, array.getCount());
        InteropInterface<CoinReference> inputInterface = (InteropInterface<CoinReference>) array.getArrayItem(0);
        CoinReference input = inputInterface.getInterface();
        Assert.assertEquals(contractTransaction.inputs[0].prevHash, input.prevHash);
        Assert.assertEquals(contractTransaction.inputs[0].prevIndex, input.prevIndex);
    }

    @Test
    public void transactionGetOutputs() {
        ContractTransaction contractTransaction = new ContractTransaction() {{
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                    }}
            };
        }};
        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(contractTransaction));

        neoService.invoke("Neo.Transaction.GetOutputs".getBytes(), engine);

        neo.vm.Types.Array array = (Array) engine.getCurrentContext().evaluationStack.pop();
        Assert.assertEquals(1, array.getCount());
        InteropInterface<TransactionOutput> outputInterface = (InteropInterface<TransactionOutput>) array.getArrayItem(0);
        TransactionOutput output = outputInterface.getInterface();
        Assert.assertEquals(contractTransaction.outputs[0].assetId, output.assetId);
        Assert.assertEquals(contractTransaction.outputs[0].value, output.value);
        Assert.assertEquals(contractTransaction.outputs[0].scriptHash, output.scriptHash);
    }

    @Test
    public void transactionGetReferences() {
        // prepare data
        MinerTransaction minerTransaction = new MinerTransaction() {{
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = MyBlockchain2.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                    }}
            };
        }};

        TransactionState txState = new TransactionState();
        txState.blockIndex = new Uint(10);
        txState.transaction = minerTransaction;
        snapshot.getTransactions().add(minerTransaction.hash(), txState);
        snapshot.commit();


        // check
        ContractTransaction contractTransaction = new ContractTransaction() {{
            inputs = new CoinReference[]{
                    new CoinReference() {{
                        prevHash = minerTransaction.hash();
                        prevIndex = Ushort.ZERO;
                    }}
            };
        }};
        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(contractTransaction));

        neoService.invoke("Neo.Transaction.GetReferences".getBytes(), engine);

        neo.vm.Types.Array array = (Array) engine.getCurrentContext().evaluationStack.pop();
        Assert.assertEquals(1, array.getCount());
        InteropInterface<TransactionOutput> refrenceInterface = (InteropInterface<TransactionOutput>) array.getArrayItem(0);
        TransactionOutput refence = refrenceInterface.getInterface();
        Assert.assertEquals(minerTransaction.outputs[0].assetId, refence.assetId);
        Assert.assertEquals(minerTransaction.outputs[0].value, refence.value);
        Assert.assertEquals(minerTransaction.outputs[0].scriptHash, refence.scriptHash);

        // clear data
        snapshot.getTransactions().delete(minerTransaction.hash());
        snapshot.commit();
    }

    @Test
    public void transactionGetUnspentCoins() {
        // prepare data
        MinerTransaction minerTransaction = new MinerTransaction() {{
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = MyBlockchain2.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                    }},
                    new TransactionOutput() {{
                        assetId = MyBlockchain2.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
                    }}
            };
        }};
        UnspentCoinState unspentCoinState = new UnspentCoinState() {{
            items = new CoinState[]{CoinState.Confirmed, CoinState.Spent};
        }};
        snapshot.getUnspentCoins().add(minerTransaction.hash(), unspentCoinState);

        TransactionState txState = new TransactionState() {{
            blockIndex = new Uint(10);
            transaction = minerTransaction;
        }};
        snapshot.getTransactions().add(minerTransaction.hash(), txState);
        snapshot.commit();

        // check
        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(minerTransaction));

        neoService.invoke("Neo.Transaction.GetUnspentCoins".getBytes(), engine);

        neo.vm.Types.Array array = (Array) engine.getCurrentContext().evaluationStack.pop();
        Assert.assertEquals(1, array.getCount());
        InteropInterface<TransactionOutput> refrenceInterface = (InteropInterface<TransactionOutput>) array.getArrayItem(0);
        TransactionOutput output = refrenceInterface.getInterface();
        Assert.assertEquals(minerTransaction.outputs[0].assetId, output.assetId);
        Assert.assertEquals(minerTransaction.outputs[0].value, output.value);
        Assert.assertEquals(minerTransaction.outputs[0].scriptHash, output.scriptHash);


        // clear data
        snapshot.getTransactions().delete(minerTransaction.hash());
        snapshot.getUnspentCoins().delete(minerTransaction.hash());
        snapshot.commit();
    }

    @Test
    public void transactionGetWitnesses() {
        // prepare data
        ContractState contractState = new ContractState();
        contractState.parameterList = new ContractParameterType[]{
                ContractParameterType.Signature,
                ContractParameterType.String,
                ContractParameterType.Hash160
        };
        contractState.author = "test";
        contractState.codeVersion = "1.0";
        contractState.contractProperties = new ContractPropertyState((byte) ((1 << 0) | (1 << 1) | (1 << 2)));
        contractState.name = "test";
        contractState.email = "test@neo.org";
        contractState.description = "desc";
        contractState.returnType = ContractParameterType.Void;
        contractState.script = new byte[]{0x01, 0x02, 0x03, 0x04};

        snapshot.getContracts().delete(contractState.getScriptHash());
        snapshot.getContracts().add(contractState.getScriptHash(), contractState);


        MinerTransaction minerTransaction = new MinerTransaction() {{
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = MyBlockchain2.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                        scriptHash = contractState.getScriptHash();
                    }},
                    new TransactionOutput() {{
                        assetId = MyBlockchain2.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
                    }}
            };
        }};
        TransactionState txState = new TransactionState() {{
            blockIndex = new Uint(10);
            transaction = minerTransaction;
        }};
        snapshot.getTransactions().add(minerTransaction.hash(), txState);
        snapshot.commit();


        // check
        ContractTransaction contractTransaction = new ContractTransaction() {{
            inputs = new CoinReference[]{
                    new CoinReference() {{
                        prevHash = minerTransaction.hash();
                        prevIndex = Ushort.ZERO;
                    }}
            };
            witnesses = new Witness[]{
                    new Witness() {{
                        invocationScript = new byte[]{0x01, 0x00};
                        verificationScript = new byte[0];
                    }}
            };
        }};

        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(contractTransaction));

        neoService.invoke("Neo.Transaction.GetWitnesses".getBytes(), engine);

        neo.vm.Types.Array array = (Array) engine.getCurrentContext().evaluationStack.pop();
        Assert.assertEquals(1, array.getCount());
        InteropInterface<WitnessWrapper> refrenceInterface = (InteropInterface<WitnessWrapper>) array.getArrayItem(0);
        WitnessWrapper wrapper = refrenceInterface.getInterface();
        Assert.assertEquals(contractState.script, wrapper.verificationScript);


        // clear data
        snapshot.getContracts().delete(contractState.getScriptHash());
        snapshot.getTransactions().delete(minerTransaction.hash());
        snapshot.commit();
    }

    @Test
    public void invocationTransactionGetScript() {
        InvocationTransaction invocationTx = new InvocationTransaction() {{
            script = new byte[]{0x00, 0x01, 0x02};
        }};

        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(invocationTx));

        neoService.invoke("Neo.InvocationTransaction.GetScript".getBytes(), engine);

        neo.vm.StackItem stackItem = engine.getCurrentContext().evaluationStack.pop();
        Assert.assertArrayEquals(invocationTx.script, stackItem.getByteArray());
    }

    @Test
    public void witnessGetVerificationScript() {
        WitnessWrapper wrapper = new WitnessWrapper(new byte[]{0x00, 0x01, 0x02});
        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(wrapper));

        neoService.invoke("Neo.Witness.GetVerificationScript".getBytes(), engine);

        neo.vm.StackItem stackItem = engine.getCurrentContext().evaluationStack.pop();
        Assert.assertArrayEquals(wrapper.verificationScript, stackItem.getByteArray());
    }

    @Test
    public void attributeGetUsage() {
        TransactionAttribute attribute = new TransactionAttribute() {{
            usage = TransactionAttributeUsage.Remark1;
            data = new byte[]{0x00, 0x01, 0x02, 0x03};
        }};
        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(attribute));

        neoService.invoke("Neo.Attribute.GetUsage".getBytes(), engine);

        neo.vm.StackItem stackItem = engine.getCurrentContext().evaluationStack.pop();
        Assert.assertEquals(attribute.usage.value(), stackItem.getBigInteger().intValue());
    }

    @Test
    public void attributeGetData() {
        TransactionAttribute attribute = new TransactionAttribute() {{
            usage = TransactionAttributeUsage.Remark1;
            data = new byte[]{0x00, 0x01, 0x02, 0x03};
        }};
        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(attribute));

        neoService.invoke("Neo.Attribute.GetData".getBytes(), engine);

        neo.vm.StackItem stackItem = engine.getCurrentContext().evaluationStack.pop();
        Assert.assertArrayEquals(attribute.data, stackItem.getByteArray());
    }

    @Test
    public void inputGetHash() {
        CoinReference input = new CoinReference() {{
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            prevIndex = Ushort.ZERO;
        }};
        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(input));

        neoService.invoke("Neo.Input.GetHash".getBytes(), engine);

        neo.vm.StackItem stackItem = engine.getCurrentContext().evaluationStack.pop();
        Assert.assertArrayEquals(input.prevHash.toArray(), stackItem.getByteArray());
    }

    @Test
    public void inputGetIndex() {
        CoinReference input = new CoinReference() {{
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            prevIndex = Ushort.ZERO;
        }};
        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(input));

        neoService.invoke("Neo.Input.GetIndex".getBytes(), engine);

        neo.vm.StackItem stackItem = engine.getCurrentContext().evaluationStack.pop();
        Assert.assertEquals(input.prevIndex.intValue(), stackItem.getBigInteger().intValue());
    }

    @Test
    public void outputGetAssetId() {
        TransactionOutput output = new TransactionOutput() {{
            assetId = MyBlockchain2.UtilityToken.hash();
            scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
        }};
        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(output));

        neoService.invoke("Neo.Output.GetAssetId".getBytes(), engine);

        neo.vm.StackItem stackItem = engine.getCurrentContext().evaluationStack.pop();
        Assert.assertArrayEquals(output.assetId.toArray(), stackItem.getByteArray());
    }

    @Test
    public void outputGetValue() {
        TransactionOutput output = new TransactionOutput() {{
            assetId = MyBlockchain2.UtilityToken.hash();
            scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
        }};
        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(output));

        neoService.invoke("Neo.Output.GetValue".getBytes(), engine);

        neo.vm.StackItem stackItem = engine.getCurrentContext().evaluationStack.pop();
        Assert.assertEquals(output.value.getData(), stackItem.getBigInteger().longValue());
    }

    @Test
    public void outputGetScriptHash() {
        TransactionOutput output = new TransactionOutput() {{
            assetId = MyBlockchain2.UtilityToken.hash();
            scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
        }};
        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(output));

        neoService.invoke("Neo.Output.GetScriptHash".getBytes(), engine);

        neo.vm.StackItem stackItem = engine.getCurrentContext().evaluationStack.pop();
        Assert.assertArrayEquals(output.scriptHash.toArray(), stackItem.getByteArray());
    }

    @Test
    public void accountGetScriptHash() {
        AccountState accountState = new AccountState();
        accountState.scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        accountState.isFrozen = false;
        accountState.votes = new ECPoint[]{new ECPoint(ECC.getInfinityPoint())};
        accountState.balances = new ConcurrentHashMap<>();
        accountState.balances.put(Blockchain.GoverningToken.hash(), new Fixed8(100));
        accountState.balances.put(Blockchain.UtilityToken.hash(), new Fixed8(200));

        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(accountState));

        neoService.invoke("Neo.Account.GetScriptHash".getBytes(), engine);

        neo.vm.StackItem stackItem = engine.getCurrentContext().evaluationStack.pop();
        Assert.assertArrayEquals(accountState.scriptHash.toArray(), stackItem.getByteArray());
    }


    private ECPoint getRandomPublic() {
        byte[] privateKey = new byte[32];
        Random rng = new Random();

        rng.nextBytes(privateKey);
        KeyPair key = new KeyPair(privateKey);
        return key.publicKey;
    }

    @Test
    public void accountGetVotes() {
        AccountState accountState = new AccountState();
        accountState.scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        accountState.isFrozen = false;
        accountState.votes = new ECPoint[]{getRandomPublic()};
        accountState.balances = new ConcurrentHashMap<>();
        accountState.balances.put(Blockchain.GoverningToken.hash(), new Fixed8(100));
        accountState.balances.put(Blockchain.UtilityToken.hash(), new Fixed8(200));

        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(accountState));

        neoService.invoke("Neo.Account.GetVotes".getBytes(), engine);

        neo.vm.Types.Array array = (Array) engine.getCurrentContext().evaluationStack.pop();
        Assert.assertEquals(1, array.getCount());
        Assert.assertArrayEquals(accountState.votes[0].getEncoded(true), array.getArrayItem(0).getByteArray());
    }

    @Test
    public void accountGetBalance() {
        AccountState accountState = new AccountState();
        accountState.scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        accountState.isFrozen = false;
        accountState.votes = new ECPoint[]{new ECPoint(ECC.getInfinityPoint())};
        accountState.balances = new ConcurrentHashMap<>();
        accountState.balances.put(Blockchain.GoverningToken.hash(), new Fixed8(100));
        accountState.balances.put(Blockchain.UtilityToken.hash(), new Fixed8(200));

        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(Blockchain.UtilityToken.hash().toArray()));
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(accountState));

        neoService.invoke("Neo.Account.GetBalance".getBytes(), engine);

        neo.vm.StackItem stackItem = engine.getCurrentContext().evaluationStack.pop();
        Assert.assertEquals(new Fixed8(200).getData(), stackItem.getBigInteger().longValue());
    }

    @Test
    public void accountIsStandard() {
        // prepare data
        ContractState contractState = new ContractState();
        contractState.parameterList = new ContractParameterType[]{
                ContractParameterType.Signature,
                ContractParameterType.String,
                ContractParameterType.Hash160
        };
        contractState.author = "test";
        contractState.codeVersion = "1.0";
        contractState.contractProperties = new ContractPropertyState((byte) ((1 << 0) | (1 << 1) | (1 << 2)));
        contractState.name = "test";
        contractState.email = "test@neo.org";
        contractState.description = "desc";
        contractState.returnType = ContractParameterType.Void;
        contractState.script = new byte[]{0x01, 0x02, 0x03, 0x04};

        snapshot.getContracts().delete(contractState.getScriptHash());
        snapshot.getContracts().add(contractState.getScriptHash(), contractState);
        snapshot.commit();


        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(contractState.getScriptHash().toArray()));

        neoService.invoke("Neo.Account.IsStandard".getBytes(), engine);

        neo.vm.StackItem stackItem = engine.getCurrentContext().evaluationStack.pop();
        Assert.assertEquals(false, stackItem.getBoolean());


        // test single signature address
        ECPoint publicKey = getRandomPublic();
        ContractState account1Contract = new ContractState() {{
            parameterList = new ContractParameterType[]{
                    ContractParameterType.Signature,
                    ContractParameterType.Hash160
            };
            author = "test";
            codeVersion = "1.0";
            contractProperties = new ContractPropertyState((byte) ((1 << 0) | (1 << 1) | (1 << 2)));
            name = "test";
            email = "test@neo.org";
            description = "desc";
            returnType = ContractParameterType.Void;
            script = Contract.createSignatureRedeemScript(publicKey);
        }};
        snapshot.getContracts().add(account1Contract.getScriptHash(), account1Contract);
        snapshot.commit();

        engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(account1Contract.getScriptHash().toArray()));

        neoService.invoke("Neo.Account.IsStandard".getBytes(), engine);

        stackItem = engine.getCurrentContext().evaluationStack.pop();
        Assert.assertEquals(true, stackItem.getBoolean());


        // test multi-signature
        ECPoint publicKey1 = getRandomPublic();
        ECPoint publicKey2 = getRandomPublic();
        ECPoint publicKey3 = getRandomPublic();
        ContractState account2Contract = new ContractState() {{
            parameterList = new ContractParameterType[]{
                    ContractParameterType.Signature,
                    ContractParameterType.Hash160
            };
            author = "test";
            codeVersion = "1.0";
            contractProperties = new ContractPropertyState((byte) ((1 << 0) | (1 << 1) | (1 << 2)));
            name = "test";
            email = "test@neo.org";
            description = "desc";
            returnType = ContractParameterType.Void;
            script = Contract.createMultiSigRedeemScript(2, new ECPoint[]{publicKey1, publicKey2, publicKey3});
        }};
        snapshot.getContracts().add(account2Contract.getScriptHash(), account1Contract);
        snapshot.commit();

        engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(account2Contract.getScriptHash().toArray()));

        neoService.invoke("Neo.Account.IsStandard".getBytes(), engine);

        stackItem = engine.getCurrentContext().evaluationStack.pop();
        Assert.assertEquals(true, stackItem.getBoolean());


        // clear data
        snapshot.getContracts().delete(contractState.getScriptHash());
        snapshot.getContracts().delete(account1Contract.getScriptHash());
        snapshot.getContracts().delete(account2Contract.getScriptHash());
        snapshot.commit();
    }


    class MyInvocationTransaction extends InvocationTransaction {

        public UInt160 owner;

        public MyInvocationTransaction(UInt160 owner) {
            this.owner = owner;
        }

        @Override
        public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
            return new UInt160[]{owner};
        }
    }

    @Test
    public void assetCreate() {
        // prepare data
        AssetState assetState = new AssetState();
        assetState.assetType = AssetType.Share;
        assetState.name = "Test";
        assetState.amount = new Fixed8(100000000);
        assetState.available = new Fixed8(100000000);
        assetState.precision = 0;
        assetState.fee = Fixed8.ZERO;
        assetState.feeAddress = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        assetState.owner = getRandomPublic();
        assetState.admin = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        assetState.issuer = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff03");
        assetState.expiration = new Uint(1000000);
        assetState.isFrozen = false;

        UInt160 ownerHash = UInt160.parseToScriptHash(Contract.createSignatureRedeemScript(assetState.owner));
        MyInvocationTransaction invocationTransaction = new MyInvocationTransaction(ownerHash);
        invocationTransaction.script = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04};
        assetState.assetId = invocationTransaction.hash();

        ExecutionEngine engine = new ExecutionEngine(invocationTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);

        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(assetState.issuer.toArray()));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(assetState.issuer.toArray()));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(assetState.admin.toArray()));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(assetState.owner.getEncoded(true)));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(StackItem.getStackItem(assetState.precision)));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(StackItem.getStackItem(assetState.amount.getData())));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(assetState.name));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(StackItem.getStackItem(assetState.assetType.value())));

        neoService.invoke("Neo.Asset.Create".getBytes(), engine);

        InteropInterface<AssetState> assetStateInteropInterface = (InteropInterface<AssetState>) engine.getCurrentContext().evaluationStack.pop();
        AssetState newAsset = assetStateInteropInterface.getInterface();
        Assert.assertEquals(assetState.assetId, newAsset.assetId);
        Assert.assertEquals(assetState.assetType, newAsset.assetType);
        Assert.assertEquals(assetState.name, newAsset.name);
        Assert.assertEquals(assetState.amount, newAsset.amount);
        Assert.assertEquals(assetState.owner, newAsset.owner);
        Assert.assertEquals(assetState.admin, newAsset.admin);
        Assert.assertEquals(assetState.issuer, newAsset.issuer);
    }

    @Test
    public void assetRenew() {
        // prepare data
        HashIndexState hashIndexState = snapshot.getBlockHashIndex().getAndChange();
        hashIndexState.index = new Uint(10);
        hashIndexState.hash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");

        AssetState assetState = new AssetState();
        assetState.assetType = AssetType.Share;
        assetState.assetId = new MinerTransaction().hash();
        assetState.name = "Test";
        assetState.amount = new Fixed8(100000000);
        assetState.available = new Fixed8(100000000);
        assetState.precision = 0;
        assetState.fee = Fixed8.ZERO;
        assetState.feeAddress = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        assetState.owner = getRandomPublic();
        assetState.admin = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        assetState.issuer = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff03");
        assetState.expiration = new Uint(1000000);
        assetState.isFrozen = false;

        snapshot.getAssets().add(assetState.assetId, assetState);
        snapshot.commit();


        // check
        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(StackItem.getStackItem(1)));
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(assetState));

        neoService.invoke("Neo.Asset.Renew".getBytes(), engine);

        Assert.assertEquals(3000000, engine.getCurrentContext().evaluationStack.pop().getBigInteger().longValue());


        // clear data
        snapshot.getAssets().delete(assetState.assetId);
        snapshot.commit();
    }

    @Test
    public void assetGetAssetId() {
        AssetState assetState = new AssetState();
        assetState.assetType = AssetType.Share;
        assetState.assetId = new MinerTransaction().hash();
        assetState.name = "Test";
        assetState.amount = new Fixed8(100000000);
        assetState.available = new Fixed8(100000000);
        assetState.precision = 0;
        assetState.fee = Fixed8.ZERO;
        assetState.feeAddress = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        assetState.owner = getRandomPublic();
        assetState.admin = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        assetState.issuer = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff03");
        assetState.expiration = new Uint(1000000);
        assetState.isFrozen = false;

        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(assetState));

        neoService.invoke("Neo.Asset.GetAssetId".getBytes(), engine);

        Assert.assertArrayEquals(assetState.assetId.toArray(), engine.getCurrentContext().evaluationStack.pop().getByteArray());
    }

    @Test
    public void assetGetAssetType() {
        AssetState assetState = new AssetState();
        assetState.assetType = AssetType.Share;
        assetState.assetId = new MinerTransaction().hash();
        assetState.name = "Test";
        assetState.amount = new Fixed8(100000000);
        assetState.available = new Fixed8(100000000);
        assetState.precision = 0;
        assetState.fee = Fixed8.ZERO;
        assetState.feeAddress = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        assetState.owner = getRandomPublic();
        assetState.admin = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        assetState.issuer = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff03");
        assetState.expiration = new Uint(1000000);
        assetState.isFrozen = false;

        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(assetState));

        neoService.invoke("Neo.Asset.GetAssetType".getBytes(), engine);

        Assert.assertEquals(assetState.assetType.value(), engine.getCurrentContext().evaluationStack.pop().getBigInteger().byteValue());
    }

    @Test
    public void assetGetAmount() {
        AssetState assetState = new AssetState();
        assetState.assetType = AssetType.Share;
        assetState.assetId = new MinerTransaction().hash();
        assetState.name = "Test";
        assetState.amount = new Fixed8(100000000);
        assetState.available = new Fixed8(100000000);
        assetState.precision = 0;
        assetState.fee = Fixed8.ZERO;
        assetState.feeAddress = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        assetState.owner = getRandomPublic();
        assetState.admin = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        assetState.issuer = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff03");
        assetState.expiration = new Uint(1000000);
        assetState.isFrozen = false;

        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(assetState));

        neoService.invoke("Neo.Asset.GetAmount".getBytes(), engine);

        Assert.assertEquals(assetState.amount.getData(), engine.getCurrentContext().evaluationStack.pop().getBigInteger().longValue());
    }

    @Test
    public void assetGetAvailable() {
        AssetState assetState = new AssetState();
        assetState.assetType = AssetType.Share;
        assetState.assetId = new MinerTransaction().hash();
        assetState.name = "Test";
        assetState.amount = new Fixed8(100000000);
        assetState.available = new Fixed8(100000000);
        assetState.precision = 0;
        assetState.fee = Fixed8.ZERO;
        assetState.feeAddress = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        assetState.owner = getRandomPublic();
        assetState.admin = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        assetState.issuer = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff03");
        assetState.expiration = new Uint(1000000);
        assetState.isFrozen = false;

        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(assetState));

        neoService.invoke("Neo.Asset.GetAvailable".getBytes(), engine);

        Assert.assertEquals(assetState.available.getData(), engine.getCurrentContext().evaluationStack.pop().getBigInteger().longValue());
    }

    @Test
    public void assetGetPrecision() {
        AssetState assetState = new AssetState();
        assetState.assetType = AssetType.Share;
        assetState.assetId = new MinerTransaction().hash();
        assetState.name = "Test";
        assetState.amount = new Fixed8(100000000);
        assetState.available = new Fixed8(100000000);
        assetState.precision = 0;
        assetState.fee = Fixed8.ZERO;
        assetState.feeAddress = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        assetState.owner = getRandomPublic();
        assetState.admin = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        assetState.issuer = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff03");
        assetState.expiration = new Uint(1000000);
        assetState.isFrozen = false;

        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(assetState));

        neoService.invoke("Neo.Asset.GetPrecision".getBytes(), engine);

        Assert.assertEquals(assetState.precision, engine.getCurrentContext().evaluationStack.pop().getBigInteger().byteValue());
    }

    @Test
    public void assetGetOwner() {
        AssetState assetState = new AssetState();
        assetState.assetType = AssetType.Share;
        assetState.assetId = new MinerTransaction().hash();
        assetState.name = "Test";
        assetState.amount = new Fixed8(100000000);
        assetState.available = new Fixed8(100000000);
        assetState.precision = 0;
        assetState.fee = Fixed8.ZERO;
        assetState.feeAddress = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        assetState.owner = getRandomPublic();
        assetState.admin = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        assetState.issuer = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff03");
        assetState.expiration = new Uint(1000000);
        assetState.isFrozen = false;

        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(assetState));

        neoService.invoke("Neo.Asset.GetOwner".getBytes(), engine);

        Assert.assertArrayEquals(assetState.owner.getEncoded(true), engine.getCurrentContext().evaluationStack.pop().getByteArray());
    }

    @Test
    public void assetGetAdmin() {
        AssetState assetState = new AssetState();
        assetState.assetType = AssetType.Share;
        assetState.assetId = new MinerTransaction().hash();
        assetState.name = "Test";
        assetState.amount = new Fixed8(100000000);
        assetState.available = new Fixed8(100000000);
        assetState.precision = 0;
        assetState.fee = Fixed8.ZERO;
        assetState.feeAddress = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        assetState.owner = getRandomPublic();
        assetState.admin = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        assetState.issuer = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff03");
        assetState.expiration = new Uint(1000000);
        assetState.isFrozen = false;

        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(assetState));

        neoService.invoke("Neo.Asset.GetAdmin".getBytes(), engine);

        Assert.assertEquals(assetState.admin.toArray(), engine.getCurrentContext().evaluationStack.pop().getByteArray());
    }

    @Test
    public void assetGetIssuer() {
        AssetState assetState = new AssetState();
        assetState.assetType = AssetType.Share;
        assetState.assetId = new MinerTransaction().hash();
        assetState.name = "Test";
        assetState.amount = new Fixed8(100000000);
        assetState.available = new Fixed8(100000000);
        assetState.precision = 0;
        assetState.fee = Fixed8.ZERO;
        assetState.feeAddress = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        assetState.owner = getRandomPublic();
        assetState.admin = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        assetState.issuer = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff03");
        assetState.expiration = new Uint(1000000);
        assetState.isFrozen = false;

        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(assetState));

        neoService.invoke("Neo.Asset.GetIssuer".getBytes(), engine);

        Assert.assertEquals(assetState.issuer.toArray(), engine.getCurrentContext().evaluationStack.pop().getByteArray());
    }

    @Test
    public void contractCreate() {
        // prepare data
        ContractState contractState = new ContractState();
        contractState.parameterList = new ContractParameterType[]{
                ContractParameterType.Signature,
                ContractParameterType.String,
                ContractParameterType.Hash160
        };
        contractState.author = "test";
        contractState.codeVersion = "1.0";
        contractState.contractProperties = new ContractPropertyState((byte) ((1 << 0) | (1 << 1) | (1 << 2)));
        contractState.name = "test";
        contractState.email = "test@neo.org";
        contractState.description = "desc";
        contractState.returnType = ContractParameterType.Void;
        contractState.script = new byte[]{0x01, 0x02, 0x03, 0x04};

        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(contractState.description));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(contractState.email));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(contractState.author));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(contractState.codeVersion));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(contractState.name));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(StackItem.getStackItem(contractState.contractProperties.value())));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(StackItem.getStackItem(contractState.returnType.value())));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(new byte[]{contractState.parameterList[0].value(),
                contractState.parameterList[1].value(),
                contractState.parameterList[2].value()
        }));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(contractState.script));

        neoService.invoke("Neo.Contract.Create".getBytes(), engine);

        InteropInterface<ContractState> contractStateInteropInterface = (InteropInterface<ContractState>) engine.getCurrentContext().evaluationStack.pop();
        ContractState otherContract = contractStateInteropInterface.getInterface();

        Assert.assertArrayEquals(contractState.script, otherContract.script);
        Assert.assertArrayEquals(contractState.parameterList, otherContract.parameterList);
        Assert.assertEquals(contractState.author, otherContract.author);
        Assert.assertEquals(contractState.codeVersion, otherContract.codeVersion);
        Assert.assertEquals(contractState.contractProperties, otherContract.contractProperties);
        Assert.assertEquals(contractState.name, otherContract.name);
        Assert.assertEquals(contractState.email, otherContract.email);
        Assert.assertEquals(contractState.description, otherContract.description);
        Assert.assertEquals(contractState.returnType, otherContract.returnType);
    }

    @Test
    public void contractMigrate() {
        // prepare data
        ExecutionEngine engine = getEngine();

        StorageKey storageKey = new StorageKey();
        storageKey.key = new byte[]{0x02, 0x02};
        storageKey.scriptHash = new UInt160(engine.getCurrentContext().getScriptHash());
        StorageItem storageItem = new StorageItem() {{
            value = new byte[]{0x01, 0x01};
        }};
        snapshot.getStorages().add(storageKey, storageItem);
        snapshot.commit();

        // check
        ContractState contractState = new ContractState();
        contractState.parameterList = new ContractParameterType[]{
                ContractParameterType.Signature,
                ContractParameterType.String,
                ContractParameterType.Hash160
        };
        contractState.author = "test";
        contractState.codeVersion = "1.0";
        contractState.contractProperties = new ContractPropertyState((byte) ((1 << 0) | (1 << 1) | (1 << 2)));
        contractState.name = "test";
        contractState.email = "test@neo.org";
        contractState.description = "desc";
        contractState.returnType = ContractParameterType.Void;
        contractState.script = new byte[]{0x01, 0x02, 0x03, 0x04};


        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(contractState.description));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(contractState.email));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(contractState.author));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(contractState.codeVersion));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(contractState.name));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(StackItem.getStackItem(contractState.contractProperties.value())));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(StackItem.getStackItem(contractState.returnType.value())));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(new byte[]{contractState.parameterList[0].value(),
                contractState.parameterList[1].value(),
                contractState.parameterList[2].value()
        }));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(contractState.script));

        neoService.invoke("Neo.Contract.Migrate".getBytes(), engine);

        InteropInterface<ContractState> contractStateInteropInterface = (InteropInterface<ContractState>) engine.getCurrentContext().evaluationStack.pop();
        ContractState otherContract = contractStateInteropInterface.getInterface();

        Assert.assertArrayEquals(contractState.script, otherContract.script);
        Assert.assertArrayEquals(contractState.parameterList, otherContract.parameterList);
        Assert.assertEquals(contractState.author, otherContract.author);
        Assert.assertEquals(contractState.codeVersion, otherContract.codeVersion);
        Assert.assertEquals(contractState.contractProperties, otherContract.contractProperties);
        Assert.assertEquals(contractState.name, otherContract.name);
        Assert.assertEquals(contractState.email, otherContract.email);
        Assert.assertEquals(contractState.description, otherContract.description);
        Assert.assertEquals(contractState.returnType, otherContract.returnType);

        StorageKey otherKey = new StorageKey() {{
            scriptHash = otherContract.getScriptHash();
            key = storageKey.key;
        }};
        storageKey.scriptHash = otherContract.getScriptHash();
        StorageItem otherItem = snapshot.getStorages().get(otherKey);
        Assert.assertArrayEquals(storageItem.value, otherItem.value);
        Assert.assertEquals(false, otherItem.isConstant);

        // clear data
        snapshot.getStorages().delete(otherKey);
        snapshot.getStorages().delete(storageKey);
        snapshot.commit();
    }

    @Test
    public void contractGetScript() {
        // prepare data
        ContractState contractState = new ContractState();
        contractState.parameterList = new ContractParameterType[]{
                ContractParameterType.Signature,
                ContractParameterType.String,
                ContractParameterType.Hash160
        };
        contractState.author = "test";
        contractState.codeVersion = "1.0";
        contractState.contractProperties = new ContractPropertyState((byte) ((1 << 0) | (1 << 1) | (1 << 2)));
        contractState.name = "test";
        contractState.email = "test@neo.org";
        contractState.description = "desc";
        contractState.returnType = ContractParameterType.Void;
        contractState.script = new byte[]{0x01, 0x02, 0x03, 0x04};

        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(contractState));

        neoService.invoke("Neo.Contract.GetScript".getBytes(), engine);
        Assert.assertArrayEquals(contractState.script, engine.getCurrentContext().evaluationStack.pop().getByteArray());
    }

    @Test
    public void contractIsPayable() {
        // prepare data
        ContractState contractState = new ContractState();
        contractState.parameterList = new ContractParameterType[]{
                ContractParameterType.Signature,
                ContractParameterType.String,
                ContractParameterType.Hash160
        };
        contractState.author = "test";
        contractState.codeVersion = "1.0";
        contractState.contractProperties = new ContractPropertyState((byte) ((1 << 0) | (1 << 1) | (1 << 2)));
        contractState.name = "test";
        contractState.email = "test@neo.org";
        contractState.description = "desc";
        contractState.returnType = ContractParameterType.Void;
        contractState.script = new byte[]{0x01, 0x02, 0x03, 0x04};

        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(contractState));

        neoService.invoke("Neo.Contract.IsPayable".getBytes(), engine);
        Assert.assertEquals(contractState.payable(), engine.getCurrentContext().evaluationStack.pop().getBoolean());
    }

    @Test
    public void storageFind() {
        // prepare data
        ContractState contractState = new ContractState();
        contractState.parameterList = new ContractParameterType[]{
                ContractParameterType.Signature,
                ContractParameterType.String,
                ContractParameterType.Hash160
        };
        contractState.author = "test";
        contractState.codeVersion = "1.0";
        contractState.contractProperties = new ContractPropertyState((byte) ((1 << 0) | (1 << 1) | (1 << 2)));
        contractState.name = "test";
        contractState.email = "test@neo.org";
        contractState.description = "desc";
        contractState.returnType = ContractParameterType.Void;
        contractState.script = new byte[]{0x01, 0x02, 0x03, 0x04};

        snapshot.getContracts().delete(contractState.getScriptHash());
        snapshot.getContracts().add(contractState.getScriptHash(), contractState);

        // check
        StorageContext storageContext = new StorageContext() {{
            scriptHash = contractState.getScriptHash();
            isReadOnly = false;
        }};
        byte[] prefixs = new byte[]{0x00, 0x01, 0x02, 0x03,
                0x00, 0x01, 0x02, 0x03,
                0x00, 0x01, 0x02, 0x03,
                0x00, 0x01, 0x02, 0x03,
                0x00, 0x01, 0x02, 0x03};

        byte[] prefix_key;
        ByteArrayOutputStream temp = new ByteArrayOutputStream();
        BinaryWriter ms = new BinaryWriter(temp);
        int index = 0;
        int remain = prefixs.length;
        while (remain >= 16) {
            ms.write(prefixs, index, 16);
            ms.write(new byte[]{0x00});
            index += 16;
            remain -= 16;
        }
        if (remain > 0)
            ms.write(prefixs, index, remain);
        prefix_key = BitConverter.merge(storageContext.scriptHash.toArray(), temp.toByteArray());

        StorageKey storageKey = new StorageKey() {{
            scriptHash = storageContext.scriptHash;
            key = prefixs;
        }};
        StorageItem storageItem = new StorageItem() {{
            value = new byte[]{0x00, 0x01, 0x02, 0x03};
        }};
        snapshot.getStorages().add(storageKey, storageItem);
        snapshot.commit();

        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(prefixs));
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(storageContext));

        neoService.invoke("Neo.Storage.Find".getBytes(), engine);

        InteropInterface<StorageIterator> storageIteratorInteropInterface = (InteropInterface<StorageIterator>) engine.getCurrentContext().evaluationStack.pop();
        StorageIterator storageIterator = storageIteratorInteropInterface.getInterface();
        Assert.assertTrue(storageIterator.next());
        StackItem stackItem = storageIterator.value();

        Assert.assertArrayEquals(storageItem.value, stackItem.getByteArray());

        // clear data
        snapshot.getContracts().delete(contractState.getScriptHash());
        snapshot.getStorages().delete(storageKey);
        snapshot.commit();
    }

    @Test
    public void enumeratorCreate() {
        neo.vm.Types.Boolean aboolean = new neo.vm.Types.Boolean(false);
        neo.vm.Types.Integer integer = new neo.vm.Types.Integer(12);

        neo.vm.Types.Array array = new neo.vm.Types.Array(new StackItem[]{aboolean, integer});

        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(array);
        neoService.invoke("Neo.Enumerator.Create".getBytes(), engine);

        InteropInterface<IEnumerator> enumeratorInteropInterface = (InteropInterface<IEnumerator>) engine.getCurrentContext().evaluationStack.pop();
        IEnumerator enumerator = enumeratorInteropInterface.getInterface();
        Assert.assertTrue(enumerator.next());
        Assert.assertEquals(aboolean, enumerator.value());
        Assert.assertTrue(enumerator.next());
        Assert.assertEquals(integer, enumerator.value());
    }

    @Test
    public void enumeratorNext() {
        neo.vm.Types.Boolean aboolean = new neo.vm.Types.Boolean(false);
        neo.vm.Types.Integer integer = new neo.vm.Types.Integer(12);
        ArrayList<StackItem> list = new ArrayList<>();
        list.add(aboolean);
        list.add(integer);

        IEnumerator enumerator = new ArrayWrapper(list);

        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(enumerator));
        neoService.invoke("Neo.Enumerator.Next".getBytes(), engine);

        Assert.assertEquals(true, engine.getCurrentContext().evaluationStack.pop().getBoolean());
    }

    @Test
    public void enumeratorValue() {
        neo.vm.Types.Boolean aboolean = new neo.vm.Types.Boolean(false);
        neo.vm.Types.Integer integer = new neo.vm.Types.Integer(12);
        ArrayList<StackItem> list = new ArrayList<>();
        list.add(aboolean);
        list.add(integer);

        IEnumerator enumerator = new ArrayWrapper(list);
        enumerator.next();

        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(enumerator));
        neoService.invoke("Neo.Enumerator.Value".getBytes(), engine);

        Assert.assertEquals(aboolean, engine.getCurrentContext().evaluationStack.pop());
    }

    @Test
    public void enumeratorConcat() {
        neo.vm.Types.Boolean aboolean = new neo.vm.Types.Boolean(false);
        neo.vm.Types.Integer integer = new neo.vm.Types.Integer(12);
        ArrayList<StackItem> list = new ArrayList<>();
        list.add(aboolean);
        list.add(integer);

        IEnumerator enumerator1 = new ArrayWrapper(list);
        IEnumerator enumerator2 = new ArrayWrapper(list);

        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(enumerator2));
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(enumerator1));

        neoService.invoke("Neo.Enumerator.Concat".getBytes(), engine);

        InteropInterface<IEnumerator> enumeratorInteropInterface = (InteropInterface<IEnumerator>) engine.getCurrentContext().evaluationStack.pop();
        IEnumerator concatEnumer = enumeratorInteropInterface.getInterface();

        Assert.assertEquals(true, concatEnumer.next());
        Assert.assertEquals(aboolean, concatEnumer.value());

        Assert.assertEquals(true, concatEnumer.next());
        Assert.assertEquals(integer, concatEnumer.value());

        Assert.assertEquals(true, concatEnumer.next());
        Assert.assertEquals(aboolean, concatEnumer.value());

        Assert.assertEquals(true, concatEnumer.next());
        Assert.assertEquals(integer, concatEnumer.value());
    }

    @Test
    public void iteratorCreate() {
        neo.vm.Types.Boolean aboolean = new neo.vm.Types.Boolean(false);
        neo.vm.Types.Integer integer = new neo.vm.Types.Integer(12);
        ArrayList<StackItem> list = new ArrayList<>();
        list.add(aboolean);
        list.add(integer);

        neo.vm.Types.Array array = new neo.vm.Types.Array(list);

        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(array);
        neoService.invoke("Neo.Iterator.Create".getBytes(), engine);

        InteropInterface<IIterator> iIteratorInteropInterface = (InteropInterface<IIterator>) engine.getCurrentContext().evaluationStack.pop();
        IIterator iIterator = iIteratorInteropInterface.getInterface();

        Assert.assertEquals(true, iIterator.next());
        Assert.assertEquals(aboolean, iIterator.value());

        Assert.assertEquals(true, iIterator.next());
        Assert.assertEquals(integer, iIterator.value());
    }

    @Test
    public void iteratorKey() {
        neo.vm.Types.Boolean aboolean = new neo.vm.Types.Boolean(false);
        neo.vm.Types.Integer integer = new neo.vm.Types.Integer(12);
        ArrayList<StackItem> list = new ArrayList<>();
        list.add(aboolean);
        list.add(integer);

        IIterator iterator = new ArrayWrapper(list);
        iterator.next();

        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(iterator));
        neoService.invoke("Neo.Iterator.Key".getBytes(), engine);

        Assert.assertEquals(StackItem.getStackItem(StackItem.getStackItem(0)), engine.getCurrentContext().evaluationStack.pop());
    }

    @Test
    public void iteratorKeys() {
        neo.vm.Types.Boolean aboolean = new neo.vm.Types.Boolean(false);
        neo.vm.Types.Integer integer = new neo.vm.Types.Integer(12);
        ArrayList<StackItem> list = new ArrayList<>();
        list.add(aboolean);
        list.add(integer);

        IIterator iterator = new ArrayWrapper(list);

        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(iterator));
        neoService.invoke("Neo.Iterator.Keys".getBytes(), engine);

        InteropInterface<IteratorKeysWrapper> keysWrapperInteropInterface = (InteropInterface<IteratorKeysWrapper>) engine.getCurrentContext().evaluationStack.pop();
        IteratorKeysWrapper keysWrapper = keysWrapperInteropInterface.getInterface();

        Assert.assertEquals(true, keysWrapper.next());
        Assert.assertEquals(StackItem.getStackItem(StackItem.getStackItem(0)), keysWrapper.value());

        Assert.assertEquals(true, keysWrapper.next());
        Assert.assertEquals(StackItem.getStackItem(StackItem.getStackItem(1)), keysWrapper.value());

        Assert.assertEquals(false, keysWrapper.next());
    }

    @Test
    public void iteratorValues() {
        neo.vm.Types.Boolean aboolean = new neo.vm.Types.Boolean(false);
        neo.vm.Types.Integer integer = new neo.vm.Types.Integer(12);
        ArrayList<StackItem> list = new ArrayList<>();
        list.add(aboolean);
        list.add(integer);

        IIterator iterator = new ArrayWrapper(list);

        ExecutionEngine engine = getEngine();
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(iterator));
        neoService.invoke("Neo.Iterator.Values".getBytes(), engine);

        InteropInterface<IteratorValuesWrapper> valuesWrapperInteropInterface = (InteropInterface<IteratorValuesWrapper>) engine.getCurrentContext().evaluationStack.pop();
        IteratorValuesWrapper valuesWrapper = valuesWrapperInteropInterface.getInterface();

        Assert.assertEquals(true, valuesWrapper.next());
        Assert.assertEquals(aboolean, valuesWrapper.value());

        Assert.assertEquals(true, valuesWrapper.next());
        Assert.assertEquals(integer, valuesWrapper.value());

        Assert.assertEquals(false, valuesWrapper.next());
    }
}