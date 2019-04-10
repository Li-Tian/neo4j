package neo.smartcontract;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import akka.testkit.TestActorRef;
import akka.testkit.TestKit;
import neo.Fixed8;
import neo.MyNeoSystem;
import neo.ledger.Blockchain;
import neo.ledger.MyBlockchain2;
import neo.network.p2p.MyLocalNode;
import neo.network.p2p.TaskManager;
import neo.persistence.AbstractBlockchainTest;
import neo.persistence.AbstractLeveldbTest;
import neo.vm.OpCode;
import neo.vm.ScriptBuilder;
import neo.vm.VMState;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: ApplicationEngineTest
 * @Package neo.smartcontract
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 10:56 2019/4/10
 */
public class ApplicationEngineTest extends AbstractBlockchainTest {

    @BeforeClass
    public static void setUp() throws IOException {
        AbstractBlockchainTest.setUp(ApplicationEngineTest.class.getSimpleName());
    }

    @AfterClass
    public static void tearDown() throws IOException {
        AbstractLeveldbTest.tearDown(ApplicationEngineTest.class.getSimpleName());
    }

    @Test
    public void getGasConsumed() throws Exception {
        neoSystem = new MyNeoSystem(store, self -> {
            testKit = new TestKit(self.actorSystem);

            // Synchronous Unit Testing with TestActorRef
            self.blockchain = TestActorRef.create(self.actorSystem, MyBlockchain2.props(self, store, testKit.testActor()));
            self.localNode = TestActorRef.create(self.actorSystem, MyLocalNode.props(self, testKit.testActor()));
            self.taskManager = TestActorRef.create(self.actorSystem, TaskManager.props(self));
            self.consensus = null;
        });
        ScriptBuilder sb = new ScriptBuilder();//asset_id_160
        sb.emitPush("aaaa");
        byte[]script = sb.toArray();
        ApplicationEngine engine = ApplicationEngine.run(script);
        if (engine.state.hasFlag(VMState.FAULT)) throw new IllegalArgumentException();
    }

    @Test
    public void getService() throws Exception {

    }

    @Test
    public void dispose() throws Exception {
        neoSystem = new MyNeoSystem(store, self -> {
            testKit = new TestKit(self.actorSystem);

            // Synchronous Unit Testing with TestActorRef
            self.blockchain = TestActorRef.create(self.actorSystem, MyBlockchain2.props(self, store, testKit.testActor()));
            self.localNode = TestActorRef.create(self.actorSystem, MyLocalNode.props(self, testKit.testActor()));
            self.taskManager = TestActorRef.create(self.actorSystem, TaskManager.props(self));
            self.consensus = null;
        });
        ScriptBuilder sb = new ScriptBuilder();//asset_id_160
        sb.emitPush("aaaa");
        byte[]script = sb.toArray();
        ApplicationEngine engine = ApplicationEngine.run(script);
        if (engine.state.hasFlag(VMState.FAULT)) throw new IllegalArgumentException();
        engine.dispose();
    }

    @Test
    public void execute2() throws Exception {

    }

    @Test
    public void getPrice() throws Exception {

    }

    @Test
    public void getPriceForSysCall() throws Exception {

    }

    @Test
    public void run() throws Exception {
    }

    @Test
    public void run1() throws Exception {
        neoSystem = new MyNeoSystem(store, self -> {
            testKit = new TestKit(self.actorSystem);

            // Synchronous Unit Testing with TestActorRef
            self.blockchain = TestActorRef.create(self.actorSystem, MyBlockchain2.props(self, store, testKit.testActor()));
            self.localNode = TestActorRef.create(self.actorSystem, MyLocalNode.props(self, testKit.testActor()));
            self.taskManager = TestActorRef.create(self.actorSystem, TaskManager.props(self));
            self.consensus = null;
        });
        ScriptBuilder sb = new ScriptBuilder();//asset_id_160
        sb.emit(OpCode.PUSH0);
        byte[]script = sb.toArray();
        ApplicationEngine engine = ApplicationEngine.run(script, Blockchain.singleton().getStore
                ().getSnapshot());
        if (engine.state.hasFlag(VMState.FAULT)) throw new IllegalArgumentException();
        engine.dispose();
    }

    @Test
    public void run2() throws Exception {
        neoSystem = new MyNeoSystem(store, self -> {
            testKit = new TestKit(self.actorSystem);

            // Synchronous Unit Testing with TestActorRef
            self.blockchain = TestActorRef.create(self.actorSystem, MyBlockchain2.props(self, store, testKit.testActor()));
            self.localNode = TestActorRef.create(self.actorSystem, MyLocalNode.props(self, testKit.testActor()));
            self.taskManager = TestActorRef.create(self.actorSystem, TaskManager.props(self));
            self.consensus = null;
        });
        ScriptBuilder sb = new ScriptBuilder();//asset_id_160
        sb.emitPush("aaaa");
        byte[]script = sb.toArray();
        ApplicationEngine engine = ApplicationEngine.run(script,null,null,false, Fixed8.ZERO);
        if (engine.state.hasFlag(VMState.FAULT)) throw new IllegalArgumentException();
        engine.dispose();
    }

    @Test
    public void run3() throws Exception {
        neoSystem = new MyNeoSystem(store, self -> {
            testKit = new TestKit(self.actorSystem);

            // Synchronous Unit Testing with TestActorRef
            self.blockchain = TestActorRef.create(self.actorSystem, MyBlockchain2.props(self, store, testKit.testActor()));
            self.localNode = TestActorRef.create(self.actorSystem, MyLocalNode.props(self, testKit.testActor()));
            self.taskManager = TestActorRef.create(self.actorSystem, TaskManager.props(self));
            self.consensus = null;
        });
        ScriptBuilder sb = new ScriptBuilder();//asset_id_160
        sb.emitPush("aaaa");
        byte[]script = sb.toArray();
        ApplicationEngine engine = ApplicationEngine.run(script);
        if (engine.state.hasFlag(VMState.FAULT)) throw new IllegalArgumentException();
        engine.dispose();
    }

}