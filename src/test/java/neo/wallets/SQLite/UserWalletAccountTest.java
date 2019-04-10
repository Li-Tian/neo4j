package neo.wallets.SQLite;

import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

import neo.UInt160;
import neo.wallets.KeyPair;

public class UserWalletAccountTest {


    private byte[] getRandomPrivateKey() {
        byte[] privateKey = new byte[32];
        Random rng = new Random();
        rng.nextBytes(privateKey);
        return privateKey;
    }

    @Test
    public void hasKey() {
        KeyPair keyPair = new KeyPair(getRandomPrivateKey());
        UInt160 hash = UInt160.parseToScriptHash(neo.smartcontract.Contract.createSignatureRedeemScript(keyPair.publicKey));
        UserWalletAccount userWalletAccount = new UserWalletAccount(hash);
        userWalletAccount.key = keyPair;

        Assert.assertTrue(userWalletAccount.hasKey());
    }

    @Test
    public void getKey() {
        KeyPair keyPair = new KeyPair(getRandomPrivateKey());
        UInt160 hash = UInt160.parseToScriptHash(neo.smartcontract.Contract.createSignatureRedeemScript(keyPair.publicKey));
        UserWalletAccount userWalletAccount = new UserWalletAccount(hash);
        userWalletAccount.key = keyPair;

        Assert.assertEquals(keyPair, userWalletAccount.getKey());
    }

    @Test
    public void getAddress() {
        KeyPair keyPair = new KeyPair(getRandomPrivateKey());
        UInt160 hash = UInt160.parseToScriptHash(neo.smartcontract.Contract.createSignatureRedeemScript(keyPair.publicKey));
        UserWalletAccount userWalletAccount = new UserWalletAccount(hash);
        userWalletAccount.key = keyPair;

        Assert.assertEquals(hash.toAddress(), userWalletAccount.getAddress());
    }

    @Test
    public void watchOnly() {
        KeyPair keyPair = new KeyPair(getRandomPrivateKey());
        UInt160 hash = UInt160.parseToScriptHash(neo.smartcontract.Contract.createSignatureRedeemScript(keyPair.publicKey));
        UserWalletAccount userWalletAccount = new UserWalletAccount(hash);
        userWalletAccount.key = keyPair;

        Assert.assertEquals(true, userWalletAccount.watchOnly());
    }



}