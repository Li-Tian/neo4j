package neo.ledger;

import neo.log.notr.TR;
import neo.network.p2p.payloads.ClaimTransaction;
import neo.network.p2p.payloads.Transaction;
import neo.network.p2p.payloads.TransactionType;
import neo.persistence.Snapshot;

public class MyTransaction extends Transaction {
    public MyTransaction() {
        super(TransactionType.ClaimTransaction, ClaimTransaction::new);
    }
    @Override
    public boolean verify(Snapshot snapshot) {
        TR.enter();
        return TR.exit(true);
    }
}
