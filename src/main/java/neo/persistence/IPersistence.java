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
import neo.cryptography.ECC.ECPoint;
import neo.ledger.ValidatorState;
import neo.ledger.ValidatorsCountState;

/**
 * 持久化操作接口
 */
public interface IPersistence {

    /**
     * 区块缓存。通过区块的哈希值快速查找区块状态。
     */
    DataCache<UInt256, BlockState> getBlocks();

    /**
     * 交易缓存。通过交易的哈希值快速查找交易状态。
     */
    DataCache<UInt256, TransactionState> getTransactions();

    /**
     * 账户缓存。通过账户的哈希值快速查找账户状态。
     */
    DataCache<UInt160, AccountState> getAccounts();

    /**
     * UTXO缓存。通过交易哈希快速查找该交易下所有UTXO的状态。
     */
    DataCache<UInt256, UnspentCoinState> getUnspentCoins();

    /**
     * 已花费的UTXO相关信息缓存。通过交易的哈希值快速查找已花费的UTXO的信息。 包括交易所在的区块高度和交易中已经被花费的UTXO在被花费时的所处区块的高度。
     */
    DataCache<UInt256, SpentCoinState> getSpentCoins();

    /**
     * 验证人的缓存。通过公钥快速查询验证人的状态。包括公钥，是否已经注册，投票数。
     */
    DataCache<ECPoint, ValidatorState> getValidators();

    /**
     * 资产的缓存。通过哈希快速查找资产的状态。
     */
    DataCache<UInt256, AssetState> getAssets();


    /**
     * 合约换成
     */
    DataCache<UInt160, ContractState> getContracts();


    /**
     * 合约的键值对存储。通过脚本哈希和存储key查询value。
     */
    DataCache<StorageKey, StorageItem> getStorages();

    /**
     * 区块头哈希列表的缓存。<br/> 每个区块头哈希列表包含2000个区块头的哈希值。<br/> 然后第一个区块头哈希列表的编号是0。<br/>
     * 第二个区块头哈希列表的编号是2000。以此类推。<br/> 这个缓存通过区块头哈希列表的编号快速查找区块头哈希列表。
     */
    DataCache<UInt32Wrapper, HeaderHashList> getHeaderHashList();

    /**
     * 验证人个数的投票池。投票时用来点票。
     */
    MetaDataCache<ValidatorsCountState> getValidatorsCount();

    /**
     * 区块索引。存放最新的区块的哈希值和高度。
     */
    MetaDataCache<HashIndexState> getBlockHashIndex();

    /**
     * 区块头索引。存放最新的区块头的哈希值和高度。
     */
    MetaDataCache<HashIndexState> getHeaderHashIndex();

}
