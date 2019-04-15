package scennario;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;

import akka.testkit.TestActorRef;
import akka.testkit.TestKit;
import neo.MyNeoSystem;
import neo.NeoSystem;
import neo.UInt160;
import neo.Utils;
import neo.csharp.BitConverter;
import neo.ledger.ContractPropertyState;
import neo.ledger.MyBlockchain2;
import neo.network.p2p.MyLocalNode;
import neo.network.p2p.MyTaskManager;
import neo.network.p2p.payloads.InvocationTransaction;
import neo.persistence.AbstractBlockchainTest;
import neo.smartcontract.ContractParameterType;
import neo.vm.ScriptBuilder;
import neo.wallets.KeyPair;

/**
 * 一笔完整的智能合约调用测试
 */
public class InvocationWithSmartContractTest extends AbstractBlockchainTest {

    private final static String privateKey = "f72b8fab85fdcc1bdd20b107e5da1ab4713487bc88fc53b5b134f5eddeaa1a19";
    private final static String publicKey = "031f64da8a38e6c1e5423a72ddd6d4fc4a777abe537e5cb5aa0425685cda8e063b";
    private final static KeyPair keypair = new KeyPair(BitConverter.hexToBytes(privateKey));

    private static final String testFile = "nep5contract.avm";

    private static final byte[] avm = loadAvm();

    private static final UInt160 scriptHash = UInt160.parseToScriptHash(avm);

    protected static TestKit testKit;
    protected static MyBlockchain2 blockchain;
    protected static NeoSystem neoSystem;


    @BeforeClass
    public static void setUp() throws IOException {
        AbstractBlockchainTest.setUp(InvocationWithSmartContractTest.class.getSimpleName());
    }

    @AfterClass
    public static void tearDown() throws IOException {
        AbstractBlockchainTest.tearDown(InvocationWithSmartContractTest.class.getSimpleName());
    }


    private static void init() {
        neoSystem = new MyNeoSystem(store, self -> {
            testKit = new TestKit(self.actorSystem);

            // Synchronous Unit Testing with TestActorRef
            self.blockchain = TestActorRef.create(self.actorSystem, MyBlockchain2.props(self, store, testKit.testActor()));
            self.localNode = TestActorRef.create(self.actorSystem, MyLocalNode.props(self, testKit.testActor()));
            self.taskManager = TestActorRef.create(self.actorSystem, MyTaskManager.props(self, testKit.testActor()));
            self.consensus = null;
        });

        blockchain = (MyBlockchain2) MyBlockchain2.singleton();


    }


    @Test
    public void smartContract() {
        // prepare data
        byte[] script = neo.smartcontract.Contract.createSignatureRedeemScript(keypair.publicKey);
        UInt160 owner = UInt160.parseToScriptHash(script);
        System.err.println(owner.toAddress());

        // check

        // S1: create smart contract
        InvocationTransaction createTx = new InvocationTransaction() {{
            script = createScript();
        }};


        // S2: deploy smart contract
        InvocationTransaction deployTx = new InvocationTransaction() {{
            script = deployScript(scriptHash);
        }};

        // S3: transfer smart contract
        UInt160 from = owner;
        UInt160 to = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        BigInteger amount = BigInteger.valueOf(1000);
        InvocationTransaction transferTx = new InvocationTransaction() {{
            script = transferScript(scriptHash, from, to, amount);
        }};


        // clear data


    }

    private byte[] createScript() {
        byte[] script = loadAvm();
        ContractParameterType[] parameter_list = new ContractParameterType[]{
                ContractParameterType.String,
                ContractParameterType.Array
        };
        ContractParameterType return_type = ContractParameterType.Boolean;
        ContractPropertyState properties = ContractPropertyState.HasStorage.or(ContractPropertyState.HasDynamicInvoke);
        String name = "test";
        String version = "1.0";
        String author = "luc";
        String email = "luchuan@neo.org";
        String description = "test";

        ScriptBuilder sb = new ScriptBuilder();
        neo.vm.Helper.emitSysCall(sb, "Neo.Contract.Create", script, parameter_list, return_type, properties, name, version, author, email, description);
        return sb.toArray();
    }

    private byte[] deployScript(UInt160 scriptHash) {
        ScriptBuilder sb = new ScriptBuilder();
        neo.vm.Helper.emitAppCall(sb, scriptHash, "Main", "deploy");
        return sb.toArray();
    }

    private byte[] transferScript(UInt160 scriptHash, UInt160 from, UInt160 to, BigInteger amount) {
        ScriptBuilder sb = new ScriptBuilder();
        neo.vm.Helper.emitAppCall(sb, scriptHash, "Main", "transfer", from, to, amount);
        return sb.toArray();
    }


    private static byte[] loadAvm() {
        String path = InvocationWithSmartContractTest.class.getClassLoader().getResource("").getPath() + testFile;
        try {
            return Utils.readContentFromFile(path);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


}
