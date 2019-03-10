package neo.persistence.leveldb;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import neo.Fixed8;
import neo.UInt160;
import neo.UInt256;
import neo.UInt32;
import neo.cryptography.ecc.ECC;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.csharp.Ushort;
import neo.io.SerializeHelper;
import neo.io.caching.DataCache;
import neo.io.caching.MetaDataCache;
import neo.io.wrappers.UInt32Wrapper;
import neo.ledger.AccountState;
import neo.ledger.AssetState;
import neo.ledger.BlockState;
import neo.ledger.Blockchain;
import neo.ledger.CoinState;
import neo.ledger.ContractPropertyState;
import neo.ledger.ContractState;
import neo.ledger.HashIndexState;
import neo.ledger.HeaderHashList;
import neo.ledger.SpentCoinState;
import neo.ledger.StorageItem;
import neo.ledger.StorageKey;
import neo.ledger.TransactionState;
import neo.ledger.TrimmedBlock;
import neo.ledger.UnspentCoinState;
import neo.ledger.ValidatorState;
import neo.ledger.ValidatorsCountState;
import neo.network.p2p.payloads.AssetType;
import neo.network.p2p.payloads.MinerTransaction;
import neo.network.p2p.payloads.Witness;
import neo.persistence.Snapshot;
import neo.cryptography.ecc.ECPoint;
import neo.smartcontract.ContractParameterType;


public class LevelDBStoreTest {

    private final static String LEVELDB_TEST_PATH = "Chain_test";

    private LevelDBStore store;

    @Before
    public void before() throws IOException {
        store = new LevelDBStore(LEVELDB_TEST_PATH);
    }

    @After
    public void after() throws IOException {
        store.close();
        // free leveldb file
        File file = new File(LEVELDB_TEST_PATH);
        if (file.exists()) {
            for (File subFile : file.listFiles()) {
                subFile.delete();
            }
            file.delete();
        }
    }


    @Test
    public void getSnapshot() {
        Snapshot snapshot = store.getSnapshot();
        Assert.assertNotNull(snapshot);
    }


    @Test
    public void getBlocks() throws IOException {
        Snapshot snapshot = store.getSnapshot();
        DataCache<UInt256, BlockState> cache = snapshot.getBlocks();

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

        UInt256 key = blockState.trimmedBlock.hash();
        cache.add(key, blockState);
        snapshot.commit();
        BlockState dbblock = cache.get(key);
        Assert.assertEquals(blockState.systemFeeAmount, dbblock.systemFeeAmount);
        Assert.assertEquals(blockState.trimmedBlock.consensusData, dbblock.trimmedBlock.consensusData);
        Assert.assertEquals(blockState.trimmedBlock.version, dbblock.trimmedBlock.version);
        Assert.assertEquals(blockState.trimmedBlock.nextConsensus, dbblock.trimmedBlock.nextConsensus);
        cache.getAndChange(key);
        snapshot.commit();
        snapshot.close();

        // read from db
        snapshot = store.getSnapshot();
        cache = snapshot.getBlocks();
        Collection<Map.Entry<UInt256, BlockState>> list = cache.find(key.toArray());
        Assert.assertEquals(1, list.size());
        dbblock = list.iterator().next().getValue();
        Assert.assertEquals(blockState.systemFeeAmount, dbblock.systemFeeAmount);
        Assert.assertEquals(blockState.trimmedBlock.consensusData, dbblock.trimmedBlock.consensusData);
        Assert.assertEquals(blockState.trimmedBlock.version, dbblock.trimmedBlock.version);
        Assert.assertEquals(blockState.trimmedBlock.nextConsensus, dbblock.trimmedBlock.nextConsensus);
        cache.delete(key); // delete
        snapshot.commit();
        snapshot.close();

        cache = store.getBlocks();
        dbblock = cache.tryGet(key);     // read null from
        Assert.assertNull(dbblock);

        list = cache.find();// read empty array from db
        Assert.assertEquals(0, list.size());

        list = cache.find(SerializeHelper.toBytes(key));
        Assert.assertEquals(0, list.size());
    }

