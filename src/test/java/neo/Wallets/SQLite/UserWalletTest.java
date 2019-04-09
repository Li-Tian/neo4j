package neo.Wallets.SQLite;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import neo.Fixed8;
import neo.UInt160;
import neo.UInt256;
import neo.Utils;
import neo.Wallets.Coin;
import neo.Wallets.KeyPair;
import neo.Wallets.SQLite.sqlitJDBC.DataAccessException;
import neo.Wallets.Wallet;
import neo.Wallets.WalletAccount;
import neo.Wallets.WalletIndexer;
import neo.Wallets.WalletIndexerTest;
import neo.Wallets.WalletTransactionEventArgs;
import neo.cryptography.Helper;
import neo.cryptography.ecc.ECC;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.ledger.Blockchain;
import neo.network.p2p.payloads.ContractTransaction;
import neo.network.p2p.payloads.TransactionOutput;
import neo.network.p2p.payloads.Witness;
import neo.persistence.AbstractBlockchainTest;
import neo.smartcontract.ContractParameterType;
import neo.smartcontract.EventHandler;

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

        String walletPath = UserWalletTest.class.getClassLoader().getResource("").getPath() + "user_wallet.db3";

        Utils.deleteFile(walletPath);
        userWallet = UserWallet.create(walletIndexer, walletPath, "123456");
        userWallet = UserWallet.open(walletIndexer, walletPath, "123456");
        context = new WalletDataContext(walletPath);
    }

    @AfterClass
    public static void tearDown() throws IOException, DataAccessException {
        walletIndexer.dispose();
        AbstractBlockchainTest.tearDown(WalletIndexerTest.class.getSimpleName());
        context.deleteDB();
        userWallet.dispose();
    }

    @Test
    public void getWalletTransaction() {
        EventHandler<WalletTransactionEventArgs> eventHandler = userWallet.getWalletTransaction();
        Assert.assertNotNull(eventHandler);
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
        KeyPair keyPair1 = Utils.getRandomKeyPair();
        WalletAccount walletAccount1 = userWallet.createAccount(keyPair1.privateKey);

        KeyPair keyPair2 = Utils.getRandomKeyPair();

        KeyPair keyPair3 = Utils.getRandomKeyPair();
        WalletAccount walletAccount3 = userWallet.createAccount(keyPair3.privateKey);

        ContractTransaction tx = new ContractTransaction() {{
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                        scriptHash = walletAccount1.scriptHash;
                    }},
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                        scriptHash = UInt160.parseToScriptHash(neo.smartcontract.Contract.createSignatureRedeemScript(keyPair2.publicKey));
                    }}
            };
            witnesses = new Witness[]{
                    new Witness() {{
                        invocationScript = new byte[0];
                        verificationScript = neo.smartcontract.Contract.createSignatureRedeemScript(keyPair3.publicKey);
                    }}
            };
        }};

        EventHandler<WalletTransactionEventArgs> eventHandler = userWallet.getWalletTransaction();
        EventHandler.Listener<WalletTransactionEventArgs> listener = (sender, eventArgs) -> {
            Assert.assertEquals(tx.hash(), eventArgs.transaction.hash());

            Assert.assertEquals(2, eventArgs.relatedAccounts.length);
            Assert.assertEquals(walletAccount1.scriptHash, eventArgs.relatedAccounts[0]);
            Assert.assertEquals(walletAccount3.scriptHash, eventArgs.relatedAccounts[1]);
        };
        eventHandler.addListener(listener);

        userWallet.applyTransaction(tx);
        eventHandler.removeListener(listener);

        // clear tx
        WalletTransactionEventArgs eventArgs = new WalletTransactionEventArgs();
        eventArgs.relatedAccounts = new UInt160[]{walletAccount1.scriptHash, walletAccount3.scriptHash};
        eventArgs.transaction = tx;
        userWallet.doWork(null, eventArgs);

        userWallet.deleteAccount(walletAccount1.scriptHash);
        userWallet.deleteAccount(walletAccount3.scriptHash);
    }

    @Test
    public void changePassword() {
//        byte[] publicKeys = userWallet.getPrivateKeyFromNEP2("6PYRRzERjWWPCqwCCt8C86YMzawGkaTqrQ8wWeD7AUjMW1EBQccie15jUF", "1234567890");

        Assert.assertEquals(true, userWallet.changePassword("123456", "1345"));
        Assert.assertEquals(false, userWallet.verifyPassword("123456"));
        Assert.assertEquals(true, userWallet.changePassword("1345", "123456"));
    }

    @Test
    public void contains() {
        KeyPair keyPair1 = Utils.getRandomKeyPair();
        WalletAccount walletAccount1 = userWallet.createAccount(keyPair1.privateKey);

        KeyPair keyPair2 = Utils.getRandomKeyPair();
        UInt160 hash2 = UInt160.parseToScriptHash(neo.smartcontract.Contract.createSignatureRedeemScript(keyPair2.publicKey));

        KeyPair keyPair3 = Utils.getRandomKeyPair();
        WalletAccount walletAccount3 = userWallet.createAccount(keyPair3.privateKey);

        Assert.assertEquals(true, userWallet.contains(walletAccount1.scriptHash));
        Assert.assertEquals(false, userWallet.contains(hash2));
        Assert.assertEquals(true, userWallet.contains(walletAccount3.scriptHash));

        userWallet.deleteAccount(UInt160.parseToScriptHash(neo.smartcontract.Contract.createSignatureRedeemScript(keyPair1.publicKey)));
        userWallet.deleteAccount(UInt160.parseToScriptHash(neo.smartcontract.Contract.createSignatureRedeemScript(keyPair3.publicKey)));

    }


    @Test
    public void createAccount() throws DataAccessException {
        KeyPair keyPair1 = Utils.getRandomKeyPair();
        WalletAccount walletAccount = userWallet.createAccount(keyPair1.privateKey);

        // get account
        Account account = context.firstOrDefaultAccount(walletAccount.getKey().getPublicKeyHash().toArray());
        Assert.assertNotNull(account);
        Assert.assertArrayEquals(walletAccount.getKey().getPublicKeyHash().toArray(), account.getPublicKeyHash());

        byte[] decryptedPrivateKey = new byte[96];
        System.arraycopy(keyPair1.publicKey.getEncoded(false), 1,
                decryptedPrivateKey, 0, 64);
        System.arraycopy(keyPair1.privateKey, 0, decryptedPrivateKey, 64, 32);
        byte[] encryptedPrivateKey = userWallet.encryptPrivateKey(decryptedPrivateKey);
        Arrays.fill(decryptedPrivateKey, 0, decryptedPrivateKey.length, (byte) 0x00);
        Assert.assertArrayEquals(encryptedPrivateKey, account.getPrivateKeyEncrypted());

        // get contract
        Contract contract = context.firstOrDefaultContract(walletAccount.contract.scriptHash().toArray());
        Assert.assertNotNull(contract);
        Assert.assertArrayEquals(keyPair1.getPublicKeyHash().toArray(), contract.publicKeyHash);

        // get address
        Address address = context.firstOrDefaultAddress(walletAccount.contract.scriptHash().toArray());
        Assert.assertNotNull(address);

        // clear data
        userWallet.deleteAccount(UInt160.parseToScriptHash(neo.smartcontract.Contract.createSignatureRedeemScript(keyPair1.publicKey)));
    }

    @Test
    public void createAccount1() throws DataAccessException {
        KeyPair keyPair1 = Utils.getRandomKeyPair();

        VerificationContract verification_contract = new VerificationContract();
        verification_contract.script = neo.smartcontract.Contract.createSignatureRedeemScript(keyPair1.publicKey);
        verification_contract.parameterList = new ContractParameterType[]{ContractParameterType.Signature};

        WalletAccount walletAccount = userWallet.createAccount(verification_contract, keyPair1);

        // get account
        Account account = context.firstOrDefaultAccount(walletAccount.getKey().getPublicKeyHash().toArray());
        Assert.assertNotNull(account);
        Assert.assertArrayEquals(walletAccount.getKey().getPublicKeyHash().toArray(), account.getPublicKeyHash());

        byte[] decryptedPrivateKey = new byte[96];
        System.arraycopy(keyPair1.publicKey.getEncoded(false), 1,
                decryptedPrivateKey, 0, 64);
        System.arraycopy(keyPair1.privateKey, 0, decryptedPrivateKey, 64, 32);
        byte[] encryptedPrivateKey = userWallet.encryptPrivateKey(decryptedPrivateKey);
        Arrays.fill(decryptedPrivateKey, 0, decryptedPrivateKey.length, (byte) 0x00);
        Assert.assertArrayEquals(encryptedPrivateKey, account.getPrivateKeyEncrypted());

        // get contract
        Contract contract = context.firstOrDefaultContract(walletAccount.contract.scriptHash().toArray());
        Assert.assertNotNull(contract);
        Assert.assertArrayEquals(keyPair1.getPublicKeyHash().toArray(), contract.publicKeyHash);

        // get address
        Address address = context.firstOrDefaultAddress(walletAccount.contract.scriptHash().toArray());
        Assert.assertNotNull(address);

        // clear data
        userWallet.deleteAccount(verification_contract.scriptHash());
    }

    @Test
    public void createAccount2() throws DataAccessException {
//        KeyPair keyPair1 = Utils.getRandomKeyPair();
//
//        VerificationContract verification_contract = new VerificationContract();
//        verification_contract.script = neo.smartcontract.Contract.createSignatureRedeemScript(keyPair1.publicKey);
//        verification_contract.parameterList = new ContractParameterType[]{ContractParameterType.Signature};
//
//        WalletAccount walletAccount = userWallet.createAccount(verification_contract, keyPair1);
//        walletAccount = userWallet.createAccount(verification_contract);
//
//        // get account
//        Account account = context.firstOrDefaultAccount(walletAccount.getKey().getPublicKeyHash().toArray());
//        Assert.assertNotNull(account);
//        Assert.assertArrayEquals(walletAccount.getKey().getPublicKeyHash().toArray(), account.getPublicKeyHash());
//
//        byte[] decryptedPrivateKey = new byte[96];
//        System.arraycopy(keyPair1.publicKey.getEncoded(false), 1,
//                decryptedPrivateKey, 0, 64);
//        System.arraycopy(keyPair1.privateKey, 0, decryptedPrivateKey, 64, 32);
//        byte[] encryptedPrivateKey = userWallet.encryptPrivateKey(decryptedPrivateKey);
//        Assert.assertArrayEquals(encryptedPrivateKey, account.getPrivateKeyEncrypted());
//
//        // get contract
//        Contract contract = context.firstOrDefaultContract(walletAccount.contract.scriptHash().toArray());
//        Assert.assertNotNull(contract);
//        Assert.assertArrayEquals(keyPair1.getPublicKeyHash().toArray(), contract.publicKeyHash);
//
//        // get address
//        Address address = context.firstOrDefaultAddress(walletAccount.contract.scriptHash().toArray());
//        Assert.assertNotNull(address);
        // TODO UserWallet.createAccount(Contract) will throw NullPointerException
    }

    @Test
    public void createAccount3() {
//        KeyPair keyPair1 = Utils.getRandomKeyPair();
//        UInt160 scriptHash = UInt160.parseToScriptHash(neo.smartcontract.Contract.createSignatureRedeemScript(keyPair1.publicKey));
//
//        userWallet.createAccount(scriptHash);

        // TODO UserWallet.createAccount(scriptHash) will throw NullPointerException
    }

    @Test
    public void deleteAccount() throws DataAccessException {
        KeyPair keyPair1 = Utils.getRandomKeyPair();
        WalletAccount walletAccount = userWallet.createAccount(keyPair1.privateKey);
        boolean success = userWallet.deleteAccount(walletAccount.scriptHash);
        Assert.assertTrue(success);

        // get account
        Account account = context.firstOrDefaultAccount(walletAccount.getKey().getPublicKeyHash().toArray());
        Assert.assertNull(account);

        // get contract
        Contract contract = context.firstOrDefaultContract(walletAccount.contract.scriptHash().toArray());
        Assert.assertNull(contract);

        // get address
        Address address = context.firstOrDefaultAddress(walletAccount.contract.scriptHash().toArray());
        Assert.assertNull(address);

        // clear data
        userWallet.deleteAccount(walletAccount.scriptHash);
    }

    @Test
    public void findUnspentCoins() {
        KeyPair keyPair1 = Utils.getRandomKeyPair();
        WalletAccount walletAccount1 = userWallet.createAccount(keyPair1.privateKey);

        KeyPair keyPair2 = Utils.getRandomKeyPair();
        WalletAccount walletAccount2 = userWallet.createAccount(keyPair2.privateKey);


        KeyPair keyPair3 = Utils.getRandomKeyPair();
        WalletAccount walletAccount3 = userWallet.createAccount(keyPair3.privateKey);

        // add tx into unconfirmed
        ContractTransaction tx = new ContractTransaction() {{
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                        scriptHash = walletAccount1.scriptHash;
                    }},
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                        scriptHash = UInt160.parseToScriptHash(neo.smartcontract.Contract.createSignatureRedeemScript(keyPair2.publicKey));
                    }}
            };
            witnesses = new Witness[]{
                    new Witness() {{
                        invocationScript = new byte[0];
                        verificationScript = neo.smartcontract.Contract.createSignatureRedeemScript(keyPair3.publicKey);
                    }}
            };
        }};
        userWallet.applyTransaction(tx);

        // getCoin -> getCoinsInternal
        Iterable<Coin> iterable = userWallet.findUnspentCoins(walletAccount1.scriptHash, walletAccount2.scriptHash, walletAccount3.scriptHash);
        Iterator<Coin> iterator = iterable.iterator();

        // unconfirmed will be ignored
        Assert.assertEquals(false, iterator.hasNext());

        // clear data
        WalletTransactionEventArgs eventArgs = new WalletTransactionEventArgs();
        eventArgs.relatedAccounts = new UInt160[]{walletAccount1.scriptHash, walletAccount2.scriptHash, walletAccount3.scriptHash};
        eventArgs.transaction = tx;
        userWallet.doWork(null, eventArgs);

        userWallet.deleteAccount(walletAccount1.scriptHash);
        userWallet.deleteAccount(walletAccount2.scriptHash);
        userWallet.deleteAccount(walletAccount3.scriptHash);
    }

    @Test
    public void getAccount() {
        KeyPair keyPair1 = Utils.getRandomKeyPair();
        WalletAccount walletAccount1 = userWallet.createAccount(keyPair1.privateKey);
        KeyPair keyPair2 = Utils.getRandomKeyPair();
        UInt160 hash2 = UInt160.parseToScriptHash(neo.smartcontract.Contract.createSignatureRedeemScript(keyPair2.publicKey));

        WalletAccount tmp = userWallet.getAccount(walletAccount1.scriptHash);
        Assert.assertEquals(walletAccount1.scriptHash, tmp.scriptHash);

        tmp = userWallet.getAccount(hash2);
        Assert.assertNull(tmp);

        // clear data
        userWallet.deleteAccount(walletAccount1.scriptHash);
    }

    @Test
    public void getAccounts() {
        Assert.assertFalse(userWallet.getAccounts().iterator().hasNext());

        KeyPair keyPair1 = Utils.getRandomKeyPair();
        WalletAccount walletAccount1 = userWallet.createAccount(keyPair1.privateKey);

        KeyPair keyPair2 = Utils.getRandomKeyPair();
        WalletAccount walletAccount2 = userWallet.createAccount(keyPair2.privateKey);


        KeyPair keyPair3 = Utils.getRandomKeyPair();
        WalletAccount walletAccount3 = userWallet.createAccount(keyPair3.privateKey);

        Iterable<WalletAccount> iterable = userWallet.getAccounts();
        Iterator<WalletAccount> iterator = iterable.iterator();

        while (iterator.hasNext()) {
            WalletAccount tmp = iterator.next();

            Assert.assertEquals(true, tmp.scriptHash.equals(walletAccount1.scriptHash)
                    || tmp.scriptHash.equals(walletAccount2.scriptHash)
                    || tmp.scriptHash.equals(walletAccount3.scriptHash));
        }

        // clear data
        userWallet.deleteAccount(walletAccount1.scriptHash);
        userWallet.deleteAccount(walletAccount2.scriptHash);
        userWallet.deleteAccount(walletAccount3.scriptHash);
    }

    @Test
    public void getCoins() {
        KeyPair keyPair1 = Utils.getRandomKeyPair();
        WalletAccount walletAccount1 = userWallet.createAccount(keyPair1.privateKey);

        KeyPair keyPair2 = Utils.getRandomKeyPair();
        WalletAccount walletAccount2 = userWallet.createAccount(keyPair2.privateKey);


        KeyPair keyPair3 = Utils.getRandomKeyPair();
        WalletAccount walletAccount3 = userWallet.createAccount(keyPair3.privateKey);

        List<UInt160> list = Arrays.asList(walletAccount1.scriptHash, walletAccount2.scriptHash, walletAccount3.scriptHash);
        Iterable<Coin> iterable = userWallet.getCoins(list);

        Assert.assertEquals(false, iterable.iterator().hasNext());

        // add tx into unconfirmed
        ContractTransaction tx = new ContractTransaction() {{
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                        scriptHash = walletAccount1.scriptHash;
                    }},
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                        scriptHash = UInt160.parseToScriptHash(neo.smartcontract.Contract.createSignatureRedeemScript(keyPair2.publicKey));
                    }}
            };
            witnesses = new Witness[]{
                    new Witness() {{
                        invocationScript = new byte[0];
                        verificationScript = neo.smartcontract.Contract.createSignatureRedeemScript(keyPair3.publicKey);
                    }}
            };
        }};
        userWallet.applyTransaction(tx);

        // getCoin -> getCoinsInternal
        list = Arrays.asList(walletAccount1.scriptHash, walletAccount2.scriptHash, walletAccount3.scriptHash);
        iterable = userWallet.getCoins(list);

        Iterator<Coin> iterator = iterable.iterator();

        Assert.assertTrue(iterator.hasNext());
        Coin coin = iterator.next();
        Assert.assertEquals(true, coin.output.equals(tx.outputs[0]) || coin.output.equals(tx.outputs[1]));

        Assert.assertTrue(iterator.hasNext());
        coin = iterator.next();
        Assert.assertEquals(true, coin.output.equals(tx.outputs[0]) || coin.output.equals(tx.outputs[1]));

        // clear tx
        WalletTransactionEventArgs eventArgs = new WalletTransactionEventArgs();
        eventArgs.relatedAccounts = new UInt160[]{walletAccount1.scriptHash, walletAccount2.scriptHash, walletAccount3.scriptHash};
        eventArgs.transaction = tx;
        userWallet.doWork(null, eventArgs);

        userWallet.deleteAccount(walletAccount1.scriptHash);
        userWallet.deleteAccount(walletAccount2.scriptHash);
        userWallet.deleteAccount(walletAccount3.scriptHash);
    }

    @Test
    public void getCoinsInternal() {
        KeyPair keyPair1 = Utils.getRandomKeyPair();
        WalletAccount walletAccount1 = userWallet.createAccount(keyPair1.privateKey);

        KeyPair keyPair2 = Utils.getRandomKeyPair();
        WalletAccount walletAccount2 = userWallet.createAccount(keyPair2.privateKey);


        KeyPair keyPair3 = Utils.getRandomKeyPair();
        WalletAccount walletAccount3 = userWallet.createAccount(keyPair3.privateKey);

        // add tx into unconfirmed
        ContractTransaction tx = new ContractTransaction() {{
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                        scriptHash = walletAccount1.scriptHash;
                    }},
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                        scriptHash = UInt160.parseToScriptHash(neo.smartcontract.Contract.createSignatureRedeemScript(keyPair2.publicKey));
                    }}
            };
            witnesses = new Witness[]{
                    new Witness() {{
                        invocationScript = new byte[0];
                        verificationScript = neo.smartcontract.Contract.createSignatureRedeemScript(keyPair3.publicKey);
                    }}
            };
        }};
        userWallet.applyTransaction(tx);

        // getCoin -> getCoinsInternal
        List<UInt160> list = Arrays.asList(walletAccount1.scriptHash, walletAccount2.scriptHash, walletAccount3.scriptHash);
        Iterable<Coin> iterable = userWallet.getCoinsInternal(list);

        Iterator<Coin> iterator = iterable.iterator();

        Assert.assertTrue(iterator.hasNext());
        Coin coin = iterator.next();
        Assert.assertEquals(true, coin.output.equals(tx.outputs[0]) || coin.output.equals(tx.outputs[1]));

        Assert.assertTrue(iterator.hasNext());
        coin = iterator.next();
        Assert.assertEquals(true, coin.output.equals(tx.outputs[0]) || coin.output.equals(tx.outputs[1]));


        // clear tx
        WalletTransactionEventArgs eventArgs = new WalletTransactionEventArgs();
        eventArgs.relatedAccounts = new UInt160[]{walletAccount1.scriptHash, walletAccount2.scriptHash, walletAccount3.scriptHash};
        eventArgs.transaction = tx;
        userWallet.doWork(null, eventArgs);

        userWallet.deleteAccount(walletAccount1.scriptHash);
        userWallet.deleteAccount(walletAccount2.scriptHash);
        userWallet.deleteAccount(walletAccount3.scriptHash);
    }

    @Test
    public void getTransactions() {
        KeyPair keyPair1 = Utils.getRandomKeyPair();
        WalletAccount walletAccount1 = userWallet.createAccount(keyPair1.privateKey);

        KeyPair keyPair2 = Utils.getRandomKeyPair();
        WalletAccount walletAccount2 = userWallet.createAccount(keyPair2.privateKey);


        KeyPair keyPair3 = Utils.getRandomKeyPair();
        WalletAccount walletAccount3 = userWallet.createAccount(keyPair3.privateKey);

        // add tx into unconfirmed
        ContractTransaction tx = new ContractTransaction() {{
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                        scriptHash = walletAccount1.scriptHash;
                    }},
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                        scriptHash = UInt160.parseToScriptHash(neo.smartcontract.Contract.createSignatureRedeemScript(keyPair2.publicKey));
                    }}
            };
            witnesses = new Witness[]{
                    new Witness() {{
                        invocationScript = new byte[0];
                        verificationScript = neo.smartcontract.Contract.createSignatureRedeemScript(keyPair3.publicKey);
                    }}
            };
        }};
        userWallet.applyTransaction(tx);
        Iterable<UInt256> iterable = userWallet.getTransactions();
        HashSet<UInt256> set = new HashSet<>();
        Iterator<UInt256> iterator = iterable.iterator();
        while (iterator.hasNext()) {
            set.add(iterator.next());
        }

        Assert.assertEquals(true, set.contains(tx.hash()));

        // clear tx
        WalletTransactionEventArgs eventArgs = new WalletTransactionEventArgs();
        eventArgs.relatedAccounts = new UInt160[]{walletAccount1.scriptHash, walletAccount2.scriptHash, walletAccount3.scriptHash};
        eventArgs.transaction = tx;
        userWallet.doWork(null, eventArgs);

        userWallet.deleteAccount(walletAccount1.scriptHash);
        userWallet.deleteAccount(walletAccount2.scriptHash);
        userWallet.deleteAccount(walletAccount3.scriptHash);
    }

    @Test
    public void verifyPassword() {
        Assert.assertEquals(false, userWallet.verifyPassword("123"));
        Assert.assertEquals(true, userWallet.verifyPassword("123456"));
    }

    @Test
    public void doWork() {
        KeyPair keyPair1 = Utils.getRandomKeyPair();
        WalletAccount walletAccount1 = userWallet.createAccount(keyPair1.privateKey);

        KeyPair keyPair2 = Utils.getRandomKeyPair();

        KeyPair keyPair3 = Utils.getRandomKeyPair();
        WalletAccount walletAccount3 = userWallet.createAccount(keyPair3.privateKey);

        ContractTransaction tx = new ContractTransaction() {{
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                        scriptHash = walletAccount1.scriptHash;
                    }},
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                        scriptHash = UInt160.parseToScriptHash(neo.smartcontract.Contract.createSignatureRedeemScript(keyPair2.publicKey));
                    }}
            };
            witnesses = new Witness[]{
                    new Witness() {{
                        invocationScript = new byte[0];
                        verificationScript = neo.smartcontract.Contract.createSignatureRedeemScript(keyPair3.publicKey);
                    }}
            };
        }};

        EventHandler<WalletTransactionEventArgs> eventHandler = userWallet.getWalletTransaction();
        EventHandler.Listener<WalletTransactionEventArgs> listener = (sender, eventArgs) -> {
            Assert.assertEquals(tx.hash(), eventArgs.transaction.hash());
            Assert.assertEquals(2, eventArgs.relatedAccounts.length);
            Assert.assertEquals(walletAccount1.scriptHash, eventArgs.relatedAccounts[0]);
            Assert.assertEquals(walletAccount3.scriptHash, eventArgs.relatedAccounts[1]);
        };
        eventHandler.addListener(listener);

        WalletTransactionEventArgs eventArgs = new WalletTransactionEventArgs() {{
            transaction = tx;
            relatedAccounts = new UInt160[]{walletAccount1.scriptHash, walletAccount3.scriptHash};
            height = new Uint(1);
            time = new Uint((int) System.currentTimeMillis() / 1000);

        }};
        userWallet.doWork(null, eventArgs);
        eventHandler.removeListener(listener);

        // clear data
        userWallet.deleteAccount(walletAccount1.scriptHash);
        userWallet.deleteAccount(walletAccount3.scriptHash);
    }
}