package neo.persistence;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.TestActorRef;
import akka.testkit.TestKit;
import neo.Fixed8;
import neo.MyNeoSystem;
import neo.NeoSystem;
import neo.UInt160;
import neo.UInt256;
import neo.cryptography.ecc.ECC;
import neo.cryptography.ecc.ECPoint;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.csharp.Ushort;
import neo.io.caching.DataCache;
import neo.io.caching.MetaDataCache;
import neo.ledger.AccountState;
import neo.ledger.BlockState;
import neo.ledger.Blockchain;
import neo.ledger.CoinState;
import neo.ledger.ContractPropertyState;
import neo.ledger.ContractState;
import neo.ledger.HashIndexState;
import neo.ledger.MemoryPool;
import neo.ledger.MyBlockchain;
import neo.ledger.SpentCoin;
import neo.ledger.SpentCoinState;
import neo.ledger.TransactionState;
import neo.ledger.TrimmedBlock;
import neo.ledger.UnspentCoinState;
import neo.ledger.ValidatorState;
import neo.ledger.ValidatorsCountState;
import neo.log.tr.TR;
import neo.network.p2p.payloads.Block;
import neo.network.p2p.payloads.CoinReference;
import neo.network.p2p.payloads.ContractTransaction;
import neo.network.p2p.payloads.Header;
import neo.network.p2p.payloads.MinerTransaction;
import neo.network.p2p.payloads.Transaction;
import neo.network.p2p.payloads.TransactionOutput;
import neo.network.p2p.payloads.Witness;
import neo.smartcontract.ContractParameterType;


public abstract class SnapshotTest {

    private Snapshot snapshot;
    private static MyBlockchain blockchain;
    private static TestKit testKit;
    private static MyNeoSystem neoSystem;

    protected abstract Snapshot init();

    public static class MyBlockchain2 extends MyBlockchain {
        public MyBlockchain2(NeoSystem system, Store store, ActorRef actorRef) {
            super(system, store, actorRef);
        }

        @Override
        protected void init(NeoSystem system, Store store) {
            this.system = system;
            this.store = store;
            this.memPool = new MemoryPool(system, MemoryPoolMaxTransactions);
            // 测试环境下，由于akka的创建，可以同时存在多个
            singleton = this;
        }

        public static Props props(NeoSystem system, Store store, ActorRef actorRef) {
            TR.enter();
            return TR.exit(Props.create(MyBlockchain2.class, system, store, actorRef).withMailbox("blockchain-mailbox"));
        }
    }


    public static void setUp() {
        neoSystem = new MyNeoSystem(null, self -> {
            testKit = new TestKit(self.actorSystem);

            // Synchronous Unit Testing with TestActorRef
            TestActorRef<MyBlockchain2> blockchainRef = TestActorRef.create(self.actorSystem, MyBlockchain2.props(self, null, testKit.testActor()));
            self.blockchain = blockchainRef;
            self.localNode = null;
            self.taskManager = null;
            self.consensus = null;
            blockchain = blockchainRef.underlyingActor();
        });
    }

    @Before
    public void before() throws IOException {
        snapshot = init();
    }


    @Test
    public void getPersistingBlock() {
        TrimmedBlock trimmedBlock = new TrimmedBlock();
        trimmedBlock.consensusData = new Ulong(10);
        trimmedBlock.version = new Uint(1);
        trimmedBlock.prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        trimmedBlock.merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        trimmedBlock.timestamp = new Uint(1568489959);
        trimmedBlock.index = new Uint(10);
        trimmedBlock.nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        trimmedBlock.witness = new Witness();
        trimmedBlock.witness.verificationScript = new byte[]{0x01, 0x02};
        trimmedBlock.witness.invocationScript = new byte[]{0x3, 0x04};
        MinerTransaction minerTransaction = new MinerTransaction();
        trimmedBlock.hashes = new UInt256[]{minerTransaction.hash()};

        DataCache<UInt256, TransactionState> cache = snapshot.getTransactions();
        TransactionState txState = new TransactionState();
        txState.transaction = minerTransaction;
        txState.blockIndex = new Uint(10);
        cache.add(minerTransaction.hash(), txState);

        Block timBlock = trimmedBlock.getBlock(cache);
        snapshot.setPersistingBlock(timBlock);
        Block persistBlock = snapshot.getPersistingBlock();

        Assert.assertEquals(1, persistBlock.transactions.length);
        Assert.assertEquals(minerTransaction, persistBlock.transactions[0]);
    }

