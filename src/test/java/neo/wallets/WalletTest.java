package neo.wallets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import akka.testkit.TestActorRef;
import akka.testkit.TestKit;
import neo.Fixed8;
import neo.MyNeoSystem;
import neo.UInt160;
import neo.UInt256;
import neo.Utils;
import neo.consensus.MyWallet;
import neo.ledger.Blockchain;
import neo.ledger.CoinState;
import neo.ledger.ContractPropertyState;
import neo.ledger.ContractState;
import neo.ledger.StorageItem;
import neo.ledger.StorageKey;
import neo.ledger.TransactionState;
import neo.ledger.UnspentCoinState;
import neo.network.p2p.payloads.InvocationTransaction;
import neo.network.p2p.payloads.MinerTransaction;
import neo.network.p2p.payloads.TransactionAttributeUsage;
import neo.smartcontract.ContractParameterType;
import neo.vm.OpCode;
import neo.vm.ScriptBuilder;
import neo.wallets.NEP6.NEP6Account;
import neo.wallets.NEP6.NEP6Contract;
import neo.wallets.NEP6.NEP6Wallet;
import neo.wallets.NEP6.NEP6WalletTest;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.Ushort;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.ledger.MyBlockchain2;
import neo.log.notr.TR;
import neo.network.p2p.MyLocalNode;
import neo.network.p2p.TaskManager;
import neo.network.p2p.payloads.CoinReference;
import neo.network.p2p.payloads.IVerifiable;
import neo.network.p2p.payloads.Transaction;
import neo.network.p2p.payloads.TransactionAttribute;
import neo.network.p2p.payloads.TransactionOutput;
import neo.network.p2p.payloads.Witness;
import neo.persistence.AbstractBlockchainTest;
import neo.persistence.Snapshot;
import neo.smartcontract.ContractParametersContext;
import scenario.InvocationTxWithContractTest;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: WalletTest
 * @Package neo.wallets
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 11:07 2019/4/9
 */
public class WalletTest extends AbstractBlockchainTest {
    class UserWalletIndexer extends WalletIndexer {

        public UserWalletIndexer() {
            //super();
        }

        @Override
        public Uint getIndexHeight() {
            return Uint.ONE;
        }

        @Override
        public void registerAccounts(Iterable<UInt160> accounts) {
            //super.registerAccounts(accounts);
        }

        @Override
        public void registerAccounts(Iterable<UInt160> accounts, Uint height) {
            //super.registerAccounts(accounts, height);
        }

        @Override
        public void unregisterAccounts(Iterable<UInt160> accounts) {
            //super.unregisterAccounts(accounts);
        }


        @Override
        public Iterable<UInt256> getTransactions(Iterable<UInt160> accounts) {
            return new HashSet<UInt256>();
        }


        @Override
        public Iterable<Coin> getCoins(Iterable<UInt160> accounts) {
            return new HashSet<Coin>();
        }
    }

    class UserWalletIndexer2 extends WalletIndexer {

        public UserWalletIndexer2() {
            //super();
        }

        @Override
        public Uint getIndexHeight() {
            return Uint.ONE;
        }

        @Override
        public void registerAccounts(Iterable<UInt160> accounts) {
            //super.registerAccounts(accounts);
        }

        @Override
        public void registerAccounts(Iterable<UInt160> accounts, Uint height) {
            //super.registerAccounts(accounts, height);
        }

        @Override
        public void unregisterAccounts(Iterable<UInt160> accounts) {
            //super.unregisterAccounts(accounts);
        }


        @Override
        public Iterable<UInt256> getTransactions(Iterable<UInt160> accounts) {
            return new HashSet<UInt256>();
        }


        @Override
        public Iterable<Coin> getCoins(Iterable<UInt160> accounts) {
            Set<Coin> temp = new HashSet<Coin>();
            Coin temp1 = new Coin();
            temp1.reference = new CoinReference();
            temp1.reference.prevHash = UInt256.Zero;
            temp1.reference.prevIndex = Ushort.ZERO;

            temp1.output = new TransactionOutput();
            temp1.output.scriptHash = UInt160.Zero;
            temp.add(temp1);
            return temp;
        }
    }

    @BeforeClass
    public static void setUp() throws IOException {
        AbstractBlockchainTest.setUp(WalletTest.class.getSimpleName());
    }

    @AfterClass
    public static void tearDown() throws IOException {
        AbstractBlockchainTest.tearDown(WalletTest.class.getSimpleName());
    }

