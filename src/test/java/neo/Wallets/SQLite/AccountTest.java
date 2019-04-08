package neo.Wallets.SQLite;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class AccountTest {

    @Test
    public void getPrivateKeyEncrypted() {
        Account account = new Account();
        account.setPrivateKeyEncrypted("private key".getBytes());
        Assert.assertArrayEquals(account.getPrivateKeyEncrypted(), "private key".getBytes());
    }

    @Test
    public void getPublicKeyHash() {
        Account account = new Account();
        account.setPublicKeyHash("public key".getBytes());
        Assert.assertArrayEquals(account.getPublicKeyHash(), "public key".getBytes());
    }
}