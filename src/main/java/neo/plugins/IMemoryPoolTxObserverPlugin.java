package neo.plugins;

import java.util.ArrayList;

import neo.network.p2p.payloads.Transaction;

public interface IMemoryPoolTxObserverPlugin {
    void transactionAdded(Transaction tx);

    void transactionsRemoved(MemoryPoolTxRemovalReason reason, ArrayList<Transaction> transactions);
}