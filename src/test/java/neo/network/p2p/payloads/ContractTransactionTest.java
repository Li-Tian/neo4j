package neo.network.p2p.payloads;

import org.junit.Assert;
import org.junit.Test;

import neo.Utils;
import neo.exception.FormatException;

public class ContractTransactionTest {

    @Test(expected = FormatException.class)
    public void deserializeExclusiveData() {
        ContractTransaction transaction = new ContractTransaction();
        transaction.version = 0;
        try {
            Utils.copyFromSerialize(transaction, ContractTransaction::new);
        } catch (Exception e) {
            Assert.fail();
        }
        transaction.version = 1;
        Utils.copyFromSerialize(transaction, ContractTransaction::new);
    }
}