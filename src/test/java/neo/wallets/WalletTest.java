package neo.wallets;

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
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import akka.testkit.TestActorRef;
import akka.testkit.TestKit;
import neo.Fixed8;
import neo.MyNeoSystem;
import neo.UInt160;
import neo.UInt256;
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
import neo.persistence.AbstractLeveldbTest;
import neo.persistence.Snapshot;
import neo.smartcontract.ContractParametersContext;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: WalletTest
 * @Package neo.wallets
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 11:07 2019/4/9
 */
public class WalletTest extends AbstractBlockchainTest {
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
    class UserWalletIndexer2 extends WalletIndexer{

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
            Set<Coin> temp=new HashSet<Coin>();
            Coin temp1=new Coin();
            temp1.reference=new CoinReference();
            temp1.reference.prevHash=UInt256.Zero;
            temp1.reference.prevIndex= Ushort.ZERO;

            temp1.output=new TransactionOutput();
            temp1.output.scriptHash=UInt160.Zero;
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
        AbstractLeveldbTest.tearDown(WalletTest.class.getSimpleName());
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
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        wallet.unlock("1234567890");
        wallet.createAccount();
        Assert.assertEquals(6, StreamSupport.stream(wallet.getAccounts().spliterator(),
                false).collect(Collectors.toList()).size());
    }

    @Test
    public void createAccount5() throws Exception {
        String json="{\"script\":\"52c56b6c766b00527ac461516c766b51527ac46203006c766b51c3616c7566" +
                "\",\"parameters\":[{\"name\":\"parameter0\",\"type\":\"Signature\"}],\"deployed\":false}";
        JsonObject object=new JsonParser().parse(json).getAsJsonObject();
        NEP6Contract contract=NEP6Contract.fromJson(object);
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        wallet.unlock("1234567890");

        WalletAccount account=wallet.createAccount(contract,
                new byte[]{0x01,0x00,0x00,0x00,0x00,0x00,
                        0x00,0x00,0x00,0x00,0x00,0x00,
                        0x00,0x02,0x00,0x00,0x00,0x00,
                        0x00,0x00,0x00,0x00,0x00,0x00,
                        0x03,0x00,0x00,0x00,0x00,0x00,
                        0x00,0x00});
        Assert.assertEquals(true,account.scriptHash.equals(contract.scriptHash()));
    }


    @Test
    public void findUnspentCoins1() throws Exception {
        String json="{\"script\":\"52c56b6c766b00527ac461516c766b51527ac46203006c766b51c3616c7566" +
                "\",\"parameters\":[{\"name\":\"parameter0\",\"type\":\"Signature\"}],\"deployed\":false}";
        JsonObject object=new JsonParser().parse(json).getAsJsonObject();
        NEP6Contract contract=NEP6Contract.fromJson(object);
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        wallet.unlock("1234567890");
        Coin[] iterable=wallet.findUnspentCoins(UInt256.Zero,Fixed8.ONE,new UInt160[]{wallet
                .getAccounts().iterator().next().scriptHash});
        Assert.assertEquals(null,iterable);
    }

    @Test
    public void findUnspentCoins2() throws Exception {
        String json="{\"script\":\"52c56b6c766b00527ac461516c766b51527ac46203006c766b51c3616c7566" +
                "\",\"parameters\":[{\"name\":\"parameter0\",\"type\":\"Signature\"}],\"deployed\":false}";
        JsonObject object=new JsonParser().parse(json).getAsJsonObject();
        NEP6Contract contract=NEP6Contract.fromJson(object);
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        wallet.unlock("1234567890");
        Iterable<Coin> iterable=wallet.findUnspentCoins(new UInt160[]{wallet.getAccounts()
                .iterator().next().scriptHash});
        Assert.assertEquals(0,StreamSupport.stream(iterable.spliterator(),false).collect
                (Collectors.toList()).size());
    }