    @Test
    public void getTransactions() throws IOException {
        Snapshot snapshot = store.getSnapshot();
        DataCache<UInt256, TransactionState> cache = snapshot.getTransactions();

        TransactionState state = new TransactionState();
        state.blockIndex = new Uint(10);
        state.transaction = new MinerTransaction();

        UInt256 key = state.transaction.hash();
        cache.add(key, state);
        snapshot.commit();
        TransactionState dbtx = cache.get(key);
        Assert.assertEquals(state.transaction, dbtx.transaction);
        Assert.assertEquals(state.blockIndex, dbtx.blockIndex);
        cache.getAndChange(key);
        snapshot.commit();
        snapshot.close();

        // read from db
        snapshot = store.getSnapshot();
        cache = snapshot.getTransactions();
        Collection<Map.Entry<UInt256, TransactionState>> list = cache.find(key.toArray());
        Assert.assertEquals(1, list.size());
        dbtx = list.iterator().next().getValue();
        Assert.assertEquals(state.transaction, dbtx.transaction);
        Assert.assertEquals(state.blockIndex, dbtx.blockIndex);
        cache.delete(key); // delete
        snapshot.commit();
        snapshot.close();

        cache = store.getTransactions();
        dbtx = cache.tryGet(key); // read null from
        Assert.assertNull(dbtx);

        list = cache.find();// read empty array from db
        Assert.assertEquals(0, list.size());

        list = cache.find(SerializeHelper.toBytes(key));
        Assert.assertEquals(0, list.size());
    }

    @Test
    public void getAccounts() throws IOException {
        Snapshot snapshot = store.getSnapshot();
        DataCache<UInt160, AccountState> cache = snapshot.getAccounts();

        AccountState state = new AccountState();
        state.scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        state.isFrozen = false;
        state.votes = new ECPoint[]{new ECPoint(ECC.getInfinityPoint())};
        state.balances = new ConcurrentHashMap<>();
        state.balances.put(Blockchain.GoverningToken.hash(), new Fixed8(100));
        state.balances.put(Blockchain.UtilityToken.hash(), new Fixed8(200));

        UInt160 key = state.scriptHash;
        cache.add(key, state);
        snapshot.commit();
        AccountState dbtx = cache.get(key);
        Assert.assertEquals(state.isFrozen, dbtx.isFrozen);
        Assert.assertEquals(state.votes, dbtx.votes);
        Assert.assertEquals(state.balances.get(Blockchain.GoverningToken.hash()), dbtx.balances.get(Blockchain.GoverningToken.hash()));
        Assert.assertEquals(state.balances.get(Blockchain.UtilityToken.hash()), dbtx.balances.get(Blockchain.UtilityToken.hash()));
        cache.getAndChange(key);
        snapshot.commit();
        snapshot.close();

        // read from db
        snapshot = store.getSnapshot();
        cache = snapshot.getAccounts();
        Collection<Map.Entry<UInt160, AccountState>> list = cache.find(key.toArray());
        Assert.assertEquals(1, list.size());
        dbtx = list.iterator().next().getValue();
        Assert.assertEquals(state.isFrozen, dbtx.isFrozen);
        Assert.assertEquals(state.votes, dbtx.votes);
        Assert.assertEquals(state.balances.get(Blockchain.GoverningToken.hash()), dbtx.balances.get(Blockchain.GoverningToken.hash()));
        Assert.assertEquals(state.balances.get(Blockchain.UtilityToken.hash()), dbtx.balances.get(Blockchain.UtilityToken.hash()));
        cache.delete(key); // delete
        snapshot.commit();
        snapshot.close();

        cache = store.getAccounts();
        dbtx = cache.tryGet(key); // read null from
        Assert.assertNull(dbtx);

        list = cache.find();// read empty array from db
        Assert.assertEquals(0, list.size());

        list = cache.find(SerializeHelper.toBytes(key));
        Assert.assertEquals(0, list.size());
    }

