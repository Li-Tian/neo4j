package neo.network.rpc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;

import akka.testkit.TestActorRef;
import akka.testkit.TestKit;
import inet.ipaddr.IPAddressString;
import neo.Fixed8;
import neo.MyNeoSystem;
import neo.NeoSystem;
import neo.UInt160;
import neo.UInt256;
import neo.UIntBase;
import neo.consensus.MyWallet;
import neo.csharp.BitConverter;
import neo.csharp.io.BinaryWriter;
import neo.ledger.MyBlockchain2;
import neo.ledger.MyConsensusService;
import neo.network.p2p.LocalNode;
import neo.network.p2p.MyLocalNode;
import neo.network.p2p.MyTaskManager;
import neo.network.p2p.payloads.AssetType;
import neo.network.p2p.payloads.Transaction;
import neo.network.p2p.payloads.TransactionType;
import neo.persistence.AbstractLeveldbTest;
import neo.smartcontract.ContractParametersContext;
import neo.vm.VMState;
import neo.wallets.AssetDescriptor;
import neo.wallets.TransferOutput;
import neo.wallets.WalletAccount;

public class RpcServerTest extends AbstractLeveldbTest {
    private static MyNeoSystem system;
    private static TestKit testKit;
    @BeforeClass
    public static void setup() {
        try {
            AbstractLeveldbTest.setUp(RpcServerTest.class.getSimpleName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterClass
    public static void tearDown() throws IOException {
        AbstractLeveldbTest.tearDown(RpcServerTest.class.getSimpleName());
        system.rpcServer.dispose();
    }

    /*@Test
    public void test() {
        system = new MyNeoSystem(store, self -> {
            testKit = new TestKit(self.actorSystem);

            // Synchronous Unit Testing with TestActorRef
            self.blockchain = TestActorRef.create(self.actorSystem, MyBlockchain2.props(self, store, testKit.testActor()));
            self.localNode = TestActorRef.create(self.actorSystem, MyLocalNode.props(self, testKit.testActor()));
            self.taskManager = TestActorRef.create(self.actorSystem, MyTaskManager.props(self, testKit.testActor()));
            self.consensus = TestActorRef.create(self.actorSystem, MyConsensusService.props(self, testKit.testActor()));
        });
        MyWallet wallet = new MyWallet();
        WalletAccount account = wallet.getAccounts().iterator().next();
        String address = account.getAddress();
        UInt160 script_hash = neo.wallets.Helper.toScriptHash(address);
        wallet.initTransaction(script_hash);
        String assetid = "c56f33fc6ecfcd0c225c4ab356fee59390af8560be0e930faebe74a6daff7c9b";//NEO
        String blockHash = "0x770491ed5d25be41c633bd4e42a06712dd5805dd7c9aea9b351bcbe33a96dc8a";
        String blockInfo = "0000000000000000000000000000000000000000000000000000000000000000000000006" +
                "5e2c8b3c0d46fd63bcb9129a9cee2c7057ca14ae243697213a0474d90f4464765fc8857000000001dac2" +
                "b7c0000000059e75d652b5d3827bf04c165bbe9ef95cca4bf55010001510400001dac2b7c00000000400" +
                "000455b7b226c616e67223a227a682d434e222c226e616d65223a22e5b08fe89a81e882a1227d2c7b226" +
                "c616e67223a22656e222c226e616d65223a22416e745368617265227d5d0000c16ff28623000000da174" +
                "5e9b549bd0bfa1a569971c77eba30cd5a4b00000000400001445b7b226c616e67223a227a682d434e222" +
                "c226e616d65223a22e5b08fe89a81e5b881227d2c7b226c616e67223a22656e222c226e616d65223a224" +
                "16e74436f696e227d5d0000c16ff28623000800da1745e9b549bd0bfa1a569971c77eba30cd5a4b00000" +
                "00001000000019b7cffdaa674beae0f930ebe6085af9093e5fe56b34a5c220ccdcf6efc336fc50000c16" +
                "ff28623005fa99d93303775fe50ca119c327759313eccfa1c01000151";
        String blockHeaderInfo = "0000000000000000000000000000000000000000000000000000000000000000000" +
                "0000065e2c8b3c0d46fd63bcb9129a9cee2c7057ca14ae243697213a0474d90f4464765fc88570000000" +
                "01dac2b7c0000000059e75d652b5d3827bf04c165bbe9ef95cca4bf550100015100";
        String transactionInfo = "400000455b7b226c616e67223a227a682d434e222c226e616d65223a22e5b08fe89" +
                "a81e882a1227d2c7b226c616e67223a22656e222c226e616d65223a22416e745368617265227d5d0000c" +
                "16ff28623000000da1745e9b549bd0bfa1a569971c77eba30cd5a4b00000000";
        int blockTimeStramp = 1468595301;

        system.startRpc(new IPAddressString("127.0.0.1").getAddress(), 8080, wallet, "", "", new String[]{}, null);

        //dumpprivkey
        Object result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=dumpprivkey&params=[\"" + address + "\"]&id=1");
        Assert.assertEquals(String.class, result.getClass());
        Assert.assertEquals(true, result.equals(account.getKey().export()));

        //getaccountstate
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=getaccountstate&params=[\"" + address + "\"]&id=1");
        Assert.assertEquals(JsonObject.class, result.getClass());
        Assert.assertEquals(5, ((JsonObject)result).size());
        Assert.assertEquals(0, ((JsonObject)result).get("version").getAsInt());
        Assert.assertEquals(true, UInt160.parse(((JsonObject)result).get("script_hash").getAsString()).equals(script_hash));
        Assert.assertEquals(false, ((JsonObject)result).get("frozen").getAsBoolean());
        Assert.assertEquals(0, ((JsonObject)result).getAsJsonArray("votes").size());
        Assert.assertEquals(0, ((JsonObject)result).getAsJsonArray("balances").size());

        //getassetstate
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=getassetstate&params=[\""
                + assetid + "\"]&id=1");
        Assert.assertEquals(JsonObject.class, result.getClass());
        Assert.assertEquals(12, ((JsonObject)result).size());
        Assert.assertEquals(0, ((JsonObject)result).get("version").getAsInt());
        Assert.assertEquals(true, UInt256.parse(((JsonObject)result).get("id").getAsString()).equals(UInt256.parse(assetid)));
        Assert.assertEquals(0, ((JsonObject)result).get("type").getAsInt());
        Assert.assertEquals(2, ((JsonObject)result).getAsJsonArray("name").size());
        Assert.assertEquals(true, ((JsonObject)result).getAsJsonArray("name").get(0).getAsJsonObject().get("lang").getAsString().equals("zh-CN"));
        Assert.assertEquals(true, ((JsonObject)result).getAsJsonArray("name").get(0).getAsJsonObject().get("name").getAsString().equals("小蚁股"));
        Assert.assertEquals(true, ((JsonObject)result).getAsJsonArray("name").get(1).getAsJsonObject().get("lang").getAsString().equals("en"));
        Assert.assertEquals(true, ((JsonObject)result).getAsJsonArray("name").get(1).getAsJsonObject().get("name").getAsString().equals("AntShare"));
        Assert.assertEquals(true, ((JsonObject)result).get("amount").getAsString().equals("100000000.00000000"));
        Assert.assertEquals(true, ((JsonObject)result).get("available").getAsString().equals("100000000.00000000"));
        Assert.assertEquals(0, ((JsonObject)result).get("precision").getAsInt());
        Assert.assertEquals(true, ((JsonObject)result).get("owner").getAsString().equals("00"));
        Assert.assertEquals(true, ((JsonObject)result).get("admin").getAsString().equals("Abf2qMs1pzQb8kYk9RuxtUb9jtRKJVuBJt"));
        Assert.assertEquals(true, ((JsonObject)result).get("issuer").getAsString().equals("Abf2qMs1pzQb8kYk9RuxtUb9jtRKJVuBJt"));
        Assert.assertEquals(4000000, ((JsonObject)result).get("expiration").getAsInt());
        Assert.assertEquals(false, ((JsonObject)result).get("frozen").getAsBoolean());

        //getbalance
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=getbalance&params=[\""
                + assetid + "\"]&id=1");
        Assert.assertEquals(JsonObject.class, result.getClass());
        Assert.assertEquals(2, ((JsonObject)result).size());
        Assert.assertEquals(true, ((JsonObject)result).get("balance").getAsString().equals("100.00000000"));
        Assert.assertEquals(true, ((JsonObject)result).get("confirmed").getAsString().equals("100.00000000"));

        //getbestblockhash
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=getbestblockhash&params=[]&id=1");
        Assert.assertEquals(String.class, result.getClass());
        Assert.assertEquals(true, result.equals(blockHash));

        //getblock
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=getblock&params=[\""
                + blockHash + "\"]&id=1");
        Assert.assertEquals(String.class, result.getClass());
        Assert.assertEquals(true, result.equals(blockInfo));
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=getblock&params=[\""
                + blockHash + "\",1]&id=1");
        Assert.assertEquals(JsonObject.class, result.getClass());
        Assert.assertEquals(11, ((JsonObject)result).size());
        Assert.assertEquals(true, ((JsonObject)result).get("hash").getAsString().equals(blockHash));
        Assert.assertEquals(401, ((JsonObject)result).get("size").getAsInt());
        Assert.assertEquals(0, ((JsonObject)result).get("version").getAsInt());
        Assert.assertEquals(true, ((JsonObject)result).get("previousblockhash").getAsString().equals("0x0000000000000000000000000000000000000000000000000000000000000000"));
        Assert.assertEquals(true, ((JsonObject)result).get("merkleroot").getAsString().equals("0x4746f4904d47a013726943e24aa17c05c7e2cea92991cb3bd66fd4c0b3c8e265"));
        Assert.assertEquals(blockTimeStramp, ((JsonObject)result).get("time").getAsInt());
        Assert.assertEquals(0, ((JsonObject)result).get("index").getAsInt());
        Assert.assertEquals(true, ((JsonObject)result).get("nonce").getAsString().equals("2083236893"));
        Assert.assertEquals(true, ((JsonObject)result).get("nextconsensus").getAsString().equals("0x55bfa4cc95efe9bb65c104bf27385d2b655de759"));
        Assert.assertEquals(2, ((JsonObject)result).getAsJsonObject("script").size());
        Assert.assertEquals(true, ((JsonObject)result).getAsJsonObject("script").get("invocation").getAsString().equals(""));
        Assert.assertEquals(true, ((JsonObject)result).getAsJsonObject("script").get("verification").getAsString().equals("51"));
        Assert.assertEquals(true, ((JsonObject)result).get("confirmations").getAsString().equals("1"));

        //getblockcount
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=getblockcount&params=[]&id=1");
        Assert.assertEquals(Integer.class, result.getClass());
        Assert.assertEquals(true, result.equals(1));

        //getblockheader
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=getblockheader&params=[\""
                + blockHash + "\"]&id=1");
        Assert.assertEquals(String.class, result.getClass());
        Assert.assertEquals(true, result.equals(blockHeaderInfo));
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=getblockheader&params=[\""
                + blockHash + "\",1]&id=1");
        Assert.assertEquals(JsonObject.class, result.getClass());
        Assert.assertEquals(11, ((JsonObject)result).size());
        Assert.assertEquals(true, ((JsonObject)result).get("hash").getAsString().equals(blockHash));
        Assert.assertEquals(109, ((JsonObject)result).get("size").getAsInt());
        Assert.assertEquals(0, ((JsonObject)result).get("version").getAsInt());
        Assert.assertEquals(true, ((JsonObject)result).get("previousblockhash").getAsString().equals("0x0000000000000000000000000000000000000000000000000000000000000000"));
        Assert.assertEquals(true, ((JsonObject)result).get("merkleroot").getAsString().equals("0x4746f4904d47a013726943e24aa17c05c7e2cea92991cb3bd66fd4c0b3c8e265"));
        Assert.assertEquals(blockTimeStramp, ((JsonObject)result).get("time").getAsInt());
        Assert.assertEquals(0, ((JsonObject)result).get("index").getAsInt());
        Assert.assertEquals(true, ((JsonObject)result).get("nonce").getAsString().equals("2083236893"));
        Assert.assertEquals(true, ((JsonObject)result).get("nextconsensus").getAsString().equals("0x55bfa4cc95efe9bb65c104bf27385d2b655de759"));
        Assert.assertEquals(2, ((JsonObject)result).getAsJsonObject("script").size());
        Assert.assertEquals(true, ((JsonObject)result).getAsJsonObject("script").get("invocation").getAsString().equals(""));
        Assert.assertEquals(true, ((JsonObject)result).getAsJsonObject("script").get("verification").getAsString().equals("51"));
        Assert.assertEquals(true, ((JsonObject)result).get("confirmations").getAsString().equals("1"));

        //getblockhash
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=getblockhash&params=[0]&id=1");
        Assert.assertEquals(String.class, result.getClass());
        Assert.assertEquals(true, result.equals(blockHash));

        //getblocksysfee
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=getblocksysfee&params=[0]&id=1");
        Assert.assertEquals(Integer.class, result.getClass());
        Assert.assertEquals(0, result);
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=getblocksysfee&params=[10]&id=1");
        Assert.assertNull(result);

        //getconnectioncount
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=getconnectioncount&params=[]&id=1");
        Assert.assertEquals(Integer.class, result.getClass());
        Assert.assertEquals(0, result);

        //getcontractstate
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=getcontractstate&params=[\""
                + script_hash + "\"]&id=1");
        Assert.assertNull(result);

        //getnewaddress
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=getnewaddress&params=[]&id=1");
        Assert.assertEquals(String.class, result.getClass());

        //getrawmempool
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=getrawmempool&params=[]&id=1");
        Assert.assertEquals(JsonArray.class, result.getClass());
        Assert.assertEquals(0, ((JsonArray)result).size());

        //getrawtransaction
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=getrawtransaction&params=[\""
                + assetid + "\"]&id=1");
        Assert.assertEquals(String.class, result.getClass());
        Assert.assertEquals(true, result.equals(transactionInfo));
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=getrawtransaction&params=[\""
                + assetid + "\",1]&id=1");
        Assert.assertEquals(JsonObject.class, result.getClass());
        Assert.assertEquals(14, ((JsonObject)result).size());
        Assert.assertEquals(true, UInt256.parse(((JsonObject)result).get("txid").getAsString()).equals(UInt256.parse(assetid)));
        Assert.assertEquals(107, ((JsonObject)result).get("size").getAsInt());
        Assert.assertEquals(TransactionType.RegisterTransaction.value(), ((JsonObject)result).get("type").getAsInt());
        Assert.assertEquals(0, ((JsonObject)result).get("version").getAsInt());
        Assert.assertEquals(0, ((JsonObject)result).getAsJsonArray("attributes").size());
        Assert.assertEquals(0, ((JsonObject)result).getAsJsonArray("vin").size());
        Assert.assertEquals(0, ((JsonObject)result).getAsJsonArray("vout").size());
        Assert.assertEquals(true, ((JsonObject)result).get("sys_fee").getAsString().equals("0.00000000"));
        Assert.assertEquals(true, ((JsonObject)result).get("net_fee").getAsString().equals("0.00000000"));
        Assert.assertEquals(6, ((JsonObject)result).getAsJsonObject("asset").size());
        Assert.assertEquals(AssetType.GoverningToken.value(), ((JsonObject)result).getAsJsonObject("asset").get("type").getAsInt());
        Assert.assertEquals(2, ((JsonObject)result).getAsJsonObject("asset").getAsJsonArray("name").size());
        Assert.assertEquals(true, ((JsonObject)result).getAsJsonObject("asset").getAsJsonArray("name").get(0).getAsJsonObject().get("lang").getAsString().equals("zh-CN"));
        Assert.assertEquals(true, ((JsonObject)result).getAsJsonObject("asset").getAsJsonArray("name").get(0).getAsJsonObject().get("name").getAsString().equals("小蚁股"));
        Assert.assertEquals(true, ((JsonObject)result).getAsJsonObject("asset").getAsJsonArray("name").get(1).getAsJsonObject().get("lang").getAsString().equals("en"));
        Assert.assertEquals(true, ((JsonObject)result).getAsJsonObject("asset").getAsJsonArray("name").get(1).getAsJsonObject().get("name").getAsString().equals("AntShare"));
        Assert.assertEquals(true, ((JsonObject)result).get("blockhash").getAsString().equals(blockHash));
        Assert.assertEquals(1, ((JsonObject)result).get("confirmations").getAsInt());
        Assert.assertEquals(blockTimeStramp, ((JsonObject)result).get("blocktime").getAsInt());

        //getstorage
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=getstorage&params=[\""
                + script_hash + "\",\"5065746572\"]&id=1");
        Assert.assertNull(result);

        //gettxout
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=gettxout&params=[\""
                + assetid + "\",0]&id=1");
        Assert.assertNull(result);

        //getpeers
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=getpeers&params=[]&id=1");
        Assert.assertEquals(JsonObject.class, result.getClass());
        Assert.assertEquals(3, ((JsonObject)result).size());
        Assert.assertEquals(0, ((JsonObject)result).getAsJsonArray("unconnected").size());
        Assert.assertEquals(0, ((JsonObject)result).getAsJsonArray("bad").size());
        Assert.assertEquals(0, ((JsonObject)result).getAsJsonArray("connected").size());

        //getvalidators
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=getvalidators&params=[]&id=1");
        Assert.assertEquals(JsonArray.class, result.getClass());
        Assert.assertEquals(0, ((JsonArray)result).size());

        //getversion
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=getversion&params=[]&id=1");
        Assert.assertEquals(JsonObject.class, result.getClass());
        Assert.assertEquals(3, ((JsonObject)result).size());
        Assert.assertEquals(0, ((JsonObject)result).get("port").getAsInt());
        Assert.assertEquals(LocalNode.NONCE.intValue(), ((JsonObject)result).get("nonce").getAsInt());
        Assert.assertEquals(true, ((JsonObject)result).get("useragent").getAsString().equals("/neo-java:/2.9.2"));

        //getwalletheight
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=getwalletheight&params=[]&id=1");
        Assert.assertEquals(Integer.class, result.getClass());
        Assert.assertEquals(0, result);

        //invoke
        JsonArray array = new JsonArray();
        JsonObject object = new JsonObject();
        object.addProperty("type", Boolean.class.getSimpleName());
        object.addProperty("value", Boolean.FALSE.toString());
        array.add(object);
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=invoke&params=[\"" +  UInt160.Zero + "\"," + array.toString() + "]&id=1");
        Assert.assertEquals(JsonObject.class, result.getClass());
        Assert.assertEquals(5, ((JsonObject)result).size());
        Assert.assertEquals(true, ((JsonObject)result).get("script").getAsString().equals("00670000000000000000000000000000000000000000"));
        Assert.assertEquals(true, ((JsonObject)result).get("state").getAsString().equals(VMState.FAULT.toString()));
        Assert.assertEquals(true, new BigDecimal(((JsonObject)result).get("gas_consumed").getAsString()).equals(new BigDecimal("0.00000000")));
        Assert.assertEquals(0, ((JsonObject)result).get("stack").getAsJsonArray().size());
        Assert.assertEquals(true, ((JsonObject)result).get("tx").getAsString().equals("d1011600670000000000000000000000000000000000000000000000000000000000000000"));

        //invokefunction
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=invokefunction&params=[\"" +  UInt160.Zero + "\",\"Hello\"," + array.toString() + "]&id=1");
        Assert.assertEquals(JsonObject.class, result.getClass());
        Assert.assertEquals(5, ((JsonObject)result).size());
        Assert.assertEquals(true, ((JsonObject)result).get("script").getAsString().equals("0051c10548656c6c6f670000000000000000000000000000000000000000"));
        Assert.assertEquals(true, ((JsonObject)result).get("state").getAsString().equals(VMState.FAULT.toString()));
        Assert.assertEquals(true, new BigDecimal(((JsonObject)result).get("gas_consumed").getAsString()).equals(new BigDecimal("0.00000000")));
        Assert.assertEquals(0, ((JsonObject)result).get("stack").getAsJsonArray().size());
        Assert.assertEquals(true, ((JsonObject)result).get("tx").getAsString().equals("d1011e0051c10548656c6c6f670000000000000000000000000000000000000000000000000000000000000000"));

        //invokescript
        String script = "00046e616d656724058e5e1b6008847cd662728549088a9ee82191";
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=invokescript&params=[\"" + script + "\"]&id=1");
        Assert.assertEquals(JsonObject.class, result.getClass());
        Assert.assertEquals(5, ((JsonObject)result).size());
        Assert.assertEquals(true, ((JsonObject)result).get("script").getAsString().equals("00046e616d656724058e5e1b6008847cd662728549088a9ee82191"));
        Assert.assertEquals(true, ((JsonObject)result).get("state").getAsString().equals(VMState.FAULT.toString()));
        Assert.assertEquals(true, new BigDecimal(((JsonObject)result).get("gas_consumed").getAsString()).equals(new BigDecimal("0.00000000")));
        Assert.assertEquals(0, ((JsonObject)result).get("stack").getAsJsonArray().size());
        Assert.assertEquals(true, ((JsonObject)result).get("tx").getAsString().equals("d1011b00046e616d656724058e5e1b6008847cd662728549088a9ee82191000000000000000000000000"));

        //listaddress
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=listaddress&params=[]&id=1");
        Assert.assertEquals(JsonArray.class, result.getClass());
        Assert.assertEquals(8, ((JsonArray)result).size());
        ((JsonArray)result).get(0).getAsJsonObject().get("address").getAsString();
        Assert.assertEquals(true, ((JsonArray)result).get(0).getAsJsonObject().get("haskey").getAsBoolean());
        Assert.assertEquals(new JsonNull(), ((JsonArray)result).get(0).getAsJsonObject().get("label"));
        Assert.assertEquals(false, ((JsonArray)result).get(0).getAsJsonObject().get("watchonly").getAsBoolean());

        //sendrawtransaction
        AssetDescriptor descriptor = new AssetDescriptor(UIntBase.parse(assetid));
        UInt160 from = neo.wallets.Helper.toScriptHash(address);
        UInt160 to = neo.wallets.Helper.toScriptHash(address);
        BigDecimal value = BigDecimal.valueOf(1);
        value.setScale(descriptor.decimals);
        Fixed8 fee = Fixed8.ZERO;
        UInt160 change_address = null;
        Transaction tx = wallet.makeTransaction(null, Arrays.asList(new TransferOutput[]
                {
                        new TransferOutput(UIntBase.parse(assetid), value, to)
                }), from, change_address, fee);
        ContractParametersContext context = new ContractParametersContext(tx);
        wallet.sign(context);
        if (context.completed()) {
            tx.witnesses = context.getWitnesses();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        BinaryWriter writer = new BinaryWriter(output);
        tx.serialize(writer);

        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=sendrawtransaction&params=[\""
                + BitConverter.toHexString(output.toByteArray()) + "\",1]&id=1");
        Assert.assertNull(result);

        //sendfrom
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=sendfrom&params=[\"" + assetid + "\",\"" + address + "\",\""+ address + "\",1]&id=1");
        Assert.assertEquals(JsonObject.class, result.getClass());
        Assert.assertEquals(10, ((JsonObject)result).size());
        ((JsonObject)result).get("txid").getAsString();
        Assert.assertEquals(262, ((JsonObject)result).get("size").getAsInt());
        Assert.assertEquals(TransactionType.ContractTransaction.value(), ((JsonObject)result).get("type").getAsInt());
        Assert.assertEquals(0, ((JsonObject)result).get("version").getAsInt());
        Assert.assertEquals(0, ((JsonObject)result).getAsJsonArray("attributes").size());
        Assert.assertEquals(1, ((JsonObject)result).getAsJsonArray("vin").size());
        Assert.assertEquals(2, ((JsonObject)result).getAsJsonArray("vout").size());
        Assert.assertEquals(0, ((JsonObject)result).getAsJsonArray("vout").get(0).getAsJsonObject().get("n").getAsInt());
        Assert.assertEquals(true, UInt256.parse(((JsonObject)result).getAsJsonArray("vout").get(0).getAsJsonObject().get("asset").getAsString()).equals(UInt256.parse(assetid)));
        Assert.assertEquals(true, new BigDecimal(((JsonObject)result).getAsJsonArray("vout").get(0).getAsJsonObject().get("value").getAsString()).equals(new BigDecimal("1.00000000")));
        Assert.assertEquals(true, new BigDecimal(((JsonObject)result).get("sys_fee").getAsString()).equals(new BigDecimal("0.00000000")));
        Assert.assertEquals(true, new BigDecimal(((JsonObject)result).get("net_fee").getAsString()).equals(new BigDecimal("1000.00000000")));
        Assert.assertEquals(1, ((JsonObject)result).getAsJsonArray("scripts").size());

        //sendtoaddress
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=sendtoaddress&params=[\"" + assetid + "\",\"" + address + "\",1]&id=1");
        Assert.assertEquals(JsonObject.class, result.getClass());
        Assert.assertEquals(10, ((JsonObject)result).size());
        ((JsonObject)result).get("txid").getAsString();
        Assert.assertEquals(262, ((JsonObject)result).get("size").getAsInt());
        Assert.assertEquals(TransactionType.ContractTransaction.value(), ((JsonObject)result).get("type").getAsInt());
        Assert.assertEquals(0, ((JsonObject)result).get("version").getAsInt());
        Assert.assertEquals(0, ((JsonObject)result).getAsJsonArray("attributes").size());
        Assert.assertEquals(1, ((JsonObject)result).getAsJsonArray("vin").size());
        Assert.assertEquals(2, ((JsonObject)result).getAsJsonArray("vout").size());
        Assert.assertEquals(0, ((JsonObject)result).getAsJsonArray("vout").get(0).getAsJsonObject().get("n").getAsInt());
        Assert.assertEquals(true, UInt256.parse(((JsonObject)result).getAsJsonArray("vout").get(0).getAsJsonObject().get("asset").getAsString()).equals(UInt256.parse(assetid)));
        Assert.assertEquals(true, new BigDecimal(((JsonObject)result).getAsJsonArray("vout").get(0).getAsJsonObject().get("value").getAsString()).equals(new BigDecimal("1.00000000")));
        Assert.assertEquals(true, new BigDecimal(((JsonObject)result).get("sys_fee").getAsString()).equals(new BigDecimal("0.00000000")));
        Assert.assertEquals(true, new BigDecimal(((JsonObject)result).get("net_fee").getAsString()).equals(new BigDecimal("1000.00000000")));
        Assert.assertEquals(1, ((JsonObject)result).getAsJsonArray("scripts").size());

        //sendmany
        object = new JsonObject();
        object.addProperty("asset", assetid);
        object.addProperty("value", 1);
        object.addProperty("address", address);
        array = new JsonArray();
        array.add(object);
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=sendmany&params=[" + array.toString() + "]&id=1");
        Assert.assertEquals(JsonObject.class, result.getClass());
        Assert.assertEquals(10, ((JsonObject)result).size());
        ((JsonObject)result).get("txid").getAsString();
        Assert.assertEquals(262, ((JsonObject)result).get("size").getAsInt());
        Assert.assertEquals(TransactionType.ContractTransaction.value(), ((JsonObject)result).get("type").getAsInt());
        Assert.assertEquals(0, ((JsonObject)result).get("version").getAsInt());
        Assert.assertEquals(0, ((JsonObject)result).getAsJsonArray("attributes").size());
        Assert.assertEquals(1, ((JsonObject)result).getAsJsonArray("vin").size());
        Assert.assertEquals(2, ((JsonObject)result).getAsJsonArray("vout").size());
        Assert.assertEquals(0, ((JsonObject)result).getAsJsonArray("vout").get(0).getAsJsonObject().get("n").getAsInt());
        Assert.assertEquals(true, UInt256.parse(((JsonObject)result).getAsJsonArray("vout").get(0).getAsJsonObject().get("asset").getAsString()).equals(UInt256.parse(assetid)));
        Assert.assertEquals(true, new BigDecimal(((JsonObject)result).getAsJsonArray("vout").get(0).getAsJsonObject().get("value").getAsString()).equals(new BigDecimal("1.00000000")));
        Assert.assertEquals(true, new BigDecimal(((JsonObject)result).get("sys_fee").getAsString()).equals(new BigDecimal("0.00000000")));
        Assert.assertEquals(true, new BigDecimal(((JsonObject)result).get("net_fee").getAsString()).equals(new BigDecimal("1000.00000000")));
        Assert.assertEquals(1, ((JsonObject)result).getAsJsonArray("scripts").size());

        //validateaddress
        result = httpGetResult("http://localhost:8080/?jsonrpc=2.0&method=validateaddress&params=[\""
                + address + "\"]&id=1");
        Assert.assertEquals(JsonObject.class, result.getClass());
        Assert.assertEquals(2, ((JsonObject)result).size());
        Assert.assertEquals(true, ((JsonObject)result).get("address").getAsString().equals(address));
        Assert.assertEquals(true, ((JsonObject)result).get("isvalid").getAsBoolean());
    }

    public Object httpGetResult(String request) {
        try {
            URLConnection connection = new URL(request).openConnection();
            HttpURLConnection httpUrlConnection = (HttpURLConnection) connection;
            httpUrlConnection.setConnectTimeout(300000);
            httpUrlConnection.setReadTimeout(300000);
            httpUrlConnection.connect();
            String answer = "";
            int contentLength = httpUrlConnection.getContentLength();
            if (contentLength > 0) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null)
                    answer += "\n" + inputLine;
            }
            JsonObject json = new JsonParser().parse(answer).getAsJsonObject();
            JsonElement result = json.get("result");
            if (result != null) {
                if (result.isJsonArray()) {
                    return result.getAsJsonArray();
                } else if (result.isJsonObject()) {
                    return result.getAsJsonObject();
                }
                if (result.equals(new JsonNull())) {
                    return null;
                }
                try {
                    return result.getAsInt();
                } catch (NumberFormatException e) {
                    return result.getAsString();
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }*/
}