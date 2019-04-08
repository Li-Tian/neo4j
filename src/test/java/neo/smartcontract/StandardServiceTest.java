package neo.smartcontract;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Stack;

import akka.testkit.TestActorRef;
import akka.testkit.TestKit;
import neo.MyNeoSystem;
import neo.NeoSystem;
import neo.UInt160;
import neo.UInt256;
import neo.Wallets.KeyPair;
import neo.Wallets.SQLite.UserWalletAccount;
import neo.Wallets.SQLite.VerificationContract;
import neo.Wallets.WalletAccount;
import neo.cryptography.Crypto;
import neo.cryptography.ecc.ECPoint;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.io.caching.DataCache;
import neo.ledger.BlockState;
import neo.ledger.ContractPropertyState;
import neo.ledger.ContractState;
import neo.ledger.HashIndexState;
import neo.ledger.MyBlockchain2;
import neo.ledger.StorageFlags;
import neo.ledger.StorageItem;
import neo.ledger.StorageKey;
import neo.ledger.TransactionState;
import neo.ledger.TrimmedBlock;
import neo.network.p2p.MyLocalNode;
import neo.network.p2p.MyTaskManager;
import neo.network.p2p.payloads.Block;
import neo.network.p2p.payloads.CoinReference;
import neo.network.p2p.payloads.ContractTransaction;
import neo.network.p2p.payloads.Header;
import neo.network.p2p.payloads.MinerTransaction;
import neo.network.p2p.payloads.Witness;
import neo.persistence.AbstractLeveldbTest;
import neo.persistence.Snapshot;
import neo.vm.ExecutionEngine;
import neo.vm.StackItem;
import neo.vm.Types.Array;
import neo.vm.Types.ByteArray;
import neo.vm.Types.InteropInterface;


public class StandardServiceTest extends AbstractLeveldbTest {

    private static StandardService standardService;
    private static Snapshot snapshot;
    private static MyBlockchain2 blockChain2;
    private static NeoSystem neoSystem;

    @BeforeClass
    public static void setUp() throws IOException {
        AbstractLeveldbTest.setUp(StandardServiceTest.class.getSimpleName());

        snapshot = store.getSnapshot();
        standardService = new StandardService(TriggerType.Application, snapshot);

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
        AbstractLeveldbTest.tearDown(StandardServiceTest.class.getSimpleName());
    }

    @Test
    public void getNotifications() {
        standardService.getNotifications().clear();
        EventHandler.Listener<NotifyEventArgs> listener = (sender, eventArgs) -> Assert.assertEquals("hello", eventArgs.getState().getString());

        ContractTransaction contractTransaction = new ContractTransaction();
        ExecutionEngine engine = new ExecutionEngine(contractTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem("hello"));

        standardService.notify.addListener(listener);
        boolean success = standardService.runtimeNotify(engine);
        Assert.assertTrue(success);

        List<NotifyEventArgs> notifyEventArgs = standardService.getNotifications();
        Assert.assertEquals(1, notifyEventArgs.size());
        Assert.assertEquals("hello", notifyEventArgs.get(0).state.getString());

    }

    @Test
    public void checkStorageContext() {
        // prepare contract data
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

        snapshot.getContracts().add(contractState.getScriptHash(), contractState);
        snapshot.commit();

        StorageContext storageContext = new StorageContext() {{
            scriptHash = contractState.getScriptHash();
            isReadOnly = false;
        }};
        Assert.assertTrue(standardService.checkStorageContext(storageContext));

        // check
        storageContext.scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        Assert.assertFalse(standardService.checkStorageContext(storageContext));

        // clear data
        snapshot.getContracts().delete(contractState.getScriptHash());
        snapshot.commit();
    }

    @Test
    public void getPrice() {
        String method = "System.Blockchain.GetTransaction";
        Uint hash = Helper.toInteropMethodHash(method);
        Assert.assertEquals(200, standardService.getPrice(hash));
    }

    @Test
    public void executionEngineGetScriptContainer() {
        MinerTransaction minerTransaction = new MinerTransaction();
        minerTransaction.nonce = new Uint(21);
        ExecutionEngine engine = new ExecutionEngine(minerTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);

        boolean success = standardService.executionEngineGetScriptContainer(engine);
        Assert.assertTrue(success);
        InteropInterface<MinerTransaction> minerInteropInterface = (InteropInterface<MinerTransaction>) engine.getCurrentContext().evaluationStack.peek();
        MinerTransaction otherMx = minerInteropInterface.getInterface();

        Assert.assertArrayEquals(minerTransaction.hash().toArray(), otherMx.hash().toArray());
        Assert.assertEquals(minerTransaction.nonce, otherMx.nonce);
    }

    @Test
    public void executionEngineGetExecutingScriptHash() {
        MinerTransaction minerTransaction = new MinerTransaction();
        minerTransaction.nonce = new Uint(21);
        ExecutionEngine engine = new ExecutionEngine(minerTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);

        boolean success = standardService.executionEngineGetExecutingScriptHash(engine);
        Assert.assertTrue(success);
        StackItem stackItem = engine.getCurrentContext().evaluationStack.peek();
        Assert.assertArrayEquals(engine.getCurrentContext().getScriptHash(), stackItem.getByteArray());
        Assert.assertArrayEquals(UInt160.parseToScriptHash(script).toArray(), stackItem.getByteArray());
    }

    @Test
    public void executionEngineGetCallingScriptHash() {
        MinerTransaction minerTransaction = new MinerTransaction();
        minerTransaction.nonce = new Uint(21);
        ExecutionEngine engine = new ExecutionEngine(minerTransaction, Crypto.Default);
        byte[] script1 = new byte[]{0x00, 0x01, 0x02, 0x03};
        byte[] script2 = new byte[]{0x04, 0x05, 0x06, 0x07};
        engine.loadScript(script1);
        engine.loadScript(script2);

        boolean success = standardService.executionEngineGetCallingScriptHash(engine);
        Assert.assertTrue(success);
        StackItem stackItem = engine.getCurrentContext().evaluationStack.peek();
        Assert.assertArrayEquals(engine.getCallingContext().getScriptHash(), stackItem.getByteArray());
        Assert.assertArrayEquals(UInt160.parseToScriptHash(script1).toArray(), stackItem.getByteArray());
    }