    @Test
    public void getUnspentCoins() throws IOException {
        Snapshot snapshot = store.getSnapshot();
        DataCache<UInt256, UnspentCoinState> cache = snapshot.getUnspentCoins();

        UnspentCoinState state = new UnspentCoinState();
        state.items = new CoinState[]{CoinState.Claimed, CoinState.Confirmed, CoinState.Spent, CoinState.Unconfirmed};

        UInt256 key = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        cache.add(key, state);
        snapshot.commit();
        UnspentCoinState dbtx = cache.get(key);
        Assert.assertEquals(state.items.length, dbtx.items.length);
        Assert.assertEquals(state.items[0], dbtx.items[0]);
        Assert.assertEquals(state.items[1], dbtx.items[1]);
        Assert.assertEquals(state.items[2], dbtx.items[2]);
        Assert.assertEquals(state.items[3], dbtx.items[3]);
        cache.getAndChange(key);
        snapshot.commit();
        snapshot.close();

        // read from db
        snapshot = store.getSnapshot();
        cache = snapshot.getUnspentCoins();
        Collection<Map.Entry<UInt256, UnspentCoinState>> list = cache.find(key.toArray());
        Assert.assertEquals(1, list.size());
        dbtx = list.iterator().next().getValue();
        Assert.assertEquals(state.items.length, dbtx.items.length);
        Assert.assertEquals(state.items[0], dbtx.items[0]);
        Assert.assertEquals(state.items[1], dbtx.items[1]);
        Assert.assertEquals(state.items[2], dbtx.items[2]);
        Assert.assertEquals(state.items[3], dbtx.items[3]);
        cache.delete(key); // delete
        snapshot.commit();
        snapshot.close();

        cache = store.getUnspentCoins();
        dbtx = cache.tryGet(key); // read null from
        Assert.assertNull(dbtx);

        list = cache.find();// read empty array from db
        Assert.assertEquals(0, list.size());

        list = cache.find(SerializeHelper.toBytes(key));
        Assert.assertEquals(0, list.size());
    }

    @Test
    public void getSpentCoins() throws IOException {
        Snapshot snapshot = store.getSnapshot();
        DataCache<UInt256, SpentCoinState> cache = snapshot.getSpentCoins();

        SpentCoinState state = new SpentCoinState();
        state.transactionHeight = new Uint(10);
        state.transactionHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        state.items = new HashMap<>();
        state.items.put(new Ushort(1), new Uint(100));
        state.items.put(new Ushort(2), new Uint(200));

        UInt256 key = state.transactionHash;
        cache.add(key, state);
        snapshot.commit();
        SpentCoinState dbtx = cache.get(key);
        Assert.assertEquals(state.transactionHeight, dbtx.transactionHeight);
        Assert.assertEquals(state.transactionHash, dbtx.transactionHash);
        Assert.assertEquals(state.items.size(), dbtx.items.size());
        Assert.assertEquals(state.items.get(new Ushort(1)), dbtx.items.get(new Ushort(1)));
        Assert.assertEquals(state.items.get(new Ushort(2)), dbtx.items.get(new Ushort(2)));
        cache.getAndChange(key);
        snapshot.commit();
        snapshot.close();

        // read from db
        snapshot = store.getSnapshot();
        cache = snapshot.getSpentCoins();
        Collection<Map.Entry<UInt256, SpentCoinState>> list = cache.find(key.toArray());
        Assert.assertEquals(1, list.size());
        dbtx = list.iterator().next().getValue();
        Assert.assertEquals(state.transactionHeight, dbtx.transactionHeight);
        Assert.assertEquals(state.transactionHash, dbtx.transactionHash);
        Assert.assertEquals(state.items.size(), dbtx.items.size());
        Assert.assertEquals(state.items.get(new Ushort(1)), dbtx.items.get(new Ushort(1)));
        Assert.assertEquals(state.items.get(new Ushort(2)), dbtx.items.get(new Ushort(2)));
        cache.delete(key); // delete
        snapshot.commit();
        snapshot.close();

        cache = store.getSpentCoins();
        dbtx = cache.tryGet(key); // read null from
        Assert.assertNull(dbtx);

        list = cache.find();// read empty array from db
        Assert.assertEquals(0, list.size());

        list = cache.find(SerializeHelper.toBytes(key));
        Assert.assertEquals(0, list.size());
    }

    @Test
    public void getValidators() throws IOException {
        Snapshot snapshot = store.getSnapshot();
        DataCache<ECPoint, ValidatorState> cache = snapshot.getValidators();

        ValidatorState state = new ValidatorState();
        state.publicKey = new ECPoint(ECC.getInfinityPoint());
        state.registered = false;
        state.votes = new Fixed8(2);

        ECPoint key = state.publicKey;
        cache.add(key, state);
        snapshot.commit();
        ValidatorState dbtx = cache.get(key);
        Assert.assertEquals(state.publicKey, dbtx.publicKey);
        Assert.assertEquals(state.registered, dbtx.registered);
        Assert.assertEquals(state.votes, dbtx.votes);
        cache.getAndChange(key);
        snapshot.commit();
        snapshot.close();

        // read from db
        snapshot = store.getSnapshot();
        cache = snapshot.getValidators();
        Collection<Map.Entry<ECPoint, ValidatorState>> list = cache.find(key.getEncoded(true));
        Assert.assertEquals(1, list.size());
        dbtx = list.iterator().next().getValue();
        Assert.assertEquals(state.publicKey, dbtx.publicKey);
        Assert.assertEquals(state.registered, dbtx.registered);
        Assert.assertEquals(state.votes, dbtx.votes);
        cache.delete(key); // delete
        snapshot.commit();
        snapshot.close();

        cache = store.getValidators();
        dbtx = cache.tryGet(key); // read null from
        Assert.assertNull(dbtx);

        list = cache.find();// read empty array from db
        Assert.assertEquals(0, list.size());

        list = cache.find(SerializeHelper.toBytes(key));
        Assert.assertEquals(0, list.size());
    }


