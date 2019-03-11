package neo.network.p2p.payloads;

import com.google.gson.JsonObject;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import akka.actor.ActorSystem;
import akka.actor.Props;
import neo.Fixed8;
import neo.UInt160;
import neo.UInt256;
import neo.csharp.Uint;
import neo.csharp.Ushort;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.io.caching.DataCache;
import neo.ledger.Blockchain;
import neo.ledger.TransactionState;
import neo.persistence.Snapshot;
import neo.persistence.leveldb.BlockchainDemo;
import neo.persistence.leveldb.LevelDBStore;

import static org.junit.Assert.*;

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

        ByteArrayOutputStream writer = new ByteArrayOutputStream();
        BinaryWriter output = new BinaryWriter(writer);
        claimTransaction.serializeExclusiveData(output);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(writer.toByteArray());
        BinaryReader reader = new BinaryReader(inputStream);
        CoinReference[] references = reader.readArray(CoinReference[]::new, CoinReference::new);

        Assert.assertEquals(2, references.length);
        Assert.assertEquals(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01"), references[0].prevHash);
        Assert.assertEquals(new Ushort(0), references[0].prevIndex);
        Assert.assertEquals(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02"), references[1].prevHash);
        Assert.assertEquals(new Ushort(1), references[1].prevIndex);
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
        // prepare data
        Snapshot snapshot = store.getSnapshot();

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

        // check 1
        claimTransaction.verify(snapshot);

        // check 2

        // clear data
    }
}