    @Test
    public void executionEngineGetEntryScriptHash() {
        MinerTransaction minerTransaction = new MinerTransaction();
        minerTransaction.nonce = new Uint(21);
        ExecutionEngine engine = new ExecutionEngine(minerTransaction, Crypto.Default);
        byte[] script1 = new byte[]{0x00, 0x01, 0x02, 0x03};
        byte[] script2 = new byte[]{0x04, 0x05, 0x06, 0x07};
        engine.loadScript(script1);
        engine.loadScript(script2);

        boolean success = standardService.executionEngineGetEntryScriptHash(engine);
        Assert.assertTrue(success);
        StackItem stackItem = engine.getCurrentContext().evaluationStack.peek();
        Assert.assertArrayEquals(engine.getCallingContext().getScriptHash(), stackItem.getByteArray());
        Assert.assertArrayEquals(UInt160.parseToScriptHash(script1).toArray(), stackItem.getByteArray());
    }

    @Test
    public void runtimePlatform() {
        MinerTransaction minerTransaction = new MinerTransaction();
        minerTransaction.nonce = new Uint(21);
        ExecutionEngine engine = new ExecutionEngine(minerTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);

        boolean success = standardService.runtimePlatform(engine);
        Assert.assertTrue(success);

        StackItem stackItem = engine.getCurrentContext().evaluationStack.peek();
        Assert.assertEquals("NEO", new String(stackItem.getByteArray()));
    }

    @Test
    public void runtimeGetTrigger() {
        MinerTransaction minerTransaction = new MinerTransaction();
        minerTransaction.nonce = new Uint(21);
        ExecutionEngine engine = new ExecutionEngine(minerTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);

        boolean success = standardService.runtimeGetTrigger(engine);
        Assert.assertTrue(success);

        StackItem stackItem = engine.getCurrentContext().evaluationStack.peek();
        Assert.assertEquals(TriggerType.Application.getTriggerType(), stackItem.getBigInteger().byteValue());
    }


    class MyContractTransaction extends ContractTransaction {

        public UInt160 hash;

        public MyContractTransaction(UInt160 hash) {
            this.hash = hash;
        }

