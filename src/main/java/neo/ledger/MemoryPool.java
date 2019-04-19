package neo.ledger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import neo.NeoSystem;
import neo.UInt256;
import neo.csharp.Out;
import neo.csharp.Uint;
import neo.log.tr.TR;
import neo.network.p2p.LocalNode;
import neo.network.p2p.payloads.Block;
import neo.network.p2p.payloads.Transaction;
import neo.persistence.Snapshot;
import neo.plugins.IMemoryPoolTxObserverPlugin;
import neo.plugins.IPolicyPlugin;
import neo.plugins.MemoryPoolTxRemovalReason;
import neo.plugins.Plugin;

public class MemoryPool {
    // Allow a reverified transaction to be rebroadcasted if it has been this many block times since last broadcast.
    private final int BlocksTillRebroadcastLowPriorityPoolTx = 30;
    private final int BlocksTillRebroadcastHighPriorityPoolTx = 10;

    private int rebroadcastMultiplierThreshold() {
        TR.enter();
        return TR.exit(capacity / 10);
    }

    private static final double MaxSecondsToReverifyHighPrioTx = (double) Blockchain.SecondsPerBlock / 3;
    private static final double MaxSecondsToReverifyLowPrioTx = (double) Blockchain.SecondsPerBlock / 5;

    // These two are not expected to be hit, they are just safegaurds.
    private static final double MaxSecondsToReverifyHighPrioTxPerIdle = (double) Blockchain.SecondsPerBlock / 15;
    private static final double MaxSecondsToReverifyLowPrioTxPerIdle = (double) Blockchain.SecondsPerBlock / 30;

    private final NeoSystem _system;

    /**
     * Guarantees consistency of the pool data structures.
     *
     * Note: The data structures are only modified from the `Blockchain` actor; so operations
     * guaranteed to be performed by the blockchain actor do not need to acquire the read lock; they
     * only need the write lock for write operations.
     */
    private final ReentrantReadWriteLock _txRwLock = new ReentrantReadWriteLock();

    /**
     * Store all verified unsorted transactions currently in the pool.
     */
    private final ConcurrentHashMap<UInt256, PoolItem> _unsortedTransactions = new ConcurrentHashMap<UInt256, PoolItem>();
    /**
     * Stores the verified high priority sorted transactins currently in the pool.
     */
    private final ConcurrentSkipListSet<PoolItem> _sortedHighPrioTransactions = new ConcurrentSkipListSet<PoolItem>();
    /**
     * Stores the verified low priority sorted transactions currently in the pool.
     */
    private final ConcurrentSkipListSet<PoolItem> _sortedLowPrioTransactions = new ConcurrentSkipListSet<PoolItem>();
    /**
     * Store the unverified transactions currently in the pool.
     *
     * Transactions in this data structure were valid in some prior block, but may no longer be
     * valid. The top ones that could make it into the next block get verified and moved into the
     * verified data structures (_unsortedTransactions, _sortedLowPrioTransactions, and
     * _sortedHighPrioTransactions) after each block.
     */
    private final ConcurrentHashMap<UInt256, PoolItem> _unverifiedTransactions = new ConcurrentHashMap<UInt256, PoolItem>();
    private final ConcurrentSkipListSet<PoolItem> _unverifiedSortedHighPriorityTransactions = new ConcurrentSkipListSet<PoolItem>();
    private final ConcurrentSkipListSet<PoolItem> _unverifiedSortedLowPriorityTransactions = new ConcurrentSkipListSet<PoolItem>();

    // Internal methods to aid in unit testing
    private int sortedHighPrioTxCount() {
        TR.enter();
        return TR.exit(_sortedHighPrioTransactions.size());
    }

    private int sortedLowPrioTxCount() {
        TR.enter();
        return TR.exit(_sortedLowPrioTransactions.size());
    }

    private int unverifiedSortedHighPrioTxCount() {
        TR.enter();
        return TR.exit(_unverifiedSortedHighPriorityTransactions.size());
    }

