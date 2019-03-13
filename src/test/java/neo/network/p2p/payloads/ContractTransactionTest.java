package neo.network.p2p.payloads;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

import neo.Fixed8;
import neo.UInt160;
import neo.Utils;
import neo.csharp.Uint;
import neo.csharp.Ushort;
import neo.csharp.io.BinaryWriter;
import neo.exception.FormatException;
import neo.ledger.Blockchain;
import neo.ledger.TransactionState;
import neo.persistence.Snapshot;

public class ContractTransactionTest extends AbstractBlockchainTest {

    @Test(expected = FormatException.class)
    public void deserializeExclusiveData() {
        ContractTransaction transaction = new ContractTransaction();
        transaction.version = 0;
        try {
            Utils.copyFromSerialize(transaction, ContractTransaction::new);
        } catch (Exception e) {
            Assert.fail();
        }
        transaction.version = 1;
        Utils.copyFromSerialize(transaction, ContractTransaction::new);
    }


    @Test
    public void getFeePerByte() {
        // empty tx, fee/per byte is 0
        ContractTransaction transaction = new ContractTransaction();
        Assert.assertEquals(Fixed8.ZERO, transaction.getFeePerByte());

        // prepare data
        Snapshot snapshot = store.getSnapshot();
        MinerTransaction minerTransaction = new MinerTransaction() {{
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                    }}
            };
        }};
        TransactionState txState = new TransactionState() {{
            blockIndex = new Uint(1);
            transaction = minerTransaction;
        }};
        snapshot.getTransactions().add(minerTransaction.hash(), txState);
        snapshot.commit();

        // transaction
        transaction = new ContractTransaction();
        transaction.inputs = new CoinReference[]{
                new CoinReference() {{
                    prevHash = minerTransaction.hash();
                    prevIndex = new Ushort(0);
                }}
        };
        transaction.outputs = new TransactionOutput[]{
                new TransactionOutput() {{
                    scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                    value = Fixed8.fromDecimal(BigDecimal.valueOf(999));
                    assetId = Blockchain.UtilityToken.hash();
                }}
        };
        // check
        Assert.assertEquals(Fixed8.divide(Fixed8.fromDecimal(BigDecimal.valueOf(1)), transaction.size()), transaction.getFeePerByte());

        // clear data
        snapshot.getTransactions().delete(minerTransaction.hash());
        snapshot.commit();
    }


    @Test
    public void deserializeFrom() {
        ContractTransaction transaction1 = new ContractTransaction();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        BinaryWriter writer = new BinaryWriter(output);

        transaction1.serialize(writer);

        byte[] bytes = output.toByteArray();

        Transaction tx = ContractTransaction.deserializeFrom(bytes);
        Assert.assertTrue(tx instanceof ContractTransaction);
        ContractTransaction tx2 = (ContractTransaction) tx;
        Assert.assertTrue(tx2.equals(transaction1));
    }


    @Test
    public void equals() {
        ContractTransaction transaction1 = new ContractTransaction();
        ContractTransaction transaction2 = new ContractTransaction();
        Assert.assertTrue(transaction1.equals(transaction2));
    }


    @Test
    public void testHashCode() {
        ContractTransaction transaction = new ContractTransaction();
        Assert.assertEquals(transaction.hash().hashCode(), transaction.hashCode());
    }

    @Test
    public void isLowPriority() {
        ContractTransaction transaction = new ContractTransaction();
        Assert.assertEquals(true, transaction.isLowPriority());

        // prepare a high tx

        Snapshot snapshot = store.getSnapshot();
        MinerTransaction minerTransaction = new MinerTransaction() {{
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                    }}
            };
        }};
        TransactionState txState = new TransactionState() {{
            blockIndex = new Uint(1);
            transaction = minerTransaction;
        }};
        snapshot.getTransactions().add(minerTransaction.hash(), txState);
        snapshot.commit();

        // transaction
        transaction = new ContractTransaction();
        transaction.inputs = new CoinReference[]{
                new CoinReference() {{
                    prevHash = minerTransaction.hash();
                    prevIndex = new Ushort(0);
                }}
        };
        transaction.outputs = new TransactionOutput[]{
                new TransactionOutput() {{
                    scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                    value = Fixed8.fromDecimal(BigDecimal.valueOf(990));
                    assetId = Blockchain.UtilityToken.hash();
                }}
        };

        Assert.assertEquals(false, transaction.isLowPriority());
    }

    @Test
    public void getHashData() {
        ContractTransaction transaction = new ContractTransaction();
        byte[] hashdata = transaction.getHashData();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        BinaryWriter writer = new BinaryWriter(output);

        transaction.serializeUnsigned(writer);
        Assert.assertArrayEquals(output.toByteArray(), hashdata);

        byte[] message = transaction.getMessage();
        Assert.assertArrayEquals(hashdata, message);

    }

    @Test
    public void inventoryType() {
        ContractTransaction transaction = new ContractTransaction();
        Assert.assertEquals(InventoryType.Tr, transaction.inventoryType());
    }

    @Test
    public void getWitnesses() {
        ContractTransaction transaction = new ContractTransaction();
        Witness[] witnesses = new Witness[]{
                new Witness() {{
                    verificationScript = new byte[]{0x00, 0x01};
                    invocationScript = new byte[]{0x02, 0x03};
                }}
        };
        transaction.setWitnesses(witnesses);
        Witness[] witnesses2 = transaction.getWitnesses();
        Assert.assertEquals(witnesses.length, witnesses2.length);
        Assert.assertArrayEquals(witnesses[0].verificationScript, witnesses2[0].verificationScript);
        Assert.assertArrayEquals(witnesses[0].invocationScript, witnesses2[0].invocationScript);
    }
}