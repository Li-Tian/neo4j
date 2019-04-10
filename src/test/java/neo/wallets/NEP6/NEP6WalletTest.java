package neo.wallets.NEP6;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import akka.testkit.TestActorRef;
import akka.testkit.TestKit;
import neo.Fixed8;
import neo.MyNeoSystem;
import neo.UInt160;
import neo.UInt256;
import neo.wallets.Coin;
import neo.wallets.KeyPair;
import neo.wallets.WalletAccount;
import neo.wallets.WalletIndexer;
import neo.wallets.WalletTransactionEventArgs;
import neo.csharp.Uint;
import neo.ledger.Blockchain;
import neo.ledger.MyBlockchain2;
import neo.log.notr.TR;
import neo.network.p2p.MyLocalNode;
import neo.network.p2p.TaskManager;
import neo.network.p2p.payloads.MinerTransaction;
import neo.network.p2p.payloads.TransactionOutput;
import neo.persistence.AbstractBlockchainTest;
import neo.persistence.AbstractLeveldbTest;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: NEP6WalletTest
 * @Package neo.wallets.NEP6
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 15:41 2019/4/2
 */
public class NEP6WalletTest extends AbstractBlockchainTest {
    @Test
    public void getWalletTransaction() throws Exception {
         NEP6Wallet wallet=new NEP6Wallet();
         Assert.assertNotNull(wallet.getWalletTransaction());
    }

    @Test
    public void getName() throws Exception {
        NEP6Wallet wallet=new NEP6Wallet();
        Assert.assertEquals(null,wallet.getName());
    }

    @Test
    public void getVersion() throws Exception {
        NEP6Wallet wallet=new NEP6Wallet();
        Assert.assertEquals(null,wallet.getVersion());
    }

    class UserWalletIndexer extends WalletIndexer{

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

    @Test
    public void getWalletHeight() throws Exception {
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        wallet.unlock("1234567890");
        Assert.assertEquals(Uint.ONE,wallet.getWalletHeight());

    }

