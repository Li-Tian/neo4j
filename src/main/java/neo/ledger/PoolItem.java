package neo.ledger;


import java.util.Date;

import neo.TimeProvider;
import neo.exception.InvalidOperationException;
import neo.network.p2p.payloads.ClaimTransaction;
import neo.network.p2p.payloads.Transaction;

/**
 * Represents an item in the Memory Pool.
 *
 * Note: PoolItem objects don't consider transaction priority (low or high) in their compare
 * CompareTo method. This is because items of differing priority are never added to the same sorted
 * set in MemoryPool.v
 */
public class PoolItem implements Comparable<Object> {

    /**
     * Internal transaction for PoolItem
     */
    public final Transaction tx;

    /**
     * Timestamp when transaction was stored on PoolItem
     */
    public final Date timestamp;

    /**
     * Timestamp when this transaction was last broadcast to other nodes
     */
    public Date lastBroadcastTimestamp;


    public PoolItem(Transaction tx) {
        this.tx = tx;
        timestamp = TimeProvider.current().utcNow();
        lastBroadcastTimestamp = timestamp;
    }

    @Override
    public int compareTo(Object o) {
        if (o == this) return 0;
        if (o == null) return 1;
        if (o instanceof Transaction) {
            Transaction otherTx = (Transaction) o;
            if (tx.isLowPriority() && otherTx.isLowPriority()) {
                boolean thisIsClaimX = tx instanceof ClaimTransaction;
                boolean otherIsClaimTx = otherTx instanceof ClaimTransaction;
                if (thisIsClaimX != otherIsClaimTx) {
                    if (thisIsClaimX) { // This is a claim Tx and other isn't.
                        return 1;
                    } else {        // The other is claim Tx and this isn't.
                        return -1;
                    }
                }
            }
            // Fees sorted ascending
            int ret = tx.getFeePerByte().compareTo(otherTx.getFeePerByte());
            if (ret != 0) {
                return ret;
            }
            ret = tx.getNetworkFee().compareTo(otherTx.getNetworkFee());
            if (ret != 0) {
                return ret;
            }
            // Transaction hash sorted descending
            return otherTx.hash().compareTo(tx.hash());
        }
        if (o instanceof PoolItem) {
            PoolItem otherItem = (PoolItem) o;
            return compareTo(otherItem.tx);
        }
        throw new InvalidOperationException();
    }
}
