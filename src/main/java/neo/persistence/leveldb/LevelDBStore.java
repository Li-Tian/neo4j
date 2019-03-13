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
import neo.cryptography.ecc.ECPoint;
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
import neo.log.notr.TR;
import neo.persistence.Snapshot;
import neo.persistence.Store;

/**
 * LevelDB Store
 */
public class LevelDBStore extends Store {

    private final DB db;
    private static final Charset CHARSET = Charset.forName("utf-8");


    /**
     * Constructor: Open the leveldb database
     *
     * @param path leveldb path
     * @throws IOException throw it when open db failed.
     */
    public LevelDBStore(String path) throws IOException {
        TR.enter();

        DBFactory factory = new JniDBFactory();
        // 默认如果没有则创建
        Options options = new Options();
        options.createIfMissing(true);
        File file = new File(path);

        db = factory.open(file, options);
        byte[] keys = new byte[]{Prefixes.SYS_Version};
        byte[] versionBytes = db.get(keys);

        WriteBatch batch = db.createWriteBatch();
        if (versionBytes != null && versionBytes.length > 0) {
            String version = new String(versionBytes);
            // TODO hard code
            if (version.compareTo("2.9.1") >= 0) {
                return;
            }
            ReadOptions readOptions = new ReadOptions();
            readOptions.fillCache(true);
            DBIterator iterator = db.iterator(readOptions);
            iterator.seekToFirst();
            while (iterator.hasNext()) {
                batch.delete(iterator.next().getKey());
            }
            iterator.close();
        }
        db.put(keys, Properties.Default.version.getBytes(CHARSET));
        db.write(batch);


        TR.exit();
    }


    /**
     * Get Snapshot
     *
     * @return Database Snapshot
     */
    @Override
    public Snapshot getSnapshot() {
        TR.enter();
        return TR.exit(new DbSnapshot(db));
    }

    /**
     * Release resources
     */
    public void close() throws IOException {
        TR.enter();
        if (db != null) {
            db.close();
        }
        TR.exit();
    }

    /**
     * Get blocks
     *
     * @return DbCache with the "DATA_Block" prefix
     */
    @Override
    public DataCache<UInt256, BlockState> getBlocks() {
        return new DbCache<>(db, null, null, Prefixes.DATA_Block, UInt256::new, BlockState::new);
    }

    /**
     * Get Transactions
     *
     * @return DbCache with the "DATA_Transaction" prefix
     */
    @Override
    public DataCache<UInt256, TransactionState> getTransactions() {
        return new DbCache<>(db, null, null, Prefixes.DATA_Transaction, UInt256::new, TransactionState::new);
    }

    /**
     * Get accounts
     *
     * @return DbCache with the "ST_Account" prefix
     */
    @Override
    public DataCache<UInt160, AccountState> getAccounts() {
        return new DbCache<>(db, null, null, Prefixes.ST_Account, UInt160::new, AccountState::new);
    }

    /**
     * Get UTXO
     *
     * @return DbCache with the "ST_Coin" prefix
     */
    @Override
    public DataCache<UInt256, UnspentCoinState> getUnspentCoins() {
        return new DbCache<>(db, null, null, Prefixes.ST_Coin, UInt256::new, UnspentCoinState::new);
    }

    /**
     * Get Spent Coins
     *
     * @return DbCache with the "ST_SpentCoin" prefix
     */
    @Override
    public DataCache<UInt256, SpentCoinState> getSpentCoins() {
        return new DbCache<>(db, null, null, Prefixes.ST_SpentCoin, UInt256::new, SpentCoinState::new);
    }

    /**
     * Get Validators
     *
     * @return DbCache with the "ST_Validator" prefix
     */
    @Override
    public DataCache<ECPoint, ValidatorState> getValidators() {
        return new DbCache<>(db, null, null, Prefixes.ST_Validator, ECPoint::new, ValidatorState::new);
    }

    /**
     * Get assets
     *
     * @return DbCache with the "ST_Asset" prefix
     */
    @Override
    public DataCache<UInt256, AssetState> getAssets() {
        return new DbCache<>(db, null, null, Prefixes.ST_Asset, UInt256::new, AssetState::new);
    }

    /**
     * Get Contracts
     *
     * @return DbCache with the "ST_Contract" prefix
     */
    @Override
    public DataCache<UInt160, ContractState> getContracts() {
        return new DbCache<>(db, null, null, Prefixes.ST_Asset, UInt160::new, ContractState::new);
    }

    /**
     * Get Validators
     *
     * @return DbCache with the "ST_Validator" prefix
     */
    @Override
    public DataCache<StorageKey, StorageItem> getStorages() {
        return new DbCache<>(db, null, null, Prefixes.ST_Contract, StorageKey::new, StorageItem::new);
    }

    /**
     * Gets the block header hash list
     *
     * @return DbCache with the "IX_HeaderHashList" prefix
     */
    @Override
    public DataCache<UInt32Wrapper, HeaderHashList> getHeaderHashList() {
        return new DbCache<>(db, null, null, Prefixes.IX_HeaderHashList, UInt32Wrapper::new, HeaderHashList::new);
    }

    /**
     * Get Validators Count
     *
     * @return DbMetaDataCache with the "IX_ValidatorsCount" prefix
     */
    @Override
    public MetaDataCache<ValidatorsCountState> getValidatorsCount() {
        return new DbMetaDataCache<>(db, null, null, Prefixes.IX_ValidatorsCount, ValidatorsCountState::new);
    }

    /**
     * Get block hash index
     *
     * @return DbMetaDataCache with the "IX_CurrentBlock" prefix
     */
    @Override
    public MetaDataCache<HashIndexState> getBlockHashIndex() {
        return new DbMetaDataCache<>(db, null, null, Prefixes.IX_CurrentBlock, HashIndexState::new);
    }

    /**
     * Get block header hash index
     *
     * @return DbMetaDataCache with the "IX_CurrentHeader" prefix
     */
    @Override
    public MetaDataCache<HashIndexState> getHeaderHashIndex() {
        return new DbMetaDataCache<>(db, null, null, Prefixes.IX_CurrentHeader, HashIndexState::new);
    }

}
