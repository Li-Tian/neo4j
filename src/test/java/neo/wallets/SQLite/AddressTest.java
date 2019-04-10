package neo.wallets.SQLite;

import org.junit.Assert;
import org.junit.Test;

public class AddressTest {

    @Test
    public void getScriptHash() {
        Address address = new Address();
        address.setScriptHash("script hash".getBytes());
        Assert.assertArrayEquals("script hash".getBytes(), address.getScriptHash());
    }
}