    @Test
    public void getAvailable() throws Exception {
        String json="{\"script\":\"52c56b6c766b00527ac461516c766b51527ac46203006c766b51c3616c7566" +
                "\",\"parameters\":[{\"name\":\"parameter0\",\"type\":\"Signature\"}],\"deployed\":false}";
        JsonObject object=new JsonParser().parse(json).getAsJsonObject();
        NEP6Contract contract=NEP6Contract.fromJson(object);
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        wallet.unlock("1234567890");
        Assert.assertEquals(Fixed8.ZERO,wallet.getAvailable(UInt256.Zero));
    }

    @Test
    public void getAvailable1() throws Exception {
        //// TODO: 2019/4/10 需要测Uint160的数据
    }

    @Test
    public void getBalance() throws Exception {
        String json="{\"script\":\"52c56b6c766b00527ac461516c766b51527ac46203006c766b51c3616c7566" +
                "\",\"parameters\":[{\"name\":\"parameter0\",\"type\":\"Signature\"}],\"deployed\":false}";
        JsonObject object=new JsonParser().parse(json).getAsJsonObject();
        NEP6Contract contract=NEP6Contract.fromJson(object);
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        wallet.unlock("1234567890");
        Assert.assertEquals(Fixed8.ZERO,wallet.getBalance(UInt256.Zero));
    }

    @Test
    public void getChangeAddress() throws Exception {
        String json="{\"script\":\"52c56b6c766b00527ac461516c766b51527ac46203006c766b51c3616c7566" +
                "\",\"parameters\":[{\"name\":\"parameter0\",\"type\":\"Signature\"}],\"deployed\":false}";
        JsonObject object=new JsonParser().parse(json).getAsJsonObject();
        NEP6Contract contract=NEP6Contract.fromJson(object);
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        wallet.unlock("1234567890");
        UInt160 uInt160=wallet.getChangeAddress();
        Assert.assertEquals("0xa007b97f811876188c1b4c00ae2b95dfc57428c3",uInt160.toString());
        //0xa007b97f811876188c1b4c00ae2b95dfc57428c3
    }

    @Test
    public void getCoins1() throws Exception {
        String json="{\"script\":\"52c56b6c766b00527ac461516c766b51527ac46203006c766b51c3616c7566" +
                "\",\"parameters\":[{\"name\":\"parameter0\",\"type\":\"Signature\"}],\"deployed\":false}";
        JsonObject object=new JsonParser().parse(json).getAsJsonObject();
        NEP6Contract contract=NEP6Contract.fromJson(object);
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer2 indexer=new UserWalletIndexer2();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        wallet.unlock("1234567890");
        Iterable<Coin> iterable=wallet.getCoins();
        Assert.assertNotNull(iterable);
    }