    @Test
    public void createAccount4() throws Exception {
        neoSystem = new MyNeoSystem(store, self -> {
            testKit = new TestKit(self.actorSystem);

            // Synchronous Unit Testing with TestActorRef
            self.blockchain = TestActorRef.create(self.actorSystem, MyBlockchain2.props(self, store, testKit.testActor()));
            self.localNode = TestActorRef.create(self.actorSystem, MyLocalNode.props(self, testKit.testActor()));
            self.taskManager = TestActorRef.create(self.actorSystem, TaskManager.props(self));
            self.consensus = null;
        });
        URL url = NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer = new UserWalletIndexer();
        NEP6Wallet wallet = new NEP6Wallet(indexer, url.getPath(), "aa");
        wallet.unlock("1234567890");
        wallet.createAccount();
        Assert.assertEquals(6, StreamSupport.stream(wallet.getAccounts().spliterator(),
                false).collect(Collectors.toList()).size());

        // free resource
        indexer.dispose();
    }

    @Test
    public void createAccount5() throws Exception {
        String json = "{\"script\":\"52c56b6c766b00527ac461516c766b51527ac46203006c766b51c3616c7566" +
                "\",\"parameters\":[{\"name\":\"parameter0\",\"type\":\"Signature\"}],\"deployed\":false}";
        JsonObject object = new JsonParser().parse(json).getAsJsonObject();
        NEP6Contract contract = NEP6Contract.fromJson(object);
        URL url = NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer = new UserWalletIndexer();
        NEP6Wallet wallet = new NEP6Wallet(indexer, url.getPath(), "aa");
        wallet.unlock("1234567890");

        WalletAccount account = wallet.createAccount(contract,
                new byte[]{0x01, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x02, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x03, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00});
        Assert.assertEquals(true, account.scriptHash.equals(contract.scriptHash()));

        // free resource
        indexer.dispose();
    }


    @Test
    public void findUnspentCoins1() throws Exception {
        String json = "{\"script\":\"52c56b6c766b00527ac461516c766b51527ac46203006c766b51c3616c7566" +
                "\",\"parameters\":[{\"name\":\"parameter0\",\"type\":\"Signature\"}],\"deployed\":false}";
        JsonObject object = new JsonParser().parse(json).getAsJsonObject();
        NEP6Contract contract = NEP6Contract.fromJson(object);
        URL url = NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer = new UserWalletIndexer();
        NEP6Wallet wallet = new NEP6Wallet(indexer, url.getPath(), "aa");
        wallet.unlock("1234567890");
        Coin[] iterable = wallet.findUnspentCoins(UInt256.Zero, Fixed8.ONE, new UInt160[]{wallet
                .getAccounts().iterator().next().scriptHash});
        Assert.assertEquals(null, iterable);

        // free resource
        indexer.dispose();
    }

    @Test
    public void findUnspentCoins2() throws Exception {
        String json = "{\"script\":\"52c56b6c766b00527ac461516c766b51527ac46203006c766b51c3616c7566" +
                "\",\"parameters\":[{\"name\":\"parameter0\",\"type\":\"Signature\"}],\"deployed\":false}";
        JsonObject object = new JsonParser().parse(json).getAsJsonObject();
        NEP6Contract contract = NEP6Contract.fromJson(object);
        URL url = NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer = new UserWalletIndexer();
        NEP6Wallet wallet = new NEP6Wallet(indexer, url.getPath(), "aa");
        wallet.unlock("1234567890");
        Iterable<Coin> iterable = wallet.findUnspentCoins(new UInt160[]{wallet.getAccounts()
                .iterator().next().scriptHash});
        Assert.assertEquals(0, StreamSupport.stream(iterable.spliterator(), false).collect
                (Collectors.toList()).size());

        // free resource
        indexer.dispose();
    }


    @Test
    public void getAvailable() throws Exception {
        String json = "{\"script\":\"52c56b6c766b00527ac461516c766b51527ac46203006c766b51c3616c7566" +
                "\",\"parameters\":[{\"name\":\"parameter0\",\"type\":\"Signature\"}],\"deployed\":false}";
        JsonObject object = new JsonParser().parse(json).getAsJsonObject();
        NEP6Contract contract = NEP6Contract.fromJson(object);
        URL url = NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer = new UserWalletIndexer();
        NEP6Wallet wallet = new NEP6Wallet(indexer, url.getPath(), "aa");
        wallet.unlock("1234567890");
        Assert.assertEquals(Fixed8.ZERO, wallet.getAvailable(UInt256.Zero));

        // free resource
        indexer.dispose();
    }

