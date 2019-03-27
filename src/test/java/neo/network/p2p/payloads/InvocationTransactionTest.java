package neo.network.p2p.payloads;

import com.google.gson.JsonObject;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;

import neo.log.tr.TR;
import neo.persistence.AbstractBlockchainTest;
import neo.Fixed8;
import neo.UInt160;
import neo.Utils;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.Ushort;
import neo.ledger.Blockchain;
import neo.ledger.CoinState;
import neo.ledger.TransactionState;
import neo.ledger.UnspentCoinState;
import neo.persistence.Snapshot;
import neo.vm.OpCode;

public class InvocationTransactionTest extends AbstractBlockchainTest {

    @BeforeClass
    public static void setUp() throws IOException {
        TR.debug("----  InvocationTransactionTest setUp......");
        AbstractBlockchainTest.setUp();
    }

    @AfterClass
    public static void tearDown() throws IOException {
        TR.debug("----  InvocationTransactionTest tearDown......");
        AbstractBlockchainTest.tearDown();
    }

    @Test
    public void size() {
        InvocationTransaction transaction = new InvocationTransaction() {{
            script = new byte[]{OpCode.PUSHT.getCode()};
            gas = new Fixed8(0);
        }};

        Assert.assertEquals(8, transaction.size());
    }

    @Test
    public void serializeExclusiveData() {
        InvocationTransaction transaction = new InvocationTransaction() {{
            script = new byte[]{OpCode.PUSHT.getCode()};
            gas = new Fixed8(0);
        }};

        InvocationTransaction tmp = Utils.copyFromSerialize(transaction, InvocationTransaction::new);

        Assert.assertEquals(transaction.gas, tmp.gas);
        Assert.assertArrayEquals(transaction.script, tmp.script);
    }

    @Test
    public void getGas() {
        InvocationTransaction transaction = new InvocationTransaction() {{
            script = new byte[]{OpCode.PUSHT.getCode()};
            gas = Fixed8.fromDecimal(BigDecimal.valueOf(12));
        }};

        Assert.assertEquals(Fixed8.fromDecimal(BigDecimal.valueOf(2)), InvocationTransaction.getGas(transaction.gas));

        transaction.gas = Fixed8.fromDecimal(BigDecimal.valueOf(12.02));
        Assert.assertEquals(Fixed8.fromDecimal(BigDecimal.valueOf(3)), InvocationTransaction.getGas(transaction.gas));
    }

    @Test
    public void getSystemFee() {
        InvocationTransaction transaction = new InvocationTransaction() {{
            script = new byte[]{OpCode.PUSHT.getCode()};
            gas = Fixed8.fromDecimal(BigDecimal.valueOf(12));
        }};

        Assert.assertEquals(transaction.gas, transaction.getSystemFee());
    }

    @Test
    public void verify() {
        // need gas
        // prepare data
        Snapshot snapshot = store.getSnapshot();

        // set gas tx and utxo
        MinerTransaction minerTransaction = new MinerTransaction() {{
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(12.2));
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                    }}
            };
        }};
        TransactionState txState = new TransactionState() {{
            blockIndex = new Uint(1);
            transaction = minerTransaction;
        }};
        snapshot.getTransactions().add(minerTransaction.hash(), txState);

        // set uxto
        UnspentCoinState utxo = new UnspentCoinState() {{
            items = new CoinState[]{
                    CoinState.Confirmed
            };
        }};
        snapshot.getUnspentCoins().add(minerTransaction.hash(), utxo);
        snapshot.commit();


        InvocationTransaction transaction = new InvocationTransaction() {{
            script = new byte[]{OpCode.PUSHT.getCode()};
            gas = Fixed8.fromDecimal(BigDecimal.valueOf(12));
            inputs = new CoinReference[]{
                    new CoinReference() {{
                        prevHash = minerTransaction.hash();
                        prevIndex = new Ushort(0);
                    }}
            };
        }};

        boolean result = transaction.verify(store.getSnapshot());
        Assert.assertTrue(result);

        transaction.gas = new Fixed8(10);
        result = transaction.verify(store.getSnapshot());
        Assert.assertFalse(result);

        // clear data
        snapshot.getTransactions().delete(minerTransaction.hash());
        snapshot.getUnspentCoins().delete(minerTransaction.hash());
    }

    @Test
    public void toJson() {
        InvocationTransaction transaction = new InvocationTransaction() {{
            script = new byte[]{OpCode.PUSHT.getCode()};
            gas = Fixed8.fromDecimal(BigDecimal.valueOf(12));
        }};

        JsonObject jsonObject = transaction.toJson();
        Assert.assertEquals(BitConverter.toHexString(transaction.script), jsonObject.get("script").getAsString());
        Assert.assertEquals(transaction.gas.toString(), jsonObject.get("gas").getAsString());
    }

    @Test
    public void testOnlineData() {
        //

    }
}