    @Test
    public void setPersistingBlock() {
        TrimmedBlock trimmedBlock = new TrimmedBlock();
        trimmedBlock.consensusData = new Ulong(10);
        trimmedBlock.version = new Uint(1);
        trimmedBlock.prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        trimmedBlock.merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        trimmedBlock.timestamp = new Uint(1568489959);
        trimmedBlock.index = new Uint(10);
        trimmedBlock.nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        trimmedBlock.witness = new Witness();
        trimmedBlock.witness.verificationScript = new byte[]{0x01, 0x02};
        trimmedBlock.witness.invocationScript = new byte[]{0x3, 0x04};
        MinerTransaction minerTransaction = new MinerTransaction();
        trimmedBlock.hashes = new UInt256[]{minerTransaction.hash()};

        DataCache<UInt256, TransactionState> cache = snapshot.getTransactions();
        TransactionState txState = new TransactionState();
        txState.transaction = minerTransaction;
        txState.blockIndex = new Uint(10);
        cache.add(minerTransaction.hash(), txState);

        Block timBlock = trimmedBlock.getBlock(cache);
        snapshot.setPersistingBlock(timBlock);
        Block persistBlock = snapshot.getPersistingBlock();

        Assert.assertEquals(1, persistBlock.transactions.length);
        Assert.assertEquals(minerTransaction, persistBlock.transactions[0]);
    }

    @Test
    public void getHeight() {
        Uint height = snapshot.getHeight();
        Assert.assertEquals(Uint.MAX_VALUE_2, height);

        MetaDataCache<HashIndexState> cache = snapshot.getBlockHashIndex();
        HashIndexState state = cache.getAndChange();
        state.index = new Uint(10);
        snapshot.commit();

        height = snapshot.getHeight();
        Assert.assertEquals(new Uint(10), height);
    }

    @Test
    public void getHeaderHeight() {
        UInt256 height = snapshot.getHeaderHeight();
        Assert.assertEquals(UInt256.Zero, height);

        MetaDataCache<HashIndexState> cache = snapshot.getBlockHashIndex();
        HashIndexState state = cache.getAndChange();
        state.hash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        height = snapshot.getHeaderHeight();
        Assert.assertEquals(state.hash, height);
    }

    @Test
    public void getCurrentBlockHash() {
        MetaDataCache<HashIndexState> cache = snapshot.getBlockHashIndex();
        HashIndexState state = cache.getAndChange();
        state.hash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        snapshot.commit();

        UInt256 hash = snapshot.getCurrentBlockHash();
        Assert.assertEquals(state.hash, hash);
    }

    @Test
    public void getCurrentHeaderHash() {
        MetaDataCache<HashIndexState> cache = snapshot.getHeaderHashIndex();
        HashIndexState state = cache.getAndChange();
        state.hash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        snapshot.commit();

        UInt256 hash = snapshot.getCurrentHeaderHash();
        Assert.assertEquals(state.hash, hash);
    }

    @Test
    public void getUnclaimed() {
        // construct tx
        MinerTransaction minerTransaction = new MinerTransaction();
        DataCache<UInt256, TransactionState> tx = snapshot.getTransactions();
        minerTransaction.outputs = new TransactionOutput[]{
                new TransactionOutput() {{
                    scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                    assetId = Blockchain.GoverningToken.hash();
                    value = new Fixed8(10000);
                }},
                new TransactionOutput() {{
                    scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
                    assetId = Blockchain.GoverningToken.hash();
                    value = new Fixed8(20000);
                }},
                new TransactionOutput() {{
                    scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff03");
                    assetId = Blockchain.GoverningToken.hash();
                    value = new Fixed8(30000);
                }}
        };
        TransactionState txState = new TransactionState() {{
            blockIndex = new Uint(0);
            transaction = minerTransaction;
        }};
        tx.add(minerTransaction.hash(), txState);

        // add spent coin
        DataCache<UInt256, SpentCoinState> spent = snapshot.getSpentCoins();
        SpentCoinState spentState = new SpentCoinState() {{
            transactionHash = minerTransaction.hash();
            transactionHeight = new Uint(0);
            items = new HashMap<Ushort, Uint>() {{
                put(new Ushort(0), new Uint(10));
                put(new Ushort(2), new Uint(20));
            }};
        }};
        spent.add(spentState.transactionHash, spentState);
        snapshot.commit();

        HashMap<Ushort, SpentCoin> unclaimSet = snapshot.getUnclaimed(minerTransaction.hash());
        Assert.assertEquals(2, unclaimSet.size());
        SpentCoin sp1 = unclaimSet.get(new Ushort(0));
        Assert.assertEquals(new Uint(0), sp1.startHeight);
        Assert.assertEquals(new Uint(10), sp1.endHeight);
        Assert.assertEquals(new Fixed8(10000), sp1.output.value);

        SpentCoin sp2 = unclaimSet.get(new Ushort(2));
        Assert.assertEquals(new Uint(0), sp2.startHeight);
        Assert.assertEquals(new Uint(20), sp2.endHeight);
        Assert.assertEquals(new Fixed8(30000), sp2.output.value);

        tx.delete(minerTransaction.hash());
        spent.delete(minerTransaction.hash());
        snapshot.commit();
    }

