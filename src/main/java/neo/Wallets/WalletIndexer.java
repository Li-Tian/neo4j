package neo.Wallets;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.ReadOptions;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import neo.UInt160;
import neo.UInt256;
import neo.csharp.Uint;
import neo.csharp.common.IDisposable;
import neo.ledger.Blockchain;
import neo.network.p2p.payloads.Block;
import neo.network.p2p.payloads.CoinReference;
import neo.smartcontract.EventHandler;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: WalletIndexer
 * @Package neo.Wallets
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 11:49 2019/3/14
 */
public class WalletIndexer implements IDisposable {
    public EventHandler<WalletTransactionEventArgs> WalletTransaction = new EventHandler<>();

    private Map<Uint, HashSet<UInt160>> indexes = new HashMap<Uint, HashSet<UInt160>>();
    private Map<UInt160, HashSet<CoinReference>> accounts_tracked = new HashMap<>();
    private Map<CoinReference, Coin> coins_tracked = new HashMap<>();

    private DB db;
    private Thread thread;
    private Object SyncRoot = new Object();
    private boolean disposed = false;

    public Uint getIndexHeight() {
        synchronized (SyncRoot) {
            if (indexes.size() == 0) return Uint.ZERO;
            List<Uint> sortlist = new ArrayList<>(indexes.keySet());
            Collections.sort(sortlist, new Comparator<Uint>() {
                @Override
                public int compare(Uint o1, Uint o2) {
                    return o1.compareTo(o2);
                }
            });
            return sortlist.get(0);
        }
    }

    public WalletIndexer(String path) {
        File tempFile=new File(path);
        path = tempFile.getAbsolutePath();
        tempFile.mkdir();

        db = DB.open(path, new Options {
            CreateIfMissing = true
        });
        if (db.TryGet(ReadOptions.Default, SliceBuilder.Begin(DataEntryPrefix.SYS_Version), out Slice value) && Version.TryParse(value.ToString(), out Version version) && version >= Version.Parse("2.5.4")) {
            ReadOptions options = new ReadOptions {
                FillCache = false
            } ;
            foreach(var group in db.Find(options, SliceBuilder.Begin(DataEntryPrefix.IX_Group), (k, v) = > new
            {
                Height = k.ToUInt32(1),
                        Id = v.ToArray()
            }))
            {
                UInt160[] accounts = db.Get(options, SliceBuilder.Begin(DataEntryPrefix.IX_Accounts).Add(group.Id)).ToArray().AsSerializableArray < UInt160 > ();
                indexes.Add(group.Height, new HashSet<UInt160>(accounts));
                foreach(UInt160 account in accounts)
                accounts_tracked.Add(account, new HashSet<CoinReference>());
            }
            foreach(Coin coin in db.Find(options, SliceBuilder.Begin(DataEntryPrefix.ST_Coin), (k, v) = > new Coin
            {
                Reference = k.ToArray().Skip(1).ToArray().AsSerializable < CoinReference > (),
                        Output = v.ToArray().AsSerializable < TransactionOutput > (),
                        State = (CoinState) v.ToArray()[60]
            }))
            {
                accounts_tracked[coin.Output.ScriptHash].Add(coin.Reference);
                coins_tracked.Add(coin.Reference, coin);
            }
        } else {
            WriteBatch batch = new WriteBatch();
            ReadOptions options = new ReadOptions {
                FillCache = false
            } ;
            using(Iterator it = db.NewIterator(options))
            {
                for (it.SeekToFirst(); it.Valid(); it.Next()) {
                    batch.Delete(it.Key());
                }
            }
            batch.Put(SliceBuilder.Begin(DataEntryPrefix.SYS_Version), Assembly.GetExecutingAssembly().GetName().Version.ToString());
            db.Write(WriteOptions.Default, batch);
        }
        thread = new Thread(ProcessBlocks) {
            IsBackground =true,
            Name =$"{nameof(WalletIndexer)}.{nameof(ProcessBlocks)}"
        };
        thread.start();
    }

    @Override
    public void dispose() {
        disposed = true;
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        db.dispose();
    }

