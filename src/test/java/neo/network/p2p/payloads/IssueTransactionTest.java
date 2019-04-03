package neo.network.p2p.payloads;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;

import neo.log.notr.TR;
import neo.persistence.AbstractBlockchainTest;
import neo.Fixed8;
import neo.ProtocolSettings;
import neo.UInt160;
import neo.UInt256;
import neo.cryptography.ecc.ECC;
import neo.cryptography.ecc.ECPoint;
import neo.csharp.Uint;
import neo.csharp.Ushort;
import neo.exception.FormatException;
import neo.ledger.AssetState;
import neo.ledger.Blockchain;
import neo.ledger.CoinState;
import neo.ledger.TransactionState;
import neo.ledger.UnspentCoinState;
import neo.persistence.Snapshot;

public class IssueTransactionTest extends AbstractBlockchainTest {

    @BeforeClass
    public static void setUp() throws IOException {
        AbstractBlockchainTest.setUp(IssueTransactionTest.class.getSimpleName());
    }

    @AfterClass
    public static void tearDown() throws IOException {
        AbstractBlockchainTest.tearDown(IssueTransactionTest.class.getSimpleName());
    }

    @Test
    public void getSystemFee() {
        IssueTransaction issue = new IssueTransaction();
        issue.version = 1;
        Assert.assertEquals(Fixed8.ZERO, issue.getSystemFee());

        issue.version = 0;
        issue.outputs = new TransactionOutput[]{
                new TransactionOutput() {{
                    assetId = Blockchain.UtilityToken.hash();
                    value = new Fixed8(100);
                    scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                }}
        };
        Assert.assertEquals(Fixed8.ZERO, issue.getSystemFee());

        issue.outputs = new TransactionOutput[0];
        Fixed8 fee = ProtocolSettings.Default.systemFee.get(TransactionType.IssueTransaction);
        fee = fee == null ? Fixed8.ZERO : fee;
        Assert.assertEquals(fee, issue.getSystemFee());
    }

    @Test(expected = FormatException.class)
    public void deserializeExclusiveData() {
        IssueTransaction issue = new IssueTransaction();
        issue.version = 2;

        issue.deserializeExclusiveData(null);
    }

    @Test
    public void getScriptHashesForVerifying() {
        // prepare data
        Snapshot snapshot = store.getSnapshot();
        AssetState assetState = new AssetState();
        assetState.assetId = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        assetState.assetType = AssetType.Share;
        assetState.name = "Test";
        assetState.amount = new Fixed8(100000000);
        assetState.available = new Fixed8(100000000);
        assetState.available = new Fixed8(100000000);
        assetState.precision = 0;
        assetState.fee = Fixed8.ZERO;
        assetState.feeAddress = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        assetState.owner = new ECPoint(ECC.Secp256r1.getCurve().getInfinity());
        assetState.admin = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        assetState.issuer = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff03");
        assetState.expiration = new Uint(1000000);
        assetState.isFrozen = false;
        snapshot.getAssets().add(assetState.assetId, assetState);
        snapshot.commit();


        IssueTransaction issue = new IssueTransaction() {{
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = assetState.assetId;
                        value = new Fixed8(100);
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
                    }}
            };
        }};

        UInt160[] hashArray = issue.getScriptHashesForVerifying(store.getSnapshot());

        Assert.assertEquals(1, hashArray.length);
        Assert.assertEquals(assetState.issuer, hashArray[0]);

        // clear data
        snapshot.getAssets().delete(assetState.assetId);
        snapshot.commit();
    }

    public static class MyIssueTransaction extends IssueTransaction {
        @Override
        public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
            return new UInt160[0];
        }

        @Override
        public Witness[] getWitnesses() {
            return new Witness[0];
        }
    }

    @Test
    public void verify() {
        // prepare data
        Snapshot snapshot = store.getSnapshot();
        AssetState assetState = new AssetState();
        assetState.assetId = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        assetState.assetType = AssetType.Share;
        assetState.name = "Test";
        assetState.amount = Fixed8.fromDecimal(BigDecimal.valueOf(100000000));
        assetState.available = Fixed8.fromDecimal(BigDecimal.valueOf(1000000));
        assetState.precision = 0;
        assetState.fee = Fixed8.ZERO;
        assetState.feeAddress = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        assetState.owner = new ECPoint(ECC.Secp256r1.getCurve().getInfinity());
        assetState.admin = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        assetState.issuer = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff03");
        assetState.expiration = new Uint(1000000);
        assetState.isFrozen = false;
        snapshot.getAssets().add(assetState.assetId, assetState);

        // 1.2 add miner tx
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

        // 1.3 add unspent coin
        UnspentCoinState utxo = new UnspentCoinState() {{
            items = new CoinState[]{
                    CoinState.Confirmed
            };
        }};
        snapshot.getUnspentCoins().add(minerTransaction.hash(), utxo);
        snapshot.commit();

        // IssueTransaction need gas
        MyIssueTransaction issue = new MyIssueTransaction() {{
            inputs = new CoinReference[]{
                    new CoinReference() {{
                        prevHash = minerTransaction.hash();
                        prevIndex = new Ushort(0);
                    }}
            };
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = assetState.assetId;
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(10000));
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
                    }}
            };
        }};

        boolean result = issue.verify(store.getSnapshot());
        Assert.assertTrue(result);

        Fixed8 fee = ProtocolSettings.Default.systemFee.get(TransactionType.IssueTransaction);
        fee = fee == null ? Fixed8.ZERO : fee;
        Fixed8 networkFee = Fixed8.subtract(minerTransaction.outputs[0].value, fee);
        Assert.assertEquals(networkFee, issue.getNetworkFee());


        ArrayList<Transaction> mempool = new ArrayList<>();
        MyIssueTransaction issue2 = new MyIssueTransaction() {{
            inputs = new CoinReference[]{
                    new CoinReference() {{
                        prevHash = minerTransaction.hash();
                        prevIndex = new Ushort(0);
                    }}
            };
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = assetState.assetId;
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(100000000));
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
                    }}
            };
        }};
        mempool.add(issue2);
        result = issue.verify(store.getSnapshot(), mempool);
        Assert.assertFalse(result);

        // clear data
        snapshot.getAssets().delete(assetState.assetId);
        snapshot.commit();
    }
}