    @Test
    public void applyTransaction() throws Exception {
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        MinerTransaction minerTransaction = new MinerTransaction() {{
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                    }}
            };
        }};
        wallet.applyTransaction(minerTransaction);
    }

    @Test
    public void contains() throws Exception {
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        WalletAccount account=wallet.createAccount(UInt160.Zero);
        Assert.assertEquals(true,wallet.contains(UInt160.Zero));
    }

    @Test
    public void createAccount() throws Exception {
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        WalletAccount account=wallet.createAccount(UInt160.Zero);
        Assert.assertEquals(true,wallet.contains(UInt160.Zero));
    }

    @Test
    public void createAccount1() throws Exception {
        String json="{\"script\":\"52c56b6c766b00527ac461516c766b51527ac46203006c766b51c3616c7566" +
                "\",\"parameters\":[{\"name\":\"parameter0\",\"type\":\"Signature\"}],\"deployed\":false}";
        JsonObject object=new JsonParser().parse(json).getAsJsonObject();
        NEP6Contract contract=NEP6Contract.fromJson(object);
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        WalletAccount account=wallet.createAccount(contract);
        Assert.assertEquals(true,account.scriptHash.equals(contract.scriptHash()));
    }

    @Test
    public void createAccount2() throws Exception {
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        wallet.unlock("1234567890");
        KeyPair keyPair=wallet.decryptKey("6PYNaeA3WyZJJNnFqq3PbXszry5hL7bbageAmN3sc4tziXmPzpcQSEqHjr");
        wallet.createAccount(keyPair.privateKey);
    }

    @Test
    public void decryptKey() throws Exception {
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        wallet.unlock("1234567890");
        KeyPair keyPair=wallet.decryptKey("6PYNaeA3WyZJJNnFqq3PbXszry5hL7bbageAmN3sc4tziXmPzpcQSEqHjr");
        Assert.assertNotNull(null,keyPair);
    }

    @Test
    public void deleteAccount() throws Exception {
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        WalletAccount account=wallet.createAccount(UInt160.Zero);
        Assert.assertEquals(true,wallet.deleteAccount(UInt160.Zero));
    }

    @Test
    public void dispose() throws Exception {
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        wallet.dispose();
    }

    @Test
    public void findUnspentCoins() throws Exception {
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        wallet.createAccount(UInt160.Zero);
        Coin[] coin=wallet.findUnspentCoins(UInt256.Zero,Fixed8.ZERO,new UInt160[]{UInt160.Zero});
        Assert.assertEquals(0,coin.length);
    }

    @Test
    public void getAccount() throws Exception {
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        WalletAccount account=wallet.createAccount(UInt160.Zero);
        Assert.assertNotNull(wallet.getAccount(UInt160.Zero));
    }

    @Test
    public void getAccounts() throws Exception {
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        Assert.assertEquals(5,StreamSupport.stream(wallet.getAccounts().spliterator(),false)
                .collect(Collectors.toList()).size());
    }

    @Test
    public void getCoins() throws Exception {
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        Assert.assertEquals(0,StreamSupport.stream(wallet.getCoins().spliterator(),false)
                .collect(Collectors.toList()).size());
    }

    @Test
    public void getCoinsInternal() throws Exception {
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        Assert.assertEquals(0,StreamSupport.stream(wallet.getCoinsInternal(new ArrayList<UInt160>())
                        .spliterator()
                ,false)
                .collect(Collectors.toList()).size());
    }

    @Test
    public void getTransactions() throws Exception {
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        MinerTransaction minerTransaction = new MinerTransaction() {{
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                    }}
            };
        }};
        wallet.applyTransaction(minerTransaction);
        Assert.assertEquals(1,StreamSupport.stream(wallet.getTransactions().spliterator(),false)
                .collect(Collectors.toList()).size());
    }

    @Test
    public void imports() throws Exception {
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        wallet.imports("6PYNaeA3WyZJJNnFqq3PbXszry5hL7bbageAmN3sc4tziXmPzpcQSEqHjr","1234567890");
        //wallet.imports("KwzsQGom6tTdiaPBh3k6q5cXsCa2kYoSQjKA4RRNLPYo87FvkZuh");
        Assert.assertEquals(5,StreamSupport.stream(wallet.getAccounts().spliterator(),false)
                .collect(Collectors.toList()).size());
    }

    @Test
    public void imports1() throws Exception {
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        wallet.unlock("1234567890");
        wallet.imports("KwzsQGom6tTdiaPBh3k6q5cXsCa2kYoSQjKA4RRNLPYo87FvkZuh");
        Assert.assertEquals(6,StreamSupport.stream(wallet.getAccounts().spliterator(),false)
                .collect(Collectors.toList()).size());
    }

    @Test
    public void lock() throws Exception {
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        wallet.lock();
    }

    @BeforeClass
    public static void setUp() throws IOException {
        AbstractBlockchainTest.setUp(NEP6WalletTest.class.getSimpleName());
    }

    @AfterClass
    public static void tearDown() throws IOException {
        AbstractLeveldbTest.tearDown(NEP6WalletTest.class.getSimpleName());
    }

    @Test
    public void migrate() throws Exception {
        neoSystem = new MyNeoSystem(store, self -> {
            testKit = new TestKit(self.actorSystem);

            // Synchronous Unit Testing with TestActorRef
            self.blockchain = TestActorRef.create(self.actorSystem, MyBlockchain2.props(self, store, testKit.testActor()));
            self.localNode = TestActorRef.create(self.actorSystem, MyLocalNode.props(self, testKit.testActor()));
            self.taskManager = TestActorRef.create(self.actorSystem, TaskManager.props(self));
            self.consensus = null;
        });
        UserWalletIndexer indexer=new UserWalletIndexer();
        URL url=NEP6WalletTest.class.getClassLoader().getResource("test5.db3");
        URL url2=NEP6WalletTest.class.getClassLoader().getResource("");
        NEP6Wallet.migrate(indexer,url2.getPath()+"test5address.json",
                url.getPath(),"1234567890");

    }

    @Test
    public void save() throws Exception {
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        wallet.save();
    }

    @Test
    public void unlock() throws Exception {
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        wallet.unlock("1234567890");
    }

    @Test
    public void verifyPassword() throws Exception {
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        Assert.assertEquals(true,wallet.verifyPassword("1234567890"));
    }

    @Test
    public void doWork() throws Exception {
        MinerTransaction minerTransaction = new MinerTransaction() {{
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                    }}
            };
        }};
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        WalletTransactionEventArgs eventArgs=new WalletTransactionEventArgs(minerTransaction,new
                UInt160[]{UInt160.Zero},new Uint(0),new Uint(0));
        wallet.doWork(this,eventArgs);
    }



}