    private int unverifiedSortedLowPrioTxCount() {
        TR.enter();
        return TR.exit(_unverifiedSortedLowPriorityTransactions.size());
    }

    private int _maxTxPerBlock;
    private int _maxLowPriorityTxPerBlock;

    /**
     * Total maximum capacity of transactions the pool can hold.
     */
    private int capacity;

    public int getCapacity() {
        TR.enter();
        return TR.exit(capacity);
    }

    public int count() {
        try {
            TR.enter();
            _txRwLock.readLock().lock();
            return TR.exit(_unsortedTransactions.size() + _unverifiedTransactions.size());
        } finally {
            _txRwLock.readLock().unlock();
        }
    }

    /**
     * Total count of verified transactions in the pool.
     */
    public int verifiedCount() {
        try {
            TR.enter();
            _txRwLock.readLock().lock();
            return TR.exit(_unsortedTransactions.size());
        } finally {
            _txRwLock.readLock().unlock();
        }
    }

    public int unVerifiedCount() {
        try {
            TR.enter();
            _txRwLock.readLock().lock();
            return TR.exit(_unverifiedTransactions.size());
        } finally {
            _txRwLock.readLock().unlock();
        }
    }

    public MemoryPool(NeoSystem system, int capacity) {
        TR.enter();
        _system = system;
        this.capacity = capacity;
        loadMaxTxLimitsFromPolicyPlugins();
        TR.exit();
    }

    public void loadMaxTxLimitsFromPolicyPlugins() {
        TR.enter();
        _maxTxPerBlock = Integer.MAX_VALUE;
        _maxLowPriorityTxPerBlock = Integer.MAX_VALUE;
        for (IPolicyPlugin plugin : Plugin.getPolicies()) {
            _maxTxPerBlock = Math.min(_maxTxPerBlock, plugin.maxTxPerBlock());
            _maxLowPriorityTxPerBlock = Math.min(_maxLowPriorityTxPerBlock, plugin.maxLowPriorityTxPerBlock());
        }
        TR.exit();
    }

    /**
     * Determine whether the pool is holding this transaction and has at some point verified it.
     * Note: The pool may not have verified it since the last block was persisted. To get only the
     * transactions that have been verified during this block use GetVerifiedTransactions()
     *
     * @param hash the transaction hash
     * @return true if the MemoryPool contain the transaction
     */
    public boolean containsKey(UInt256 hash) {
        try {
            TR.enter();
            _txRwLock.readLock().lock();
            return TR.exit(_unsortedTransactions.containsKey(hash)
                    || _unverifiedTransactions.containsKey(hash));
        } finally {
            _txRwLock.readLock().unlock();
        }
    }

    public boolean tryGetValue(UInt256 hash, Out<Transaction> tx) {
        try {
            TR.enter();
            _txRwLock.readLock().lock();
            PoolItem unsortedItem = _unsortedTransactions.get(hash);
            PoolItem unverifieditem = _unverifiedTransactions.get(hash);
            if (unsortedItem != null) {
                tx.set(unsortedItem.tx);
                return TR.exit(true);
            } else if (unverifieditem != null) {
                tx.set(unverifieditem.tx);
                return TR.exit(true);
            } else {
                return TR.exit(false);
            }
        } finally {
            _txRwLock.readLock().unlock();
        }
    }

    // Note: This isn't used in Fill during consensus, fill uses GetSortedVerifiedTransactions()
    public Collection<Transaction> getEnumerator() {
        try {
            TR.enter();
            _txRwLock.readLock().lock();
            //return _unsortedTransactions.Select(p => p.Value.Tx)
            //                    .Concat(_unverifiedTransactions.Select(p => p.Value.Tx))
            //                    .ToList()
            //                    .GetEnumerator();
            Collection<Transaction> result = new ArrayList<Transaction>();
            _unsortedTransactions.forEach((p, q) -> result.add(q.tx));
            _unverifiedTransactions.forEach((p, q) -> result.add(q.tx));
            return TR.exit(result);
        } finally {
            _txRwLock.readLock().unlock();
        }
    }

