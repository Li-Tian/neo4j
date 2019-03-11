package neo.network.p2p.payloads;

import neo.csharp.io.BinaryReader;
import neo.exception.FormatException;

/**
 * common transaction (not issuing a smart contract)
 */
public class ContractTransaction extends Transaction {

    /**
     * constructor
     */
    public ContractTransaction() {
        super(TransactionType.ContractTransaction);
    }

    /**
     * Deserialize method.No data was read.Only verify whether the transaction version number is 0
     *
     * @param reader BinaryReader
     * @throws FormatException the transaction version number is not 0
     */
    @Override
    protected void deserializeExclusiveData(BinaryReader reader) {
        if (version != 0) throw new FormatException();
    }
}
