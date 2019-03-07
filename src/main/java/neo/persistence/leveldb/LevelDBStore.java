package neo.persistence.leveldb;

import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBFactory;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.ReadOptions;
import org.iq80.leveldb.WriteBatch;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import neo.Properties;
import neo.UInt160;
import neo.UInt256;
import neo.cryptography.ECC.ECPoint;
import neo.io.caching.DataCache;
import neo.io.caching.MetaDataCache;
import neo.io.wrappers.UInt32Wrapper;
import neo.ledger.AccountState;
import neo.ledger.AssetState;
import neo.ledger.BlockState;
import neo.ledger.ContractState;
import neo.ledger.HashIndexState;
import neo.ledger.HeaderHashList;
import neo.ledger.SpentCoinState;
import neo.ledger.StorageItem;
import neo.ledger.StorageKey;
import neo.ledger.TransactionState;
import neo.ledger.UnspentCoinState;
import neo.ledger.ValidatorState;
import neo.ledger.ValidatorsCountState;
import neo.persistence.Snapshot;
import neo.persistence.Store;

/**
 * Leveldb存储器
 */
public class LevelDBStore extends Store {

    private final DB db;
    private static final Charset CHARSET = Charset.forName("utf-8");


    /**
     * 构造函数：打开leveldb数据库
     *
     * @param path 数据库路径
     */
    public LevelDBStore(String path) throws IOException {
        DBFactory factory = new JniDBFactory();
        // 默认如果没有则创建
        Options options = new Options();
        options.createIfMissing(true);
        File file = new File(path);

        db = factory.open(file, options);
        byte[] keys = new byte[]{Prefixes.SYS_Version};
        byte[] versionBytes = db.get(keys);
        String version = new String(versionBytes);
        // TODO 硬代码
        if (version.compareTo("2.9.1") >= 0) {
            return;
        }

        WriteBatch batch = db.createWriteBatch();
        ReadOptions readOptions = new ReadOptions();
        readOptions.fillCache(true);
        DBIterator iterator = db.iterator(readOptions);
        iterator.seekToFirst();
        while (iterator.hasNext()) {
            batch.delete(iterator.next().getKey());
        }
        iterator.close();
        db.put(keys, Properties.Default.version.getBytes(CHARSET));
        db.write(batch);
    }


    @Override
    public Snapshot getSnapshot() {
        return new DbSnapshot(db);
    }

    public void close() throws IOException {
        if (db != null) {
            db.close();
        }
    }


    @Override
    public DataCache<UInt256, BlockState> getBlocks() {
        return new DbCache<>(db, null, null, Prefixes.DATA_Block, UInt256::new, BlockState::new);
    }

    @Override
    public DataCache<UInt256, TransactionState> getTransactions() {
        return new DbCache<>(db, null, null, Prefixes.DATA_Transaction, UInt256::new, TransactionState::new);
    }

    @Override
    public DataCache<UInt160, AccountState> getAccounts() {
        return new DbCache<>(db, null, null, Prefixes.ST_Account, UInt160::new, AccountState::new);
    }

    @Override
    public DataCache<UInt256, UnspentCoinState> getUnspentCoins() {
        return new DbCache<>(db, null, null, Prefixes.ST_Coin, UInt256::new, UnspentCoinState::new);
    }

    @Override
    public DataCache<UInt256, SpentCoinState> getSpentCoins() {
        return new DbCache<>(db, null, null, Prefixes.ST_SpentCoin, UInt256::new, SpentCoinState::new);
    }

    @Override
    public DataCache<ECPoint, ValidatorState> getValidators() {
        return new DbCache<>(db, null, null, Prefixes.ST_Validator, ECPoint::new, ValidatorState::new);
    }

    @Override
    public DataCache<UInt256, AssetState> getAssets() {
        return new DbCache<>(db, null, null, Prefixes.ST_Asset, UInt256::new, AssetState::new);
    }

    @Override
    public DataCache<UInt160, ContractState> getContracts() {
        return new DbCache<>(db, null, null, Prefixes.ST_Asset, UInt160::new, ContractState::new);
    }

    @Override
    public DataCache<StorageKey, StorageItem> getStorages() {
        return new DbCache<>(db, null, null, Prefixes.ST_Contract, StorageKey::new, StorageItem::new);
    }

    @Override
    public DataCache<UInt32Wrapper, HeaderHashList> getHeaderHashList() {
        return new DbCache<>(db, null, null, Prefixes.IX_HeaderHashList, UInt32Wrapper::new, HeaderHashList::new);
    }

    @Override
    public MetaDataCache<ValidatorsCountState> getValidatorsCount() {
        return new DbMetaDataCache<>(db, null, null, Prefixes.IX_ValidatorsCount, ValidatorsCountState::new);
    }

    @Override
    public MetaDataCache<HashIndexState> getBlockHashIndex() {
        return new DbMetaDataCache<>(db, null, null, Prefixes.IX_CurrentBlock, HashIndexState::new);
    }

    @Override
    public MetaDataCache<HashIndexState> getHeaderHashIndex() {
        return new DbMetaDataCache<>(db, null, null, Prefixes.IX_CurrentHeader, HashIndexState::new);
    }

}
