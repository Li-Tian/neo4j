package neo.persistence;

import neo.network.p2p.payloads.Transaction;

public abstract class Snapshot implements IPersistence {

    public void commit() {

    }

    public boolean IsDoubleSpend(Transaction tx) {
        //TODO...
        return false;
    }
}