    @Test
    public void getAvailable1() throws Exception {
        //// TODO: 2019/4/10 需要测Uint160的数据
    }

    @Test
    public void getBalance() throws Exception {
        String json = "{\"script\":\"52c56b6c766b00527ac461516c766b51527ac46203006c766b51c3616c7566" +
                "\",\"parameters\":[{\"name\":\"parameter0\",\"type\":\"Signature\"}],\"deployed\":false}";
        JsonObject object = new JsonParser().parse(json).getAsJsonObject();
        NEP6Contract contract = NEP6Contract.fromJson(object);
        URL url = NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer = new UserWalletIndexer();
        NEP6Wallet wallet = new NEP6Wallet(indexer, url.getPath(), "aa");
        wallet.unlock("1234567890");
        Assert.assertEquals(Fixed8.ZERO, wallet.getBalance(UInt256.Zero));

        // free resource
        indexer.dispose();
    }

    @Test
    public void getChangeAddress() throws Exception {
        String json = "{\"script\":\"52c56b6c766b00527ac461516c766b51527ac46203006c766b51c3616c7566" +
                "\",\"parameters\":[{\"name\":\"parameter0\",\"type\":\"Signature\"}],\"deployed\":false}";
        JsonObject object = new JsonParser().parse(json).getAsJsonObject();
        NEP6Contract contract = NEP6Contract.fromJson(object);
        URL url = NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer = new UserWalletIndexer();
        NEP6Wallet wallet = new NEP6Wallet(indexer, url.getPath(), "aa");
        wallet.unlock("1234567890");
        UInt160 uInt160 = wallet.getChangeAddress();
        Assert.assertEquals("0xa007b97f811876188c1b4c00ae2b95dfc57428c3", uInt160.toString());
        //0xa007b97f811876188c1b4c00ae2b95dfc57428c3

        // free resource
        indexer.dispose();
    }

    @Test
    public void getCoins1() throws Exception {
        String json = "{\"script\":\"52c56b6c766b00527ac461516c766b51527ac46203006c766b51c3616c7566" +
                "\",\"parameters\":[{\"name\":\"parameter0\",\"type\":\"Signature\"}],\"deployed\":false}";
        JsonObject object = new JsonParser().parse(json).getAsJsonObject();
        NEP6Contract contract = NEP6Contract.fromJson(object);
        URL url = NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer2 indexer = new UserWalletIndexer2();
        NEP6Wallet wallet = new NEP6Wallet(indexer, url.getPath(), "aa");
        wallet.unlock("1234567890");
        Iterable<Coin> iterable = wallet.getCoins();
        Assert.assertNotNull(iterable);

        // free resource
        indexer.dispose();
    }