    @Test
    public void getValidatorsCount() throws IOException {
        Snapshot snapshot = store.getSnapshot();
        MetaDataCache<ValidatorsCountState> cache = snapshot.getValidatorsCount();

        ValidatorsCountState state = cache.get();
        state.votes[0] = new Fixed8(1);
        state.votes[1] = new Fixed8(2);
        state.votes[2] = new Fixed8(3);
        state.votes[3] = new Fixed8(0);

        snapshot.commit();
        ValidatorsCountState dbtx = cache.get();

        Assert.assertEquals(state.votes.length, dbtx.votes.length);
        Assert.assertEquals(state.votes[0], dbtx.votes[0]);
        Assert.assertEquals(state.votes[1], dbtx.votes[1]);
        Assert.assertEquals(state.votes[2], dbtx.votes[2]);
        Assert.assertEquals(state.votes[3], dbtx.votes[3]);
        cache.getAndChange();
        snapshot.commit();
        snapshot.close();

        // read from db
        snapshot = store.getSnapshot();
        cache = snapshot.getValidatorsCount();
        dbtx = cache.get();
        Assert.assertEquals(state.votes.length, dbtx.votes.length);
        Assert.assertEquals(state.votes[0], dbtx.votes[0]);
        Assert.assertEquals(state.votes[1], dbtx.votes[1]);
        Assert.assertEquals(state.votes[2], dbtx.votes[2]);
        Assert.assertEquals(state.votes[3], dbtx.votes[3]);
        dbtx.votes = new Fixed8[0];
        snapshot.commit();
        snapshot.close();

        dbtx = cache.get(); // get empty array
        Assert.assertNotNull(dbtx);

        Assert.assertEquals(0, dbtx.votes.length);
    }


    @Test
    public void getAssets() throws IOException {
        Snapshot snapshot = store.getSnapshot();
        DataCache<UInt256, AssetState> cache = snapshot.getAssets();

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

        UInt256 key = state.assetId;
        cache.add(key, state);
        snapshot.commit();
        AssetState dbtx = cache.get(key);
        Assert.assertEquals(state.assetId, dbtx.assetId);
        Assert.assertEquals(state.assetType.value(), dbtx.assetType.value());
        Assert.assertEquals(state.name, dbtx.name);
        Assert.assertEquals(state.amount, dbtx.amount);
        Assert.assertEquals(state.available, dbtx.available);
        Assert.assertEquals(state.precision, dbtx.precision);
        Assert.assertEquals(state.fee, dbtx.fee);
        Assert.assertEquals(state.feeAddress, dbtx.feeAddress);
        Assert.assertEquals(state.owner, dbtx.owner);
        Assert.assertEquals(state.admin, dbtx.admin);
        Assert.assertEquals(state.isFrozen, dbtx.isFrozen);
        cache.getAndChange(key);
        snapshot.commit();
        snapshot.close();

        // read from db
        snapshot = store.getSnapshot();
        cache = snapshot.getAssets();
        Collection<Map.Entry<UInt256, AssetState>> list = cache.find(key.toArray());
        Assert.assertEquals(1, list.size());
        dbtx = list.iterator().next().getValue();
        Assert.assertEquals(state.assetId, dbtx.assetId);
        Assert.assertEquals(state.assetType.value(), dbtx.assetType.value());
        Assert.assertEquals(state.name, dbtx.name);
        Assert.assertEquals(state.amount, dbtx.amount);
        Assert.assertEquals(state.available, dbtx.available);
        Assert.assertEquals(state.precision, dbtx.precision);
        Assert.assertEquals(state.fee, dbtx.fee);
        Assert.assertEquals(state.feeAddress, dbtx.feeAddress);
        Assert.assertEquals(state.owner, dbtx.owner);
        Assert.assertEquals(state.admin, dbtx.admin);
        Assert.assertEquals(state.isFrozen, dbtx.isFrozen);
        cache.delete(key); // delete
        snapshot.commit();
        snapshot.close();

        cache = store.getAssets();
        dbtx = cache.tryGet(key); // read null from
        Assert.assertNull(dbtx);

        list = cache.find();// read empty array from db
        Assert.assertEquals(0, list.size());

        list = cache.find(SerializeHelper.toBytes(key));
        Assert.assertEquals(0, list.size());
    }


