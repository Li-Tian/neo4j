package neo.Wallets.SQLite;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class ContractTest {

    @Test
    public void testGetMethod() {
        Contract contract = new Contract();
        contract.setScriptHash("script hash".getBytes());
        contract.setPublicKeyHash("public key".getBytes());
        contract.setRawData("contract raw data".getBytes());

        Assert.assertArrayEquals("script hash".getBytes(), contract.getScriptHash());
        Assert.assertArrayEquals("public key".getBytes(), contract.getPublicKeyHash());
        Assert.assertArrayEquals("contract raw data".getBytes(), contract.getRawData());
    }

}