    @Test
    public void calculateBonus() {
        for (int i = 0; i < 20; i++) {
            switch (i) {
                case 0:
                    blockchain.myheaderIndex.add(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01"));
                    break;
                case 9:
                    blockchain.myheaderIndex.add(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02"));
                    break;
                case 19:
                    blockchain.myheaderIndex.add(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff03"));
                    break;
                default:
                    blockchain.myheaderIndex.add(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00"));
            }
        }
        blockchain.getBlockHash(new Uint(9));

        // construct tx
        MinerTransaction minerTransaction = new MinerTransaction();
        DataCache<UInt256, TransactionState> tx = snapshot.getTransactions();
        minerTransaction.outputs = new TransactionOutput[]{
                new TransactionOutput() {{
                    scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                    assetId = Blockchain.GoverningToken.hash();
                    value = Fixed8.fromDecimal(BigDecimal.valueOf(10000));
                }},
                new TransactionOutput() {{
                    scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
                    assetId = Blockchain.GoverningToken.hash();
                    value = Fixed8.fromDecimal(BigDecimal.valueOf(20000));
                }},
                new TransactionOutput() {{
                    scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff03");
                    assetId = Blockchain.GoverningToken.hash();
                    value = Fixed8.fromDecimal(BigDecimal.valueOf(30000));
                }}
        };
        TransactionState txState = new TransactionState() {{
            blockIndex = new Uint(0);
            transaction = minerTransaction;
        }};
        tx.add(minerTransaction.hash(), txState);

        // add spent coin
        DataCache<UInt256, SpentCoinState> spent = snapshot.getSpentCoins();
        SpentCoinState spentState = new SpentCoinState() {{
            transactionHash = minerTransaction.hash();
            transactionHeight = new Uint(0);
            items = new HashMap<Ushort, Uint>() {{
                put(new Ushort(0), new Uint(10)); // 10000
                put(new Ushort(2), new Uint(20)); // 30000
            }};
        }};
        spent.add(spentState.transactionHash, spentState);
        snapshot.commit();

        ArrayList<CoinReference> unclaimReference = new ArrayList<>(2);
        unclaimReference.add(new CoinReference() {{
            prevHash = minerTransaction.hash();
            prevIndex = new Ushort(0);
        }});
        unclaimReference.add(new CoinReference() {{
            prevHash = minerTransaction.hash();
            prevIndex = new Ushort(1);
        }});
        unclaimReference.add(new CoinReference() {{
            prevHash = minerTransaction.hash();
            prevIndex = new Ushort(2);
        }});

        Fixed8 bonus = snapshot.calculateBonus(unclaimReference);
        Assert.assertEquals(new Fixed8(5600000), bonus);

        // set block and fee
        // set block and systemfee
        BlockState blockState = new BlockState();
        blockState.trimmedBlock = new TrimmedBlock();
        blockState.trimmedBlock.consensusData = new Ulong(10);
        blockState.trimmedBlock.version = new Uint(1);
        blockState.trimmedBlock.prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        blockState.trimmedBlock.timestamp = new Uint(1568489959);
        blockState.trimmedBlock.index = new Uint(Blockchain.DecrementInterval + 10);
        blockState.trimmedBlock.nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.witness = new Witness();
        blockState.trimmedBlock.witness.verificationScript = new byte[]{0x01, 0x02};
        blockState.trimmedBlock.witness.invocationScript = new byte[]{0x3, 0x04};
        blockState.trimmedBlock.hashes = new UInt256[]{new MinerTransaction().hash()};
        blockState.systemFeeAmount = 10000;
        DataCache<UInt256, BlockState> blocks = snapshot.getBlocks();
        UInt256 BlockHsah = blockState.trimmedBlock.hash();
        blocks.add(blockState.trimmedBlock.hash(), blockState);

        // cross decrementInterval and systemfee
        spent.delete(minerTransaction.hash());
        snapshot.commit();
        blockchain.myheaderIndex.clear();
        for (int i = 0; i < Blockchain.DecrementInterval * 1.5; i++) {
            switch (i) {
                case 0:
                    blockchain.myheaderIndex.add(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01"));
                    break;
                case 9:
                    blockchain.myheaderIndex.add(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02"));
                    break;
                case Blockchain.DecrementInterval + 9:
                    blockchain.myheaderIndex.add(BlockHsah);
                    break;
                default:
                    blockchain.myheaderIndex.add(UInt256.Zero);
            }
        }
        // set spent coin
        spentState = new SpentCoinState() {{
            transactionHash = minerTransaction.hash();
            transactionHeight = new Uint(0);
            items = new HashMap<Ushort, Uint>() {{
                put(new Ushort(0), new Uint(10)); // 10000
                put(new Ushort(2), new Uint(Blockchain.DecrementInterval + 10)); // 30000
            }};
        }};
        spent.add(spentState.transactionHash, spentState);
        snapshot.commit();

        bonus = snapshot.calculateBonus(unclaimReference);

        // the first unclaimed tx's gas = 10 * 8 / 10000 = 0.008
        // the second unclaimed tx's gas = (2000000*8 + 10 * 7 + 10000)/10000*3 = 4 803.021
        // total = 0.008 + 4803.021 = 4803.029
        Assert.assertEquals(new Fixed8(480302900000l), bonus);

        // test calculateBonus(Collection<CoinReference> inputs, Uint height_end)
        unclaimReference.clear();
        unclaimReference.add(new CoinReference() {{
            prevHash = minerTransaction.hash();
            prevIndex = new Ushort(2);
        }});
        // unclaimed tx's gas = (2000000*8 + 10 * 7 + 10000)/10000*3 = 4 803.021
        bonus = snapshot.calculateBonus(unclaimReference, new Uint(Blockchain.DecrementInterval + 10));
        Assert.assertEquals(new Fixed8(480302100000l), bonus);

        // clear data
        blockchain.myheaderIndex.clear();
        tx.delete(minerTransaction.hash());
        spent.delete(minerTransaction.hash());
        blocks.delete(blockState.trimmedBlock.hash());
        snapshot.commit();
    }


    @Test
    public void getValidatorPubkeys() {
        // prepare validator, validator_count data
        MetaDataCache<ValidatorsCountState> validatorCount = snapshot.getValidatorsCount();
        ValidatorsCountState countState = validatorCount.getAndChange();
        countState.votes[1] = new Fixed8(100);
        countState.votes[2] = new Fixed8(200);
        countState.votes[3] = new Fixed8(300);
        countState.votes[4] = new Fixed8(400);
        countState.votes[5] = new Fixed8(500);

        // who to construct ECPoint
        DataCache<ECPoint, ValidatorState> validators = snapshot.getValidators();
        ValidatorState validatorState = new ValidatorState();
        validatorState.votes = new Fixed8(1000);
        validatorState.registered = true;
        validatorState.publicKey = ECC.parseFromHexString("0327da12b5c40200e9f65569476bbff2218da4f32548ff43b6387ec1416a231ee8");
        validators.add(validatorState.publicKey, validatorState);

        validatorState = new ValidatorState();
        validatorState.votes = new Fixed8(2000);
        validatorState.registered = true;
        validatorState.publicKey = ECC.parseFromHexString("026ce35b29147ad09e4afe4ec4a7319095f08198fa8babbe3c56e970b143528d22");
        validators.add(validatorState.publicKey, validatorState);

        validatorState = new ValidatorState();
        validatorState.votes = new Fixed8(3000);
        validatorState.registered = true;
        validatorState.publicKey = ECC.parseFromHexString("0209e7fd41dfb5c2f8dc72eb30358ac100ea8c72da18847befe06eade68cebfcb9");
        validators.add(validatorState.publicKey, validatorState);
        snapshot.commit();

        // test without tx
        // ascending order
        ECPoint[] validatorPubkeys = snapshot.getValidatorPubkeys();
        for (int i = 0; i < validatorPubkeys.length - 1; i++) {
            Assert.assertTrue(validatorPubkeys[i].getXCoord().toBigInteger().compareTo(validatorPubkeys[i + 1].getXCoord().toBigInteger()) <= 0);
        }
        Assert.assertEquals(Blockchain.StandbyValidators.length, validatorPubkeys.length);
        // test with voting tx
        validatorState = new ValidatorState();
        validatorState.votes = new Fixed8(4000);
        validatorState.registered = true;
        validatorState.publicKey = ECC.parseFromHexString("038dddc06ce687677a53d54f096d2591ba2302068cf123c1f2d75c2dddc5425579");
        validators.add(validatorState.publicKey, validatorState);

        validatorState = new ValidatorState();
        validatorState.votes = new Fixed8(5000);
        validatorState.registered = true;
        validatorState.publicKey = ECC.parseFromHexString("02d02b1873a0863cd042cc717da31cea0d7cf9db32b74d4c72c01b0011503e2e22");
        validators.add(validatorState.publicKey, validatorState);

        validatorState = new ValidatorState();
        validatorState.votes = new Fixed8(6000);
        validatorState.registered = true;
        validatorState.publicKey = ECC.parseFromHexString("034ff5ceeac41acf22cd5ed2da17a6df4dd8358fcb2bfb1a43208ad0feaab2746b");
        validators.add(validatorState.publicKey, validatorState);

        validatorState = new ValidatorState();
        validatorState.votes = new Fixed8(7000);
        validatorState.registered = true;
        validatorState.publicKey = ECC.parseFromHexString("02fe6044efdff25a7fbe4b105339b1253068cbcbb19c5f3da0cb2d0a068d27b007");
        validators.add(validatorState.publicKey, validatorState);

        validatorState = new ValidatorState();
        validatorState.votes = new Fixed8(8000);
        validatorState.registered = true;
        validatorState.publicKey = ECC.parseFromHexString("03ca37f059a8f3975e29b9d0cb4694de6c3d555add2242191c39c88994013eeff5");
        validators.add(validatorState.publicKey, validatorState);

        countState = validatorCount.getAndChange();
        for (int i = 0; i < 8; i++) {
            countState.votes[i] = new Fixed8(1000);
        }

        // 构建交易
        MinerTransaction minerTransaction = new MinerTransaction();
        minerTransaction.nonce = new Uint(100);
        minerTransaction.outputs = new TransactionOutput[]{
                new TransactionOutput() {{
                    assetId = Blockchain.GoverningToken.hash();
                    value = new Fixed8(10000000000000l);
                    scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
                }}
        };

        UInt160 pubkey = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        AccountState account = snapshot.getAccounts().getAndChange(pubkey, AccountState::new);
        account.votes = new ECPoint[]{ECC.parseFromHexString("0327da12b5c40200e9f65569476bbff2218da4f32548ff43b6387ec1416a231ee8"),
                ECC.parseFromHexString("026ce35b29147ad09e4afe4ec4a7319095f08198fa8babbe3c56e970b143528d22")};
        account.isFrozen = false;
        account.scriptHash = pubkey;
        snapshot.commit();

        Collection<ECPoint> validatorPoints = snapshot.getValidators(Collections.singletonList(minerTransaction));

        Assert.assertEquals(7, validatorPoints.size());
        boolean hasFirstPoint = false;
        boolean hasSecondPoint = false;
        for (ECPoint point : validatorPoints) {
            if (point.equals(ECC.parseFromHexString("0327da12b5c40200e9f65569476bbff2218da4f32548ff43b6387ec1416a231ee8"))) {
                hasFirstPoint = true;
            }
            if (point.equals(ECC.parseFromHexString("026ce35b29147ad09e4afe4ec4a7319095f08198fa8babbe3c56e970b143528d22"))) {
                hasSecondPoint = true;
            }
        }
        Assert.assertTrue(hasFirstPoint);
        Assert.assertTrue(hasSecondPoint);

        // clear data
        validators.delete(ECC.parseFromHexString("0327da12b5c40200e9f65569476bbff2218da4f32548ff43b6387ec1416a231ee8"));
        validators.delete(ECC.parseFromHexString("026ce35b29147ad09e4afe4ec4a7319095f08198fa8babbe3c56e970b143528d22"));
        validators.delete(ECC.parseFromHexString("02fe6044efdff25a7fbe4b105339b1253068cbcbb19c5f3da0cb2d0a068d27b007"));
        validators.delete(ECC.parseFromHexString("034ff5ceeac41acf22cd5ed2da17a6df4dd8358fcb2bfb1a43208ad0feaab2746b"));
        validators.delete(ECC.parseFromHexString("02d02b1873a0863cd042cc717da31cea0d7cf9db32b74d4c72c01b0011503e2e22"));
        validators.delete(ECC.parseFromHexString("038dddc06ce687677a53d54f096d2591ba2302068cf123c1f2d75c2dddc5425579"));
        validators.delete(ECC.parseFromHexString("0209e7fd41dfb5c2f8dc72eb30358ac100ea8c72da18847befe06eade68cebfcb9"));
        snapshot.commit();
    }

    @Test
    public void containsBlock() {
        // prepare block data
        DataCache<UInt256, BlockState> blocks = snapshot.getBlocks();
        BlockState blockState = new BlockState();
        blockState.trimmedBlock = new TrimmedBlock();
        blockState.trimmedBlock.consensusData = new Ulong(10);
        blockState.trimmedBlock.version = new Uint(1);
        blockState.trimmedBlock.prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        blockState.trimmedBlock.timestamp = new Uint(1568489959);
        blockState.trimmedBlock.index = new Uint(Blockchain.DecrementInterval + 10);
        blockState.trimmedBlock.nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.witness = new Witness();
        blockState.trimmedBlock.witness.verificationScript = new byte[]{0x01, 0x02};
        blockState.trimmedBlock.witness.invocationScript = new byte[]{0x3, 0x04};
        blockState.trimmedBlock.hashes = new UInt256[]{new MinerTransaction().hash()};
        blockState.systemFeeAmount = 10000;
        blocks.add(blockState.trimmedBlock.hash(), blockState);
        snapshot.commit();

        // check
        boolean result = snapshot.containsBlock(blockState.trimmedBlock.hash());
        Assert.assertTrue(result);
        result = snapshot.containsBlock(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01"));
        Assert.assertFalse(result);

        // clear block data
        blocks.delete(blockState.trimmedBlock.hash());
        snapshot.commit();
    }


    @Test
    public void containsTransaction() {
        // prepare data
        DataCache<UInt256, TransactionState> txs = snapshot.getTransactions();
        TransactionState txState = new TransactionState();
        txState.blockIndex = new Uint(10);
        txState.transaction = new MinerTransaction() {
            {
                nonce = new Uint(100);
                outputs = new TransactionOutput[]{
                        new TransactionOutput() {{
                            assetId = Blockchain.GoverningToken.hash();
                            value = new Fixed8(10000000000000l);
                            scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
                        }}
                };
            }
        };
        txs.add(txState.transaction.hash(), txState);
        snapshot.commit();

        // check
        boolean result = snapshot.containsTransaction(txState.transaction.hash());
        Assert.assertTrue(result);
        result = snapshot.containsBlock(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01"));
        Assert.assertFalse(result);

        // clear data
        txs.delete(txState.transaction.hash());
        snapshot.commit();
    }


    @Test
    public void getBlock() {
        // prepare block data
        DataCache<UInt256, TransactionState> txs = snapshot.getTransactions();

        MinerTransaction minerTransaction = new MinerTransaction();
        TransactionState txState = new TransactionState() {{
            transaction = minerTransaction;
            blockIndex = new Uint(10);
        }};
        txs.add(minerTransaction.hash(), txState);

        DataCache<UInt256, BlockState> blocks = snapshot.getBlocks();
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
        blockState.trimmedBlock.hashes = new UInt256[]{minerTransaction.hash()};
        blockState.systemFeeAmount = 10000;
        blocks.add(blockState.trimmedBlock.hash(), blockState);
        snapshot.commit();
        for (int i = 0; i < 11; i++) {
            switch (i) {
                case 10:
                    blockchain.myheaderIndex.add(blockState.trimmedBlock.hash());
                    break;
                default:
                    blockchain.myheaderIndex.add(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01"));
                    break;
            }
        }

        // check
        Block result = snapshot.getBlock(blockState.trimmedBlock.hash());
        Assert.assertNotNull(result);
        result = snapshot.getBlock(blockState.trimmedBlock.index);
        Assert.assertNotNull(result);

        result = snapshot.getBlock(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01"));
        Assert.assertNull(result);
        result = snapshot.getBlock(new Uint(100));
        Assert.assertNull(result);

        // clear block data
        txs.delete(minerTransaction.hash());
        blockchain.myheaderIndex.clear();
        blocks.delete(blockState.trimmedBlock.hash());
        snapshot.commit();
    }


    @Test
    public void getEnrollments() {
        // prepare validator, validator_count data
        DataCache<ECPoint, ValidatorState> validators = snapshot.getValidators();
        ValidatorState validatorState = new ValidatorState();
        validatorState.votes = new Fixed8(1000);
        validatorState.registered = true;
        validatorState.publicKey = ECC.parseFromHexString("0327da12b5c40200e9f65569476bbff2218da4f32548ff43b6387ec1416a231ee8");
        validators.add(validatorState.publicKey, validatorState);

        validatorState = new ValidatorState();
        validatorState.votes = new Fixed8(2000);
        validatorState.registered = true;
        validatorState.publicKey = ECC.parseFromHexString("026ce35b29147ad09e4afe4ec4a7319095f08198fa8babbe3c56e970b143528d22");
        validators.add(validatorState.publicKey, validatorState);

        validatorState = new ValidatorState();
        validatorState.votes = new Fixed8(3000);
        validatorState.registered = false;
        validatorState.publicKey = ECC.parseFromHexString("0209e7fd41dfb5c2f8dc72eb30358ac100ea8c72da18847befe06eade68cebfcb9");
        validators.add(validatorState.publicKey, validatorState);
        snapshot.commit();

        // test without tx
        // ascending order
        Collection<ValidatorState> validatorStates = snapshot.getEnrollments();
        Assert.assertEquals(2, validatorStates.size());
        boolean hasFirst = false;
        boolean hasSecond = false;
        boolean dontHasTheLastPoint = true;
        for (ValidatorState state : validatorStates) {
            Assert.assertTrue(state.registered);
            if (state.publicKey.equals(ECC.parseFromHexString("0327da12b5c40200e9f65569476bbff2218da4f32548ff43b6387ec1416a231ee8"))) {
                hasFirst = true;
            }
            if (state.publicKey.equals(ECC.parseFromHexString("026ce35b29147ad09e4afe4ec4a7319095f08198fa8babbe3c56e970b143528d22"))) {
                hasSecond = true;
            }
            if (state.publicKey.equals(ECC.parseFromHexString("0209e7fd41dfb5c2f8dc72eb30358ac100ea8c72da18847befe06eade68cebfcb9"))) {
                dontHasTheLastPoint = false;
            }
        }
        Assert.assertTrue(hasFirst);
        Assert.assertTrue(hasSecond);
        Assert.assertTrue(dontHasTheLastPoint);

        // clear data
        validators.delete(ECC.parseFromHexString("0327da12b5c40200e9f65569476bbff2218da4f32548ff43b6387ec1416a231ee8"));
        validators.delete(ECC.parseFromHexString("026ce35b29147ad09e4afe4ec4a7319095f08198fa8babbe3c56e970b143528d22"));
        validators.delete(ECC.parseFromHexString("0209e7fd41dfb5c2f8dc72eb30358ac100ea8c72da18847befe06eade68cebfcb9"));
        snapshot.commit();
    }

    @Test
    public void getHeader() {
        // prepare data
        DataCache<UInt256, BlockState> blocks = snapshot.getBlocks();
        BlockState blockState = new BlockState();
        blockState.trimmedBlock = new TrimmedBlock();
        blockState.trimmedBlock.consensusData = new Ulong(10);
        blockState.trimmedBlock.version = new Uint(1);
        blockState.trimmedBlock.prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        blockState.trimmedBlock.timestamp = new Uint(1568489959);
        blockState.trimmedBlock.index = new Uint(9);
        blockState.trimmedBlock.nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.witness = new Witness();
        blockState.trimmedBlock.hashes = new UInt256[0];
        blockState.trimmedBlock.witness.verificationScript = new byte[]{0x01, 0x02};
        blockState.trimmedBlock.witness.invocationScript = new byte[]{0x3, 0x04};
        blockState.trimmedBlock.hashes = new UInt256[]{new MinerTransaction().hash()};
        blockState.systemFeeAmount = 10000;

        for (int i = 0; i < 20; i++) {
            switch (i) {
                case 10:
                    blockchain.myheaderIndex.add(blockState.trimmedBlock.hash());
                    break;
                default:
                    blockchain.myheaderIndex.add(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00"));
            }
        }

        blocks.add(blockState.trimmedBlock.hash(), blockState);
        snapshot.commit();

        Header header = snapshot.getHeader(new Uint(10));
        Assert.assertNotNull(header);
        Assert.assertEquals(blockState.trimmedBlock.hash(), header.hash());
        header = snapshot.getHeader(blockState.trimmedBlock.hash());
        Assert.assertNotNull(header);
        Assert.assertEquals(blockState.trimmedBlock.hash(), header.hash());

        // clear data
        blockchain.myheaderIndex.clear();
        blocks.delete(blockState.trimmedBlock.hash());
        snapshot.commit();
    }

    @Test
    public void getNextBlockHash() {
        // prepare data
        DataCache<UInt256, BlockState> blocks = snapshot.getBlocks();
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
        blockState.trimmedBlock.hashes = new UInt256[0];
        blockState.trimmedBlock.witness.verificationScript = new byte[]{0x01, 0x02};
        blockState.trimmedBlock.witness.invocationScript = new byte[]{0x3, 0x04};
        blockState.trimmedBlock.hashes = new UInt256[]{new MinerTransaction().hash()};
        blockState.systemFeeAmount = 10000;

        for (int i = 0; i < 20; i++) {
            switch (i) {
                case 10:
                    blockchain.myheaderIndex.add(blockState.trimmedBlock.hash());
                    break;
                case 11:
                    blockchain.myheaderIndex.add(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01"));
                    break;
                default:
                    blockchain.myheaderIndex.add(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00"));
                    break;
            }
        }
        blocks.add(blockState.trimmedBlock.hash(), blockState);
        snapshot.commit();

        // check
        UInt256 hash = snapshot.getNextBlockHash(blockState.trimmedBlock.hash());
        Assert.assertEquals(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01"), hash);

        // clear data
        blockchain.myheaderIndex.clear();
        blocks.delete(blockState.trimmedBlock.hash());
        snapshot.commit();
    }

    @Test
    public void getSysFeeAmount() {
        // prepare data
        DataCache<UInt256, BlockState> blocks = snapshot.getBlocks();
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
        blockState.trimmedBlock.hashes = new UInt256[0];
        blockState.trimmedBlock.witness.verificationScript = new byte[]{0x01, 0x02};
        blockState.trimmedBlock.witness.invocationScript = new byte[]{0x3, 0x04};
        blockState.trimmedBlock.hashes = new UInt256[]{new MinerTransaction().hash()};
        blockState.systemFeeAmount = 10000;

        for (int i = 0; i < 20; i++) {
            switch (i) {
                case 10:
                    blockchain.myheaderIndex.add(blockState.trimmedBlock.hash());
                    break;
                default:
                    blockchain.myheaderIndex.add(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00"));
                    break;
            }
        }
        blocks.add(blockState.trimmedBlock.hash(), blockState);
        snapshot.commit();


        // check

        long systemfee = snapshot.getSysFeeAmount(blockState.trimmedBlock.hash());
        Assert.assertEquals(10000, systemfee);

        systemfee = snapshot.getSysFeeAmount(new Uint(10));
        Assert.assertEquals(10000, systemfee);


        // clear data
        blockchain.myheaderIndex.clear();
        blocks.delete(blockState.trimmedBlock.hash());
        snapshot.commit();
    }

    @Test
    public void getTransaction() {
        // prepare data
        DataCache<UInt256, TransactionState> txs = snapshot.getTransactions();
        MinerTransaction minerTransaction = new MinerTransaction();
        minerTransaction.outputs = new TransactionOutput[]{
                new TransactionOutput() {{
                    scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                    assetId = Blockchain.GoverningToken.hash();
                    value = new Fixed8(10000);
                }},
                new TransactionOutput() {{
                    scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
                    assetId = Blockchain.GoverningToken.hash();
                    value = new Fixed8(20000);
                }},
                new TransactionOutput() {{
                    scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff03");
                    assetId = Blockchain.GoverningToken.hash();
                    value = new Fixed8(30000);
                }}
        };
        TransactionState txState = new TransactionState() {{
            blockIndex = new Uint(0);
            transaction = minerTransaction;
        }};
        txs.add(minerTransaction.hash(), txState);
        snapshot.commit();

        // check
        Transaction tx = snapshot.getTransaction(minerTransaction.hash());
        Assert.assertTrue(tx instanceof MinerTransaction);
        MinerTransaction minertx = (MinerTransaction) tx;
        Assert.assertEquals(3, minertx.outputs.length);

        //clear data
        txs.delete(minerTransaction.hash());
        snapshot.commit();
    }

    @Test
    public void getUnspent() {
        // prepare data
        DataCache<UInt256, TransactionState> txs = snapshot.getTransactions();
        MinerTransaction minerTransaction = new MinerTransaction() {{
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                        assetId = Blockchain.GoverningToken.hash();
                        value = new Fixed8(10000);
                    }},
                    new TransactionOutput() {{
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
                        assetId = Blockchain.GoverningToken.hash();
                        value = new Fixed8(20000);
                    }},
                    new TransactionOutput() {{
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff03");
                        assetId = Blockchain.GoverningToken.hash();
                        value = new Fixed8(30000);
                    }}
            };
        }};
        TransactionState txState = new TransactionState() {{
            blockIndex = new Uint(10);
            transaction = minerTransaction;
        }};

        txs.add(minerTransaction.hash(), txState);

        DataCache<UInt256, UnspentCoinState> utxos = snapshot.getUnspentCoins();
        UnspentCoinState utxo = new UnspentCoinState() {{
            items = new CoinState[]{CoinState.Confirmed, CoinState.Spent, CoinState.Unconfirmed};
        }};
        utxos.add(minerTransaction.hash(), utxo);
        snapshot.commit();

        // check
        Collection<TransactionOutput> outputs = snapshot.getUnspent(minerTransaction.hash());
        Assert.assertEquals(2, outputs.size());
        int i = 0;
        for (TransactionOutput output : outputs) {
            if (i == 0) {
                Assert.assertEquals(new Fixed8(10000), output.value);
            }
            if (i == 1) {
                Assert.assertEquals(new Fixed8(30000), output.value);
            }
            i++;
        }

        TransactionOutput output = snapshot.getUnspent(minerTransaction.hash(), new Ushort(0));
        Assert.assertEquals(new Fixed8(10000), output.value);
        output = snapshot.getUnspent(minerTransaction.hash(), new Ushort(1));
        Assert.assertNull(output);
        output = snapshot.getUnspent(minerTransaction.hash(), new Ushort(2));
        Assert.assertEquals(new Fixed8(30000), output.value);

        // clear data
        txs.delete(minerTransaction.hash());
        utxos.delete(minerTransaction.hash());
        snapshot.commit();
    }

    @Test
    public void isDoubleSpend() {
        // prepare data
        DataCache<UInt256, TransactionState> txs = snapshot.getTransactions();
        MinerTransaction minerTransaction = new MinerTransaction() {{
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                        assetId = Blockchain.GoverningToken.hash();
                        value = new Fixed8(10000);
                    }},
                    new TransactionOutput() {{
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
                        assetId = Blockchain.GoverningToken.hash();
                        value = new Fixed8(20000);
                    }},
                    new TransactionOutput() {{
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff03");
                        assetId = Blockchain.GoverningToken.hash();
                        value = new Fixed8(30000);
                    }}
            };
        }};
        TransactionState txState = new TransactionState() {{
            blockIndex = new Uint(10);
            transaction = minerTransaction;
        }};

        txs.add(minerTransaction.hash(), txState);

        DataCache<UInt256, UnspentCoinState> utxos = snapshot.getUnspentCoins();
        UnspentCoinState utxo = new UnspentCoinState() {{
            items = new CoinState[]{CoinState.Confirmed, CoinState.Spent, CoinState.Unconfirmed};
        }};
        utxos.add(minerTransaction.hash(), utxo);
        snapshot.commit();

        ContractTransaction tx = new ContractTransaction() {{
            inputs = new CoinReference[]{
                    new CoinReference() {{
                        prevHash = minerTransaction.hash();
                        prevIndex = new Ushort(0);
                    }},
                    new CoinReference() {{
                        prevHash = minerTransaction.hash();
                        prevIndex = new Ushort(2);
                    }}
            };
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                        assetId = Blockchain.GoverningToken.hash();
                        value = new Fixed8(40000);
                    }}
            };
        }};


        // check
        boolean result = snapshot.isDoubleSpend(tx);
        Assert.assertFalse(result);

        tx = new ContractTransaction() {{
            inputs = new CoinReference[]{
                    new CoinReference() {{
                        prevHash = minerTransaction.hash();
                        prevIndex = new Ushort(0);
                    }},
                    new CoinReference() {{
                        prevHash = minerTransaction.hash();
                        prevIndex = new Ushort(1);
                    }}
            };
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                        assetId = Blockchain.GoverningToken.hash();
                        value = new Fixed8(30000);
                    }}
            };
        }};
        result = snapshot.isDoubleSpend(tx);
        Assert.assertTrue(result);


        // clear data
        txs.delete(minerTransaction.hash());
        utxos.delete(minerTransaction.hash());
        snapshot.commit();
    }

    @Test
    public void getScript() {
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
        snapshot.getContracts().add(key, state);
        snapshot.commit();

        byte[] sourceCode = snapshot.getScript(key.toArray());
        Assert.assertArrayEquals(state.script, sourceCode);

        snapshot.getContracts().delete(key);
        snapshot.commit();
    }


    @Test
    public void dispose() {
        snapshot.dispose();
        Assert.assertTrue(true);
    }

}