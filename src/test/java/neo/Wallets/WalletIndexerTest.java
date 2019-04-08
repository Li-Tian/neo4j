package neo.Wallets;

import org.iq80.leveldb.WriteBatch;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import neo.UInt160;
import neo.UInt256;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.Ushort;
import neo.io.SerializeHelper;
import neo.ledger.CoinState;
import neo.network.p2p.payloads.CoinReference;
import neo.network.p2p.payloads.IVerifiable;
import neo.network.p2p.payloads.MinerTransaction;
import neo.persistence.AbstractBlockchainTest;
import neo.persistence.leveldb.DBHelper;

import static org.junit.Assert.*;

public class WalletIndexerTest extends AbstractBlockchainTest {

    private static WalletIndexer walletIndexer;

    @BeforeClass
    public static void setup() throws IOException {
        AbstractBlockchainTest.setUp(WalletIndexerTest.class.getSimpleName());

        String path = WalletIndexerTest.class.getClassLoader().getResource("").getPath() + "wallet_index_leveldb";
        walletIndexer = new WalletIndexer(path);
    }

    @AfterClass
    public static void tearDown() throws IOException {
        AbstractBlockchainTest.tearDown(WalletIndexerTest.class.getSimpleName());
    }

    @Test
    public void getIndexHeight() {
        // prepare data
        HashSet<UInt160> set = new HashSet<>();
        set.add(UInt160.Zero);
        walletIndexer.indexes.put(Uint.ONE, set);

        // check
        Assert.assertEquals(Uint.ONE, walletIndexer.getIndexHeight());

        // clear data
        walletIndexer.indexes.clear();
    }

    @Test
    public void getCoins() {
        // prepare data
        UInt160 accountHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        HashSet<CoinReference> set = new HashSet<>();
        CoinReference reference1 = new CoinReference() {{
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            prevIndex = new Ushort(1);
        }};
        CoinReference reference2 = new CoinReference() {{
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            prevIndex = new Ushort(3);
        }};
        set.add(reference1);
        set.add(reference2);
        walletIndexer.accounts_tracked.put(accountHash, set);


        Coin coin1 = new Coin() {{
            reference = reference1;
            output = null;
            state = CoinState.Spent;
        }};
        Coin coin2 = new Coin() {{
            reference = reference2;
            output = null;
            state = CoinState.Spent;
        }};
        walletIndexer.coins_tracked.put(coin1.reference, coin1);
        walletIndexer.coins_tracked.put(coin2.reference, coin2);

        // check
        Iterable<Coin> iterable = walletIndexer.getCoins(Arrays.asList(UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01")));
        Iterator<Coin> iterator = iterable.iterator();
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(reference1, iterator.next().reference);

        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(reference2, iterator.next().reference);

        Assert.assertFalse(iterator.hasNext());

        // clear data
        walletIndexer.accounts_tracked.clear();
        walletIndexer.coins_tracked.clear();
    }

    @Test
    public void getTransactions() {
        // prepare data
        UInt160 account = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");

        WriteBatch batch = walletIndexer.db.createWriteBatch();
        MinerTransaction minerTransaction1 = new MinerTransaction() {{
            nonce = Uint.ZERO;
        }};
        MinerTransaction minerTransaction2 = new MinerTransaction() {{
            nonce = Uint.ONE;
        }};

        byte[] key1 = BitConverter.merge(account.toArray(), minerTransaction1.hash().toArray());
        byte[] value = BitConverter.getBytes(false);
        DBHelper.batchPut(batch, DataEntryPrefix.ST_Transaction, key1, value);

        byte[] key2 = BitConverter.merge(account.toArray(), minerTransaction2.hash().toArray());
        DBHelper.batchPut(batch, DataEntryPrefix.ST_Transaction, key2, value);
        walletIndexer.db.write(batch);


        // check

        Iterable<UInt256> iterable = walletIndexer.getTransactions(Arrays.asList(account));
        Iterator<UInt256> iterator = iterable.iterator();

        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(minerTransaction1.hash(), iterator.next());

        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(minerTransaction2.hash(), iterator.next());

        Assert.assertFalse(iterator.hasNext());

        // clear data
        batch = walletIndexer.db.createWriteBatch();
        batch.delete(BitConverter.merge(DataEntryPrefix.ST_Transaction, key1));
        batch.delete(BitConverter.merge(DataEntryPrefix.ST_Transaction, key2));
        walletIndexer.db.write(batch);
    }

    @Test
    public void rebuildIndex() {

    }

    @Test
    public void registerAccounts() {
        // prepare data
        UInt160 hash1 = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        UInt160 hash2 = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        walletIndexer.accounts_tracked.put(hash2, new HashSet<>());

        HashSet<UInt160> accounts = new HashSet<>();
        accounts.add(hash2);
        walletIndexer.indexes.put(new Uint(21), accounts);

        // check
        walletIndexer.registerAccounts(Arrays.asList(hash1, hash2), new Uint(10));
        byte[] groupId = walletIndexer.db.get(BitConverter.merge(DataEntryPrefix.IX_Group, new Uint(10).toBytes()));
        Assert.assertTrue(groupId.length > 0);
        byte[] value = walletIndexer.db.get(BitConverter.merge(DataEntryPrefix.IX_Accounts, groupId));
        UInt160[] hashes = SerializeHelper.asAsSerializableArray(value, UInt160[]::new, UInt160::new);
        Assert.assertEquals(hash1, hashes[0]);

        // add hash1, hash2
        walletIndexer.indexes.remove(new Uint(21));
        walletIndexer.accounts_tracked.remove(hash2);
        walletIndexer.registerAccounts(Arrays.asList(hash1, hash2), new Uint(10));
        value = walletIndexer.db.get(BitConverter.merge(DataEntryPrefix.IX_Accounts, groupId));
        hashes = SerializeHelper.asAsSerializableArray(value, UInt160[]::new, UInt160::new);
        Assert.assertEquals(hash1, hashes[0]);
        Assert.assertEquals(hash2, hashes[1]);


        // test unregisterAccounts
        walletIndexer.unregisterAccounts(Arrays.asList(hash1, hash2));
        value = walletIndexer.db.get(BitConverter.merge(DataEntryPrefix.IX_Accounts, groupId));
        Assert.assertNull(value);

        Assert.assertFalse(walletIndexer.accounts_tracked.containsKey(hash1));
        Assert.assertFalse(walletIndexer.accounts_tracked.containsKey(hash2));


        // clear
        walletIndexer.indexes.clear();
        walletIndexer.accounts_tracked.clear();
    }

}