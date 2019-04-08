package neo.Wallets;

import org.iq80.leveldb.WriteBatch;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

import neo.Fixed8;
import neo.UInt160;
import neo.UInt256;
import neo.Utils;
import neo.consensus.ConsensusContextTest;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.csharp.Ushort;
import neo.io.SerializeHelper;
import neo.io.caching.DataCache;
import neo.ledger.BlockChainTest;
import neo.ledger.BlockState;
import neo.ledger.Blockchain;
import neo.ledger.CoinState;
import neo.ledger.TransactionState;
import neo.ledger.TrimmedBlock;
import neo.network.p2p.payloads.AssetType;
import neo.network.p2p.payloads.Block;
import neo.network.p2p.payloads.ClaimTransaction;
import neo.network.p2p.payloads.CoinReference;
import neo.network.p2p.payloads.ContractTransaction;
import neo.network.p2p.payloads.EnrollmentTransaction;
import neo.network.p2p.payloads.IVerifiable;
import neo.network.p2p.payloads.MinerTransaction;
import neo.network.p2p.payloads.RegisterTransaction;
import neo.network.p2p.payloads.StateDescriptor;
import neo.network.p2p.payloads.StateTransaction;
import neo.network.p2p.payloads.Transaction;
import neo.network.p2p.payloads.TransactionAttribute;
import neo.network.p2p.payloads.TransactionOutput;
import neo.network.p2p.payloads.Witness;
import neo.persistence.AbstractBlockchainTest;
import neo.persistence.Snapshot;
import neo.persistence.leveldb.DBHelper;
import neo.smartcontract.Contract;
import neo.vm.OpCode;

import static org.junit.Assert.*;

public class WalletIndexerTest extends AbstractBlockchainTest {

    private static WalletIndexer walletIndexer;


    @BeforeClass
    public static void setup() throws IOException {
        AbstractBlockchainTest.setUp(WalletIndexerTest.class.getSimpleName());

        String path = WalletIndexerTest.class.getClassLoader().getResource("").getPath() + "wallet_index_leveldb";
        Utils.deleteFolder(path);
        walletIndexer = new WalletIndexer(path);
        walletIndexer.thread.interrupt();
    }

