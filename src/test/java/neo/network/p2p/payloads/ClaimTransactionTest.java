package neo.network.p2p.payloads;

import com.google.gson.JsonObject;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;

import akka.actor.ActorSystem;
import akka.actor.Props;
import neo.Fixed8;
import neo.UInt160;
import neo.UInt256;
import neo.Utils;
import neo.csharp.Uint;
import neo.csharp.Ushort;
import neo.io.caching.DataCache;
import neo.ledger.AssetState;
import neo.ledger.Blockchain;
import neo.ledger.SpentCoinState;
import neo.ledger.TransactionState;
import neo.persistence.Snapshot;
import neo.persistence.leveldb.BlockchainDemo;
import neo.persistence.leveldb.LevelDBStore;

public class ClaimTransactionTest {


    private final static String LEVELDB_TEST_PATH = "Chain_test";

    private LevelDBStore store;
    private BlockchainDemo blockchainDemo;

    @Before
    public void before() throws IOException {
        store = new LevelDBStore(LEVELDB_TEST_PATH);

        ActorSystem system = ActorSystem.create("neosystem");
        system.actorOf(Props.create(BlockchainDemo.class, store));
        blockchainDemo = (BlockchainDemo) BlockchainDemo.singleton();
    }

    @After
    public void after() throws IOException {
        store.close();
        // free leveldb file
        File file = new File(LEVELDB_TEST_PATH);
        if (file.exists()) {
            for (File subFile : file.listFiles()) {
                subFile.delete();
            }
            file.delete();
        }
    }

    @Test
    public void size() {
        ClaimTransaction claimTransaction = new ClaimTransaction() {{
            claims = new CoinReference[]{
                    new CoinReference() {{
                        prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                        prevIndex = new Ushort(0);
                    }},
                    new CoinReference() {{
                        prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                        prevIndex = new Ushort(1);
                    }}
            };
        }};

        // base 6 + 1 + (32 +2) * 2 = 75
        Assert.assertEquals(75, claimTransaction.size());
    }

    @Test
    public void getNetworkFee() {
        ClaimTransaction claimTransaction = new ClaimTransaction() {{
            claims = new CoinReference[]{
                    new CoinReference() {{
                        prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                        prevIndex = new Ushort(0);
                    }},
                    new CoinReference() {{
                        prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                        prevIndex = new Ushort(1);
                    }}
            };
        }};

        Fixed8 fee = claimTransaction.getNetworkFee();
        Assert.assertEquals(Fixed8.ZERO, fee);
    }

