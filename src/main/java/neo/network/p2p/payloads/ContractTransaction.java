package neo.network.p2p.payloads;

import neo.csharp.io.BinaryReader;
import neo.exception.FormatException;

public class ContractTransaction extends Transaction {

    public ContractTransaction() {
        super(TransactionType.ContractTransaction);
    }

    @Override
    protected void deserializeExclusiveData(BinaryReader reader) {
        if (version != 0) throw new FormatException();
    }
}