    @AfterClass
    public static void tearDown() throws IOException {
        walletIndexer.dispose();
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
        // prepare data
        UInt160 hash1 = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        UInt160 hash2 = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        CoinReference reference = new CoinReference() {{
            prevIndex = new Ushort(1);
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        }};
        HashSet<CoinReference> coins = new HashSet<>();
        coins.add(reference);
        walletIndexer.accounts_tracked.put(hash2, coins);

        HashSet<UInt160> accounts = new HashSet<>();
        accounts.add(hash2);
        walletIndexer.indexes.put(new Uint(21), accounts);

        walletIndexer.registerAccounts(Arrays.asList(hash1, hash2), new Uint(10));

        // check
        walletIndexer.rebuildIndex();
        Assert.assertEquals(1, walletIndexer.indexes.size());

        coins = walletIndexer.accounts_tracked.get(hash2);
        Assert.assertTrue(coins.isEmpty());
        Assert.assertTrue(walletIndexer.coins_tracked.isEmpty());

        // clear data
        walletIndexer.accounts_tracked.clear();
        walletIndexer.indexes.clear();
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


    @Test
    public void processBlocks() throws InterruptedException {
        // prepare data
        Snapshot snapshot = store.getSnapshot();

        // prepare coin_tracked accounts_tracked
        KeyPair keyPair1 = Utils.getRandomKeyPair();
        KeyPair keyPair2 = Utils.getRandomKeyPair();

        UInt160 account1 = UInt160.parseToScriptHash(Contract.createSignatureRedeemScript(keyPair1.publicKey));
        UInt160 account2 = UInt160.parseToScriptHash(Contract.createSignatureRedeemScript(keyPair2.publicKey));
        walletIndexer.accounts_tracked.put(account1, new HashSet<>());


        CoinReference reference1 = new CoinReference() {{
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            prevIndex = new Ushort(0);
        }};
        Coin coin1 = new Coin() {{
            reference = null;
            output = new TransactionOutput() {{
                assetId = Blockchain.UtilityToken.hash();
                value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                scriptHash = account1;
            }};
            state = CoinState.Confirmed;
        }};

        CoinReference reference2 = new CoinReference() {{
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            prevIndex = new Ushort(1);
        }};
        Coin coin2 = new Coin() {{
            reference = null;
            output = new TransactionOutput() {{
                assetId = Blockchain.UtilityToken.hash();
                value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                scriptHash = account2;
            }};
            state = CoinState.Confirmed;
        }};
        walletIndexer.coins_tracked.put(reference1, coin1);
        walletIndexer.coins_tracked.put(reference2, coin2);

        HashSet<CoinReference> account1Coins = new HashSet<>();
        account1Coins.add(reference1);
        walletIndexer.accounts_tracked.put(account1, account1Coins);

        HashSet<CoinReference> account2Coins = new HashSet<>();
        account2Coins.add(reference2);
        walletIndexer.accounts_tracked.put(account2, account2Coins);


        KeyPair keyPair3 = Utils.getRandomKeyPair();
        UInt160 account3 = UInt160.parseToScriptHash(Contract.createSignatureRedeemScript(keyPair3.publicKey));
        KeyPair keyPair4 = Utils.getRandomKeyPair();
        UInt160 account4 = UInt160.parseToScriptHash(Contract.createSignatureRedeemScript(keyPair4.publicKey));
        KeyPair keyPair5 = Utils.getRandomKeyPair();
        UInt160 account5 = UInt160.parseToScriptHash(Contract.createSignatureRedeemScript(keyPair5.publicKey));
        walletIndexer.accounts_tracked.put(account3, new HashSet<>());
        walletIndexer.accounts_tracked.put(account5, new HashSet<>());


        UInt160 consensusAddress = Blockchain.singleton().getConsensusAddress(snapshot.getValidatorPubkeys());
        Block block1 = new Block() {
            {
                prevHash = Blockchain.GenesisBlock.hash();
                timestamp = new Uint(Long.toString(new Date("Jul 15 15:08:21 UTC 2018").getTime() / 1000));
                index = new Uint(1);
                consensusData = new Ulong(2083236894); //向比特币致敬
                nextConsensus = consensusAddress;
                witness = new Witness() {
                    {
                        invocationScript = new byte[0];
                        verificationScript = new byte[]{OpCode.PUSHT.getCode()};
                    }
                };
                transactions = new Transaction[]{
                        new MinerTransaction() {
                            {
                                nonce = new Uint(2083236891);
                                attributes = new TransactionAttribute[0];
                                inputs = new CoinReference[0];
                                outputs = new TransactionOutput[0];
                                witnesses = new Witness[0];
                            }
                        },
                        new ContractTransaction() {
                            {
                                attributes = new TransactionAttribute[0];
                                witnesses = new Witness[0];
                                inputs = new CoinReference[]{reference1};
                                outputs = new TransactionOutput[]{
                                        new TransactionOutput() {{
                                            assetId = Blockchain.UtilityToken.hash();
                                            value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                                            scriptHash = account2;
                                        }},
                                        new TransactionOutput() {{
                                            assetId = Blockchain.UtilityToken.hash();
                                            value = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                                            scriptHash = account3;
                                        }}
                                };
                            }
                        },
                        new ClaimTransaction() {{
                            claims = new CoinReference[]{
                                    reference2
                            };
                            attributes = new TransactionAttribute[0];
                            inputs = new CoinReference[0];
                            outputs = new TransactionOutput[0];
                            witnesses = new Witness[0];
                        }},
                        new EnrollmentTransaction() {{
                            publicKey = keyPair3.publicKey;
                            attributes = new TransactionAttribute[0];
                            inputs = new CoinReference[0];
                            outputs = new TransactionOutput[0];
                            witnesses = new Witness[0];
                        }},
                        new RegisterTransaction() {{
                            assetType = AssetType.Share;
                            name = "test";
                            amount = Fixed8.fromDecimal(BigDecimal.valueOf(1000));
                            precision = 0x00;
                            owner = keyPair4.publicKey;
                            admin = account4;
                            attributes = new TransactionAttribute[0];
                            inputs = new CoinReference[0];
                            outputs = new TransactionOutput[0];
                            witnesses = new Witness[0];
                        }},
                        new StateTransaction() {{
                            descriptors = new StateDescriptor[0];
                            witnesses = new Witness[]{
                                    new Witness() {{
                                        invocationScript = new byte[0];
                                        verificationScript = Contract.createSignatureRedeemScript(keyPair1.publicKey);
                                    }}
                            };
                            attributes = new TransactionAttribute[0];
                            inputs = new CoinReference[0];
                            outputs = new TransactionOutput[0];
                        }}
                };
            }
        };
        block1.rebuildMerkleRoot();

        BlockState blockState = new BlockState();
        blockState.trimmedBlock = block1.trim();
        blockState.systemFeeAmount = 10;

        UInt256 key = blockState.trimmedBlock.hash();
        snapshot.getBlocks().add(key, blockState);

        // add tx
        for (Transaction tx : block1.transactions) {
            TransactionState txState = new TransactionState() {{
                transaction = tx;
                blockIndex = block1.index;
            }};
            snapshot.getTransactions().add(tx.hash(), txState);
        }
        snapshot.commit();

        blockchain.myheaderIndex.clear();
        blockchain.myheaderIndex.add(Blockchain.GenesisBlock.hash());
        blockchain.myheaderIndex.add(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff03"));
        blockchain.myheaderIndex.add(block1.hash());


        // 准备 coins_tracked, accounts_tracked， 账户的移动 (group, accounts) 两张表

        // check
        HashSet<UInt160> accounts = new HashSet<>();
        accounts.add(account1);
        accounts.add(account2);
        accounts.add(account3);
        accounts.add(account5);
        walletIndexer.indexes.clear();
        walletIndexer.indexes.put(new Uint(2), accounts);
        byte[] groupId = new byte[]{0x01, 0x02};
        walletIndexer.db.put(BitConverter.merge(DataEntryPrefix.IX_Group, new Uint(2).toBytes()), groupId);

        // 结束掉 WalletIndex 内部的 processBlocks 循环
        new Thread(() -> {
            try {
                while (true) {
                    Uint height = walletIndexer.indexes.keySet().stream().min(Uint::compareTo).get();
                    if (height.intValue() == new Uint(3).intValue()) {
                        walletIndexer.disposed = true;
                        break;
                    } else {
                        Thread.currentThread().sleep(100);
                    }
                }
            } catch (InterruptedException e) {
            }
        }).start();
        walletIndexer.processBlocks();


        // accounts_tracked 更新，account -> coinReference 新增
        HashSet<CoinReference> account2CoinSet = walletIndexer.accounts_tracked.get(account2);
        Assert.assertEquals(1, account2CoinSet.size());
        CoinReference watchReference = account2CoinSet.iterator().next();
        Assert.assertEquals(block1.transactions[1].hash(), watchReference.prevHash);
        Assert.assertEquals(Ushort.ZERO, watchReference.prevIndex);

        // input 对应的 account的交易，则被清除
        Assert.assertEquals(false, walletIndexer.accounts_tracked.get(account1).contains(reference1));
        Assert.assertEquals(false, walletIndexer.coins_tracked.containsKey(reference1));


        // claim tx 对应所引用的交易，则被清除
        Assert.assertEquals(false, walletIndexer.accounts_tracked.get(account2).contains(reference2));
        Assert.assertEquals(false, walletIndexer.coins_tracked.containsKey(reference2));


        // EnrollmentTransaction 新增验证申请人地址 对应的交易
        byte[] values = DBHelper.get(walletIndexer.db, DataEntryPrefix.ST_Transaction, BitConverter.merge(account3.toArray(), block1.transactions[3].hash().toArray()));
        Assert.assertEquals(false, BitConverter.toBoolean(values));

        // RegisterTransaction 新增验证申请人地址 对应的交易 ( account4 no in account_tracked)
        values = DBHelper.get(walletIndexer.db, DataEntryPrefix.ST_Transaction, BitConverter.merge(account4.toArray(), block1.transactions[4].hash().toArray()));
        Assert.assertEquals(null, values);


        // StateTransaction 新增验证申请人地址 对应的交易
        values = DBHelper.get(walletIndexer.db, DataEntryPrefix.ST_Transaction, BitConverter.merge(account1.toArray(), block1.transactions[5].hash().toArray()));
        Assert.assertEquals(false, BitConverter.toBoolean(values));


        accounts = walletIndexer.indexes.get(new Uint(3));
        Assert.assertEquals(4, accounts.size());
        Assert.assertEquals(true, accounts.contains(account1));
        Assert.assertEquals(true, accounts.contains(account2));
        Assert.assertEquals(true, accounts.contains(account3));
        Assert.assertEquals(true, accounts.contains(account5));

        // clear data
        walletIndexer.indexes.clear();
        walletIndexer.coins_tracked.clear();
        walletIndexer.accounts_tracked.clear();
        snapshot.getBlocks().delete(block1.hash());
        walletIndexer.disposed = true;
    }

}