    public Collection<Transaction> getVerifiedTransactions() {
        try {
            TR.enter();
            _txRwLock.readLock().lock();
            //return _unsortedTransactions.Select(p = > p.Value.Tx).ToArray();
            Collection<Transaction> result = new ArrayList<Transaction>();
            _unsortedTransactions.forEach((p, q) -> result.add(q.tx));
            return TR.exit(result);
        } finally {
            _txRwLock.readLock().unlock();
        }
    }

    public void getVerifiedAndUnverifiedTransactions(Out<Collection<Transaction>> verifiedTransactions,
                                                     Out<Collection<Transaction>> unverifiedTransactions) {
        try {
            TR.enter();
            _txRwLock.readLock().lock();
            //verifiedTransactions = _sortedHighPrioTransactions.Reverse().Select(p = > p.Tx)
            //                    .Concat(_sortedLowPrioTransactions.Reverse().Select(p = > p.Tx)).ToArray();
            Collection<Transaction> verifiedResult = new ArrayList<Transaction>();
            _sortedHighPrioTransactions.descendingSet().forEach(p -> verifiedResult.add(p.tx));
            _sortedLowPrioTransactions.descendingSet().forEach(p -> verifiedResult.add(p.tx));
            verifiedTransactions.set(verifiedResult);
            //unverifiedTransactions = _unverifiedSortedHighPriorityTransactions.Reverse().Select(p = > p.Tx)
            //                    .Concat(_unverifiedSortedLowPriorityTransactions.Reverse().Select(p = > p.Tx)).
            Collection<Transaction> unVerifiedResult = new ArrayList<Transaction>();
            _unverifiedSortedHighPriorityTransactions.descendingSet().forEach(p -> unVerifiedResult.add(p.tx));
            _unverifiedSortedLowPriorityTransactions.descendingSet().forEach(p -> unVerifiedResult.add(p.tx));
            unverifiedTransactions.set(unVerifiedResult);
            TR.exit();
        } finally {
            _txRwLock.readLock().unlock();
        }
    }

    public Collection<Transaction> getSortedVerifiedTransactions() {
        try {
            TR.enter();
            _txRwLock.readLock().lock();
            //return _sortedHighPrioTransactions.Reverse().Select(p = > p.Tx)
            //            .Concat(_sortedLowPrioTransactions.Reverse().Select(p = > p.Tx)).ToArray();
            Collection<Transaction> result = new ArrayList<Transaction>();
            _sortedHighPrioTransactions.descendingSet().forEach(p -> result.add(p.tx));
            _sortedLowPrioTransactions.descendingSet().forEach(p -> result.add(p.tx));
            return TR.exit(result);
        } finally {
            _txRwLock.readLock().unlock();
        }
    }

    private PoolItem getLowestFeeTransaction(ConcurrentSkipListSet<PoolItem> verifiedTxSorted,
                                             ConcurrentSkipListSet<PoolItem> unverifiedTxSorted,
                                             Out<ConcurrentSkipListSet<PoolItem>> sortedPool) {
        TR.enter();
        PoolItem minItem = unverifiedTxSorted.isEmpty() ? null : unverifiedTxSorted.first();
        sortedPool.set(minItem != null ? unverifiedTxSorted : null);

        PoolItem verifiedMin = verifiedTxSorted.isEmpty() ? null : verifiedTxSorted.first();
        if (verifiedMin == null) {
            return TR.exit(minItem);
        }

        if (minItem != null && verifiedMin.compareTo(minItem) >= 0) {
            return TR.exit(minItem);
        }

        sortedPool.set(verifiedTxSorted);
        minItem = verifiedMin;

        return TR.exit(minItem);
    }

