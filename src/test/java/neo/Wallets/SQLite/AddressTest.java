package neo.Wallets.SQLite;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class AddressTest {

    @Test
    public void getScriptHash() {
        Address address = new Address();
        address.setScriptHash("script hash".getBytes());
        Assert.assertArrayEquals("script hash".getBytes(), address.getScriptHash());
    }
}