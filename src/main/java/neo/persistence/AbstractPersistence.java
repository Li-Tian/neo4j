package neo.persistence;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import neo.UInt256;
import neo.csharp.Uint;
import neo.csharp.Ushort;
import neo.ledger.BlockState;
import neo.ledger.Blockchain;
import neo.ledger.CoinState;
import neo.ledger.TransactionState;
import neo.ledger.UnspentCoinState;
import neo.ledger.ValidatorState;
import neo.network.p2p.payloads.Block;
import neo.network.p2p.payloads.CoinReference;
import neo.network.p2p.payloads.Header;
import neo.network.p2p.payloads.Transaction;
import neo.network.p2p.payloads.TransactionOutput;
import neo.cryptography.ECC.ECPoint;

/**
 * 抽象的持久化服务方法
 */
public abstract class AbstractPersistence implements IPersistence {

    /**
     * 是否包含某个区块hash
     *
     * @param hash 待查询区块hash
     * @return 存在则返回true.不存在则返回false
     */
    public boolean containsBlock(UInt256 hash) {
        BlockState state = getBlocks().tryGet(hash);
        if (state == null) return false;
        return state.trimmedBlock.isBlock();
    }

    /**
     * 查询是否包含某个交易hash
     *
     * @param hash 交易hash
     * @return 存在则返回true, 不存在返回false
     */
    public boolean containsTransaction(UInt256 hash) {
        TransactionState state = getTransactions().tryGet(hash);
        return state != null;
    }

    /**
     * 获取某个区块
     *
     * @param index 区块高度
     * @return 返回指定高度对应的区块，若不存在则返回null
     */
    public Block getBlock(Uint index) {
        UInt256 hash = Blockchain.singleton().getBlockHash(index);
        if (hash == null) return null;
        return getBlock(hash);
    }

    /**
     * 获取某个区块
     *
     * @param hash 区块hash
     * @return 返回指定区块哈希对应的区块，若不存在则返回null
     */
    public Block getBlock(UInt256 hash) {
        BlockState state = getBlocks().tryGet(hash);
        if (state == null) return null;
        if (!state.trimmedBlock.isBlock()) return null;
        return state.trimmedBlock.getBlock(getTransactions());
    }

    /**
     * 获取验证人候选人列表。包括已经登记为验证候选人的列表和备用验证人列表。<br/> 不包含是否当选为验证人的信息。
     */
    public Collection<ValidatorState> getEnrollments() {
        HashSet<ECPoint> sv = new HashSet<>();
        for (ECPoint ecpoint : Blockchain.StandbyValidators) {
            sv.add(ecpoint);
        }
        return getValidators().find().stream().map(p -> p.getValue()).filter(p -> p.registered || sv.contains(p.publicKey)).collect(Collectors.toList());
    }

    /**
     * 获取某个高度的区块头
     *
     * @param index 区块头高度
     * @return 指定高度的区块的区块头，如果该高度区块不存在，则返回null
     */
    public Header getHeader(Uint index) {
        UInt256 hash = Blockchain.singleton().getBlockHash(index);
        if (hash == null) {
            return null;
        }
        return getHeader(hash);
    }

    /**
     * 获取某个区块头
     *
     * @param hash 区块头hash
     * @return 指定区块的区块头。不存在时返回null
     */
    public Header getHeader(UInt256 hash) {
        BlockState state = getBlocks().tryGet(hash);
        if (state == null) {
            return null;
        }
        return state.trimmedBlock.getHeader();
    }

    /**
     * 获取下一个区块的哈希
     *
     * @param hash 待查询的区块hash
     * @return 下一个区块的哈希。不存在时返回null
     */
    public UInt256 getNextBlockHash(UInt256 hash) {
        BlockState state = getBlocks().tryGet(hash);
        if (state == null) {
            return null;
        }

        Uint index = new Uint(state.trimmedBlock.index.intValue() + 1);
        return Blockchain.singleton().getBlockHash(index);
    }

    /**
     * 查询到某个高度位置，总的系统手续费（包含该区块）总的系统手续费
     *
     * @param height 区块高度
     * @return 总的系统手续费金额
     */
    public long getSysFeeAmount(Uint height) {
        return getSysFeeAmount(Blockchain.singleton().getBlockHash(height));
    }

    /**
     * 查询从区块0开始到指定哈希的区块位置（包含该区块）总的系统手续费
     *
     * @param hash 指定的区块hash
     * @return 总的系统手续费金额
     */
    public long getSysFeeAmount(UInt256 hash) {
        BlockState block_state = getBlocks().tryGet(hash);
        if (block_state == null) return 0;
        return block_state.systemFeeAmount;
    }

    /**
     * 查询交易
     *
     * @param hash 交易hash
     * @return 指定哈希对应的交易
     */
    public Transaction getTransaction(UInt256 hash) {
        TransactionState state = getTransactions().tryGet(hash);
        if (state == null) {
            return null;
        }
        return state.transaction;
    }

    /**
     * 查询某一笔交易的未花费交易输出
     *
     * @param hash  交易hash
     * @param index 第几个output
     * @return 指定索引的未花费交易输出，查询不到交易hash对应的未花费交易输出或未花费交易输出的输出个数小于索引或处于被花费的状态的时候返回null
     */
    public TransactionOutput getUnspent(UInt256 hash, Ushort index) {
        UnspentCoinState state = getUnspentCoins().tryGet(hash);
        if (state == null) {
            return null;
        }
        if (index.intValue() >= state.items.length) {
            return null;
        }
        if (state.items[index.intValue()].hasFlag(CoinState.Spent)) {
            return null;
        }

        return getTransaction(hash).outputs[index.intValue()];
    }

    /**
     * 查询某一笔交易的未花费交易输出
     *
     * @param hash 交易Hash
     * @return 该交易所有的未花费交易输出
     */
    public Collection<TransactionOutput> getUnspent(UInt256 hash) {
        ArrayList<TransactionOutput> outputs = new ArrayList<>();
        UnspentCoinState state = getUnspentCoins().tryGet(hash);
        if (state != null) {
            Transaction tx = getTransaction(hash);
            for (int i = 0; i < state.items.length; i++) {
                if (!state.items[i].hasFlag(CoinState.Spent)) {
                    outputs.add(tx.outputs[i]);
                }
            }
        }
        return outputs;
    }

    /**
     * 检测交易是否需多重支付
     *
     * @param tx 交易hash
     * @return 若交易中input所指向的每一笔output都存在，且没有被花费掉，则返回false。否则返回true。
     */
    public boolean isDoubleSpend(Transaction tx) {
        if (tx.inputs.length == 0) return false;
        Map<UInt256, List<CoinReference>> groupMap = Arrays.stream(tx.inputs).collect(Collectors.groupingBy(p -> p.prevHash));
        for (Map.Entry<UInt256, List<CoinReference>> entry : groupMap.entrySet()) {
            UnspentCoinState state = getUnspentCoins().tryGet(entry.getKey());
            if (state == null) {
                return true;
            }
            for (CoinReference input : entry.getValue()) {
                if (input.prevIndex.intValue() >= state.items.length ||
                        state.items[input.prevIndex.intValue()].hasFlag(CoinState.Spent)) {
                    return true;
                }
            }
        }
        return false;
    }
}