    private PoolItem getLowestFeeTransaction(Out<ConcurrentHashMap<UInt256, PoolItem>> unsortedTxPool,
                                             Out<ConcurrentSkipListSet<PoolItem>> sortedPool) {
        PoolItem minItem = getLowestFeeTransaction(_sortedLowPrioTransactions,
                _unverifiedSortedLowPriorityTransactions, sortedPool);

        if (minItem != null) {
            unsortedTxPool.set(sortedPool.get() == _unverifiedSortedLowPriorityTransactions
                    ? _unverifiedTransactions : _unsortedTransactions);
            return TR.exit(minItem);
        }

        try {
            return TR.exit(getLowestFeeTransaction(_sortedHighPrioTransactions, _unverifiedSortedHighPriorityTransactions,
                    sortedPool));
        } finally {
            unsortedTxPool.set(sortedPool.get() == _unverifiedSortedHighPriorityTransactions
                    ? _unverifiedTransactions : _unsortedTransactions);
        }
    }

    // Note: this must only be called from a single thread (the Blockchain actor)
    public boolean canTransactionFitInPool(Transaction tx) {
        TR.enter();
        if (count() < capacity) {
            return TR.exit(true);
        }
        Out<ConcurrentHashMap<UInt256, PoolItem>> unsortedTxPool = new Out<>();
        Out<ConcurrentSkipListSet<PoolItem>> sortedPool = new Out<>();
        return TR.exit(getLowestFeeTransaction(unsortedTxPool, sortedPool).compareTo(tx) <= 0);
    }

    /**
     * Adds an already verified transaction to the memory pool.
     *
     * Note: This must only be called from a single thread (the Blockchain actor). To add a
     * transaction to the pool tell the Blockchain actor about the transaction.
     */
    public boolean tryAdd(UInt256 hash, Transaction tx) {
        TR.enter();
        PoolItem poolItem = new PoolItem(tx);
        if (_unsortedTransactions.containsKey(hash)) {
            return TR.exit(false);
        }

        ArrayList<Transaction> removedTransactions = null;
        try {
            _txRwLock.writeLock().lock();
            _unsortedTransactions.put(hash, poolItem);
            ConcurrentSkipListSet<PoolItem> pool = tx.isLowPriority() ? _sortedLowPrioTransactions : _sortedHighPrioTransactions;
            pool.add(poolItem);
            if (count() > capacity)
                removedTransactions = removeOverCapacity();
        } finally {
            _txRwLock.writeLock().unlock();
        }

        for (IMemoryPoolTxObserverPlugin plugin : Plugin.getTXObserverPlugins()) {
            plugin.transactionAdded(poolItem.tx);
            if (removedTransactions != null) {
                plugin.transactionsRemoved(MemoryPoolTxRemovalReason.CapacityExceeded, removedTransactions);
            }
        }
        return TR.exit(_unsortedTransactions.containsKey(hash));
    }

    private ArrayList<Transaction> removeOverCapacity() {
        TR.enter();
        ArrayList<Transaction> removedTransactions = new ArrayList<Transaction>();
        do {
            Out<ConcurrentHashMap<UInt256, PoolItem>> unsortedPool = new Out<>();
            Out<ConcurrentSkipListSet<PoolItem>> sortedPool = new Out<>();
            PoolItem minItem = getLowestFeeTransaction(unsortedPool, sortedPool);

            unsortedPool.get().remove(minItem.tx.hash());
            sortedPool.get().remove(minItem);
            removedTransactions.add(minItem.tx);
        } while (count() > capacity);

        return TR.exit(removedTransactions);
    }

    private boolean tryRemoveVerified(UInt256 hash, Out<PoolItem> item) {
        TR.enter();
        item.set(_unsortedTransactions.get(hash));
        if (item.get() == null) {
            return TR.exit(false);
        }
        _unsortedTransactions.remove(hash);
        ConcurrentSkipListSet<PoolItem> pool = item.get().tx.isLowPriority()
                ? _sortedLowPrioTransactions : _sortedHighPrioTransactions;
        pool.remove(item.get());
        return TR.exit(true);
    }

    private boolean tryRemoveUnVerified(UInt256 hash, Out<PoolItem> item) {
        TR.enter();
        item.set(_unverifiedTransactions.get(hash));
        if (item.get() == null) {
            return TR.exit(false);
        }

        _unverifiedTransactions.remove(hash);
        ConcurrentSkipListSet<PoolItem> pool = item.get().tx.isLowPriority()
                ? _unverifiedSortedLowPriorityTransactions : _unverifiedSortedHighPriorityTransactions;
        pool.remove(item.get());
        return TR.exit(true);
    }

