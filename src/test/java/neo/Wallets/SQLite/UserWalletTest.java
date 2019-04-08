package neo.Wallets.SQLite;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import neo.Utils;
import neo.Wallets.KeyPair;
import neo.Wallets.SQLite.sqlitJDBC.DataAccessException;
import neo.Wallets.WalletAccount;
import neo.Wallets.WalletIndexer;
import neo.Wallets.WalletIndexerTest;
import neo.csharp.Uint;
import neo.persistence.AbstractBlockchainTest;

import static org.junit.Assert.*;

public class UserWalletTest extends AbstractBlockchainTest {

    private static WalletIndexer walletIndexer;

    private static UserWallet userWallet;
    private static WalletDataContext context;

    @BeforeClass
    public static void setup() throws IOException, DataAccessException {
        AbstractBlockchainTest.setUp(WalletIndexerTest.class.getSimpleName());

        String path = UserWalletTest.class.getClassLoader().getResource("").getPath() + "wallet_index_leveldb";
        Utils.deleteFolder(path);
        walletIndexer = new WalletIndexer(path);
        walletIndexer.dispose();

        String walletPath = UserWalletTest.class.getClassLoader().getResource("").getPath() + "user_wallet.db3";
//        Utils.deleteFile(walletPath);
//        userWallet = UserWallet.create(walletIndexer, walletPath, "123456");
        userWallet = UserWallet.open(walletIndexer, walletPath, "123456");
        context = new WalletDataContext(walletPath);
    }

    @AfterClass
    public static void tearDown() throws IOException, DataAccessException {
        walletIndexer.dispose();
        AbstractBlockchainTest.tearDown(WalletIndexerTest.class.getSimpleName());
//        context.deleteDB();
        userWallet.dispose();
    }

    @Test
    public void getWalletTransaction() {

    }

    @Test
    public void getName() {
        Assert.assertEquals("user_wallet.db3", userWallet.getName());
    }

    @Test
    public void getWalletHeight() {
        Assert.assertEquals(Uint.ZERO, userWallet.getWalletHeight());
    }

    @Test
    public void getVersion() {
        Assert.assertEquals("2.9.2.0", userWallet.getVersion().toString());
    }

    @Test
    public void applyTransaction() {


    }

    @Test
    public void changePassword() {

    }

    @Test
    public void contains() {
    }

    @Test
    public void create() {
    }

    @Test
    public void createAccount() {
        KeyPair keyPair1 = Utils.getRandomKeyPair();
//        WalletAccount account = userWallet.createAccount(keyPair1.privateKey);
    }

    @Test
    public void createAccount1() {
    }

    @Test
    public void createAccount2() {
    }

    @Test
    public void createAccount3() {
    }

    @Test
    public void deleteAccount() {
    }

    @Test
    public void dispose() {
    }

    @Test
    public void findUnspentCoins() {
    }

    @Test
    public void getAccount() {
    }

    @Test
    public void getAccounts() {
    }

    @Test
    public void getCoins() {
    }

    @Test
    public void getCoinsInternal() {
    }

    @Test
    public void getTransactions() {
    }

    @Test
    public void open() {
    }

    @Test
    public void verifyPassword() {
    }

    @Test
    public void doWork() {
    }
}