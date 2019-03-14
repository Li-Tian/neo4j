package neo.persistence;


import org.iq80.leveldb.WriteBatch;

import neo.log.notr.TR;

/**
 * Snapshot's clone
 */
public class CloneSnapshot extends Snapshot {

    private WriteBatch writeBatch;

    /**
     * Copy from snapshot
     *
     * @param snapshot snapshot
     */
    public CloneSnapshot(Snapshot snapshot) {
        TR.enter();
        this.persistingBlock = snapshot.getPersistingBlock();
        this.blocks = snapshot.getBlocks();
        this.transactions = snapshot.getTransactions();
        this.accounts = snapshot.getAccounts();
        this.unspentCoins = snapshot.getUnspentCoins();
        this.spentCoins = snapshot.getSpentCoins();
        this.validators = snapshot.getValidators();
        this.assets = snapshot.getAssets();
        this.contracts = snapshot.getContracts();
        this.storages = snapshot.getStorages();
        this.headerHashList = snapshot.getHeaderHashList();
        this.validatorsCount = snapshot.getValidatorsCount();
        this.blockHashIndex = snapshot.getBlockHashIndex();
        this.headerHashIndex = snapshot.getHeaderHashIndex();
        this.writeBatch = snapshot.getWriteBatch();
        TR.exit();
    }

    @Override
    public WriteBatch getWriteBatch() {
        return writeBatch;
    }
}
