package neo.smartcontract;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Iterator;

import akka.testkit.TestActorRef;
import akka.testkit.TestKit;
import neo.Fixed8;
import neo.MyNeoSystem;
import neo.NeoSystem;
import neo.UInt160;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.network.p2p.payloads.IVerifiable;
import neo.wallets.KeyPair;
import neo.consensus.MyWallet;
import neo.cryptography.ecc.ECPoint;
import neo.csharp.Uint;
import neo.csharp.Ushort;
import neo.ledger.Blockchain;
import neo.ledger.MyBlockchain2;
import neo.ledger.MyConsensusService;
import neo.ledger.TransactionState;
import neo.network.p2p.MyLocalNode;
import neo.network.p2p.MyTaskManager;
import neo.network.p2p.payloads.CoinReference;
import neo.network.p2p.payloads.ContractTransaction;
import neo.network.p2p.payloads.MinerTransaction;
import neo.network.p2p.payloads.TransactionOutput;
import neo.network.p2p.payloads.Witness;
import neo.persistence.AbstractLeveldbTest;
import neo.persistence.Snapshot;
import neo.wallets.WalletAccount;

public class ContractParametersContextTest extends AbstractLeveldbTest {
    private static NeoSystem neoSystem;
    private static TestKit testKit;

    @BeforeClass
    public static void setUp() throws IOException {
        AbstractLeveldbTest.setUp(ContractParametersContextTest.class.getSimpleName());

        neoSystem = new MyNeoSystem(store, self -> {
            testKit = new TestKit(self.actorSystem);
            // Synchronous Unit Testing with TestActorRef
            self.blockchain = TestActorRef.create(self.actorSystem, MyBlockchain2.props(self, store, testKit.testActor()));
            self.localNode = TestActorRef.create(self.actorSystem, MyLocalNode.props(self, testKit.testActor()));
            self.taskManager = TestActorRef.create(self.actorSystem, MyTaskManager.props(self, testKit.testActor()));
            self.consensus = TestActorRef.create(self.actorSystem, MyConsensusService.props(self, testKit.testActor()));
        });
    }

    @Test
    public void test() {
        // prepare data
        Snapshot snapshot = store.getSnapshot();
        MyWallet wallet = new MyWallet();
        ECPoint pubKey = wallet.getAccounts().iterator().next().getKey().publicKey;
        KeyPair key = wallet.getAccount(pubKey).getKey();
        UInt160 hash = wallet.getAccount(pubKey).scriptHash;

        MinerTransaction minerTransaction = new MinerTransaction() {{
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                        scriptHash = hash;
                    }}
            };
        }};
        TransactionState txState = new TransactionState() {{
            blockIndex = new Uint(1);
            transaction = minerTransaction;
        }};
        snapshot.getTransactions().add(minerTransaction.hash(), txState);
        snapshot.commit();

        ContractTransaction transaction = new ContractTransaction();
        transaction.inputs = new CoinReference[]{
                new CoinReference() {{
                    prevHash = minerTransaction.hash();
                    prevIndex = new Ushort(0);
                }}
        };
        transaction.outputs = new TransactionOutput[]{
                new TransactionOutput() {{
                    scriptHash = hash;
                    value = Fixed8.fromDecimal(BigDecimal.valueOf(999));
                    assetId = Blockchain.UtilityToken.hash();
                }}
        };
        transaction.witnesses = new Witness[]{
                new Witness() {{
                    verificationScript = new byte[]{0x00, 0x01};
                    invocationScript = new byte[]{0x02, 0x03};
                }}
        };

        Contract contract = Contract.createMultiSigContract(1, new ECPoint[]{pubKey});
        ContractParametersContext context = new ContractParametersContext(transaction);
        wallet.sign(context);
        byte[] signature = neo.wallets.Helper.sign(context.verifiable, key);
        boolean success = context.addSignature(wallet.getAccount(pubKey).contract, key.publicKey, signature);
        Assert.assertEquals(true, success);

        ContractParametersContext replica = ContractParametersContext.fromJson(context.toJson());
        ContractParametersContext replica2 = ContractParametersContext.parse(context.toString());
        Assert.assertEquals(true, context.toString().equals(replica.toString()));
        Assert.assertEquals(true, context.toString().equals(replica2.toString()));
        Assert.assertEquals(true, context.completed());

        Assert.assertEquals(hash, context.scriptHashes()[0]);
        Witness[] witness = context.getWitnesses();
        Assert.assertEquals(1, witness.length);
        Assert.assertEquals(hash, Helper.toScriptHash(witness[0].verificationScript));
        ContractParameter[] contractParameter = context.getParameters(hash);
        Assert.assertEquals(1, contractParameter.length);
        snapshot.getTransactions().delete(minerTransaction.hash());
        snapshot.commit();
    }

    @Test
    public void testMutliSignatureContract() {
        MyWallet wallet = new MyWallet();
        Iterator<? extends WalletAccount> iterator = wallet.getAccounts().iterator();
        WalletAccount account1 = iterator.next();
        WalletAccount account2 = iterator.next();
        WalletAccount account3 = iterator.next();

        System.err.println("account1: " + account1.scriptHash);
        System.err.println("account2: " + account2.scriptHash);
        System.err.println("account3: " + account3.scriptHash);

        Contract contract = Contract.createMultiSigContract(2, new ECPoint[]{account1.getKey().publicKey, account2.getKey().publicKey, account3.getKey().publicKey});

        MyVerificable verificable = new MyVerificable() {{
            message = "hello world";
            hashesForVerify = new UInt160[]{contract.scriptHash()};
        }};

        ContractParametersContext context = new ContractParametersContext(verificable);
        boolean success = wallet.sign(context);
        System.err.println("add signature => " + success);

        byte[] signature1 = neo.wallets.Helper.sign(verificable, account1.getKey());
        byte[] signature2 = neo.wallets.Helper.sign(verificable, account2.getKey());
        boolean success1 = context.addSignature(contract, account1.getKey().publicKey, signature1);
//        boolean success2 = context.addSignature(contract, account2.getKey().publicKey, signature1);

        System.err.println(context.completed());

        System.err.println(context.toJson());
    }

    public static class MyVerificable implements IVerifiable {

        public Witness witness;
        public String message;
        public UInt160[] hashesForVerify;

        @Override
        public Witness[] getWitnesses() {
            return new Witness[]{witness};
        }

        @Override
        public void setWitnesses(Witness[] witnesses) {
            witness = witnesses[0];
        }

        @Override
        public void deserializeUnsigned(BinaryReader reader) {
            message = reader.readVarString();
        }

        @Override
        public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
            return hashesForVerify;
        }

        @Override
        public void serializeUnsigned(BinaryWriter writer) {
            writer.writeVarString(message);
        }

        @Override
        public int size() {
            return message.length();
        }

        @Override
        public void serialize(BinaryWriter writer) {
            writer.writeVarString(message);
        }

        @Override
        public void deserialize(BinaryReader reader) {
            message = reader.readVarString();
        }

        @Override
        public byte[] getMessage() {
            return message.getBytes();
        }
    }
}