    @Test
    public void getScriptHashesForVerifying() {
        // prepare data
        Snapshot snapshot = store.getSnapshot();

        DataCache<UInt256, TransactionState> txdb = snapshot.getTransactions();
        MinerTransaction minerTransaction = new MinerTransaction() {{
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = new Fixed8(10000);
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                    }},
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = new Fixed8(20000);
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
                    }},
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = new Fixed8(30000);
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff03");
                    }}
            };
        }};
        TransactionState txState = new TransactionState() {{
            blockIndex = new Uint(10);
            transaction = minerTransaction;
        }};
        txdb.add(minerTransaction.hash(), txState);
        snapshot.commit();


        // check
        ClaimTransaction claimTransaction = new ClaimTransaction() {{
            claims = new CoinReference[]{
                    new CoinReference() {{
                        prevHash = minerTransaction.hash();
                        prevIndex = new Ushort(0);
                    }},
                    new CoinReference() {{
                        prevHash = minerTransaction.hash();
                        prevIndex = new Ushort(1);
                    }}
            };
        }};
        UInt160[] verifyHashes = claimTransaction.getScriptHashesForVerifying(snapshot);
        Assert.assertEquals(2, verifyHashes.length);
        Assert.assertEquals(UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01"), verifyHashes[0]);
        Assert.assertEquals(UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02"), verifyHashes[1]);

        // clear data
        txdb.delete(minerTransaction.hash());
        snapshot.commit();
    }

    @Test
    public void serializeExclusiveData() {
        ClaimTransaction claimTransaction = new ClaimTransaction() {{
            claims = new CoinReference[]{
                    new CoinReference() {{
                        prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                        prevIndex = new Ushort(0);
                    }},
                    new CoinReference() {{
                        prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
                        prevIndex = new Ushort(1);
                    }}
            };
        }};

        ClaimTransaction claim2 = Utils.copyFromSerialize(claimTransaction, ClaimTransaction::new);

        Assert.assertEquals(2, claim2.claims.length);
        Assert.assertEquals(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01"), claim2.claims[0].prevHash);
        Assert.assertEquals(new Ushort(0), claim2.claims[0].prevIndex);
        Assert.assertEquals(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02"), claim2.claims[1].prevHash);
        Assert.assertEquals(new Ushort(1), claim2.claims[1].prevIndex);
    }

    @Test
    public void toJson() {
        ClaimTransaction claimTransaction = new ClaimTransaction() {{
            claims = new CoinReference[]{
                    new CoinReference() {{
                        prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                        prevIndex = new Ushort(0);
                    }},
                    new CoinReference() {{
                        prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                        prevIndex = new Ushort(1);
                    }}
            };
        }};

        JsonObject jsonObject = claimTransaction.toJson();
        Assert.assertEquals(75, jsonObject.get("size").getAsInt());
        Assert.assertEquals(TransactionType.ClaimTransaction.value(), jsonObject.get("type").getAsInt());
        Assert.assertEquals(2, jsonObject.get("claims").getAsJsonArray().size());
    }

    @Test
    public void verify() {
        // 1. prepare data
        Snapshot snapshot = store.getSnapshot();

        // 1.1 set gas asset
        DataCache<UInt256, AssetState> assetDb = snapshot.getAssets();
        RegisterTransaction gasAsset = Blockchain.UtilityToken;
        AssetState assetState = new AssetState() {{
            assetId = gasAsset.hash();
            assetType = gasAsset.assetType;
            name = gasAsset.name;
            amount = gasAsset.amount;
            available = Fixed8.ZERO;
            precision = gasAsset.precision;
            fee = Fixed8.ZERO;
            feeAddress = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00");
            owner = gasAsset.owner;
            admin = gasAsset.admin;
            issuer = gasAsset.admin;
            expiration = Uint.ZERO;
            isFrozen = false;
        }};
        assetDb.add(assetState.assetId, assetState);

        // 1.2 set neo tx
        DataCache<UInt256, TransactionState> txdb = snapshot.getTransactions();
        MinerTransaction minerTransaction = new MinerTransaction() {{
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = Blockchain.GoverningToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(10000));
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                    }},
                    new TransactionOutput() {{
                        assetId = Blockchain.GoverningToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(20000));
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
                    }},
                    new TransactionOutput() {{
                        assetId = Blockchain.GoverningToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(30000));
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff03");
                    }}
            };
        }};
        TransactionState txState = new TransactionState() {{
            blockIndex = new Uint(0);
            transaction = minerTransaction;
        }};
        txdb.add(minerTransaction.hash(), txState);

        // 1.3 set spentcoin to claim
        DataCache<UInt256, SpentCoinState> spentCoinDb = snapshot.getSpentCoins();
        SpentCoinState spentCoin = new SpentCoinState() {{
            transactionHash = minerTransaction.hash();
            transactionHeight = new Uint(0);
            items = new HashMap<Ushort, Uint>() {{
                put(new Ushort(0), new Uint(100));
                put(new Ushort(1), new Uint(200));
            }};
        }};
        spentCoinDb.add(minerTransaction.hash(), spentCoin);

        snapshot.commit();

        // add test

        ClaimTransaction claimTransaction = new ClaimTransaction() {{
            claims = new CoinReference[]{
                    new CoinReference() {{
                        prevHash = minerTransaction.hash();
                        prevIndex = new Ushort(0);
                    }},
                    new CoinReference() {{
                        prevHash = minerTransaction.hash();
                        prevIndex = new Ushort(1);
                    }}
            };
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(0.3));
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                    }},
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(0.1));
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                    }}
            };
        }};

        // check 1
        //  claim the first tx gas = 100 * 8 * 1 /10000 = 0.08
        //  claim the second tx gas = 200 * 8 * 2 / 10000 = 0.32
        //  claim total = 0.08 + 0.32 = 0.40
        boolean result = claimTransaction.verify(snapshot);
        Assert.assertTrue(result);

        // check 2
        ArrayList<Transaction> mempool = new ArrayList<>();
        ClaimTransaction claimTransaction2 = new ClaimTransaction() {{
            claims = new CoinReference[]{
                    new CoinReference() {{
                        prevHash = minerTransaction.hash();
                        prevIndex = new Ushort(0);
                    }}
            };
            outputs = new TransactionOutput[]{
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(0.3));
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                    }},
                    new TransactionOutput() {{
                        assetId = Blockchain.UtilityToken.hash();
                        value = Fixed8.fromDecimal(BigDecimal.valueOf(0.1));
                        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                    }}
            };
        }};
        mempool.add(claimTransaction2);
        result = claimTransaction.verify(snapshot, mempool);
        Assert.assertFalse(result);


        // clear data
        assetDb.delete(gasAsset.hash());
        txdb.delete(minerTransaction.hash());
        spentCoinDb.delete(minerTransaction.hash());
        snapshot.commit();
    }
}