    // Note: this must only be called from a single thread (the Blockchain actor)
    public void updatePoolForBlockPersisted(Block block, Snapshot snapshot) {
        try {
            TR.enter();
            _txRwLock.writeLock().lock();
            // First remove the transactions verified in the block.
            for (Transaction tx : block.transactions) {
                if (tryRemoveVerified(tx.hash(), new Out<PoolItem>())) {
                    continue;
                }
                tryRemoveUnVerified(tx.hash(), new Out<PoolItem>());
            }

            // Add all the previously verified transactions back to the unverified transactions
            for (PoolItem item : _sortedHighPrioTransactions) {
                UInt256 hash = item.tx.hash();
                if (!_unverifiedTransactions.containsKey(hash)) {
                    _unverifiedTransactions.put(hash, item);
                    _unverifiedSortedHighPriorityTransactions.add(item);
                }
            }

            for (PoolItem item : _sortedLowPrioTransactions) {
                UInt256 hash = item.tx.hash();
                if (!_unverifiedTransactions.containsKey(hash)) {
                    _unverifiedTransactions.put(hash, item);
                    _unverifiedSortedLowPriorityTransactions.add(item);
                }
            }

            // Clear the verified transactions now, since they all must be reverified.
            _unsortedTransactions.clear();
            _sortedHighPrioTransactions.clear();
            _sortedLowPrioTransactions.clear();
        } finally {
            _txRwLock.writeLock().unlock();
        }

        // If we know about headers of future blocks, no point in verifying transactions from the unverified tx pool
        // until we get caught up.
        if (block.index.compareTo(Uint.ZERO) > 0 && block.index.compareTo(Blockchain.singleton().headerHeight()) < 0) {
            TR.exit();
            return;
        }

        if (Plugin.getPolicies().size() == 0) {
            TR.exit();
            return;
        }

        loadMaxTxLimitsFromPolicyPlugins();

        reverifyTransactions(_sortedHighPrioTransactions, _unverifiedSortedHighPriorityTransactions,
                _maxTxPerBlock, MaxSecondsToReverifyHighPrioTx, snapshot);
        reverifyTransactions(_sortedLowPrioTransactions, _unverifiedSortedLowPriorityTransactions,
                _maxLowPriorityTxPerBlock, MaxSecondsToReverifyLowPrioTx, snapshot);
        TR.exit();
    }

