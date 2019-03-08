package neo.persistence;

import neo.UInt160;
import neo.UInt256;
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
import neo.cryptography.ecc.ECPoint;
import neo.ledger.ValidatorState;
import neo.ledger.ValidatorsCountState;

/**
 * Persistent operation interface
 */
public interface IPersistence {

    /**
     * Block cache. Quickly find the block status by the hash of the block.
     */
    DataCache<UInt256, BlockState> getBlocks();

    /**
     * Transaction cache. Quickly find transaction status by the hash of the transaction.
     */
    DataCache<UInt256, TransactionState> getTransactions();

    /**
     * Account cache. Quickly find the account status by the hash of the account.
     */
    DataCache<UInt160, AccountState> getAccounts();

    /**
     * UTXO cache. Quickly find the status of all UTXOs of the transaction by the hash of the
     * transaction.
     */
    DataCache<UInt256, UnspentCoinState> getUnspentCoins();

    /**
     * The information cacahe about the UTXO that has been spent. Quickly find information about the
     * UTXO that has been spent by hash of the transaction. This includes the block height of the
     * transaction and the height of the block in which the UTXO that has been spent.
     */
    DataCache<UInt256, SpentCoinState> getSpentCoins();

    /**
     * The validators cache. Quickly query the status of the validator through the public key.
     * Including the public key, whether it has been registered, and the number of votes.
     */
    DataCache<ECPoint, ValidatorState> getValidators();

    /**
     * The assets cache. Quickly find the status of an asset by the hash.
     */
    DataCache<UInt256, AssetState> getAssets();


    /**
     * Smart contracts cache. Quickly find information about smart contracts through the hash of
     * smart contracts.
     */
    DataCache<UInt160, ContractState> getContracts();


    /**
     * Key-value pair storage for contracts. Query value by script hash and storing key.
     */
    DataCache<StorageKey, StorageItem> getStorages();

    /**
     * Block header hash list cache. <br/>Each block header hash list contains the hash values of
     * 2000 block headers. <br/> The number of the first block header hash list is 0. <br/> The
     * number of the second block header hash list is 2000. And so on. <br/> This cache quickly
     * finds the block header hash list by the number of the block header hash list.
     */
    DataCache<UInt32Wrapper, HeaderHashList> getHeaderHashList();

    /**
     * The voting pool of ValidatorsCount. Used to vote for votes.
     */
    MetaDataCache<ValidatorsCountState> getValidatorsCount();

    /**
     * Block index. Stores the hash value and height of the latest block.
     */
    MetaDataCache<HashIndexState> getBlockHashIndex();

    /**
     * Block header index. Stores the hash and height of the latest block header.
     */
    MetaDataCache<HashIndexState> getHeaderHashIndex();

}