    public Iterable<Coin> getCoins(Iterable<UInt160> accounts) {
        synchronized (SyncRoot) {
            for (UInt160 account : accounts)
                for (CoinReference reference : accounts_tracked[account])
                    yield return coins_tracked[reference];
        }
    }

    private static byte[] getGroupId() {
        byte[] groupId = new byte[32];
        Random rng = new Random();
        rng.nextBytes(groupId);
        return groupId;
    }

    public Iterable<UInt256> getTransactions(Iterable<UInt160> accounts) {
        ReadOptions options = new ReadOptions {
            FillCache = false
        } ;
        Iterable<UInt256> results = Enumerable.Empty < UInt256 > ();
        for (UInt160 account : accounts)
            results = results.Union(db.Find(options, SliceBuilder.Begin(DataEntryPrefix.ST_Transaction).Add(account), (k, v) = > new UInt256(k.ToArray().Skip(21).ToArray())))
        ;
        for (UInt256 hash : results)
            yield return hash;
    }

    private (Transaction,UInt160[])[]

    ProcessBlock(Block block, HashSet<UInt160> accounts, WriteBatch batch) {
        var change_set = new List<(Transaction, UInt160[])>();
        foreach(Transaction tx in block.Transactions)
        {
            HashSet<UInt160> accounts_changed = new HashSet<UInt160>();
            for (ushort index = 0; index < tx.Outputs.Length; index++) {
                TransactionOutput output = tx.Outputs[index];
                if (accounts_tracked.ContainsKey(output.ScriptHash)) {
                    CoinReference reference = new CoinReference
                    {
                        PrevHash = tx.Hash,
                                PrevIndex = index
                    } ;
                    if (coins_tracked.TryGetValue(reference, out Coin coin)) {
                        coin.State |= CoinState.Confirmed;
                    } else {
                        accounts_tracked[output.ScriptHash].Add(reference);
                        coins_tracked.Add(reference, coin = new Coin
                        {
                            Reference = reference,
                                    Output = output,
                                    State = CoinState.Confirmed
                        });
                    }
                    batch.Put(SliceBuilder.Begin(DataEntryPrefix.ST_Coin).Add(reference), SliceBuilder.Begin().Add(output).Add((byte) coin.State));
                    accounts_changed.Add(output.ScriptHash);
                }
            }
            foreach(CoinReference input in tx.Inputs)
            {
                if (coins_tracked.TryGetValue(input, out Coin coin)) {
                    if (coin.Output.AssetId.Equals(Blockchain.GoverningToken.Hash)) {
                        coin.State |= CoinState.Spent | CoinState.Confirmed;
                        batch.Put(SliceBuilder.Begin(DataEntryPrefix.ST_Coin).Add(input), SliceBuilder.Begin().Add(coin.Output).Add((byte) coin.State));
                    } else {
                        accounts_tracked[coin.Output.ScriptHash].Remove(input);
                        coins_tracked.Remove(input);
                        batch.Delete(DataEntryPrefix.ST_Coin, input);
                    }
                    accounts_changed.Add(coin.Output.ScriptHash);
                }
            }
            switch (tx) {
                case MinerTransaction _:
                case ContractTransaction _:
#pragma warning disable CS0612
                case PublishTransaction _:
#pragma warning restore CS0612
                    break;
                case ClaimTransaction tx_claim:
                foreach(CoinReference claim in tx_claim.Claims)
                {
                    if (coins_tracked.TryGetValue(claim, out Coin coin)) {
                        accounts_tracked[coin.Output.ScriptHash].Remove(claim);
                        coins_tracked.Remove(claim);
                        batch.Delete(DataEntryPrefix.ST_Coin, claim);
                        accounts_changed.Add(coin.Output.ScriptHash);
                    }
                }
                break;
#pragma warning disable CS0612
                case EnrollmentTransaction tx_enrollment:
                if (accounts_tracked.ContainsKey(tx_enrollment.ScriptHash))
                    accounts_changed.Add(tx_enrollment.ScriptHash);
                    break;
                case RegisterTransaction tx_register:
                if (accounts_tracked.ContainsKey(tx_register.OwnerScriptHash))
                    accounts_changed.Add(tx_register.OwnerScriptHash);
                    break;
#pragma warning restore CS0612
                default:
                    foreach(UInt160 hash in tx.Witnesses.Select(p = > p.ScriptHash))
                    if (accounts_tracked.ContainsKey(hash))
                        accounts_changed.Add(hash);
                    break;
            }
            if (accounts_changed.Count > 0) {
                foreach(UInt160 account in accounts_changed)
                batch.Put(SliceBuilder.Begin(DataEntryPrefix.ST_Transaction).Add(account).Add(tx.Hash), false);
                change_set.Add((tx, accounts_changed.ToArray()));
            }
        }
        return change_set.ToArray();
    }