    private int reverifyTransactions(ConcurrentSkipListSet<PoolItem> verifiedSortedTxPool,
                                     ConcurrentSkipListSet<PoolItem> unverifiedSortedTxPool,
                                     int count, double secondsTimeout, Snapshot snapshot) {
        TR.enter();
        Date reverifyCutOffTimeStamp = new Date(System.currentTimeMillis() - (long) secondsTimeout * 1000);
        ArrayList<PoolItem> reverifiedItems = new ArrayList<PoolItem>(count);
        ArrayList<PoolItem> invalidItems = new ArrayList<PoolItem>();

        // Since unverifiedSortedTxPool is ordered in an ascending manner, we take from the end.
        for (PoolItem item : Arrays.copyOf(unverifiedSortedTxPool.descendingSet().toArray(new PoolItem[unverifiedSortedTxPool.size()]), Math.min(unverifiedSortedTxPool.size(), count))) {
            ArrayList<Transaction> transactions = new ArrayList<Transaction>();
            _unsortedTransactions.forEach((p, q) -> transactions.add(q.tx));
            if (item.tx.verify(snapshot, transactions)) {
                reverifiedItems.add(item);
            } else {
                // Transaction no longer valid -- it will be removed from unverifiedTxPool.
                invalidItems.add(item);
            }
            if (new Date(System.currentTimeMillis()).compareTo(reverifyCutOffTimeStamp) > 0) {
                break;
            }
        }

        try {
            _txRwLock.writeLock().lock();
            int blocksTillRebroadcast = unverifiedSortedTxPool == _sortedHighPrioTransactions
                    ? BlocksTillRebroadcastHighPriorityPoolTx : BlocksTillRebroadcastLowPriorityPoolTx;

            if (count() > rebroadcastMultiplierThreshold()) {
                blocksTillRebroadcast = blocksTillRebroadcast * count() / rebroadcastMultiplierThreshold();
            }

            Date rebroadcastCutOffTime = new Date(System.currentTimeMillis()
                    - Blockchain.SecondsPerBlock * blocksTillRebroadcast * 1000);
            for (PoolItem item : reverifiedItems) {
                UInt256 hash = item.tx.hash();
                if (!_unsortedTransactions.containsKey(hash)) {
                    _unsortedTransactions.put(hash, item);
                    verifiedSortedTxPool.add(item);

                    if (item.lastBroadcastTimestamp.compareTo(rebroadcastCutOffTime) < 0) {
                        _system.localNode.tell(new LocalNode.RelayDirectly(item.tx), _system.blockchain);
                        item.lastBroadcastTimestamp = new Date(System.currentTimeMillis());
                    }
                }

                _unverifiedTransactions.remove(item.tx.hash());
                unverifiedSortedTxPool.remove(item);
            }

            for (PoolItem item : invalidItems) {
                _unverifiedTransactions.remove(item.tx.hash());
                unverifiedSortedTxPool.remove(item);
            }
        } finally {
            _txRwLock.writeLock().unlock();
        }
        //var invalidTransactions = invalidItems.Select(p => p.Tx).ToArray();
        ArrayList<Transaction> invalidTransactions = new ArrayList<Transaction>();
        invalidItems.forEach(p -> invalidTransactions.add(p.tx));
        for (IMemoryPoolTxObserverPlugin plugin : Plugin.getTXObserverPlugins()) {
            plugin.transactionsRemoved(MemoryPoolTxRemovalReason.NoLongerValid, invalidTransactions);
        }

        return TR.exit(reverifiedItems.size());
    }

    /**
     * Reverify up to a given maximum count of transactions. Verifies less at a time once the max
     * that can be persisted per block has been reached.
     *
     * Note: this must only be called from a single thread (the Blockchain actor)
     *
     * @param maxToVerify: Max transactions to reverify, the value passed should be >=2. If 1 is
     *                     passed it will still potentially use 2.
     * @param snapshot:    The snapshot to use for verifying.
     * @return: true if more unsorted messages exist, otherwise false
     */
    public boolean reVerifyTopUnverifiedTransactionsIfNeeded(int maxToVerify, Snapshot snapshot) {
        TR.enter();
        if (Blockchain.singleton().height().compareTo(Blockchain.singleton().headerHeight()) < 0) {
            return TR.exit(false);
        }

        if (_unverifiedSortedHighPriorityTransactions.size() > 0) {
            // Always leave at least 1 tx for low priority tx
            int verifyCount = _sortedHighPrioTransactions.size() > _maxTxPerBlock || maxToVerify == 1
                    ? 1 : maxToVerify - 1;
            maxToVerify -= reverifyTransactions(_sortedHighPrioTransactions,
                    _unverifiedSortedHighPriorityTransactions,
                    verifyCount, MaxSecondsToReverifyHighPrioTxPerIdle, snapshot);

            if (maxToVerify == 0) {
                maxToVerify++;
            }
        }

        if (_unverifiedSortedLowPriorityTransactions.size() > 0) {
            int verifyCount = _sortedLowPrioTransactions.size() > _maxLowPriorityTxPerBlock
                    ? 1 : maxToVerify;
            reverifyTransactions(_sortedLowPrioTransactions, _unverifiedSortedLowPriorityTransactions,
                    verifyCount, MaxSecondsToReverifyLowPrioTxPerIdle, snapshot);
        }

        return TR.exit(_unverifiedTransactions.size() > 0);
    }
}