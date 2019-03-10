package neo.persistence;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import neo.UInt256;
import neo.cryptography.ecc.ECPoint;
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

/**
 * Abstract persistence, provides business operations.
 */
public abstract class AbstractPersistence implements IPersistence {

    /**
     * Determine whether it contains specified block hash
     *
     * @param hash specified block hash
     * @return If it contains,return true.Otherwise,return false
     */
    public boolean containsBlock(UInt256 hash) {
        BlockState state = getBlocks().tryGet(hash);
        if (state == null) return false;
        return state.trimmedBlock.isBlock();
    }

    /**
     * Determine whether it contains specified transcation hash
     *
     * @param hash specified transcation hash
     * @return If it contains,return true.Otherwise,return false
     */
    public boolean containsTransaction(UInt256 hash) {
        TransactionState state = getTransactions().tryGet(hash);
        return state != null;
    }

    /**
     * Get the block of a specified height
     *
     * @param index block height
     * @return Returns the block corresponding to the specified height, or null if it does not exist
     */
    public Block getBlock(Uint index) {
        UInt256 hash = Blockchain.singleton().getBlockHash(index);
        if (hash == null) return null;
        return getBlock(hash);
    }

    /**
     * Get the block of a specified block hash
     *
     * @param hash specified block hash
     * @return Returns the block corresponding to the specified block hash,  otherwise null.
     */
    public Block getBlock(UInt256 hash) {
        BlockState state = getBlocks().tryGet(hash);
        if (state == null) return null;
        if (!state.trimmedBlock.isBlock()) return null;
        return state.trimmedBlock.getBlock(getTransactions());
    }

    /**
     * get a validator candidate list. Includes a list of verified candidates who had registered and
     * a list of stand validator. Does not include information on whether to be elected as a
     * validator.
     */
    public Collection<ValidatorState> getEnrollments() {
        HashSet<ECPoint> sv = new HashSet<>();
        for (ECPoint ecpoint : Blockchain.StandbyValidators) {
            sv.add(ecpoint);
        }
        return getValidators()
                .find()
                .stream()
                .map(p -> p.getValue())
                .filter(p -> p.registered || sv.contains(p.publicKey))
                .collect(Collectors.toList());
    }

    /**
     * Get the block header of a specified height
     *
     * @param index specified height of block header
     * @return specified block header.Return null if it does not exist
     */
    public Header getHeader(Uint index) {
        UInt256 hash = Blockchain.singleton().getBlockHash(index);
        if (hash == null) {
            return null;
        }
        return getHeader(hash);
    }

    /**
     * Get the block header of a specified block hash
     *
     * @param hash specified block header hash
     * @return specified block header. Return null if it does not exist
     */
    public Header getHeader(UInt256 hash) {
        BlockState state = getBlocks().tryGet(hash);
        if (state == null) {
            return null;
        }
        return state.trimmedBlock.getHeader();
    }

    /**
     * get next block hash
     *
     * @param hash current block hash
     * @return next block hash.Return null if it does not exist
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
     * Query total system fee from the block 0 to the specified block
     *
     * @param height specified block height
     * @return total system fee amount
     */
    public long getSysFeeAmount(Uint height) {
        UInt256 hash = Blockchain.singleton().getBlockHash(height);
        return getSysFeeAmount(hash);
    }

    /**
     * Query the total system fee from the block 0 to the specified hash location (including
     * specified block)
     *
     * @param hash specified block hash
     * @return total system fee amount
     */
    public long getSysFeeAmount(UInt256 hash) {
        BlockState block_state = getBlocks().tryGet(hash);
        if (block_state == null) return 0;
        return block_state.systemFeeAmount;
    }

    /**
     * Query transaction
     *
     * @param hash transaction hash
     * @return transaction output corresponding to transaction hash
     */
    public Transaction getTransaction(UInt256 hash) {
        TransactionState state = getTransactions().tryGet(hash);
        if (state == null) {
            return null;
        }
        return state.transaction;
    }

    /**
     * Query unspent transaction outputs of a transaction
     *
     * @param hash  transcation hash
     * @param index transcation output index
     * @return a unspent transaction output of specified index. Returns null if can not query the
     * unspent transaction outputs corresponding to transaction hash or the number of unspent
     * transaction output is less than the index or the unspent transaction output is in the state
     * of being spent.
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
     * Query  unspent transaction outputs of a transaction
     *
     * @param hash transcation hash
     * @return all unspent transcation outputs of the transaction
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
     * Check if the transaction is a multiple payment
     *
     * @param tx transaction hash
     * @return Returns false if each previous transcation output pointed to by current transaction
     * input exists and is not spent, otherwise,return true.
     */
    public boolean isDoubleSpend(Transaction tx) {
        if (tx.inputs.length == 0) return false;
        Map<UInt256, List<CoinReference>> groupMap = Arrays.stream(tx.inputs)
                .collect(Collectors.groupingBy(p -> p.prevHash));
        for (Map.Entry<UInt256, List<CoinReference>> entry : groupMap.entrySet()) {
            UnspentCoinState state = getUnspentCoins().tryGet(entry.getKey());
            if (state == null) {
                return true;
            }
            // C# code: if (group.Any(p => p.PrevIndex >= state.Items.Length || state.Items[p.PrevIndex].HasFlag(CoinState.Spent)))
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