    @Test
    public void getPrivateKeyFromNEP2() throws Exception {
        byte[] result = Wallet.getPrivateKeyFromNEP2
                ("6PYNaeA3WyZJJNnFqq3PbXszry5hL7bbageAmN3sc4tziXmPzpcQSEqHjr" +
                        "", "1234567890");
        Assert.assertEquals("69213c5dd95cd686c5cb8473ff72ceda07926841205b18e077fa24b591ac1a64",
                BitConverter.toHexString(result));
        byte[] result2 = Wallet.getPrivateKeyFromNEP2
                ("6PYPcDneGpVjNcfwNc4BAvQAzZ8e3wRGZeixcFSSLQo9gpjLtPXcxQ7fMr", "11111111");

        URL url = NEP6WalletTest.class.getClassLoader().getResource("1w.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer = new UserWalletIndexer();
        NEP6Wallet wallet = new NEP6Wallet(indexer, url.getPath(), "aa");
        //wallet.unlock("11111111");
        Iterator<NEP6Account> iterator = wallet.getAccounts().iterator();
        int i = 0;
        int j = 0;
        int k = 0;
        while (iterator.hasNext()) {
            if (k > 10) {
                break;
            }
            k++;
            try {
                Wallet.getPrivateKeyFromNEP2(iterator.next().nep2key, "11111111");
                j++;
            } catch (Exception e) {
                i++;
            }

        }
        Assert.assertEquals(0, i);
        Assert.assertEquals(11, j);

        // free resource
        indexer.dispose();
    }

    @Test
    public void getPrivateKeyFromNEP21() throws Exception {
        byte[] result = Wallet.getPrivateKeyFromNEP2
                ("6PYNaeA3WyZJJNnFqq3PbXszry5hL7bbageAmN3sc4tziXmPzpcQSEqHjr" +
                        "", "1234567890", 16384, 8, 8);
        Assert.assertEquals("69213c5dd95cd686c5cb8473ff72ceda07926841205b18e077fa24b591ac1a64",
                BitConverter.toHexString(result));
    }

    @Test
    public void getPrivateKeyFromWIF() throws Exception {
        byte[] result = Wallet.getPrivateKeyFromWIF("KwzsQGom6tTdiaPBh3k6q5cXsCa2kYoSQjKA4RRNLPYo87FvkZuh");
        Assert.assertEquals("173b40f65fe6567e8954f03440458633da5eeaa41f8d637a5f3d6baf5a5699aa",
                BitConverter.toHexString(result));
    }

    @Test
    public void getUnclaimedCoins() throws Exception {
        URL url = NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer = new UserWalletIndexer();
        NEP6Wallet wallet = new NEP6Wallet(indexer, url.getPath(), "aa");
        wallet.unlock("1234567890");
        Iterable<Coin> iterable = wallet.getUnclaimedCoins();
        Assert.assertNotNull(iterable);

        // free resource
        indexer.dispose();
    }

    @Test
    public void makeTransaction() throws Exception {
        URL url = NEP6WalletTest.class.getClassLoader().getResource("1w.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer = new UserWalletIndexer();
        NEP6Wallet wallet = new NEP6Wallet(indexer, url.getPath(), "aa");
        //wallet.unlock("11111111");
        Set<TransferOutput> set = new HashSet<>();
        set.add(new TransferOutput(UInt256.Zero, BigDecimal.ONE, UInt160.Zero));
        Transaction tx = wallet.makeTransaction(new ArrayList<TransactionAttribute>(), set);

        // free resource
        indexer.dispose();
    }

    @Test
    public void makeTransaction1() throws Exception {
        // prepare data
        String privateKey = "f72b8fab85fdcc1bdd20b107e5da1ab4713487bc88fc53b5b134f5eddeaa1a19";
        KeyPair keypair = new KeyPair(BitConverter.hexToBytes(privateKey));
        UInt160 owner = UInt160.parseToScriptHash(neo.smartcontract.Contract.createSignatureRedeemScript(keypair.publicKey));

        MyWallet myWallet = new MyWallet();
        myWallet.createAccount(keypair.privateKey);

        Snapshot snapshot = blockchain.getSnapshot();

        byte[] avmScript = loadAvm();
        UInt160 avmScriptHash = UInt160.parseToScriptHash(avmScript);

        // add contract
        ContractState contractState = new ContractState() {{
            script = avmScript;
            parameterList = new ContractParameterType[]{ContractParameterType.String, ContractParameterType.Array};
            returnType = ContractParameterType.ByteArray;
            contractProperties = ContractPropertyState.HasStorage;
            name = "luc";
            codeVersion = "1.0";
            author = "luchuan";
            email = "luchuan@neo.org";
            description = "....";
        }};
        snapshot.getContracts().add(contractState.getScriptHash(), contractState);

        // add storage
        StorageKey storageKey = new StorageKey() {{
            scriptHash = avmScriptHash;
            key = owner.toArray();
        }};
        StorageItem storageItem = new StorageItem() {{
            value = BigInteger.valueOf(100000000L).multiply(BigInteger.valueOf(100000000L)).toByteArray();
        }};
        snapshot.getStorages().add(storageKey, storageItem);

        // add utxo
        TransactionState txState = new TransactionState() {{
            transaction = new MinerTransaction() {{
                inputs = new CoinReference[0];
                outputs = new TransactionOutput[]{
                        new TransactionOutput() {{
                            scriptHash = owner;
                            value = Fixed8.fromDecimal(BigDecimal.valueOf(100000));
                            assetId = Blockchain.UtilityToken.hash();
                        }}
                };
            }};
            blockIndex = new Uint(10);
        }};

        UnspentCoinState unspentCoinState = new UnspentCoinState() {{
            items = new CoinState[]{CoinState.Confirmed};
        }};
        snapshot.getTransactions().add(txState.transaction.hash(), txState);
        snapshot.getUnspentCoins().add(txState.transaction.hash(), unspentCoinState);
        snapshot.commit();


        // verify
        Set<TransferOutput> set = new HashSet<>();
        set.add(new TransferOutput(avmScriptHash, BigDecimal.ONE, UInt160.Zero));
        Transaction tx = myWallet.makeTransaction(new ArrayList<>(), set,owner, owner, Fixed8.ZERO);

        InvocationTransaction invocationTransaction = (InvocationTransaction) tx;

        Assert.assertNotNull(invocationTransaction);
        Assert.assertEquals(Fixed8.ZERO, invocationTransaction.getSystemFee());
        Assert.assertEquals(Fixed8.ZERO, invocationTransaction.gas);
        Assert.assertEquals(1, invocationTransaction.attributes.length);
        Assert.assertEquals(TransactionAttributeUsage.Script, invocationTransaction.attributes[0].usage);
        Assert.assertArrayEquals(owner.toArray(), invocationTransaction.attributes[0].data);

        ScriptBuilder sb2 = new ScriptBuilder();
        neo.vm.Helper.emitAppCall(sb2, avmScriptHash, "transfer", owner, UInt160.Zero, BigInteger.valueOf(1));
        sb2.emit(OpCode.THROWIFNOT);
        byte[] nonce = new byte[8];
        Random rand = new Random();
        rand.nextBytes(nonce);
        sb2.emit(OpCode.RET, nonce);
        byte[] script = sb2.toArray();

        Assert.assertArrayEquals(BitConverter.subBytes(script, 0, script.length - 8),
                BitConverter.subBytes(invocationTransaction.script, 0, invocationTransaction.script.length - 8));


        // clear data
        snapshot.getTransactions().delete(txState.transaction.hash());
        snapshot.getStorages().delete(storageKey);
        snapshot.getUnspentCoins().delete(txState.transaction.hash());
        snapshot.commit();
    }

    @Test // transfer nep5 token
    public void makeTransaction2() throws Exception {
        // prepare data
        String privateKey = "f72b8fab85fdcc1bdd20b107e5da1ab4713487bc88fc53b5b134f5eddeaa1a19";
        KeyPair keypair = new KeyPair(BitConverter.hexToBytes(privateKey));
        UInt160 owner = UInt160.parseToScriptHash(neo.smartcontract.Contract.createSignatureRedeemScript(keypair.publicKey));

        MyWallet myWallet = new MyWallet();
        myWallet.createAccount(keypair.privateKey);

        Snapshot snapshot = blockchain.getSnapshot();

        byte[] avmScript = loadAvm();
        UInt160 avmScriptHash = UInt160.parseToScriptHash(avmScript);

        // add contract
        ContractState contractState = new ContractState() {{
            script = avmScript;
            parameterList = new ContractParameterType[]{ContractParameterType.String, ContractParameterType.Array};
            returnType = ContractParameterType.ByteArray;
            contractProperties = ContractPropertyState.HasStorage;
            name = "luc";
            codeVersion = "1.0";
            author = "luchuan";
            email = "luchuan@neo.org";
            description = "....";
        }};
        snapshot.getContracts().add(contractState.getScriptHash(), contractState);

        // add storage
        StorageKey storageKey = new StorageKey() {{
            scriptHash = avmScriptHash;
            key = owner.toArray();
        }};
        StorageItem storageItem = new StorageItem() {{
            value = BigInteger.valueOf(100000000L).multiply(BigInteger.valueOf(100000000L)).toByteArray();
        }};
        snapshot.getStorages().add(storageKey, storageItem);

        // add utxo
        TransactionState txState = new TransactionState() {{
            transaction = new MinerTransaction() {{
                inputs = new CoinReference[0];
                outputs = new TransactionOutput[]{
                        new TransactionOutput() {{
                            scriptHash = owner;
                            value = Fixed8.fromDecimal(BigDecimal.valueOf(100000));
                            assetId = Blockchain.UtilityToken.hash();
                        }}
                };
            }};
            blockIndex = new Uint(10);
        }};

        UnspentCoinState unspentCoinState = new UnspentCoinState() {{
            items = new CoinState[]{CoinState.Confirmed};
        }};
        snapshot.getTransactions().add(txState.transaction.hash(), txState);
        snapshot.getUnspentCoins().add(txState.transaction.hash(), unspentCoinState);
        snapshot.commit();


        // verify
        Set<TransferOutput> set = new HashSet<>();
        set.add(new TransferOutput(avmScriptHash, BigDecimal.ONE, UInt160.Zero));
        Transaction tx = myWallet.makeTransaction(new ArrayList<>(), set);
//        tx = myWallet.makeTransaction(tx);

        InvocationTransaction invocationTransaction = (InvocationTransaction) tx;

        Assert.assertNotNull(invocationTransaction);
        Assert.assertEquals(Fixed8.ZERO, invocationTransaction.getSystemFee());
        Assert.assertEquals(Fixed8.ZERO, invocationTransaction.gas);
        Assert.assertEquals(1, invocationTransaction.attributes.length);
        Assert.assertEquals(TransactionAttributeUsage.Script, invocationTransaction.attributes[0].usage);
        Assert.assertArrayEquals(owner.toArray(), invocationTransaction.attributes[0].data);

        ScriptBuilder sb2 = new ScriptBuilder();
        neo.vm.Helper.emitAppCall(sb2, avmScriptHash, "transfer", owner, UInt160.Zero, BigInteger.valueOf(1));
        sb2.emit(OpCode.THROWIFNOT);
        byte[] nonce = new byte[8];
        Random rand = new Random();
        rand.nextBytes(nonce);
        sb2.emit(OpCode.RET, nonce);
        byte[] script = sb2.toArray();

        Assert.assertArrayEquals(BitConverter.subBytes(script, 0, script.length - 8),
                BitConverter.subBytes(invocationTransaction.script, 0, invocationTransaction.script.length - 8));


        // clear data
        snapshot.getTransactions().delete(txState.transaction.hash());
        snapshot.getStorages().delete(storageKey);
        snapshot.getUnspentCoins().delete(txState.transaction.hash());
        snapshot.commit();
    }


    @Test   // transfer nep5 token
    public void makeTransaction3() throws Exception {
        // prepare data
        String privateKey = "f72b8fab85fdcc1bdd20b107e5da1ab4713487bc88fc53b5b134f5eddeaa1a19";
        KeyPair keypair = new KeyPair(BitConverter.hexToBytes(privateKey));
        UInt160 owner = UInt160.parseToScriptHash(neo.smartcontract.Contract.createSignatureRedeemScript(keypair.publicKey));

        MyWallet myWallet = new MyWallet();
        myWallet.createAccount(keypair.privateKey);

        // prepare contract in leveldb
        // prepare two account with balance
        // prepare utxo in leveldb
        Snapshot snapshot = blockchain.getSnapshot();

        byte[] avmScript = loadAvm();
        UInt160 avmScriptHash = UInt160.parseToScriptHash(avmScript);

        // add contract
        ContractState contractState = new ContractState() {{
            script = avmScript;
            parameterList = new ContractParameterType[]{ContractParameterType.String, ContractParameterType.Array};
            returnType = ContractParameterType.ByteArray;
            contractProperties = ContractPropertyState.HasStorage;
            name = "luc";
            codeVersion = "1.0";
            author = "luchuan";
            email = "luchuan@neo.org";
            description = "....";
        }};
        snapshot.getContracts().add(contractState.getScriptHash(), contractState);

        // add storage
        StorageKey storageKey = new StorageKey() {{
            scriptHash = avmScriptHash;
            key = owner.toArray();
        }};
        StorageItem storageItem = new StorageItem() {{
            value = BigInteger.valueOf(100000000L).multiply(BigInteger.valueOf(100000000L)).toByteArray();
        }};
        snapshot.getStorages().add(storageKey, storageItem);

        // add utxo
        TransactionState txState = new TransactionState() {{
            transaction = new MinerTransaction() {{
                inputs = new CoinReference[0];
                outputs = new TransactionOutput[]{
                        new TransactionOutput() {{
                            scriptHash = owner;
                            value = Fixed8.fromDecimal(BigDecimal.valueOf(100000));
                            assetId = Blockchain.UtilityToken.hash();
                        }}
                };
            }};
            blockIndex = new Uint(10);
        }};

        UnspentCoinState unspentCoinState = new UnspentCoinState() {{
            items = new CoinState[]{CoinState.Confirmed};
        }};
        snapshot.getTransactions().add(txState.transaction.hash(), txState);
        snapshot.getUnspentCoins().add(txState.transaction.hash(), unspentCoinState);
        snapshot.commit();


        // verify...
        Set<TransferOutput> set = new HashSet<>();
        set.add(new TransferOutput(avmScriptHash, BigDecimal.ONE, UInt160.Zero));
        Transaction tx = myWallet.makeTransaction(new ArrayList<>(), set, owner, owner, Fixed8.ZERO);
//        tx = myWallet.makeTransaction(tx);
        InvocationTransaction invocationTransaction = (InvocationTransaction) tx;

        Assert.assertNotNull(invocationTransaction);
        Assert.assertEquals(Fixed8.ZERO, invocationTransaction.getSystemFee());
        Assert.assertEquals(Fixed8.ZERO, invocationTransaction.gas);
        Assert.assertEquals(1, invocationTransaction.attributes.length);
        Assert.assertEquals(TransactionAttributeUsage.Script, invocationTransaction.attributes[0].usage);
        Assert.assertArrayEquals(owner.toArray(), invocationTransaction.attributes[0].data);

        ScriptBuilder sb2 = new ScriptBuilder();
        neo.vm.Helper.emitAppCall(sb2, avmScriptHash, "transfer", owner, UInt160.Zero, BigInteger.valueOf(1));
        sb2.emit(OpCode.THROWIFNOT);
        byte[] nonce = new byte[8];
        Random rand = new Random();
        rand.nextBytes(nonce);
        sb2.emit(OpCode.RET, nonce);
        byte[] script = sb2.toArray();

        Assert.assertArrayEquals(BitConverter.subBytes(script, 0, script.length - 8),
                BitConverter.subBytes(invocationTransaction.script, 0, invocationTransaction.script.length - 8));


        // clear data
        snapshot.getTransactions().delete(txState.transaction.hash());
        snapshot.getStorages().delete(storageKey);
        snapshot.getUnspentCoins().delete(txState.transaction.hash());
        snapshot.commit();
    }

    @Test // transfer NEO or Gas, which are UTXO assets
    public void makeTransaction4() {
        // prepare data
        String privateKey = "f72b8fab85fdcc1bdd20b107e5da1ab4713487bc88fc53b5b134f5eddeaa1a19";
        KeyPair keypair = new KeyPair(BitConverter.hexToBytes(privateKey));
        UInt160 owner = UInt160.parseToScriptHash(neo.smartcontract.Contract.createSignatureRedeemScript(keypair.publicKey));

        URL url = NEP6WalletTest.class.getClassLoader().getResource("1w.json");
        TR.fixMe(url.getPath());
        String walletIndexPath = NEP6WalletTest.class.getClassLoader().getResource("").getPath() + "wallet_indexer002";
        neo.wallets.WalletIndexer indexer = new neo.wallets.WalletIndexer(walletIndexPath);

        NEP6Wallet wallet = new NEP6Wallet(indexer, url.getPath(), "aa");
        //wallet.unlock("11111111");
        Iterator<NEP6Account> iterator = wallet.getAccounts().iterator();
        WalletAccount account1 = iterator.next();
        WalletAccount account2 = iterator.next();
        WalletAccount account3 = iterator.next();
        WalletAccount account4 = iterator.next();
        WalletAccount account5 = iterator.next();


        // add utxo
        Snapshot snapshot = Blockchain.singleton().getSnapshot();
        TransactionState txState = new TransactionState() {{
            transaction = new MinerTransaction() {{
                inputs = new CoinReference[0];
                outputs = new TransactionOutput[]{
                        new TransactionOutput() {{
                            scriptHash = account1.scriptHash;
                            value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                            assetId = Blockchain.UtilityToken.hash();
                        }},
                        new TransactionOutput() {{
                            scriptHash = account2.scriptHash;
                            value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                            assetId = Blockchain.UtilityToken.hash();
                        }},
                        new TransactionOutput() {{
                            scriptHash = account3.scriptHash;
                            value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                            assetId = Blockchain.UtilityToken.hash();
                        }}
                };
            }};
            blockIndex = new Uint(10);
        }};

        UnspentCoinState unspentCoinState = new UnspentCoinState() {{
            items = new CoinState[]{CoinState.Confirmed, CoinState.Spent, CoinState.Confirmed};
        }};
        snapshot.getTransactions().add(txState.transaction.hash(), txState);
        snapshot.getUnspentCoins().add(txState.transaction.hash(), unspentCoinState);
        snapshot.commit();

        CoinReference input1 = new CoinReference() {{
            prevIndex = new Ushort(0);
            prevHash = txState.transaction.hash();
        }};
        Coin coin1 = new Coin() {{
            reference = input1;
            output = txState.transaction.outputs[0];
            state = CoinState.Confirmed;
        }};

        HashSet<CoinReference> set1 = new HashSet<>();
        set1.add(input1);
        indexer.accounts_tracked.put(account1.scriptHash, set1);
        indexer.coins_tracked.put(input1, coin1);

        CoinReference input2 = new CoinReference() {{
            prevIndex = new Ushort(1);
            prevHash = txState.transaction.hash();
        }};
        Coin coin2 = new Coin() {{
            reference = input2;
            output = txState.transaction.outputs[1];
            state = CoinState.Spent;
        }};

        HashSet<CoinReference> set2 = new HashSet<>();
        set2.add(input2);
        indexer.accounts_tracked.put(account2.scriptHash, set2);
        indexer.coins_tracked.put(input2, coin2);


        CoinReference input3 = new CoinReference() {{
            prevIndex = new Ushort(2);
            prevHash = txState.transaction.hash();
        }};
        Coin coin3 = new Coin() {{
            reference = input3;
            output = txState.transaction.outputs[2];
            state = CoinState.Confirmed;
        }};

        HashSet<CoinReference> set3 = new HashSet<>();
        set3.add(input3);
        indexer.accounts_tracked.put(account3.scriptHash, set3);
        indexer.coins_tracked.put(input3, coin3);

        // verify
        Set<TransferOutput> set = new HashSet<>();
        set.add(new TransferOutput(Blockchain.UtilityToken.hash(), BigDecimal.valueOf(1500), account4.scriptHash));
        set.add(new TransferOutput(Blockchain.UtilityToken.hash(), BigDecimal.valueOf(50), account5.scriptHash));

        Transaction tx = wallet.makeTransaction(new ArrayList<>(), set);

        Assert.assertEquals(2, tx.inputs.length);
        Assert.assertEquals(input1, tx.inputs[0]);
        Assert.assertEquals(input3, tx.inputs[1]);

        Assert.assertEquals(3, tx.outputs.length); // 多一个找零地址

        HashMap<UInt160, TransactionOutput> outputMap = new HashMap<>();
        outputMap.put(tx.outputs[0].scriptHash, tx.outputs[0]);
        outputMap.put(tx.outputs[1].scriptHash, tx.outputs[1]);
        outputMap.put(tx.outputs[2].scriptHash, tx.outputs[2]);

        Assert.assertEquals(true, outputMap.containsKey(account4.scriptHash));
        Assert.assertEquals(Fixed8.fromDecimal(BigDecimal.valueOf(1500)),outputMap.get(account4.scriptHash).value);

        Assert.assertEquals(true, outputMap.containsKey(account5.scriptHash));
        Assert.assertEquals(Fixed8.fromDecimal(BigDecimal.valueOf(50)),outputMap.get(account5.scriptHash).value);

        Assert.assertEquals(true, outputMap.containsKey(account1.scriptHash)); // 多一个找零地址
        Assert.assertEquals(Fixed8.fromDecimal(BigDecimal.valueOf(450)),outputMap.get(account1.scriptHash).value);

        // clear data
        snapshot.getTransactions().delete(txState.transaction.hash());
        snapshot.getUnspentCoins().delete(txState.transaction.hash());
        snapshot.commit();

        indexer.coins_tracked.clear();
        indexer.accounts_tracked.clear();

        indexer.dispose();
    }

    private static byte[] loadAvm() {
        String path = InvocationTxWithContractTest.class.getClassLoader().getResource("").getPath() + "nep5contract.avm";
        try {
            return Utils.readContentFromFile(path);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    @Test
    public void sign() throws Exception {
        URL url = NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer = new UserWalletIndexer();
        NEP6Wallet wallet = new NEP6Wallet(indexer, url.getPath(), "aa");
        wallet.unlock("1234567890");
        ContractParametersContext context = new ContractParametersContext(new IVerifiable() {
            @Override
            public Witness[] getWitnesses() {
                return new Witness[0];
            }

            @Override
            public void setWitnesses(Witness[] witnesses) {

            }

            @Override
            public void deserializeUnsigned(BinaryReader reader) {

            }

            @Override
            public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
                return new UInt160[]{new UInt160(BitConverter.reverse(BitConverter.hexToBytes
                        ("0xa007b97f811876188c1b4c00ae2b95dfc57428c3")))};
            }

            @Override
            public void serializeUnsigned(BinaryWriter writer) {

            }

            @Override
            public int size() {
                return 1;
            }

            @Override
            public void serialize(BinaryWriter binaryWriter) {

            }

            @Override
            public void deserialize(BinaryReader binaryReader) {

            }

            @Override
            public byte[] getMessage() {
                return new byte[0];
            }
        });
        Assert.assertEquals(true, wallet.sign(context));

        // free resource
        indexer.dispose();
    }

}