package neo.persistence;


/**
 * Snapshot's clone
 */
public class CloneSnapshot extends Snapshot {

    /**
     * Copy from snapshot
     *
     * @param snapshot snapshot
     */
    public CloneSnapshot(Snapshot snapshot) {
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
    }

}
