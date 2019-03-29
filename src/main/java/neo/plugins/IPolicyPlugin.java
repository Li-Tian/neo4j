package neo.plugins;

import java.util.Collection;

import neo.network.p2p.payloads.Transaction;

public interface IPolicyPlugin {
    boolean filterForMemoryPool(Transaction tx);

    Collection<Transaction> filterForBlock(Collection<Transaction> transactions);

    int maxTxPerBlock();
    int maxLowPriorityTxPerBlock();
}