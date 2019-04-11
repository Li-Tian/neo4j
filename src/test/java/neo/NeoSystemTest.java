package neo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import akka.testkit.TestActorRef;
import akka.testkit.TestKit;
import neo.consensus.MyWallet;
import neo.ledger.BlockChainTest;
import neo.ledger.Blockchain;
import neo.ledger.MyBlockchain2;
import neo.ledger.MyConsensusService;
import neo.network.p2p.MyLocalNode;
import neo.network.p2p.MyTaskManager;
import neo.network.p2p.Peer;
import neo.persistence.AbstractLeveldbTest;
import neo.wallets.WalletAccount;

public class NeoSystemTest extends AbstractLeveldbTest {
    private static TestKit testKit;
    @BeforeClass
    public static void setup () {
        try {
            AbstractLeveldbTest.setUp(BlockChainTest.class.getSimpleName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void test () throws UnknownHostException {
        NeoSystem system = new NeoSystem(store);
        MyWallet wallet = new MyWallet();
        system.startConsensus(wallet);
        Assert.assertEquals(true, system.consensus != null);
        InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 8080);
        system.startRpc(socketAddress, wallet, "", "", new String[]{}, null);
        Assert.assertEquals(true, system.rpcServer != null);

        system = new MyNeoSystem(store, self -> {
            testKit = new TestKit(self.actorSystem);
            // Synchronous Unit Testing with TestActorRef
            self.blockchain = TestActorRef.create(self.actorSystem, MyBlockchain2.props(self, store, testKit.testActor()));
            self.localNode = TestActorRef.create(self.actorSystem, MyLocalNode.props(self, testKit.testActor()));
            self.taskManager = TestActorRef.create(self.actorSystem, MyTaskManager.props(self, testKit.testActor()));
            self.consensus = TestActorRef.create(self.actorSystem, MyConsensusService.props(self, testKit.testActor()));
        });
        system.startNode(10, 5, 100);
        testKit.expectMsgClass(Peer.Start.class);
        Peer.Start startNodeMessage = ((Peer.Start)testKit.lastMessage().msg());
        Assert.assertEquals(10, startNodeMessage.port);
        Assert.assertEquals(5, startNodeMessage.minDesiredConnections);
        Assert.assertEquals(100, startNodeMessage.maxConnections);

        system.suspendNodeStartup();
        system.startNode(15, 7, 200);
        system.resumeNodeStartup();
        testKit.expectMsgClass(Peer.Start.class);
        startNodeMessage = ((Peer.Start)testKit.lastMessage().msg());
        Assert.assertEquals(15, startNodeMessage.port);
        Assert.assertEquals(7, startNodeMessage.minDesiredConnections);
        Assert.assertEquals(200, startNodeMessage.maxConnections);
    }
}