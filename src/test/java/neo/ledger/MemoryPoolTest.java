package neo.ledger;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

import akka.testkit.TestActorRef;
import akka.testkit.TestKit;
import neo.MyNeoSystem;
import neo.NeoSystem;
import neo.consensus.ConsensusServiceTest;
import neo.csharp.Out;
import neo.csharp.Uint;
import neo.network.p2p.MyLocalNode;
import neo.network.p2p.MyTaskManager;
import neo.network.p2p.payloads.Block;
import neo.network.p2p.payloads.Transaction;
import neo.network.p2p.payloads.TransactionAttribute;
import neo.network.p2p.payloads.TransactionAttributeUsage;
import neo.persistence.AbstractLeveldbTest;
import neo.plugins.MyPlugin;

public class MemoryPoolTest extends AbstractLeveldbTest {
    private static NeoSystem neoSystem;
    private static TestKit testKit;

    @BeforeClass
    public static void setUp() throws IOException {
        AbstractLeveldbTest.setUp(MemoryPoolTest.class.getSimpleName());
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
        AbstractLeveldbTest.tearDown(MemoryPoolTest.class.getSimpleName());
    }

    @Test
    public void memoryPoolTest() {
        MyPlugin plugin = new MyPlugin();
        MemoryPool memoryPool = new MemoryPool(neoSystem, 10);

        ConsensusServiceTest.MyContractTransaction[] contractTransactions = new ConsensusServiceTest.MyContractTransaction[11];
        for (int i = 0; i < contractTransactions.length; i++) {
            int pos = i;
            contractTransactions[i] = new ConsensusServiceTest.MyContractTransaction() {{
                attributes = new TransactionAttribute[]{
                        new TransactionAttribute() {{
                            usage = TransactionAttributeUsage.Remark1;
                            data = ("Transaction " + pos).getBytes();
                        }}
                };
            }};
            Assert.assertEquals(true, memoryPool.tryAdd(contractTransactions[i].hash(), contractTransactions[i]));
        }
        //contractTransactions[3] is kicked off due to lowest hash value.
        Assert.assertEquals(10, memoryPool.count());
        Assert.assertEquals(0, memoryPool.unVerifiedCount());
        Assert.assertEquals(10, memoryPool.verifiedCount());
        Assert.assertEquals(true, memoryPool.containsKey(contractTransactions[0].hash()));
        Assert.assertEquals(true, memoryPool.containsKey(contractTransactions[1].hash()));
        Assert.assertEquals(true, memoryPool.containsKey(contractTransactions[2].hash()));
        Assert.assertEquals(false, memoryPool.containsKey(contractTransactions[3].hash()));
        Assert.assertEquals(true, memoryPool.containsKey(contractTransactions[4].hash()));
        Assert.assertEquals(true, memoryPool.containsKey(contractTransactions[5].hash()));
        Assert.assertEquals(true, memoryPool.containsKey(contractTransactions[6].hash()));
        Assert.assertEquals(true, memoryPool.containsKey(contractTransactions[7].hash()));
        Assert.assertEquals(true, memoryPool.containsKey(contractTransactions[8].hash()));
        Assert.assertEquals(true, memoryPool.containsKey(contractTransactions[9].hash()));
        Assert.assertEquals(true, memoryPool.containsKey(contractTransactions[10].hash()));

        Out<Transaction> transaction = new Out<>();
        memoryPool.tryGetValue(contractTransactions[0].hash(), transaction);
        Assert.assertEquals(contractTransactions[0], transaction.get());
        Assert.assertEquals(10, memoryPool.getEnumerator().size());
        Assert.assertEquals(10, memoryPool.getVerifiedTransactions().size());

        Out<Collection<Transaction>> verifiedTransactions = new Out<>();
        Out<Collection<Transaction>> unverifiedTransactions = new Out<>();
        memoryPool.getVerifiedAndUnverifiedTransactions(verifiedTransactions, unverifiedTransactions);
        Assert.assertEquals(10, verifiedTransactions.get().size());
        Assert.assertEquals(0, unverifiedTransactions.get().size());

        Collection<Transaction> sortedVerifiedTransactions = memoryPool.getSortedVerifiedTransactions();
        List<Transaction> sortedVerifiedTransactionList = sortedVerifiedTransactions.stream().collect(Collectors.toList());
        Assert.assertEquals(10, sortedVerifiedTransactionList.size());
        for (int i = 0; i < sortedVerifiedTransactionList.size() - 1; i++) {
            Assert.assertEquals(true, sortedVerifiedTransactionList.get(i).hash().compareTo(sortedVerifiedTransactionList.get(i + 1).hash()) < 0);
        }

        Assert.assertEquals(false, memoryPool.canTransactionFitInPool(contractTransactions[3]));

        Block block = new Block() {
            {
                index = Uint.ZERO;
                transactions = contractTransactions;
            }
        };
        memoryPool.updatePoolForBlockPersisted(block, store.getSnapshot());
        Assert.assertEquals(0, memoryPool.count());
        Assert.assertEquals(0, memoryPool.getSortedVerifiedTransactions().size());
    }
}