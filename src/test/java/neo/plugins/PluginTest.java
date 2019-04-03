package neo.plugins;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.typesafe.config.Config;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import akka.testkit.TestActorRef;
import akka.testkit.TestKit;
import neo.MyNeoSystem;
import neo.NeoSystem;
import neo.ledger.Blockchain;
import neo.ledger.MyBlockchain2;
import neo.ledger.MyConsensusService;
import neo.log.tr.TR;
import neo.network.p2p.MyLocalNode;
import neo.network.p2p.MyTaskManager;
import neo.network.p2p.Peer;
import neo.network.p2p.payloads.ClaimTransaction;
import neo.network.p2p.payloads.Transaction;
import neo.persistence.AbstractLeveldbTest;
import neo.persistence.Snapshot;

public class PluginTest extends AbstractLeveldbTest {
    private class MyIPolicyPlugin implements IPolicyPlugin {
        @Override
        public boolean filterForMemoryPool(Transaction tx) {
            TR.enter();
            return TR.exit(true);
        }

        @Override
        public Collection<Transaction> filterForBlock(Collection<Transaction> transactions) {
            TR.enter();
            return TR.exit(transactions);
        }

        @Override
        public int maxTxPerBlock() {
            TR.enter();
            return TR.exit(10000);
        }

        @Override
        public int maxLowPriorityTxPerBlock() {
            TR.enter();
            return TR.exit(1000);
        }
    }

    private class MyILogPlugin implements ILogPlugin {
        @Override
        public void log(String source, LogLevel level, String message) {
            TR.enter();
            TR.exit();
        }
    }

    private class MyIRpcPlugin implements IRpcPlugin {
        @Override
        public JsonObject onProcess(HttpServletRequest req, HttpServletResponse res, String method, JsonArray _params) {
            TR.enter();
            return TR.exit(new JsonObject());
        }
    }

    private class MyIPersistencePlugin implements IPersistencePlugin {
        @Override
        public void onPersist(Snapshot snapshot, ArrayList<Blockchain.ApplicationExecuted> applicationExecutedList) {
            TR.enter();
            TR.exit();
        }

        @Override
        public void onCommit(Snapshot snapshot) {
            TR.enter();
            TR.exit();
        }

        @Override
        public boolean shouldThrowExceptionFromCommit(Exception ex) {
            TR.enter();
            return TR.exit(true);
        }
    }

    private class MyIMemoryPoolTxObserverPlugin implements IMemoryPoolTxObserverPlugin {
        @Override
        public void transactionAdded(Transaction tx) {
            TR.enter();
            TR.exit();
        }

        @Override
        public void transactionsRemoved(MemoryPoolTxRemovalReason reason, ArrayList<Transaction> transactions) {
            TR.enter();
            TR.exit();
        }
    }

    private static final String pluginsPath = System.getProperty("user.dir") + "\\Plugins";
    private static NeoSystem neoSystem;
    private static TestKit testKit;

    @BeforeClass
    public static void setUp() throws IOException {
        File file = new File(pluginsPath);
        if (file.exists()) {
            for (File subFile : file.listFiles()) {
                for (File subFile2 : subFile.listFiles()) {
                    subFile2.delete();
                }
                subFile.delete();
            }
            file.delete();
        }
        file.mkdir();
        file = new File(pluginsPath + "\\" + MyPlugin.class.getSimpleName());
        file.mkdir();

        neoSystem = new MyNeoSystem(store, self -> {
            testKit = new TestKit(self.actorSystem);
            // Synchronous Unit Testing with TestActorRef
            self.blockchain = TestActorRef.create(self.actorSystem, MyBlockchain2.props(self, store, testKit.testActor()));
            self.localNode = TestActorRef.create(self.actorSystem, MyLocalNode.props(self, testKit.testActor()));
            self.taskManager = TestActorRef.create(self.actorSystem, MyTaskManager.props(self, testKit.testActor()));
            self.consensus = TestActorRef.create(self.actorSystem, MyConsensusService.props(self, testKit.testActor()));
        });
    }

    @AfterClass
    public static void tearDown() throws IOException {
        File file = new File(pluginsPath);
        if (file.exists()) {
            for (File subFile : file.listFiles()) {
                for (File subFile2 : subFile.listFiles()) {
                    subFile2.delete();
                }
                subFile.delete();
            }
            file.delete();
        }
    }

    @Test
    public void nameTest() {
        MyPlugin plugin = new MyPlugin();
        Assert.assertEquals("MyPlugin", plugin.name());
    }

    @Test
    public void configFileTest() {
        MyPlugin plugin = new MyPlugin();
        Assert.assertEquals(pluginsPath + "\\" + plugin.name() + "\\config.json", plugin.configFile());
    }

    @Test
    public void PluginTest() {
        try {
            MyPlugin plugin = new MyPlugin();
            File file = new File(Paths.get(pluginsPath).resolve("MyPlugin1").toString());
            if (!file.exists()) {
                file.mkdir();
            }
            Thread.sleep(1000);
            file = new File(file.toPath().resolve("config.json").toString());
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("{\n" +
                    "  \"PluginConfiguration\": {\n" +
                    "    \"MaxOnImportHeight\": 0\n" +
                    "  }\n" +
                    "}");
            bw.flush();
            bw.close();
            while(true){}
        } catch (Exception e){
        }
    }

    @Test
    public void checkPolicyTest() {
        MyPlugin plugin = new MyPlugin();
        Assert.assertEquals(true, MyPlugin.checkPolicy(new ClaimTransaction()));
    }

    @Test
    public void getConfigurationTest() {
        try {
            MyPlugin plugin = new MyPlugin();
            File file = new File(plugin.configFile());
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("{\n" +
                    "  \"PluginConfiguration\": {\n" +
                    "    \"MaxOnImportHeight\": 0\n" +
                    "  }\n" +
                    "}");
            bw.flush();
            bw.close();
            Config config = plugin.getConfiguration();
            Assert.assertEquals(0, config.getInt("MaxOnImportHeight"));
        } catch (IOException e) {
        }
    }

    @Test
    public void loadPluginsTest () {
        MyPlugin plugin = new MyPlugin();
        MyPlugin.loadPlugins(neoSystem);
    }

    @Test
    public void pluginLogTest() {
        MyPlugin plugin = new MyPlugin();
        plugin.pluginLog("message1", null);
        plugin.pluginLog("message2", LogLevel.Warning);
        Assert.assertEquals(true, plugin.verifyLog("message1", null));
        Assert.assertEquals(true, plugin.verifyLog("message2", LogLevel.Warning));
    }

    @Test
    public void resumeNodeTest () {
        MyPlugin plugin = new MyPlugin();
        plugin.setSystem(neoSystem);
        MyPlugin.suspendNodeStartup();
        neoSystem.startNode(10, 5, 100);
        MyPlugin.resumeNodeStartup();
        testKit.expectMsgClass(Peer.Start.class);
    }

    @Test
    public void sendMessageTest () {
        MyPlugin plugin = new MyPlugin();
        Assert.assertEquals(true, MyPlugin.sendMessage("message"));
    }
}