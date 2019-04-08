package neo.Wallets;

import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBFactory;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.ReadOptions;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import neo.UInt160;
import neo.UInt256;
import neo.Wallets.SQLite.Version;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.Ushort;
import neo.csharp.common.IDisposable;
import neo.io.SerializeHelper;
import neo.ledger.Blockchain;
import neo.ledger.CoinState;
import neo.log.notr.TR;
import neo.network.p2p.payloads.Block;
import neo.network.p2p.payloads.ClaimTransaction;
import neo.network.p2p.payloads.CoinReference;
import neo.network.p2p.payloads.EnrollmentTransaction;
import neo.network.p2p.payloads.RegisterTransaction;
import neo.network.p2p.payloads.Transaction;
import neo.network.p2p.payloads.TransactionOutput;
import neo.persistence.leveldb.DBHelper;
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
    public EventHandler<WalletTransactionEventArgs> walletTransaction = new EventHandler<>();

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

    public WalletIndexer(){}

    public WalletIndexer(String path) {
        DBFactory factory = new JniDBFactory();
        // 默认如果没有则创建
        Options tempOptions = new Options();
        tempOptions.createIfMissing(true);
        File file = new File(path);

        try {
            db = factory.open(file, tempOptions);
        } catch (IOException e) {
            TR.fixMe("levelDB读取异常");
            throw new RuntimeException(e);
        }

        byte[] value = db.get(new byte[]{DataEntryPrefix.SYS_Version}, new ReadOptions());
        if (value != null && Version.tryParse(new String(value), null) != null && Version.parse(new
                String(value)).compareTo(Version.parse("2.5.4")) >= 0) {
            ReadOptions options = new ReadOptions();
            options.fillCache(false);

            //LINQ START
/*            foreach (var group in db.Find(options, SliceBuilder.Begin(DataEntryPrefix.IX_Group), (k, v) => new
            {
                Height = k.ToUInt32(1),
                        Id = v.ToArray()
            }))*/
            for (AbstractMap.SimpleEntry<Uint, byte[]> group : DBHelper.find(db, new
                    byte[]{DataEntryPrefix
                    .IX_Group}, (k, v) -> new AbstractMap.SimpleEntry(BitConverter.toUint
                    (BitConverter.subBytes(k, 1, k.length - 1)), v))) {
                //LINQ END
                UInt160[] accounts = SerializeHelper.asAsSerializableArray(db.get(BitConverter.merge(new
                        byte[]{DataEntryPrefix
                        .IX_Accounts}, group.getValue()), options), UInt160[]::new, UInt160::new);
                indexes.put(group.getKey(), new HashSet<UInt160>(Arrays.asList(accounts)));
                for (UInt160 account : accounts)
                    accounts_tracked.put(account, new HashSet<CoinReference>());
            }
            //LINQ END
            for (Coin coin : DBHelper.find(db, new byte[]{DataEntryPrefix.ST_Coin}, (k, v) -> {

                Coin tempCoin = new Coin();
                tempCoin.reference = SerializeHelper.parse(CoinReference::new,BitConverter.subBytes
                        (k, 1, k.length - 1));

                tempCoin.output = SerializeHelper.parse(TransactionOutput::new,(v));
                tempCoin.state = new CoinState(v[60]);
                return tempCoin;
/*
                Reference = k.toArray().Skip(1).ToArray().AsSerializable < CoinReference > (),
                        Output = v.ToArray().AsSerializable < TransactionOutput > (),
                        State = (CoinState) v.ToArray()[60]*/
//LINQ END
            })) {
                accounts_tracked.get(coin.output.scriptHash).add(coin.reference);
                coins_tracked.put(coin.reference, coin);
            }
        } else {
            WriteBatch batch = db.createWriteBatch();
            ReadOptions options = new ReadOptions();
            options.fillCache(false);
            DBIterator it = db.iterator(options);

            while (it.hasNext()) {
                batch.delete(it.next().getKey());
            }

            batch.put(new byte[]{DataEntryPrefix.SYS_Version}, "2.9.2".getBytes());
            db.write(batch, new WriteOptions());
        }
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                processBlocks();
            }
        }, "WalletIndexer.ProcessBlocks");
        thread.setDaemon(true);
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
        try {
            db.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Iterable<Coin> getCoins(Iterable<UInt160> accounts) {
        synchronized (SyncRoot) {
            HashSet<Coin> resultSet = new HashSet<>();
            for (UInt160 account : accounts) {
                for (CoinReference reference : accounts_tracked.get(account)) {
                    resultSet.add(coins_tracked.get(reference));
                }
            }
            return resultSet;
        }
    }

    private static byte[] getGroupId() {
        byte[] groupId = new byte[32];
        Random rng = new Random();
        rng.nextBytes(groupId);
        return groupId;
    }

    public Iterable<UInt256> getTransactions(Iterable<UInt160> accounts) {
        ReadOptions options = new ReadOptions();
        options.fillCache(false);
        HashSet<UInt256> results = new HashSet<>();
        for (UInt160 account : accounts)
            results.addAll(DBHelper.find(db, BitConverter.merge(new byte[]{DataEntryPrefix
                    .ST_Transaction}, SerializeHelper.toBytes(account)), (k, v) -> {
                byte[] tempArray = new byte[k.length - 21];
                System.arraycopy(k, 21, tempArray, 0, k.length - 21);
                return new UInt256(tempArray);
            }));
        return results;
    }

    private AbstractMap.SimpleEntry<Transaction, UInt160[]>[] processBlock(Block block,
                                                                           HashSet<UInt160> accounts, WriteBatch batch) {
        List<AbstractMap.SimpleEntry<Transaction, UInt160[]>> change_set = new ArrayList<>();
        for (Transaction tx : block.transactions) {
            HashSet<UInt160> accounts_changed = new HashSet<UInt160>();
            for (int index = 0; index < tx.outputs.length; index++) {
                TransactionOutput output = tx.outputs[index];
                if (accounts_tracked.containsKey(output.scriptHash)) {
                    CoinReference reference = new CoinReference();
                    reference.prevHash = tx.hash();
                    reference.prevIndex = new Ushort(index);
                    Coin coin = null;
                    if ((coin = coins_tracked.getOrDefault(reference, null)) != null) {

                        coin.state = new CoinState((byte) (coin.state.value() | CoinState.Confirmed.value()));
                    } else {
                        accounts_tracked.get(output.scriptHash).add(reference);
                        coin = new Coin();
                        coin.reference = reference;
                        coin.output = output;
                        coin.state = CoinState.Confirmed;
                        coins_tracked.put(reference, coin);
                    }
                    batch.put(BitConverter.merge(new byte[]{DataEntryPrefix.ST_Coin}, SerializeHelper.toBytes(reference)),
                            BitConverter.merge(SerializeHelper.toBytes(output), new byte[]{coin.state
                                    .value()}));
                    accounts_changed.add(output.scriptHash);
                }
            }
            for (CoinReference input : tx.inputs) {
                Coin coin = null;
                if ((coin = coins_tracked.getOrDefault(input, null)) != null) {
                    if (coin.output.assetId.equals(Blockchain.GoverningToken.hash())) {
                        coin.state = new CoinState((byte) (coin.state.value() | CoinState.Spent.value() |
                                CoinState.Confirmed.value()));

                        batch.put(BitConverter.merge(new byte[]{DataEntryPrefix.ST_Coin},
                                SerializeHelper.toBytes(input)),
                                BitConverter.merge(SerializeHelper.toBytes(coin.output), new
                                        byte[]{coin.state.value()}));
                    } else {
                        accounts_tracked.get(coin.output.scriptHash).remove(input);
                        coins_tracked.remove(input);
                        batch.delete(BitConverter.merge(new byte[]{DataEntryPrefix.ST_Coin},
                                SerializeHelper.toBytes(input)));
                    }
                    accounts_changed.add(coin.output.scriptHash);
                }
            }
            if (tx instanceof neo.network.p2p.payloads.MinerTransaction) {

            } else if (tx instanceof neo.network.p2p.payloads.ContractTransaction) {

            } else if (tx instanceof neo.network.p2p.payloads.PublishTransaction) {

            } else if (tx instanceof neo.network.p2p.payloads.ClaimTransaction) {
                for (CoinReference claim : ((ClaimTransaction) tx).claims) {
                    Coin coin = null;
                    if ((coin = coins_tracked.getOrDefault(claim, null)) != null) {
                        accounts_tracked.get(coin.output.scriptHash).remove(claim);
                        coins_tracked.remove(claim);
                        batch.delete(BitConverter.merge(new byte[]{DataEntryPrefix.ST_Coin},
                                SerializeHelper.toBytes(claim)));
                        accounts_changed.add(coin.output.scriptHash);
                    }
                }
            } else if (tx instanceof neo.network.p2p.payloads.EnrollmentTransaction) {
                if (accounts_tracked.containsKey(((EnrollmentTransaction) tx).getScriptHash()))
                    accounts_changed.add(((EnrollmentTransaction) tx).getScriptHash());
            } else if (tx instanceof neo.network.p2p.payloads.RegisterTransaction) {
                if (accounts_tracked.containsKey(((RegisterTransaction) tx).getOwnerScriptHash()))
                    accounts_changed.add(((RegisterTransaction) tx).getOwnerScriptHash());
            } else {
                for (UInt160 hash : Arrays.asList(tx.witnesses).stream().map(p -> p
                        .scriptHash()).collect(Collectors.toList()))
                    if (accounts_tracked.containsKey(hash))
                        accounts_changed.add(hash);
            }
            if (accounts_changed.size() > 0) {
                for (UInt160 account : accounts_changed) {
                    byte[] tempbytes = BitConverter.merge(new byte[]{DataEntryPrefix.ST_Transaction},
                            SerializeHelper
                                    .toBytes(account));
                    batch.put(BitConverter.merge(tempbytes, SerializeHelper.toBytes(tx.hash())),
                            BitConverter.getBytes(false));
                }
                change_set.add(new AbstractMap.SimpleEntry<Transaction, UInt160[]>(tx,
                        accounts_changed.toArray(new UInt160[0])));
            }
        }
        return change_set.toArray(new AbstractMap.SimpleEntry[0]);
    }

    private void processBlocks() {
        while (!disposed) {
            while (!disposed) {
                Block block;
                AbstractMap.SimpleEntry<Transaction, UInt160[]>[] change_set;
                synchronized (SyncRoot) {
                    if (indexes.size() == 0) break;
                    //LINQ START
                    //uint height = indexes.Keys.Min();
                    Uint height = indexes.keySet().stream().min((x, y) -> x.compareTo(y)).get();
                    //LINQ END
                    block = Blockchain.singleton().getStore().getBlock(height);
                    if (block == null) break;
                    WriteBatch batch = db.createWriteBatch();
                    HashSet<UInt160> accounts = indexes.get(height);
                    change_set = processBlock(block, accounts, batch);
                    ReadOptions options = new ReadOptions();
                    byte[] groupId = db.get(BitConverter.merge(new byte[]{DataEntryPrefix
                            .IX_Group}, BitConverter.getBytes(height)), options);
                    indexes.remove(height);
                    batch.delete(BitConverter.merge(new byte[]{DataEntryPrefix
                            .IX_Group}, BitConverter.getBytes(height)));
                    height.add(Uint.ONE);
                    HashSet<UInt160> accounts_next = null;
                    if ((accounts_next = indexes.getOrDefault(height, null)) != null) {
                        accounts_next.addAll(accounts);
                        groupId = db.get(BitConverter.merge(new byte[]{DataEntryPrefix
                                .IX_Group}, BitConverter.getBytes(height)), options);
                        batch.put(BitConverter.merge(new byte[]{DataEntryPrefix.IX_Accounts},
                                groupId), SerializeHelper.toBytes(accounts_next.toArray(new
                                UInt160[0])));
                    } else {
                        indexes.put(height, accounts);
                        batch.put(BitConverter.merge(new byte[]{DataEntryPrefix
                                .IX_Group}, BitConverter.getBytes(height)), groupId);
                    }
                    db.write(batch);
                }
                for (AbstractMap.SimpleEntry<Transaction, UInt160[]> e : change_set) {
                    walletTransaction.invoke(null, new WalletTransactionEventArgs(
                            e.getKey(), e.getValue(), block.index, block.timestamp
                    ));
                }
            }
            for (int i = 0; i < 20 && !disposed; i++)
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }
    }

    public void rebuildIndex() {
        synchronized (SyncRoot) {
            WriteBatch batch = db.createWriteBatch();
            ReadOptions options = new ReadOptions();
            options.fillCache(false);
            for (Uint height : indexes.keySet()) {
                byte[] groupId = db.get(BitConverter.merge(new byte[]{DataEntryPrefix
                        .IX_Group}, BitConverter.getBytes(height)));
                batch.delete(BitConverter.merge(new byte[]{DataEntryPrefix
                        .IX_Group}, BitConverter.getBytes(height)));
                batch.delete(BitConverter.merge(new byte[]{DataEntryPrefix
                        .IX_Accounts}, BitConverter.getBytes(height)));
            }
            indexes.clear();
            if (accounts_tracked.size() > 0) {
                indexes.put(Uint.ZERO, new HashSet<UInt160>(accounts_tracked.keySet()));
                byte[] groupId = getGroupId();
                batch.put(BitConverter.merge(new byte[]{DataEntryPrefix.IX_Group}, BitConverter
                        .getBytes(Uint.ZERO)), groupId);
                batch.put(BitConverter.merge(new byte[]{DataEntryPrefix.IX_Accounts}, groupId),
                        SerializeHelper.toBytes(accounts_tracked.keySet().toArray(new UInt160[0])));
                for (HashSet<CoinReference> coins : accounts_tracked.values())
                    coins.clear();
            }
            for (CoinReference reference : coins_tracked.keySet())
                batch.delete(BitConverter.merge(new byte[]{DataEntryPrefix.ST_Coin},
                        SerializeHelper.toBytes(reference)));
            coins_tracked.clear();
            for (byte[] key : DBHelper.find(db, new byte[]{DataEntryPrefix.ST_Transaction}, (k, v) -> k))
                batch.delete(key);
            db.write(batch);
        }
    }

    public void registerAccounts(Iterable<UInt160> accounts, Uint height) {
        synchronized (SyncRoot) {
            boolean index_exists = true;
            HashSet<UInt160> index = indexes.getOrDefault(height, null);
            if (index == null) {
                index_exists = false;
                index = new HashSet<UInt160>();
            }
            for (UInt160 account : accounts)
                if (!accounts_tracked.containsKey(account)) {
                    index.add(account);
                    accounts_tracked.put(account, new HashSet<CoinReference>());
                }
            if (index.size() > 0) {
                WriteBatch batch = db.createWriteBatch();
                byte[] groupId;
                if (!index_exists) {
                    indexes.put(height, index);
                    groupId = getGroupId();
                    batch.put(BitConverter.merge(new byte[]{DataEntryPrefix
                            .IX_Group}, BitConverter.getBytes(height)), groupId);
                } else {
                    groupId = db.get(BitConverter.merge(new byte[]{DataEntryPrefix
                            .IX_Group}, BitConverter.getBytes(height)), new ReadOptions());
                }
                batch.put(BitConverter.merge(new byte[]{DataEntryPrefix.IX_Accounts}, groupId)
                        , SerializeHelper.toBytes(index.toArray(new UInt160[0])));
                db.write(batch, new WriteOptions());
            }
        }
    }

    public void registerAccounts(Iterable<UInt160> accounts) {
        Uint height = Uint.ZERO;
        synchronized (SyncRoot) {
            boolean index_exists = true;
            HashSet<UInt160> index = indexes.getOrDefault(height, null);
            if (index == null) {
                index_exists = false;
                index = new HashSet<UInt160>();
            }
            for (UInt160 account : accounts)
                if (!accounts_tracked.containsKey(account)) {
                    index.add(account);
                    accounts_tracked.put(account, new HashSet<CoinReference>());
                }
            if (index.size() > 0) {
                WriteBatch batch = db.createWriteBatch();
                byte[] groupId;
                if (!index_exists) {
                    indexes.put(height, index);
                    groupId = getGroupId();
                    batch.put(BitConverter.merge(new byte[]{DataEntryPrefix
                            .IX_Group}, BitConverter.getBytes(height)), groupId);
                } else {
                    groupId = db.get(BitConverter.merge(new byte[]{DataEntryPrefix
                            .IX_Group}, BitConverter.getBytes(height)), new ReadOptions());
                }
                batch.put(BitConverter.merge(new byte[]{DataEntryPrefix
                        .IX_Accounts}, groupId), SerializeHelper.toBytes(index.toArray(new
                        UInt160[0])));
                db.write(batch, new WriteOptions());
            }
        }
    }

    public void unregisterAccounts(Iterable<UInt160> accounts) {
        synchronized (SyncRoot) {
            WriteBatch batch = db.createWriteBatch();
            ReadOptions options = new ReadOptions();
            options.fillCache(false);
            for (UInt160 account : accounts) {
                HashSet<CoinReference> references = accounts_tracked.getOrDefault(account, null);
                if (references != null) {
                    for (Uint height : indexes.keySet().toArray(new Uint[0])) {
                        HashSet<UInt160> index = indexes.get(height);
                        if (index.remove(account)) {
                            byte[] groupId = db.get(BitConverter.merge(new byte[]{DataEntryPrefix
                                    .IX_Group}, BitConverter.getBytes(height)), options);
                            if (index.size() == 0) {
                                indexes.remove(height);
                                batch.delete(BitConverter.merge(new byte[]{DataEntryPrefix
                                        .IX_Group}, BitConverter.getBytes(height)));
                                batch.delete(BitConverter.merge(new byte[]{DataEntryPrefix
                                        .IX_Accounts}, groupId));
                            } else {
                                batch.put(BitConverter.merge(new byte[]{DataEntryPrefix
                                        .IX_Accounts}, groupId), SerializeHelper.toBytes(index
                                        .toArray(new UInt160[0])));
                            }
                            break;
                        }
                    }
                    accounts_tracked.remove(account);
                    for (CoinReference reference : references) {
                        batch.delete(BitConverter.merge(new byte[]{DataEntryPrefix.ST_Coin},
                                SerializeHelper.toBytes(reference)));
                        coins_tracked.remove(reference);
                    }
                    for (byte[] key : DBHelper.find(db, BitConverter.merge(new byte[]{DataEntryPrefix
                            .ST_Transaction}, SerializeHelper.toBytes(account)), (k, v) -> k))
                        batch.delete(key);
                }
            }
            db.write(batch, new WriteOptions());
        }
    }
}