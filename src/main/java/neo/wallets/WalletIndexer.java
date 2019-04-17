package neo.wallets;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import neo.UInt160;
import neo.UInt256;
import neo.wallets.SQLite.Version;
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
import neo.network.p2p.payloads.Witness;
import neo.persistence.leveldb.DBHelper;
import neo.smartcontract.EventHandler;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: WalletIndexer
 * @Package neo.wallets
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 11:49 2019/3/14
 */
public class WalletIndexer implements IDisposable {
    public EventHandler<WalletTransactionEventArgs> walletTransaction = new EventHandler<>();

    protected HashMap<Uint, HashSet<UInt160>> indexes = new HashMap<Uint, HashSet<UInt160>>();
    protected HashMap<UInt160, HashSet<CoinReference>> accounts_tracked = new HashMap<>();
    protected HashMap<CoinReference, Coin> coins_tracked = new HashMap<>();

    protected DB db;
    protected Thread thread;
    protected Object SyncRoot = new Object();
    protected boolean disposed = false;

    public Uint getIndexHeight() {
        synchronized (SyncRoot) {
            if (indexes.size() == 0) return Uint.ZERO;
            List<Uint> sortlist = new ArrayList<>(indexes.keySet());
            Collections.sort(sortlist, Uint::compareTo);
            return sortlist.get(0);
        }
    }

    public WalletIndexer() {
    }

