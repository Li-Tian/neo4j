package neo.wallets;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import akka.testkit.TestActorRef;
import akka.testkit.TestKit;
import neo.MyNeoSystem;
import neo.ledger.Blockchain;
import neo.ledger.MyBlockchain2;
import neo.network.p2p.MyLocalNode;
import neo.network.p2p.TaskManager;
import neo.persistence.AbstractBlockchainTest;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: AssetDescriptorTest
 * @Package neo.wallets
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 14:50 2019/4/8
 */
public class AssetDescriptorTest extends AbstractBlockchainTest {

    @BeforeClass
    public static void setUp() throws IOException {
        AbstractBlockchainTest.setUp(AssetDescriptorTest.class.getSimpleName());
    }

    @AfterClass
    public static void tearDown() throws IOException {
        AbstractBlockchainTest.tearDown(AssetDescriptorTest.class.getSimpleName());
    }
    @Test
    public void toStringTest() throws Exception {
        neoSystem = new MyNeoSystem(store, self -> {
            testKit = new TestKit(self.actorSystem);

            // Synchronous Unit Testing with TestActorRef
            self.blockchain = TestActorRef.create(self.actorSystem, MyBlockchain2.props(self, store, testKit.testActor()));
            self.localNode = TestActorRef.create(self.actorSystem, MyLocalNode.props(self, testKit.testActor()));
            self.taskManager = TestActorRef.create(self.actorSystem, TaskManager.props(self));
            self.consensus = null;
        });
        AssetDescriptor assetDescriptor=new AssetDescriptor(Blockchain.GoverningToken.hash());
        assetDescriptor.assetName="aaa";
        Assert.assertEquals("aaa",assetDescriptor.toString());
    }

}