    @Test
    public void getPrivateKeyFromNEP2() throws Exception {
        byte[] result=Wallet.getPrivateKeyFromNEP2
                ("6PYNaeA3WyZJJNnFqq3PbXszry5hL7bbageAmN3sc4tziXmPzpcQSEqHjr" +
                "","1234567890");
        Assert.assertEquals("69213c5dd95cd686c5cb8473ff72ceda07926841205b18e077fa24b591ac1a64",
                BitConverter.toHexString(result));
        byte[] result2=Wallet.getPrivateKeyFromNEP2
                ("6PYPcDneGpVjNcfwNc4BAvQAzZ8e3wRGZeixcFSSLQo9gpjLtPXcxQ7fMr","11111111");

        URL url=NEP6WalletTest.class.getClassLoader().getResource("1w.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        //wallet.unlock("11111111");
        Iterator<NEP6Account> iterator=wallet.getAccounts().iterator();
        int i=0;
        int j=0;
        int k=0;
        while (iterator.hasNext()){
            if (k>10){
                break;
            }
            k++;
            try {
                Wallet.getPrivateKeyFromNEP2(iterator.next().nep2key,"11111111");
                j++;
            }catch (Exception e){
               i++;
            }

        }
        Assert.assertEquals(0,i);
        Assert.assertEquals(11,j);



    }

    @Test
    public void getPrivateKeyFromNEP21() throws Exception {
        byte[] result=Wallet.getPrivateKeyFromNEP2
                ("6PYNaeA3WyZJJNnFqq3PbXszry5hL7bbageAmN3sc4tziXmPzpcQSEqHjr" +
                        "","1234567890",16384,8,8);
        Assert.assertEquals("69213c5dd95cd686c5cb8473ff72ceda07926841205b18e077fa24b591ac1a64",
                BitConverter.toHexString(result));
    }

    @Test
    public void getPrivateKeyFromWIF() throws Exception {
        byte[] result=Wallet.getPrivateKeyFromWIF("KwzsQGom6tTdiaPBh3k6q5cXsCa2kYoSQjKA4RRNLPYo87FvkZuh");
        Assert.assertEquals("173b40f65fe6567e8954f03440458633da5eeaa41f8d637a5f3d6baf5a5699aa",
                BitConverter.toHexString(result));
    }

    @Test
    public void getUnclaimedCoins() throws Exception {
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        wallet.unlock("1234567890");
        Iterable<Coin> iterable=wallet.getUnclaimedCoins();
        Assert.assertNotNull(iterable);
    }

    @Test
    public void makeTransaction() throws Exception {
        URL url=NEP6WalletTest.class.getClassLoader().getResource("1w.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        //wallet.unlock("11111111");
        Set<TransferOutput> set=new HashSet<>();
        set.add(new TransferOutput(UInt256.Zero, BigDecimal.ONE,UInt160.Zero));
        Transaction tx=wallet.makeTransaction(new ArrayList<TransactionAttribute>(),set);
    }

    @Test
    public void makeTransaction1() throws Exception {
        URL url=NEP6WalletTest.class.getClassLoader().getResource("1w.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        //wallet.unlock("11111111");
        Set<TransferOutput> set=new HashSet<>();
        set.add(new TransferOutput(UInt160.Zero, BigDecimal.ONE,UInt160.Zero));
        wallet.makeTransaction(new ArrayList<TransactionAttribute>(),set,UInt160.Zero,
                UInt160.Zero,Fixed8.ZERO);
    }

    @Test
    public void makeTransaction2() throws Exception {
        URL url=NEP6WalletTest.class.getClassLoader().getResource("1w.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        //wallet.unlock("11111111");
        Set<TransferOutput> set=new HashSet<>();
        set.add(new TransferOutput(UInt256.Zero, BigDecimal.ONE,UInt160.Zero));
        Transaction tx=wallet.makeTransaction(new ArrayList<TransactionAttribute>(),set);
        wallet.makeTransaction(tx);
    }

    @Test
    public void makeTransaction3() throws Exception {
        URL url=NEP6WalletTest.class.getClassLoader().getResource("1w.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        //wallet.unlock("11111111");
        Set<TransferOutput> set=new HashSet<>();
        set.add(new TransferOutput(UInt256.Zero, BigDecimal.ONE,UInt160.Zero));
        Transaction tx=wallet.makeTransaction(new ArrayList<TransactionAttribute>(),set);
        wallet.makeTransaction(tx,UInt160.Zero, UInt160.Zero,Fixed8.ZERO);
    }

    @Test
    public void sign() throws Exception {
        URL url=NEP6WalletTest.class.getClassLoader().getResource("testaddress1.json");
        TR.fixMe(url.getPath());
        UserWalletIndexer indexer=new UserWalletIndexer();
        NEP6Wallet wallet=new NEP6Wallet(indexer,url.getPath(),"aa");
        wallet.unlock("1234567890");
        ContractParametersContext context=new ContractParametersContext(new IVerifiable() {
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
        Assert.assertEquals(true,wallet.sign(context));
    }

}