    public WalletIndexer(String path) {
        DBFactory factory = new JniDBFactory();
        Options tempOptions = new Options();
        tempOptions.createIfMissing(true);      // 默认如果没有则创建

        File file = new File(path);
        try {
            db = factory.open(file, tempOptions);
        } catch (IOException e) {
            TR.fixMe("levelDB读取异常");
            throw new RuntimeException(e);
        }

        byte[] value = DBHelper.get(db, DataEntryPrefix.SYS_Version);

        if (value != null && Version.tryParse(new String(value), null) != null
                && Version.parse(new String(value)).compareTo(Version.parse("2.5.4")) >= 0) {
            ReadOptions options = new ReadOptions();
            options.fillCache(false);

            //LINQ START
            /*   foreach (var group in db.Find(options, SliceBuilder.Begin(DataEntryPrefix.IX_Group), (k, v) => new
            {
                Height = k.ToUInt32(1),
                        Id = v.ToArray()
            }))*/
            //LINQ END

            DBHelper.findForEach(db, DataEntryPrefix.IX_Group, (keyBytes, accountIdBytes) -> {
                byte[] accountBytes = DBHelper.get(db, DataEntryPrefix.IX_Accounts, accountIdBytes);
                if (accountBytes == null || accountBytes.length == 0) {
                    return;
                }
                UInt160[] accounts = SerializeHelper.asAsSerializableArray(accountBytes, UInt160[]::new, UInt160::new);
                Uint height = BitConverter.toUint(BitConverter.subBytes(keyBytes, 1));
                indexes.put(height, new HashSet<>(Arrays.asList(accounts)));

                for (UInt160 account : accounts) {
                    accounts_tracked.put(account, new HashSet<>());
                }
            });


            //LINQ START
            /*
                Reference = k.toArray().Skip(1).ToArray().AsSerializable < CoinReference > (),
                        Output = v.ToArray().AsSerializable < TransactionOutput > (),
                        State = (CoinState) v.ToArray()[60]*/
            //LINQ END

            DBHelper.findForEach(db, DataEntryPrefix.ST_Coin, (k, v) -> {
                Coin coin = new Coin();
                coin.reference = SerializeHelper.parse(CoinReference::new, BitConverter.subBytes(k, 1));
                coin.output = SerializeHelper.parse(TransactionOutput::new, (v));
                coin.state = new CoinState(v[60]);

                accounts_tracked.get(coin.output.scriptHash).add(coin.reference);
                coins_tracked.put(coin.reference, coin);
            });
        } else {
            WriteBatch batch = db.createWriteBatch();
            ReadOptions options = new ReadOptions();
            options.fillCache(false);
            DBIterator it = db.iterator(options);
            while (it.hasNext()) {
                batch.delete(it.next().getKey());
            }
            DBHelper.batchPut(batch, DataEntryPrefix.SYS_Version, "2.9.2".getBytes());
            db.write(batch, new WriteOptions());
        }

        thread = new Thread(() -> processBlocks(), "WalletIndexer.ProcessBlocks");
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
        for (UInt160 account : accounts) {
            DBHelper.findForEach(db, DataEntryPrefix.ST_Transaction, account.toArray(), (k, v) -> {
                UInt256 txHash = new UInt256(BitConverter.subBytes(k, 21, k.length));
                results.add(txHash);
            });
        }
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

                    Coin coin;
                    if ((coin = coins_tracked.getOrDefault(reference, null)) != null) {
                        coin.state = coin.state.or(CoinState.Confirmed);
                    } else {
                        accounts_tracked.get(output.scriptHash).add(reference);
                        coin = new Coin();
                        coin.reference = reference;
                        coin.output = output;
                        coin.state = CoinState.Confirmed;
                        coins_tracked.put(reference, coin);
                    }
                    byte[] value = BitConverter.merge(SerializeHelper.toBytes(output), coin.state.value());
                    DBHelper.batchPut(batch, DataEntryPrefix.ST_Coin, SerializeHelper.toBytes(reference), value);
                    accounts_changed.add(output.scriptHash);
                }
            }
            for (CoinReference input : tx.inputs) {
                Coin coin;
                if ((coin = coins_tracked.getOrDefault(input, null)) != null) {
                    if (coin.output.assetId.equals(Blockchain.GoverningToken.hash())) {
                        coin.state = coin.state.or(CoinState.Spent).or(CoinState.Confirmed);
                        byte[] value = BitConverter.merge(SerializeHelper.toBytes(coin.output), coin.state.value());
                        DBHelper.batchPut(batch, DataEntryPrefix.ST_Coin, SerializeHelper.toBytes(input), value);
                    } else {
                        accounts_tracked.get(coin.output.scriptHash).remove(input);
                        coins_tracked.remove(input);
                        DBHelper.batchDelete(batch, DataEntryPrefix.ST_Coin, SerializeHelper.toBytes(input));
                    }
                    accounts_changed.add(coin.output.scriptHash);
                }
            }
            if (tx instanceof neo.network.p2p.payloads.MinerTransaction) {

            } else if (tx instanceof neo.network.p2p.payloads.ContractTransaction) {

            } else if (tx instanceof neo.network.p2p.payloads.PublishTransaction) {

            } else if (tx instanceof neo.network.p2p.payloads.ClaimTransaction) {
                for (CoinReference claim : ((ClaimTransaction) tx).claims) {
                    Coin coin;
                    if ((coin = coins_tracked.getOrDefault(claim, null)) != null) {
                        accounts_tracked.get(coin.output.scriptHash).remove(claim);
                        coins_tracked.remove(claim);
                        DBHelper.batchDelete(batch, DataEntryPrefix.ST_Coin, SerializeHelper.toBytes(claim));
                        accounts_changed.add(coin.output.scriptHash);
                    }
                }
            } else if (tx instanceof neo.network.p2p.payloads.EnrollmentTransaction) {
                EnrollmentTransaction enrollTx = (EnrollmentTransaction) tx;
                if (accounts_tracked.containsKey(enrollTx.getScriptHash())) {
                    accounts_changed.add(enrollTx.getScriptHash());
                }
            } else if (tx instanceof neo.network.p2p.payloads.RegisterTransaction) {
                RegisterTransaction registerTx = (RegisterTransaction) tx;
                if (accounts_tracked.containsKey(registerTx.getOwnerScriptHash())) {
                    accounts_changed.add(registerTx.getOwnerScriptHash());
                }
            } else {
                Arrays.asList(tx.witnesses).stream().map(Witness::scriptHash).forEach(hash -> {
                    if (accounts_tracked.containsKey(hash)) {
                        accounts_changed.add(hash);
                    }
                });
            }
            if (accounts_changed.size() > 0) {
                for (UInt160 account : accounts_changed) {
                    byte[] key = BitConverter.merge(account.toArray(), tx.hash().toArray());
                    byte[] value = BitConverter.getBytes(false);
                    DBHelper.batchPut(batch, DataEntryPrefix.ST_Transaction, key, value);
                }
                change_set.add(new AbstractMap.SimpleEntry<>(tx, accounts_changed.toArray(new UInt160[0])));
            }
        }
        return change_set.toArray(new AbstractMap.SimpleEntry[0]);
    }

    protected void processBlocks() {
        while (!disposed) {
            while (!disposed) {
                Block block;
                AbstractMap.SimpleEntry<Transaction, UInt160[]>[] change_set;
                synchronized (SyncRoot) {
                    if (indexes.size() == 0) break;
                    //LINQ START
                    //uint height = indexes.Keys.Min();
                    Uint height = indexes.keySet().stream().min(Uint::compareTo).get();
                    //LINQ END

                    block = Blockchain.singleton().getStore().getBlock(height);
                    if (block == null) {
                        break;
                    }

                    WriteBatch batch = db.createWriteBatch();
                    HashSet<UInt160> accounts = indexes.get(height);
                    change_set = processBlock(block, accounts, batch);

                    byte[] groupId = DBHelper.get(db, DataEntryPrefix.IX_Group, height.toBytes());

                    indexes.remove(height);
                    DBHelper.batchDelete(batch, DataEntryPrefix.IX_Group, height.toBytes());
                    height = height.add(Uint.ONE);

                    HashSet<UInt160> accounts_next;
                    if ((accounts_next = indexes.getOrDefault(height, null)) != null) {
                        accounts_next.addAll(accounts);
                        groupId = DBHelper.get(db, DataEntryPrefix.IX_Group, height.toBytes());
                        byte[] value = SerializeHelper.toBytes(accounts_next.toArray(new UInt160[0]));
                        DBHelper.batchPut(batch, DataEntryPrefix.IX_Accounts, groupId, value);
                    } else {
                        indexes.put(height, accounts);
                        DBHelper.batchPut(batch, DataEntryPrefix.IX_Group, height.toBytes(), groupId);
                    }
                    db.write(batch);
                }
                for (AbstractMap.SimpleEntry<Transaction, UInt160[]> e : change_set) {
                    walletTransaction.invoke(null, new WalletTransactionEventArgs(
                            e.getKey(), e.getValue(), block.index, block.timestamp
                    ));
                }
            }
            for (int i = 0; i < 20 && !disposed; i++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void rebuildIndex() {
        synchronized (SyncRoot) {
            WriteBatch batch = db.createWriteBatch();
            ReadOptions options = new ReadOptions();
            options.fillCache(false);
            for (Uint height : indexes.keySet()) {
                byte[] groupId = DBHelper.get(db, DataEntryPrefix.IX_Group, height.toBytes());
                DBHelper.batchDelete(batch, DataEntryPrefix.IX_Group, height.toBytes());
                DBHelper.batchDelete(batch, DataEntryPrefix.IX_Accounts, groupId);
            }
            indexes.clear();
            if (accounts_tracked.size() > 0) {
                indexes.put(Uint.ZERO, new HashSet<>(accounts_tracked.keySet()));

                byte[] groupId = getGroupId();
                DBHelper.batchPut(batch, DataEntryPrefix.IX_Group, BitConverter.getBytes(Uint.ZERO), groupId);

                Set<UInt160> accountKeySet = accounts_tracked.keySet();
                byte[] value = SerializeHelper.toBytes(accountKeySet.toArray(new UInt160[0]));
                DBHelper.batchPut(batch, DataEntryPrefix.IX_Accounts, groupId, value);
                for (HashSet<CoinReference> coins : accounts_tracked.values()) {
                    coins.clear();
                }
            }
            for (CoinReference reference : coins_tracked.keySet()) {
                DBHelper.batchDelete(batch, DataEntryPrefix.ST_Coin, SerializeHelper.toBytes(reference));
            }

            coins_tracked.clear();
            DBHelper.findForEach(db, DataEntryPrefix.ST_Transaction, (k, v) -> batch.delete(k));
            db.write(batch);
        }
    }

    public void registerAccounts(Iterable<UInt160> accounts, Uint height) {
        synchronized (SyncRoot) {
            boolean index_exists = true;
            HashSet<UInt160> index = indexes.getOrDefault(height, null);
            if (index == null) {
                index_exists = false;
                index = new HashSet<>();
            }
            for (UInt160 account : accounts)
                if (!accounts_tracked.containsKey(account)) {
                    index.add(account); // why not `index.add(account) if !index.containKey(account)
                    accounts_tracked.put(account, new HashSet<>());
                }
            if (index.size() > 0) {
                WriteBatch batch = db.createWriteBatch();
                byte[] groupId;
                if (!index_exists) {
                    indexes.put(height, index);
                    groupId = getGroupId();
                    DBHelper.batchPut(batch, DataEntryPrefix.IX_Group, height.toBytes(), groupId);
                } else {
                    groupId = DBHelper.get(db, DataEntryPrefix.IX_Group, height.toBytes());
                }
                byte[] value = SerializeHelper.toBytes(index.toArray(new UInt160[0]));
                DBHelper.batchPut(batch, DataEntryPrefix.IX_Accounts, groupId, value);
                db.write(batch);
            }
        }
    }

    public void registerAccounts(Iterable<UInt160> accounts) {
        registerAccounts(accounts, Uint.ZERO);
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
                            byte[] groupId = DBHelper.get(db, DataEntryPrefix.IX_Group, height.toBytes(), options);
                            if (index.size() == 0) {
                                indexes.remove(height);
                                DBHelper.batchDelete(batch, DataEntryPrefix.IX_Group, height.toBytes());
                                DBHelper.batchDelete(batch, DataEntryPrefix.IX_Accounts, groupId);
                            } else {
                                byte[] value = SerializeHelper.toBytes(index.toArray(new UInt160[0]));
                                DBHelper.batchPut(batch, DataEntryPrefix.IX_Accounts, groupId, value);
                            }
                            break;
                        }
                    }
                    accounts_tracked.remove(account);
                    for (CoinReference reference : references) {
                        DBHelper.batchDelete(batch, DataEntryPrefix.ST_Coin, SerializeHelper.toBytes(reference));
                        coins_tracked.remove(reference);
                    }
                    DBHelper.findForEach(db, DataEntryPrefix.ST_Transaction, account.toArray(), (k, v) -> batch.delete(k));
                }
            }
            db.write(batch, new WriteOptions());
        }
    }
}