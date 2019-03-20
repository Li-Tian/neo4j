package neo.plugins;

import neo.network.p2p.payloads.Transaction;

public interface IPolicyPlugin {
    boolean filterForMemoryPool(Transaction tx);

    Transaction[] filterForBlock(Transaction[] transactions);

    int maxTxPerBlock = 0;
    int maxLowPriorityTxPerBlock = 0;
}