    private void processBlocks() {
        while (!disposed) {
            while (!disposed) {
                Block block;
                (Transaction, UInt160[])[]change_set;
                synchronized (SyncRoot) {
                    if (indexes.Count == 0) break;
                    uint height = indexes.Keys.Min();
                    block = Blockchain.Singleton.Store.GetBlock(height);
                    if (block == null) break;
                    WriteBatch batch = new WriteBatch();
                    HashSet<UInt160> accounts = indexes[height];
                    change_set = ProcessBlock(block, accounts, batch);
                    ReadOptions options = ReadOptions.Default;
                    byte[] groupId = db.Get(options, SliceBuilder.Begin(DataEntryPrefix.IX_Group).Add(height)).ToArray();
                    indexes.Remove(height);
                    batch.Delete(SliceBuilder.Begin(DataEntryPrefix.IX_Group).Add(height));
                    height++;
                    if (indexes.TryGetValue(height, out HashSet < UInt160 > accounts_next)) {
                        accounts_next.UnionWith(accounts);
                        groupId = db.Get(options, SliceBuilder.Begin(DataEntryPrefix.IX_Group).Add(height)).ToArray();
                        batch.Put(SliceBuilder.Begin(DataEntryPrefix.IX_Accounts).Add(groupId), accounts_next.ToArray().ToByteArray());
                    } else {
                        indexes.Add(height, accounts);
                        batch.Put(SliceBuilder.Begin(DataEntryPrefix.IX_Group).Add(height), groupId);
                    }
                    db.Write(WriteOptions.Default, batch);
                }
                foreach(var(tx, accounts)in change_set)
                {
                    WalletTransaction ?.Invoke(null, new WalletTransactionEventArgs
                    {
                        Transaction = tx,
                                RelatedAccounts = accounts,
                                Height = block.Index,
                                Time = block.Timestamp
                    });
                }
            }
            for (int i = 0; i < 20 && !disposed; i++)
                Thread.Sleep(100);
        }
    }

    public void rebuildIndex() {
        synchronized (SyncRoot) {
            WriteBatch batch = new WriteBatch();
            ReadOptions options = new ReadOptions {
                FillCache = false
            } ;
            for (Uint height : indexes.keySet()) {
                byte[] groupId = db.Get(options, SliceBuilder.Begin(DataEntryPrefix.IX_Group).Add(height)).ToArray();
                batch.Delete(SliceBuilder.Begin(DataEntryPrefix.IX_Group).Add(height));
                batch.Delete(SliceBuilder.Begin(DataEntryPrefix.IX_Accounts).Add(groupId));
            }
            indexes.clear();
            if (accounts_tracked.Count > 0) {
                indexes[0] = new HashSet<UInt160>(accounts_tracked.Keys);
                byte[] groupId = GetGroupId();
                batch.Put(SliceBuilder.Begin(DataEntryPrefix.IX_Group).Add(0u), groupId);
                batch.Put(SliceBuilder.Begin(DataEntryPrefix.IX_Accounts).Add(groupId), accounts_tracked.Keys.ToArray().ToByteArray());
                foreach(HashSet < CoinReference > coins in accounts_tracked.Values)
                coins.Clear();
            }
            foreach(CoinReference reference in coins_tracked.Keys)
            batch.Delete(DataEntryPrefix.ST_Coin, reference);
            coins_tracked.Clear();
            foreach(Slice key in db.Find(options, SliceBuilder.Begin(DataEntryPrefix.ST_Transaction), (k, v) = > k))
            batch.Delete(key);
            db.Write(WriteOptions.Default, batch);
        }
    }