    @Test
    public void getContracts() throws IOException {
        Snapshot snapshot = store.getSnapshot();
        DataCache<UInt160, ContractState> cache = snapshot.getContracts();

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
        cache.add(key, state);
        snapshot.commit();
        ContractState dbtx = cache.get(key);
        Assert.assertEquals(state.author, dbtx.author);
        Assert.assertEquals(state.codeVersion, dbtx.codeVersion);
        Assert.assertEquals(state.name, dbtx.name);
        Assert.assertEquals(state.returnType.value(), dbtx.returnType.value());
        Assert.assertArrayEquals(state.script, dbtx.script);
        Assert.assertEquals(state.contractProperties.value(), dbtx.contractProperties.value());
        Assert.assertEquals(state.parameterList.length, dbtx.parameterList.length);
        Assert.assertEquals(state.parameterList[0], dbtx.parameterList[0]);
        Assert.assertEquals(state.parameterList[1], dbtx.parameterList[1]);
        Assert.assertEquals(state.parameterList[2], dbtx.parameterList[2]);
        cache.getAndChange(key);
        snapshot.commit();
        snapshot.close();

        // read from db
        snapshot = store.getSnapshot();
        cache = snapshot.getContracts();
        Collection<Map.Entry<UInt160, ContractState>> list = cache.find(key.toArray());
        Assert.assertEquals(1, list.size());
        dbtx = list.iterator().next().getValue();
        Assert.assertEquals(state.author, dbtx.author);
        Assert.assertEquals(state.codeVersion, dbtx.codeVersion);
        Assert.assertEquals(state.name, dbtx.name);
        Assert.assertEquals(state.returnType.value(), dbtx.returnType.value());
        Assert.assertArrayEquals(state.script, dbtx.script);
        Assert.assertEquals(state.contractProperties.value(), dbtx.contractProperties.value());
        Assert.assertEquals(state.parameterList.length, dbtx.parameterList.length);
        Assert.assertEquals(state.parameterList[0], dbtx.parameterList[0]);
        Assert.assertEquals(state.parameterList[1], dbtx.parameterList[1]);
        Assert.assertEquals(state.parameterList[2], dbtx.parameterList[2]);
        cache.delete(key); // delete
        snapshot.commit();
        snapshot.close();

        cache = store.getContracts();
        dbtx = cache.tryGet(key); // read null from
        Assert.assertNull(dbtx);

        list = cache.find();// read empty array from db
        Assert.assertEquals(0, list.size());

        list = cache.find(SerializeHelper.toBytes(key));
        Assert.assertEquals(0, list.size());
    }

    @Test
    public void getStorages() throws IOException {
        Snapshot snapshot = store.getSnapshot();
        DataCache<StorageKey, StorageItem> cache = snapshot.getStorages();

        StorageItem state = new StorageItem();
        state.isConstant = false;
        state.value = new byte[]{0x01, 0x02, 0x03, 0x04};

        StorageKey key = new StorageKey();
        key.scriptHash = UInt160.Zero;
        key.key = new byte[]{0x01, 0x02, 0x03, 0x04};

        cache.add(key, state);
        snapshot.commit();
        StorageItem dbtx = cache.get(key);
        Assert.assertEquals(state.isConstant, dbtx.isConstant);
        Assert.assertArrayEquals(state.value, dbtx.value);
        cache.getAndChange(key);
        snapshot.commit();
        snapshot.close();

        // read from db
        snapshot = store.getSnapshot();
        cache = snapshot.getStorages();
        Collection<Map.Entry<StorageKey, StorageItem>> list = cache.find(SerializeHelper.toBytes(key));
        Assert.assertEquals(1, list.size());
        dbtx = list.iterator().next().getValue();
        Assert.assertEquals(state.isConstant, dbtx.isConstant);
        Assert.assertArrayEquals(state.value, dbtx.value);
        cache.delete(key); // delete
        snapshot.commit();
        snapshot.close();

        cache = store.getStorages();
        dbtx = cache.tryGet(key); // read null from
        Assert.assertNull(dbtx);

        list = cache.find();// read empty array from db
        Assert.assertEquals(0, list.size());

        list = cache.find(SerializeHelper.toBytes(key));
        Assert.assertEquals(0, list.size());
    }