        @Override
        public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
            return new UInt160[]{hash};
        }
    }

    @Test
    public void checkWitness() {
        UInt160 scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        MyContractTransaction contractTransaction = new MyContractTransaction(scriptHash);

        ExecutionEngine engine = new ExecutionEngine(contractTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);


        boolean success = standardService.checkWitness(engine, scriptHash);
        Assert.assertTrue(success);
    }

    public WalletAccount createAccount(byte[] privateKey) {
        KeyPair key = new KeyPair(privateKey);
        VerificationContract contract = new VerificationContract() {
            {
                script = Contract.createSignatureRedeemScript(key.publicKey);
                parameterList = new ContractParameterType[]{
                        ContractParameterType.Signature
                };
            }
        };

        UserWalletAccount account = new UserWalletAccount(contract.scriptHash());
        account.key = key;
        account.contract = contract;
        return account;
    }

    @Test
    public void checkWitness1() {
        byte[] privateKey = new byte[32];
        Random rng = new Random();

        rng.nextBytes(privateKey);
        WalletAccount account = createAccount(privateKey);
        account.isDefault = true; // the first account is default account

        ECPoint publicKey = account.getKey().publicKey;
        UInt160 scriptHash = account.scriptHash;
        MyContractTransaction contractTransaction = new MyContractTransaction(scriptHash);

        ExecutionEngine engine = new ExecutionEngine(contractTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);


        boolean success = standardService.checkWitness(engine, publicKey);
        Assert.assertTrue(success);
    }

    @Test
    public void runtimeCheckWitness() {
        byte[] privateKey = new byte[32];
        Random rng = new Random();

        rng.nextBytes(privateKey);
        WalletAccount account = createAccount(privateKey);
        account.isDefault = true; // the first account is default account

        ECPoint publicKey = account.getKey().publicKey;
        UInt160 scriptHash = account.scriptHash;
        MyContractTransaction contractTransaction = new MyContractTransaction(scriptHash);

        ExecutionEngine engine = new ExecutionEngine(contractTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);

        // check by scriptHash
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(scriptHash.toArray()));
        boolean success = standardService.runtimeCheckWitness(engine);
        Assert.assertTrue(success);
        StackItem stackItem = engine.getCurrentContext().evaluationStack.peek();
        Assert.assertEquals(true, stackItem.getBoolean());


        // check by publicKey
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(publicKey.getEncoded(true)));
        success = standardService.runtimeCheckWitness(engine);
        Assert.assertTrue(success);
        stackItem = engine.getCurrentContext().evaluationStack.peek();
        Assert.assertEquals(true, stackItem.getBoolean());
    }

    @Test
    public void runtimeNotify() {
        EventHandler.Listener<NotifyEventArgs> listener = new EventHandler.Listener<NotifyEventArgs>() {
            @Override
            public void doWork(Object sender, NotifyEventArgs eventArgs) {
                Assert.assertEquals("hello", eventArgs.getState().getString());
            }
        };

        ContractTransaction contractTransaction = new ContractTransaction();
        ExecutionEngine engine = new ExecutionEngine(contractTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem("hello"));

        standardService.notify.addListener(listener);

        boolean success = standardService.runtimeNotify(engine);
        Assert.assertTrue(success);
    }


    @Test
    public void runtimeLog() {
        EventHandler.Listener<LogEventArgs> listener = new EventHandler.Listener<LogEventArgs>() {
            @Override
            public void doWork(Object sender, LogEventArgs eventArgs) {
                Assert.assertEquals("hello", eventArgs.message);
            }
        };

        ContractTransaction contractTransaction = new ContractTransaction();
        ExecutionEngine engine = new ExecutionEngine(contractTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem("hello"));

        standardService.log.addListener(listener);

        boolean success = standardService.runtimeLog(engine);
        Assert.assertTrue(success);
    }

    @Test
    public void runtimeGetTime() {
        // prepare data
        MinerTransaction minerTransaction = new MinerTransaction();
        BlockState blockState = new BlockState();
        blockState.trimmedBlock = new TrimmedBlock();
        blockState.trimmedBlock.consensusData = new Ulong(10);
        blockState.trimmedBlock.version = new Uint(1);
        blockState.trimmedBlock.prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        blockState.trimmedBlock.timestamp = new Uint(1568489959);
        blockState.trimmedBlock.index = new Uint(1);
        blockState.trimmedBlock.nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.witness = new Witness();
        blockState.trimmedBlock.witness.verificationScript = new byte[]{0x01, 0x02};
        blockState.trimmedBlock.witness.invocationScript = new byte[]{0x3, 0x04};
        blockState.trimmedBlock.hashes = new UInt256[]{minerTransaction.hash()};
        blockState.systemFeeAmount = 10;

        TransactionState state = new TransactionState() {{
            transaction = minerTransaction;
            blockIndex = new Uint(1);
        }};
        snapshot.getTransactions().add(minerTransaction.hash(), state);
        snapshot.getBlocks().add(blockState.trimmedBlock.hash(), blockState);
        snapshot.commit();


        Block block = blockState.trimmedBlock.getBlock(snapshot.getTransactions());
        snapshot.setPersistingBlock(block);

        ContractTransaction contractTransaction = new ContractTransaction();
        ExecutionEngine engine = new ExecutionEngine(contractTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem("hello"));

        standardService.runtimeGetTime(engine);

        StackItem stackItem = engine.getCurrentContext().evaluationStack.peek();
        Assert.assertEquals(block.timestamp.longValue(), stackItem.getBigInteger().longValue());

        // clear data
        snapshot.getTransactions().delete(minerTransaction.hash());
        snapshot.getBlocks().delete(block.hash());
        snapshot.commit();
    }

    @Test
    public void runtimeSerialize() {
        neo.vm.Types.Integer integer = new neo.vm.Types.Integer(10);
        neo.vm.Types.Boolean aBoolean = new neo.vm.Types.Boolean(true);
        neo.vm.Types.ByteArray byteArray = new neo.vm.Types.ByteArray(new byte[]{0x00, 0x01});
        neo.vm.Types.Array array = new neo.vm.Types.Array();
        array.add(integer);
        array.add(aBoolean);
        array.add(byteArray);
        neo.vm.Types.Map map = new neo.vm.Types.Map();
        map.add(integer, aBoolean);
        neo.vm.Types.Struct struct = new neo.vm.Types.Struct(Arrays.asList(integer, aBoolean));

        StackItem stackItem = StackItem.fromInterface(new MinerTransaction());

        ContractTransaction contractTransaction = new ContractTransaction();
        ExecutionEngine engine = new ExecutionEngine(contractTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);

        // test integer
        boolean result;
        engine.getCurrentContext().evaluationStack.push(integer);
        result = standardService.runtimeSerialize(engine);
        Assert.assertTrue(result);
        Assert.assertEquals(1, engine.getCurrentContext().evaluationStack.getCount());

        result = standardService.runtimeDeserialize(engine);
        Assert.assertTrue(result);
        Assert.assertEquals(1, engine.getCurrentContext().evaluationStack.getCount());
        Assert.assertEquals(integer.getBigInteger(), engine.getCurrentContext().evaluationStack.pop().getBigInteger());

        // test aBoolean
        engine.getCurrentContext().evaluationStack.push(aBoolean);
        result = standardService.runtimeSerialize(engine);
        Assert.assertTrue(result);
        Assert.assertEquals(1, engine.getCurrentContext().evaluationStack.getCount());

        result = standardService.runtimeDeserialize(engine);
        Assert.assertTrue(result);
        Assert.assertEquals(1, engine.getCurrentContext().evaluationStack.getCount());
        Assert.assertEquals(aBoolean.getBoolean(), engine.getCurrentContext().evaluationStack.pop().getBoolean());

        // test byteArray
        engine.getCurrentContext().evaluationStack.push(byteArray);
        result = standardService.runtimeSerialize(engine);
        Assert.assertTrue(result);
        Assert.assertEquals(1, engine.getCurrentContext().evaluationStack.getCount());

        result = standardService.runtimeDeserialize(engine);
        Assert.assertTrue(result);
        Assert.assertEquals(1, engine.getCurrentContext().evaluationStack.getCount());
        Assert.assertArrayEquals(byteArray.getByteArray(), engine.getCurrentContext().evaluationStack.pop().getByteArray());


        // test array
        engine.getCurrentContext().evaluationStack.push(array);
        result = standardService.runtimeSerialize(engine);
        Assert.assertTrue(result);
        Assert.assertEquals(1, engine.getCurrentContext().evaluationStack.getCount());

        result = standardService.runtimeDeserialize(engine);
        Assert.assertTrue(result);
        Assert.assertEquals(1, engine.getCurrentContext().evaluationStack.getCount());
        neo.vm.Types.Array otherArray = (Array) engine.getCurrentContext().evaluationStack.pop();
        Assert.assertEquals(array.getCount(), otherArray.getCount());
        Assert.assertEquals(array.getArrayItem(0).getBigInteger(), otherArray.getArrayItem(0).getBigInteger());
        Assert.assertEquals(array.getArrayItem(1).getBoolean(), otherArray.getArrayItem(1).getBoolean());
        Assert.assertArrayEquals(array.getArrayItem(2).getByteArray(), otherArray.getArrayItem(2).getByteArray());

        // test struct
        engine.getCurrentContext().evaluationStack.push(struct);
        result = standardService.runtimeSerialize(engine);
        Assert.assertTrue(result);
        Assert.assertEquals(1, engine.getCurrentContext().evaluationStack.getCount());

        result = standardService.runtimeDeserialize(engine);
        Assert.assertTrue(result);
        Assert.assertEquals(1, engine.getCurrentContext().evaluationStack.getCount());
        neo.vm.Types.Struct otherStruct = (neo.vm.Types.Struct) engine.getCurrentContext().evaluationStack.pop();
        Assert.assertEquals(struct.getCount(), otherStruct.getCount());
        Assert.assertEquals(struct.getArrayItem(0).getBigInteger(), otherStruct.getArrayItem(0).getBigInteger());
        Assert.assertEquals(struct.getArrayItem(1).getBoolean(), otherStruct.getArrayItem(1).getBoolean());

        // test map
        engine.getCurrentContext().evaluationStack.push(map);
        result = standardService.runtimeSerialize(engine);
        Assert.assertTrue(result);
        Assert.assertEquals(1, engine.getCurrentContext().evaluationStack.getCount());

        result = standardService.runtimeDeserialize(engine);
        Assert.assertTrue(result);
        Assert.assertEquals(1, engine.getCurrentContext().evaluationStack.getCount());
        neo.vm.Types.Map otherMap = (neo.vm.Types.Map) engine.getCurrentContext().evaluationStack.pop();
        Assert.assertEquals(map.getCount(), otherMap.getCount());
        Assert.assertEquals(map.getKeys().size(), otherMap.getKeys().size());

        StackItem key = otherMap.getKeys().iterator().next();
        StackItem value = otherMap.getValues().iterator().next();


        Assert.assertEquals(map.getKeys().iterator().next(), key);
        Assert.assertEquals(map.getValues().iterator().next(), value);

        Assert.assertTrue(integer.equals(key));
        Assert.assertEquals(map.getMapItem(integer), otherMap.getMapItem(key));
        Assert.assertEquals(map.getMapItem(integer), otherMap.getMapItem(integer));
    }


    @Test
    public void blockchainGetHeight() {
        // prepare data
        HashIndexState indexState = snapshot.getBlockHashIndex().getAndChange();
        indexState.hash = UInt256.Zero;
        indexState.index = new Uint(10);
        snapshot.commit();

        // check
        String method = "System.Blockchain.GetHeight";
        MinerTransaction minerTransaction = new MinerTransaction();
        ExecutionEngine engine = new ExecutionEngine(minerTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);
        standardService.invoke(method.getBytes(), engine);

        StackItem height = engine.getCurrentContext().evaluationStack.peek();
        neo.vm.Types.Integer vHeight = (neo.vm.Types.Integer) height;
        Assert.assertEquals(10, vHeight.getBigInteger().intValue());

        // clear data
        indexState = snapshot.getBlockHashIndex().getAndChange();
        indexState.hash = UInt256.Zero;
        indexState.index = new Uint(0);
        snapshot.commit();
    }

    @Test
    public void blockchainGetHeader() {
        // prepare data
        BlockState blockState = new BlockState();
        blockState.trimmedBlock = new TrimmedBlock();
        blockState.trimmedBlock.consensusData = new Ulong(10);
        blockState.trimmedBlock.version = new Uint(1);
        blockState.trimmedBlock.prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        blockState.trimmedBlock.timestamp = new Uint(1568489959);
        blockState.trimmedBlock.index = new Uint(1);
        blockState.trimmedBlock.nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.witness = new Witness();
        blockState.trimmedBlock.witness.verificationScript = new byte[]{0x01, 0x02};
        blockState.trimmedBlock.witness.invocationScript = new byte[]{0x3, 0x04};
        blockState.trimmedBlock.hashes = new UInt256[]{new MinerTransaction().hash()};
        blockState.systemFeeAmount = 10;
        snapshot.getBlocks().add(blockState.trimmedBlock.hash(), blockState);
        snapshot.commit();

        blockChain2.myheaderIndex.add(blockState.trimmedBlock.hash());
        blockChain2.myheaderIndex.add(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01"));
        blockChain2.myheaderIndex.add(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff03"));


        // check
        MinerTransaction minerTransaction = new MinerTransaction();
        ExecutionEngine engine = new ExecutionEngine(minerTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);

        Uint height = new Uint(1);
        byte[] heightBytes = BitConverter.getBytes(height);
        engine.getCurrentContext().evaluationStack.push(new ByteArray(heightBytes));

        boolean success = standardService.blockchainGetHeader(engine);

        Assert.assertTrue(success);
        InteropInterface<Header> headerInteropInterface = (InteropInterface<Header>) engine.getCurrentContext().evaluationStack.peek();
        Assert.assertNotNull(headerInteropInterface);
        Header header = blockState.trimmedBlock.getHeader();
        Header otherHeader = headerInteropInterface.getInterface();
        Assert.assertNotNull(otherHeader);

        Assert.assertEquals(header.hash(), otherHeader.hash());
        Assert.assertEquals(header.consensusData, otherHeader.consensusData);
        Assert.assertEquals(header.version, otherHeader.version);
        Assert.assertEquals(header.prevHash, otherHeader.prevHash);
        Assert.assertEquals(header.merkleRoot, otherHeader.merkleRoot);
        Assert.assertEquals(header.timestamp, otherHeader.timestamp);
        Assert.assertEquals(header.index, otherHeader.index);
        Assert.assertEquals(header.nextConsensus, otherHeader.nextConsensus);
        Assert.assertArrayEquals(header.witness.verificationScript, otherHeader.witness.verificationScript);
        Assert.assertArrayEquals(header.witness.invocationScript, otherHeader.witness.invocationScript);

        // clear data
        blockChain2.myheaderIndex.clear();
        snapshot.getBlocks().delete(blockState.trimmedBlock.hash());
        snapshot.commit();
    }

    @Test
    public void blockchainGetBlock() {
        // prepare data
        MinerTransaction minerTransaction = new MinerTransaction();
        BlockState blockState = new BlockState();
        blockState.trimmedBlock = new TrimmedBlock();
        blockState.trimmedBlock.consensusData = new Ulong(10);
        blockState.trimmedBlock.version = new Uint(1);
        blockState.trimmedBlock.prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        blockState.trimmedBlock.timestamp = new Uint(1568489959);
        blockState.trimmedBlock.index = new Uint(1);
        blockState.trimmedBlock.nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.witness = new Witness();
        blockState.trimmedBlock.witness.verificationScript = new byte[]{0x01, 0x02};
        blockState.trimmedBlock.witness.invocationScript = new byte[]{0x3, 0x04};
        blockState.trimmedBlock.hashes = new UInt256[]{minerTransaction.hash()};
        blockState.systemFeeAmount = 10;
        snapshot.getBlocks().add(blockState.trimmedBlock.hash(), blockState);
        TransactionState txState = new TransactionState() {{
            blockIndex = new Uint(1);
            transaction = minerTransaction;
        }};
        snapshot.getTransactions().add(minerTransaction.hash(), txState);
        snapshot.commit();
        blockChain2.myheaderIndex.add(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01"));
        blockChain2.myheaderIndex.add(blockState.trimmedBlock.hash());


        // check

        // get block by height
        ExecutionEngine engine = new ExecutionEngine(minerTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);

        Uint height = new Uint(1);
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(StackItem.getStackItem(height)));

        boolean success = standardService.blockchainGetBlock(engine);
        Assert.assertTrue(success);

        InteropInterface<Block> blockInteropInterface = (InteropInterface<Block>) engine.getCurrentContext().evaluationStack.peek();
        Block other = blockInteropInterface.getInterface();
        Block block = blockState.trimmedBlock.getBlock(snapshot.getTransactions());

        Assert.assertEquals(block.hash(), other.hash());
        Assert.assertEquals(block.consensusData, other.consensusData);
        Assert.assertEquals(block.version, other.version);
        Assert.assertEquals(block.prevHash, other.prevHash);
        Assert.assertEquals(block.merkleRoot, other.merkleRoot);
        Assert.assertEquals(block.timestamp, other.timestamp);
        Assert.assertEquals(block.index, other.index);
        Assert.assertEquals(block.nextConsensus, other.nextConsensus);
        Assert.assertArrayEquals(block.witness.verificationScript, other.witness.verificationScript);
        Assert.assertArrayEquals(block.witness.invocationScript, other.witness.invocationScript);

        // get block by hash
        engine.getCurrentContext().evaluationStack.push(new ByteArray(block.hash().toArray()));
        success = standardService.blockchainGetBlock(engine);
        Assert.assertTrue(success);
        blockInteropInterface = (InteropInterface<Block>) engine.getCurrentContext().evaluationStack.peek();
        other = blockInteropInterface.getInterface();
        Assert.assertEquals(block.hash(), other.hash());
        Assert.assertEquals(block.consensusData, other.consensusData);
        Assert.assertEquals(block.version, other.version);
        Assert.assertEquals(block.prevHash, other.prevHash);
        Assert.assertEquals(block.merkleRoot, other.merkleRoot);
        Assert.assertEquals(block.timestamp, other.timestamp);
        Assert.assertEquals(block.index, other.index);
        Assert.assertEquals(block.nextConsensus, other.nextConsensus);
        Assert.assertArrayEquals(block.witness.verificationScript, other.witness.verificationScript);
        Assert.assertArrayEquals(block.witness.invocationScript, other.witness.invocationScript);


        // clean data
        blockChain2.myheaderIndex.clear();
        snapshot.getBlocks().delete(blockState.trimmedBlock.hash());
        snapshot.getTransactions().delete(minerTransaction.hash());
        snapshot.commit();
    }

    @Test
    public void blockchainGetTransaction() {
        // prepare data
        MinerTransaction minerTransaction = new MinerTransaction();
        TransactionState txState = new TransactionState() {{
            blockIndex = new Uint(1);
            transaction = minerTransaction;
        }};
        snapshot.getTransactions().add(minerTransaction.hash(), txState);
        snapshot.commit();


        // check
        ExecutionEngine engine = new ExecutionEngine(minerTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(minerTransaction.hash().toArray()));

        boolean success = standardService.blockchainGetTransaction(engine);
        Assert.assertTrue(success);

        InteropInterface<MinerTransaction> minerTxInterface = (InteropInterface<MinerTransaction>) engine.getCurrentContext().evaluationStack.peek();
        MinerTransaction other = minerTxInterface.getInterface();

        Assert.assertEquals(minerTransaction.hash(), other.hash());
        Assert.assertEquals(minerTransaction.nonce, other.nonce);
        Assert.assertEquals(minerTransaction.size(), other.size());


        // clear data
        snapshot.getTransactions().delete(minerTransaction.hash());
        snapshot.commit();
    }

    @Test
    public void blockchainGetTransactionHeight() {
        // prepare data
        MinerTransaction minerTransaction = new MinerTransaction();
        TransactionState txState = new TransactionState() {{
            blockIndex = new Uint(1);
            transaction = minerTransaction;
        }};
        snapshot.getTransactions().add(minerTransaction.hash(), txState);
        snapshot.commit();


        // check
        ExecutionEngine engine = new ExecutionEngine(minerTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(minerTransaction.hash().toArray()));

        boolean success = standardService.blockchainGetTransactionHeight(engine);
        Assert.assertTrue(success);

        StackItem stackItem = engine.getCurrentContext().evaluationStack.peek();
        Assert.assertEquals(txState.blockIndex.intValue(), stackItem.getBigInteger().intValue());


        // clear data
        snapshot.getTransactions().delete(minerTransaction.hash());
        snapshot.commit();
    }

    @Test
    public void blockchainGetContract() {
        // prepare data
        DataCache<UInt160, ContractState> contracts = snapshot.getContracts();
        ContractState state = new ContractState();
        state.parameterList = new ContractParameterType[]{
                ContractParameterType.Signature,
                ContractParameterType.String,
                ContractParameterType.Hash160
        };
        state.author = "test";
        state.codeVersion = "1.0";
        state.contractProperties = new ContractPropertyState((byte) ((1 << 0) | (1 << 1) | (1 << 2)));
        state.name = "test";
        state.email = "test@neo.org";
        state.description = "desc";
        state.returnType = ContractParameterType.Void;
        state.script = new byte[]{0x01, 0x02, 0x03, 0x04};

        UInt160 key = state.getScriptHash();
        contracts.add(key, state);
        snapshot.commit();

        // check
        MinerTransaction minerTransaction = new MinerTransaction();
        ExecutionEngine engine = new ExecutionEngine(minerTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(state.getScriptHash().toArray()));

        boolean success = standardService.blockchainGetContract(engine);
        Assert.assertTrue(success);

        InteropInterface<ContractState> contractInterface = (InteropInterface<ContractState>) engine.getCurrentContext().evaluationStack.peek();
        ContractState otherState = contractInterface.getInterface();

        Assert.assertArrayEquals(state.parameterList, otherState.parameterList);
        Assert.assertEquals(state.author, otherState.author);
        Assert.assertEquals(state.codeVersion, otherState.codeVersion);
        Assert.assertEquals(state.name, otherState.name);
        Assert.assertEquals(state.email, otherState.email);
        Assert.assertEquals(state.description, otherState.description);
        Assert.assertArrayEquals(state.script, otherState.script);


        // clear data
        contracts.delete(state.getScriptHash());
        snapshot.commit();
    }

    @Test
    public void headerGetIndex() {
        // prepare data
        MinerTransaction minerTransaction = new MinerTransaction();
        BlockState blockState = new BlockState();
        blockState.trimmedBlock = new TrimmedBlock();
        blockState.trimmedBlock.consensusData = new Ulong(10);
        blockState.trimmedBlock.version = new Uint(1);
        blockState.trimmedBlock.prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        blockState.trimmedBlock.timestamp = new Uint(1568489959);
        blockState.trimmedBlock.index = new Uint(1);
        blockState.trimmedBlock.nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.witness = new Witness();
        blockState.trimmedBlock.witness.verificationScript = new byte[]{0x01, 0x02};
        blockState.trimmedBlock.witness.invocationScript = new byte[]{0x3, 0x04};
        blockState.trimmedBlock.hashes = new UInt256[]{minerTransaction.hash()};
        blockState.systemFeeAmount = 10;

        // check
        ExecutionEngine engine = new ExecutionEngine(minerTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);

        StackItem headerInteropInterface = StackItem.fromInterface(blockState.trimmedBlock.getHeader());
        engine.getCurrentContext().evaluationStack.push(headerInteropInterface);

        boolean success = standardService.headerGetIndex(engine);
        Assert.assertTrue(success);

        StackItem stackItem = engine.getCurrentContext().evaluationStack.peek();
        Assert.assertEquals(blockState.trimmedBlock.index.intValue(), stackItem.getBigInteger().intValue());
    }

    @Test
    public void headerGetHash() {
        // prepare data
        MinerTransaction minerTransaction = new MinerTransaction();
        BlockState blockState = new BlockState();
        blockState.trimmedBlock = new TrimmedBlock();
        blockState.trimmedBlock.consensusData = new Ulong(10);
        blockState.trimmedBlock.version = new Uint(1);
        blockState.trimmedBlock.prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        blockState.trimmedBlock.timestamp = new Uint(1568489959);
        blockState.trimmedBlock.index = new Uint(1);
        blockState.trimmedBlock.nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.witness = new Witness();
        blockState.trimmedBlock.witness.verificationScript = new byte[]{0x01, 0x02};
        blockState.trimmedBlock.witness.invocationScript = new byte[]{0x3, 0x04};
        blockState.trimmedBlock.hashes = new UInt256[]{minerTransaction.hash()};
        blockState.systemFeeAmount = 10;

        // check
        ExecutionEngine engine = new ExecutionEngine(minerTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);

        StackItem headerInteropInterface = StackItem.fromInterface(blockState.trimmedBlock.getHeader());
        engine.getCurrentContext().evaluationStack.push(headerInteropInterface);

        boolean success = standardService.headerGetHash(engine);
        Assert.assertTrue(success);

        StackItem stackItem = engine.getCurrentContext().evaluationStack.peek();
        Assert.assertArrayEquals(blockState.trimmedBlock.hash().toArray(), stackItem.getByteArray());
    }

    @Test
    public void headerGetPrevHash() {
        // prepare data
        MinerTransaction minerTransaction = new MinerTransaction();
        BlockState blockState = new BlockState();
        blockState.trimmedBlock = new TrimmedBlock();
        blockState.trimmedBlock.consensusData = new Ulong(10);
        blockState.trimmedBlock.version = new Uint(1);
        blockState.trimmedBlock.prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        blockState.trimmedBlock.timestamp = new Uint(1568489959);
        blockState.trimmedBlock.index = new Uint(1);
        blockState.trimmedBlock.nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.witness = new Witness();
        blockState.trimmedBlock.witness.verificationScript = new byte[]{0x01, 0x02};
        blockState.trimmedBlock.witness.invocationScript = new byte[]{0x3, 0x04};
        blockState.trimmedBlock.hashes = new UInt256[]{minerTransaction.hash()};
        blockState.systemFeeAmount = 10;

        // check
        ExecutionEngine engine = new ExecutionEngine(minerTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);

        StackItem headerInteropInterface = StackItem.fromInterface(blockState.trimmedBlock.getHeader());
        engine.getCurrentContext().evaluationStack.push(headerInteropInterface);

        boolean success = standardService.headerGetPrevHash(engine);
        Assert.assertTrue(success);

        StackItem stackItem = engine.getCurrentContext().evaluationStack.peek();
        Assert.assertArrayEquals(blockState.trimmedBlock.prevHash.toArray(), stackItem.getByteArray());
    }

    @Test
    public void headerGetTimestamp() {
        // prepare data
        MinerTransaction minerTransaction = new MinerTransaction();
        BlockState blockState = new BlockState();
        blockState.trimmedBlock = new TrimmedBlock();
        blockState.trimmedBlock.consensusData = new Ulong(10);
        blockState.trimmedBlock.version = new Uint(1);
        blockState.trimmedBlock.prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        blockState.trimmedBlock.timestamp = new Uint(1568489959);
        blockState.trimmedBlock.index = new Uint(1);
        blockState.trimmedBlock.nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.witness = new Witness();
        blockState.trimmedBlock.witness.verificationScript = new byte[]{0x01, 0x02};
        blockState.trimmedBlock.witness.invocationScript = new byte[]{0x3, 0x04};
        blockState.trimmedBlock.hashes = new UInt256[]{minerTransaction.hash()};
        blockState.systemFeeAmount = 10;

        // check
        ExecutionEngine engine = new ExecutionEngine(minerTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);

        StackItem headerInteropInterface = StackItem.fromInterface(blockState.trimmedBlock.getHeader());
        engine.getCurrentContext().evaluationStack.push(headerInteropInterface);

        boolean success = standardService.headerGetTimestamp(engine);
        Assert.assertTrue(success);

        StackItem stackItem = engine.getCurrentContext().evaluationStack.peek();
        Assert.assertEquals(blockState.trimmedBlock.timestamp.longValue(), stackItem.getBigInteger().longValue());
    }


    @Test
    public void blockGetTransactionCount() {
        MinerTransaction minerTransaction = new MinerTransaction();
        BlockState blockState = new BlockState();
        blockState.trimmedBlock = new TrimmedBlock();
        blockState.trimmedBlock.consensusData = new Ulong(10);
        blockState.trimmedBlock.version = new Uint(1);
        blockState.trimmedBlock.prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        blockState.trimmedBlock.timestamp = new Uint(1568489959);
        blockState.trimmedBlock.index = new Uint(1);
        blockState.trimmedBlock.nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.witness = new Witness();
        blockState.trimmedBlock.witness.verificationScript = new byte[]{0x01, 0x02};
        blockState.trimmedBlock.witness.invocationScript = new byte[]{0x3, 0x04};
        blockState.trimmedBlock.hashes = new UInt256[]{minerTransaction.hash()};
        blockState.systemFeeAmount = 10;

        TransactionState txState = new TransactionState() {{
            blockIndex = new Uint(1);
            transaction = minerTransaction;
        }};
        snapshot.getTransactions().add(minerTransaction.hash(), txState);
        snapshot.commit();


        Block block = blockState.trimmedBlock.getBlock(snapshot.getTransactions());

        ExecutionEngine engine = new ExecutionEngine(minerTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);

        StackItem headerInteropInterface = StackItem.fromInterface(block);
        engine.getCurrentContext().evaluationStack.push(headerInteropInterface);

        boolean success = standardService.blockGetTransactionCount(engine);
        Assert.assertTrue(success);

        StackItem stackItem = engine.getCurrentContext().evaluationStack.peek();
        Assert.assertEquals(block.transactions.length, stackItem.getBigInteger().intValue());

        // clear data
        snapshot.getTransactions().delete(minerTransaction.hash());
        snapshot.commit();
    }

    @Test
    public void blockGetTransactions() {
        MinerTransaction minerTransaction = new MinerTransaction();
        BlockState blockState = new BlockState();
        blockState.trimmedBlock = new TrimmedBlock();
        blockState.trimmedBlock.consensusData = new Ulong(10);
        blockState.trimmedBlock.version = new Uint(1);
        blockState.trimmedBlock.prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        blockState.trimmedBlock.timestamp = new Uint(1568489959);
        blockState.trimmedBlock.index = new Uint(1);
        blockState.trimmedBlock.nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.witness = new Witness();
        blockState.trimmedBlock.witness.verificationScript = new byte[]{0x01, 0x02};
        blockState.trimmedBlock.witness.invocationScript = new byte[]{0x3, 0x04};
        blockState.trimmedBlock.hashes = new UInt256[]{minerTransaction.hash()};
        blockState.systemFeeAmount = 10;

        TransactionState txState = new TransactionState() {{
            blockIndex = new Uint(1);
            transaction = minerTransaction;
        }};
        snapshot.getTransactions().add(minerTransaction.hash(), txState);
        snapshot.commit();


        Block block = blockState.trimmedBlock.getBlock(snapshot.getTransactions());

        ExecutionEngine engine = new ExecutionEngine(minerTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);

        StackItem headerInteropInterface = StackItem.fromInterface(block);
        engine.getCurrentContext().evaluationStack.push(headerInteropInterface);

        boolean success = standardService.blockGetTransactions(engine);
        Assert.assertTrue(success);

        neo.vm.Types.Array stackItems = (Array) engine.getCurrentContext().evaluationStack.peek();
        Assert.assertEquals(block.transactions.length, stackItems.getCount());

        InteropInterface<MinerTransaction> minerTransactionInteropInterface = (InteropInterface<MinerTransaction>) stackItems.getArrayItem(0);
        MinerTransaction other = minerTransactionInteropInterface.getInterface();
        Assert.assertEquals(minerTransaction.hash(), other.hash());
        Assert.assertEquals(minerTransaction.nonce, other.nonce);

        // clear data
        snapshot.getTransactions().delete(minerTransaction.hash());
        snapshot.commit();
    }

    @Test
    public void blockGetTransaction() {
        MinerTransaction minerTransaction = new MinerTransaction();
        BlockState blockState = new BlockState();
        blockState.trimmedBlock = new TrimmedBlock();
        blockState.trimmedBlock.consensusData = new Ulong(10);
        blockState.trimmedBlock.version = new Uint(1);
        blockState.trimmedBlock.prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        blockState.trimmedBlock.timestamp = new Uint(1568489959);
        blockState.trimmedBlock.index = new Uint(1);
        blockState.trimmedBlock.nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.witness = new Witness();
        blockState.trimmedBlock.witness.verificationScript = new byte[]{0x01, 0x02};
        blockState.trimmedBlock.witness.invocationScript = new byte[]{0x3, 0x04};
        blockState.trimmedBlock.hashes = new UInt256[]{minerTransaction.hash()};
        blockState.systemFeeAmount = 10;

        TransactionState txState = new TransactionState() {{
            blockIndex = new Uint(1);
            transaction = minerTransaction;
        }};
        snapshot.getTransactions().add(minerTransaction.hash(), txState);
        snapshot.commit();


        Block block = blockState.trimmedBlock.getBlock(snapshot.getTransactions());

        ExecutionEngine engine = new ExecutionEngine(minerTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);

        StackItem headerInteropInterface = StackItem.fromInterface(block);
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(StackItem.getStackItem(Uint.ZERO)));
        engine.getCurrentContext().evaluationStack.push(headerInteropInterface);

        boolean success = standardService.blockGetTransaction(engine);
        Assert.assertTrue(success);

        InteropInterface<MinerTransaction> minerTransactionInteropInterface = (InteropInterface<MinerTransaction>) engine.getCurrentContext().evaluationStack.peek();
        MinerTransaction other = minerTransactionInteropInterface.getInterface();
        Assert.assertEquals(minerTransaction.hash(), other.hash());
        Assert.assertEquals(minerTransaction.nonce, other.nonce);

        // clear data
        snapshot.getTransactions().delete(minerTransaction.hash());
        snapshot.commit();
    }

    @Test
    public void transactionGetHash() {
        MinerTransaction minerTransaction = new MinerTransaction();
        ExecutionEngine engine = new ExecutionEngine(minerTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);

        StackItem headerInteropInterface = StackItem.fromInterface(minerTransaction);
        engine.getCurrentContext().evaluationStack.push(headerInteropInterface);

        // check
        boolean success = standardService.transactionGetHash(engine);
        Assert.assertTrue(success);

        StackItem stackItem = engine.getCurrentContext().evaluationStack.peek();
        Assert.assertArrayEquals(minerTransaction.hash().toArray(), stackItem.getByteArray());
    }

    @Test
    public void storageGetContext() {
        MinerTransaction minerTransaction = new MinerTransaction();
        ExecutionEngine engine = new ExecutionEngine(minerTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);

        boolean success = standardService.storageGetContext(engine);
        Assert.assertTrue(success);

        InteropInterface<StorageContext> storageContextInteropInterface = (InteropInterface<StorageContext>) engine.getCurrentContext().evaluationStack.peek();
        StorageContext storageContext = storageContextInteropInterface.getInterface();
        Assert.assertArrayEquals(engine.getCurrentContext().getScriptHash(), storageContext.scriptHash.toArray());
        Assert.assertEquals(false, storageContext.isReadOnly);
    }

    @Test
    public void storageGetReadOnlyContext() {
        MinerTransaction minerTransaction = new MinerTransaction();
        ExecutionEngine engine = new ExecutionEngine(minerTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);

        boolean success = standardService.storageGetReadOnlyContext(engine);
        Assert.assertTrue(success);

        InteropInterface<StorageContext> storageContextInteropInterface = (InteropInterface<StorageContext>) engine.getCurrentContext().evaluationStack.peek();
        StorageContext storageContext = storageContextInteropInterface.getInterface();
        Assert.assertArrayEquals(engine.getCurrentContext().getScriptHash(), storageContext.scriptHash.toArray());
        Assert.assertEquals(true, storageContext.isReadOnly);
    }

    @Test
    public void storageGet() {
        // prepare contract data
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

        snapshot.getContracts().add(contractState.getScriptHash(), contractState);

        StorageKey storageKey = new StorageKey() {{
            scriptHash = contractState.getScriptHash();
            key = "test_key".getBytes();
        }};
        StorageItem storageItem = new StorageItem() {{
            value = "hello world".getBytes();
            isConstant = true;
        }};

        snapshot.getStorages().add(storageKey, storageItem);
        snapshot.commit();

        // check
        MinerTransaction minerTransaction = new MinerTransaction();
        ExecutionEngine engine = new ExecutionEngine(minerTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);

        StorageContext storageContext = new StorageContext() {{
            scriptHash = contractState.getScriptHash();
            isReadOnly = false;
        }};

        String key = "test_key";
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(key.getBytes()));
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(storageContext));

        boolean success = standardService.storageGet(engine);
        Assert.assertTrue(success);

        StackItem stackItem = engine.getCurrentContext().evaluationStack.peek();
        Assert.assertArrayEquals(storageItem.value, stackItem.getByteArray());

        // clear
        snapshot.getContracts().delete(contractState.getScriptHash());
        snapshot.commit();
    }

    @Test
    public void storageContextAsReadOnly() {
        MinerTransaction minerTransaction = new MinerTransaction();
        ExecutionEngine engine = new ExecutionEngine(minerTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);

        StorageContext storageContext = new StorageContext() {{
            scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            isReadOnly = false;
        }};
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(storageContext));

        // check
        boolean success = standardService.storageContextAsReadOnly(engine);
        Assert.assertTrue(success);

        InteropInterface<StorageContext> storageContextInteropInterface = (InteropInterface<StorageContext>) engine.getCurrentContext().evaluationStack.peek();
        StorageContext otherContext = storageContextInteropInterface.getInterface();
        Assert.assertArrayEquals(storageContext.scriptHash.toArray(), otherContext.scriptHash.toArray());
        Assert.assertEquals(true, otherContext.isReadOnly);
    }

    @Test
    public void contractGetStorageContext() {
        // prepare contract data
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

        // check
        MinerTransaction minerTransaction = new MinerTransaction();
        ExecutionEngine engine = new ExecutionEngine(minerTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);

        StorageContext storageContext = new StorageContext() {{
            scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            isReadOnly = false;
        }};

        standardService.contractsCreated.put(contractState.getScriptHash(), new UInt160(engine.getCurrentContext().getScriptHash()));
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(contractState));

        boolean success = standardService.contractGetStorageContext(engine);
        Assert.assertTrue(success);

        InteropInterface<StorageContext> storageContextInteropInterface = (InteropInterface<StorageContext>) engine.getCurrentContext().evaluationStack.peek();
        StorageContext otherContext = storageContextInteropInterface.getInterface();
        Assert.assertArrayEquals(contractState.getScriptHash().toArray(), otherContext.scriptHash.toArray());
        Assert.assertEquals(false, otherContext.isReadOnly);
    }

    @Test
    public void contractDestroy() {
        // prepare contract data
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


        // check
        MinerTransaction minerTransaction = new MinerTransaction();
        ExecutionEngine engine = new ExecutionEngine(minerTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);
        UInt160 scriptHash = new UInt160(engine.getCurrentContext().getScriptHash());

        snapshot.getContracts().add(scriptHash, contractState);
        snapshot.commit();

        boolean success = standardService.contractDestroy(engine);
        standardService.commit();

        Assert.assertTrue(success);
        ContractState otherContractState = snapshot.getContracts().tryGet(scriptHash);
        Assert.assertNull(otherContractState);

        // clear
        snapshot.getContracts().delete(contractState.getScriptHash());
        snapshot.commit();
    }

    @Test
    public void storagePut() {
        // prepare contract data
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


        MinerTransaction minerTransaction = new MinerTransaction();
        ExecutionEngine engine = new ExecutionEngine(minerTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);

        StorageContext storageContext = new StorageContext() {{
            scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            isReadOnly = false;
        }};
        snapshot.getContracts().add(storageContext.scriptHash, contractState);
        snapshot.commit();

        StorageKey storageKey = new StorageKey() {{
            scriptHash = storageContext.scriptHash;
            key = "test_key".getBytes();
        }};
        StorageItem storageItem = new StorageItem() {{
            value = "hello world".getBytes();
            isConstant = true;
        }};

        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(storageItem.value));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(storageKey.key));
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(storageContext));

        boolean success = standardService.storagePut(engine);
        standardService.commit();
        Assert.assertTrue(success);

        StorageItem otherItem = snapshot.getStorages().get(storageKey);
        Assert.assertArrayEquals(storageItem.value, otherItem.value);

        // clear data
        snapshot.getContracts().delete(storageContext.scriptHash);
        snapshot.getStorages().delete(storageKey);
        snapshot.commit();
    }

    @Test
    public void storagePutEx() {
        // prepare contract data
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


        MinerTransaction minerTransaction = new MinerTransaction();
        ExecutionEngine engine = new ExecutionEngine(minerTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);

        StorageContext storageContext = new StorageContext() {{
            scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            isReadOnly = false;
        }};
        snapshot.getContracts().add(storageContext.scriptHash, contractState);
        snapshot.commit();

        StorageKey storageKey = new StorageKey() {{
            scriptHash = storageContext.scriptHash;
            key = "test_key".getBytes();
        }};
        StorageItem storageItem = new StorageItem() {{
            value = "hello world".getBytes();
            isConstant = true;
        }};

        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(StackItem.getStackItem(StorageFlags.Constant.value())));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(storageItem.value));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(storageKey.key));
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(storageContext));

        boolean success = standardService.storagePutEx(engine);
        standardService.commit();
        Assert.assertTrue(success);

        StorageItem otherItem = snapshot.getStorages().get(storageKey);
        Assert.assertArrayEquals(storageItem.value, otherItem.value);

        // failed, when add again
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(StackItem.getStackItem(StorageFlags.Constant.value())));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(storageItem.value));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(storageKey.key));
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(storageContext));

        success = standardService.storagePutEx(engine);
        Assert.assertFalse(success);

        // clear data
        snapshot.getContracts().delete(storageContext.scriptHash);
        snapshot.getStorages().delete(storageKey);
        snapshot.commit();
    }

    @Test
    public void storageDelete() {
        // prepare contract data
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


        MinerTransaction minerTransaction = new MinerTransaction();
        ExecutionEngine engine = new ExecutionEngine(minerTransaction, Crypto.Default);
        byte[] script = new byte[]{0x00, 0x01, 0x02, 0x03};
        engine.loadScript(script);

        StorageContext storageContext = new StorageContext() {{
            scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            isReadOnly = false;
        }};
        snapshot.getContracts().add(storageContext.scriptHash, contractState);
        snapshot.commit();

        StorageKey storageKey = new StorageKey() {{
            scriptHash = storageContext.scriptHash;
            key = "test_key".getBytes();
        }};
        StorageItem storageItem = new StorageItem() {{
            value = "hello world".getBytes();
            isConstant = false;
        }};

        // add storage key -> storage item
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(StackItem.getStackItem(StorageFlags.None.value())));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(storageItem.value));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(storageKey.key));
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(storageContext));

        boolean success = standardService.storagePutEx(engine);
        standardService.commit();
        Assert.assertTrue(success);


        // remove storage key
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(storageKey.key));
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(storageContext));
        success = standardService.storageDelete(engine);
        standardService.commit();
        Assert.assertTrue(success);

        StorageItem otherItem = snapshot.getStorages().tryGet(storageKey);
        Assert.assertNull(otherItem);

        // add storage key(constants) -> storage item
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(StackItem.getStackItem(StorageFlags.Constant.value())));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(storageItem.value));
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(storageKey.key));
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(storageContext));

        success = standardService.storagePutEx(engine);
        standardService.commit();
        Assert.assertTrue(success);


        // remove storage key
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(storageKey.key));
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(storageContext));
        success = standardService.storageDelete(engine);
        standardService.commit();
        Assert.assertFalse(success);

        // clear data
        snapshot.getContracts().delete(contractState.getScriptHash());
        snapshot.getStorages().delete(storageKey);
        snapshot.commit();
    }
}