    public void registerAccounts(Iterable<UInt160> accounts, Uint height) {
        synchronized (SyncRoot) {
            boolean index_exists = true;
            HashSet<UInt160> index = indexes.getOrDefault(height, null);
            if (index == null) {
                index_exists=false;
                index = new HashSet<UInt160>();
            }
            for (UInt160 account : accounts)
                if (!accounts_tracked.containsKey(account)) {
                    index.add(account);
                    accounts_tracked.put(account, new HashSet<CoinReference>());
                }
            if (index.size() > 0) {
                WriteBatch batch = new WriteBatch();
                byte[] groupId;
                if (!index_exists) {
                    indexes.put(height, index);
                    groupId = getGroupId();
                    batch.put(SliceBuilder.Begin(DataEntryPrefix.IX_Group).Add(height), groupId);
                } else {
                    groupId = db.get(ReadOptions.Default, SliceBuilder.Begin(DataEntryPrefix
                            .IX_Group).Add(height)).ToArray();
                }
                batch.put(SliceBuilder.Begin(DataEntryPrefix.IX_Accounts).Add(groupId), index
                        .ToArray().ToByteArray());
                db.write(WriteOptions.Default, batch);
            }
        }
    }

    public void registerAccounts(Iterable<UInt160> accounts) {
        Uint height =Uint.ZERO;
        synchronized (SyncRoot) {
            boolean index_exists = true;
            HashSet<UInt160> index = indexes.getOrDefault(height, null);
            if (index == null) {
                index_exists=false;
                index = new HashSet<UInt160>();
            }
            for (UInt160 account : accounts)
                if (!accounts_tracked.containsKey(account)) {
                    index.add(account);
                    accounts_tracked.put(account, new HashSet<CoinReference>());
                }
            if (index.size() > 0) {
                WriteBatch batch = new WriteBatch();
                byte[] groupId;
                if (!index_exists) {
                    indexes.put(height, index);
                    groupId = getGroupId();
                    batch.put(SliceBuilder.Begin(DataEntryPrefix.IX_Group).Add(height), groupId);
                } else {
                    groupId = db.get(ReadOptions.Default, SliceBuilder.Begin(DataEntryPrefix
                            .IX_Group).Add(height)).ToArray();
                }
                batch.put(SliceBuilder.Begin(DataEntryPrefix.IX_Accounts).Add(groupId), index
                        .ToArray().ToByteArray());
                db.write(WriteOptions.Default, batch);
            }
        }
    }

    public void unregisterAccounts(Iterable<UInt160> accounts) {
        synchronized (SyncRoot) {
            WriteBatch batch = db;
            ReadOptions options = new ReadOptions {
                FillCache = false
            } ;
            foreach(UInt160 account in accounts)
            {
                if (accounts_tracked.TryGetValue(account, out HashSet < CoinReference > references)) {
                    foreach(uint height in indexes.Keys.ToArray())
                    {
                        HashSet<UInt160> index = indexes[height];
                        if (index.Remove(account)) {
                            byte[] groupId = db.Get(options, SliceBuilder.Begin(DataEntryPrefix.IX_Group).Add(height)).ToArray();
                            if (index.Count == 0) {
                                indexes.Remove(height);
                                batch.Delete(SliceBuilder.Begin(DataEntryPrefix.IX_Group).Add(height));
                                batch.Delete(SliceBuilder.Begin(DataEntryPrefix.IX_Accounts).Add(groupId));
                            } else {
                                batch.Put(SliceBuilder.Begin(DataEntryPrefix.IX_Accounts).Add(groupId), index.ToArray().ToByteArray());
                            }
                            break;
                        }
                    }
                    accounts_tracked.Remove(account);
                    foreach(CoinReference reference in references)
                    {
                        batch.Delete(DataEntryPrefix.ST_Coin, reference);
                        coins_tracked.Remove(reference);
                    }
                    foreach(Slice key in db.Find(options, SliceBuilder.Begin(DataEntryPrefix.ST_Transaction).Add(account), (k, v) = > k))
                    batch.Delete(key);
                }
            }
            db.Write(WriteOptions.Default, batch);
        }
    }
}