    @Test
    public void getHeaderHashList() throws IOException {
        Snapshot snapshot = store.getSnapshot();
        DataCache<UInt32Wrapper, HeaderHashList> cache = snapshot.getHeaderHashList();

        HeaderHashList state = new HeaderHashList();
        state.hashes = new UInt256[]{UInt256.Zero, UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01")};

        UInt32Wrapper key = new UInt32Wrapper(UInt32.parse("0xff00ff01"));

        cache.add(key, state);
        snapshot.commit();
        HeaderHashList dbtx = cache.get(key);
        Assert.assertArrayEquals(state.hashes, dbtx.hashes);
        cache.getAndChange(key);
        snapshot.commit();
        snapshot.close();

        // read from db
        snapshot = store.getSnapshot();
        cache = snapshot.getHeaderHashList();
        Collection<Map.Entry<UInt32Wrapper, HeaderHashList>> list = cache.find(SerializeHelper.toBytes(key));
        Assert.assertEquals(1, list.size());
        dbtx = list.iterator().next().getValue();
        Assert.assertArrayEquals(state.hashes, dbtx.hashes);
        cache.delete(key); // delete
        snapshot.commit();
        snapshot.close();

        cache = store.getHeaderHashList();
        dbtx = cache.tryGet(key); // read null from
        Assert.assertNull(dbtx);

        list = cache.find();// read empty array from db
        Assert.assertEquals(0, list.size());

        list = cache.find(SerializeHelper.toBytes(key));
        Assert.assertEquals(0, list.size());
    }

    @Test
    public void getBlockHashIndex() throws IOException {
        Snapshot snapshot = store.getSnapshot();
        MetaDataCache<HashIndexState> cache = snapshot.getBlockHashIndex();

        HashIndexState state = cache.get();
        state.hash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        state.index = new Uint(10);

        snapshot.commit();
        HashIndexState dbtx = cache.get();
        Assert.assertEquals(state.hash, dbtx.hash);
        Assert.assertEquals(state.index, dbtx.index);
        cache.getAndChange();
        snapshot.commit();
        snapshot.close();

        // read from db
        snapshot = store.getSnapshot();
        cache = snapshot.getBlockHashIndex();
        dbtx = cache.get();
        Assert.assertEquals(state.hash, dbtx.hash);
        Assert.assertEquals(state.index, dbtx.index);
        dbtx.hash = UInt256.Zero;
        snapshot.commit();
        snapshot.close();

        dbtx = cache.get(); // get empty array
        Assert.assertNotNull(dbtx);

        Assert.assertEquals(UInt256.Zero, dbtx.hash);
    }

    @Test
    public void getHeaderHashIndex() throws IOException {
        Snapshot snapshot = store.getSnapshot();
        MetaDataCache<HashIndexState> cache = snapshot.getHeaderHashIndex();

        HashIndexState state = cache.get();
        state.hash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        state.index = new Uint(10);

        snapshot.commit();
        HashIndexState dbtx = cache.get();
        Assert.assertEquals(state.hash, dbtx.hash);
        Assert.assertEquals(state.index, dbtx.index);
        cache.getAndChange();
        snapshot.commit();
        snapshot.close();

        // read from db
        snapshot = store.getSnapshot();
        cache = snapshot.getHeaderHashIndex();
        dbtx = cache.get();
        Assert.assertEquals(state.hash, dbtx.hash);
        Assert.assertEquals(state.index, dbtx.index);
        dbtx.hash = UInt256.Zero;
        snapshot.commit();
        snapshot.close();

        dbtx = cache.get(); // get empty array
        Assert.assertNotNull(dbtx);

        Assert.assertEquals(UInt256.Zero, dbtx.hash);
    }

    @Test
    public void close() {
        try {
            LevelDBStore test = new LevelDBStore("test");
            test.close();
            Assert.assertTrue(true);
            File file = new File("test");
            if (file.exists()) {
                for (File subFile : file.listFiles()) {
                    subFile.delete();
                }
                file.delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}