package neo.persistence.leveldb;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.ReadOptions;
import org.iq80.leveldb.WriteBatch;

import java.io.IOException;

import neo.UInt160;
import neo.UInt256;
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
import neo.cryptography.ecc.ECPoint;

public class DbSnapshot extends Snapshot {

    private final DB db;
    private final org.iq80.leveldb.Snapshot snapshot;
    private final WriteBatch batch;

    public DbSnapshot(DB db) {
        this.db = db;
        this.snapshot = db.getSnapshot();
        this.batch = db.createWriteBatch();

        ReadOptions options = new ReadOptions();
        options.fillCache(false);
        options.snapshot(this.snapshot);

        blocks = new DbCache<>(db, options, batch, Prefixes.DATA_Block, UInt256::new, BlockState::new);
        transactions = new DbCache<>(db, options, batch, Prefixes.DATA_Transaction, UInt256::new, TransactionState::new);
        accounts = new DbCache<>(db, options, batch, Prefixes.ST_Account, UInt160::new, AccountState::new);
        unspentCoins = new DbCache<>(db, options, batch, Prefixes.ST_Coin, UInt256::new, UnspentCoinState::new);
        spentCoins = new DbCache<>(db, options, batch, Prefixes.ST_SpentCoin, UInt256::new, SpentCoinState::new);
        validators = new DbCache<>(db, options, batch, Prefixes.ST_Validator, ECPoint::new, ValidatorState::new);
        assets = new DbCache<>(db, options, batch, Prefixes.ST_Asset, UInt256::new, AssetState::new);
        contracts = new DbCache<>(db, options, batch, Prefixes.ST_Contract, UInt160::new, ContractState::new);
        storages = new DbCache<>(db, options, batch, Prefixes.ST_Storage, StorageKey::new, StorageItem::new);
        headerHashList = new DbCache<>(db, options, batch, Prefixes.IX_HeaderHashList, UInt32Wrapper::new, HeaderHashList::new);
        validatorsCount = new DbMetaDataCache<>(db, options, batch, Prefixes.IX_ValidatorsCount, ValidatorsCountState::new);
        blockHashIndex = new DbMetaDataCache<>(db, options, batch, Prefixes.IX_CurrentBlock, HashIndexState::new);
        headerHashIndex = new DbMetaDataCache<>(db, options, batch, Prefixes.IX_CurrentHeader, HashIndexState::new);
    }

    @Override
    public void commit() {
        super.commit();
        db.write(batch);
    }

    @Override
    public void close() throws IOException {
        snapshot.close();
    }

}
