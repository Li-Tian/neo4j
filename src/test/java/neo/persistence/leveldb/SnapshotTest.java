package neo.persistence.leveldb;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import neo.Fixed8;
import neo.UInt160;
import neo.UInt256;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.csharp.Ushort;
import neo.io.caching.DataCache;
import neo.io.caching.MetaDataCache;
import neo.ledger.Blockchain;
import neo.ledger.HashIndexState;
import neo.ledger.SpentCoin;
import neo.ledger.SpentCoinState;
import neo.ledger.TransactionState;
import neo.ledger.TrimmedBlock;
import neo.network.p2p.payloads.Block;
import neo.network.p2p.payloads.CoinReference;
import neo.network.p2p.payloads.MinerTransaction;
import neo.network.p2p.payloads.TransactionOutput;
import neo.network.p2p.payloads.Witness;
import neo.persistence.Snapshot;


public abstract class SnapshotTest {

    private Snapshot snapshot;
    private BlockchainDemo blockchainDemo;

    protected abstract Snapshot init();

    @Before
    public void before() throws IOException {
        snapshot = init();

        ActorSystem system = ActorSystem.create("neosystem");
        system.actorOf(Props.create(BlockchainDemo.class));
        blockchainDemo = (BlockchainDemo) BlockchainDemo.singleton();
        System.out.println(blockchainDemo);
    }


    @Test
    public void getPersistingBlock() {
        TrimmedBlock trimmedBlock = new TrimmedBlock();
        trimmedBlock.consensusData = new Ulong(10);
        trimmedBlock.version = new Uint(1);
        trimmedBlock.prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        trimmedBlock.merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        trimmedBlock.timestamp = new Uint(1568489959);
        trimmedBlock.index = new Uint(10);
        trimmedBlock.nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        trimmedBlock.witness = new Witness();
        trimmedBlock.witness.verificationScript = new byte[]{0x01, 0x02};
        trimmedBlock.witness.invocationScript = new byte[]{0x3, 0x04};
        MinerTransaction minerTransaction = new MinerTransaction();
        trimmedBlock.hashes = new UInt256[]{minerTransaction.hash()};

        DataCache<UInt256, TransactionState> cache = snapshot.getTransactions();
        TransactionState txState = new TransactionState();
        txState.transaction = minerTransaction;
        txState.blockIndex = new Uint(10);
        cache.add(minerTransaction.hash(), txState);

        Block timBlock = trimmedBlock.getBlock(cache);
        snapshot.setPersistingBlock(timBlock);
        Block persistBlock = snapshot.getPersistingBlock();

        Assert.assertEquals(1, persistBlock.transactions.length);
        Assert.assertEquals(minerTransaction, persistBlock.transactions[0]);
    }

    @Test
    public void getHeight() {
        Uint height = snapshot.getHeight();
        Assert.assertEquals(Uint.MAX_VALUE_2, height);

        MetaDataCache<HashIndexState> cache = snapshot.getBlockHashIndex();
        HashIndexState state = cache.getAndChange();
        state.index = new Uint(10);
        snapshot.commit();

        height = snapshot.getHeight();
        Assert.assertEquals(new Uint(10), height);
    }

    @Test
    public void getHeaderHeight() {
        UInt256 height = snapshot.getHeaderHeight();
        Assert.assertEquals(UInt256.Zero, height);

        MetaDataCache<HashIndexState> cache = snapshot.getBlockHashIndex();
        HashIndexState state = cache.getAndChange();
        state.hash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        height = snapshot.getHeaderHeight();
        Assert.assertEquals(state.hash, height);
    }

    @Test
    public void getCurrentBlockHash() {
        MetaDataCache<HashIndexState> cache = snapshot.getBlockHashIndex();
        HashIndexState state = cache.getAndChange();
        state.hash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        snapshot.commit();

        UInt256 hash = snapshot.getCurrentBlockHash();
        Assert.assertEquals(state.hash, hash);
    }

    @Test
    public void getCurrentHeaderHash() {
        MetaDataCache<HashIndexState> cache = snapshot.getHeaderHashIndex();
        HashIndexState state = cache.getAndChange();
        state.hash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        snapshot.commit();

        UInt256 hash = snapshot.getCurrentHeaderHash();
        Assert.assertEquals(state.hash, hash);
    }

    @Test
    public void getUnclaimed() {
        // construct tx
        MinerTransaction minerTransaction = new MinerTransaction();
        DataCache<UInt256, TransactionState> tx = snapshot.getTransactions();
        minerTransaction.outputs = new TransactionOutput[]{
                new TransactionOutput() {{
                    scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                    assetId = Blockchain.GoverningToken.hash();
                    value = new Fixed8(10000);
                }},
                new TransactionOutput() {{
                    scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
                    assetId = Blockchain.GoverningToken.hash();
                    value = new Fixed8(20000);
                }},
                new TransactionOutput() {{
                    scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff03");
                    assetId = Blockchain.GoverningToken.hash();
                    value = new Fixed8(30000);
                }}
        };
        TransactionState txState = new TransactionState() {{
            blockIndex = new Uint(0);
            transaction = minerTransaction;
        }};
        tx.add(minerTransaction.hash(), txState);

        // add spent coin
        DataCache<UInt256, SpentCoinState> spent = snapshot.getSpentCoins();
        SpentCoinState spentState = new SpentCoinState() {{
            transactionHash = minerTransaction.hash();
            transactionHeight = new Uint(0);
            items = new HashMap<Ushort, Uint>() {{
                put(new Ushort(0), new Uint(10));
                put(new Ushort(2), new Uint(20));
            }};
        }};
        spent.add(spentState.transactionHash, spentState);
        snapshot.commit();

        HashMap<Ushort, SpentCoin> unclaimSet = snapshot.getUnclaimed(minerTransaction.hash());
        Assert.assertEquals(2, unclaimSet.size());
        SpentCoin sp1 = unclaimSet.get(new Ushort(0));
        Assert.assertEquals(new Uint(0), sp1.startHeight);
        Assert.assertEquals(new Uint(10), sp1.endHeight);
        Assert.assertEquals(new Fixed8(10000), sp1.output.value);

        SpentCoin sp2 = unclaimSet.get(new Ushort(2));
        Assert.assertEquals(new Uint(0), sp2.startHeight);
        Assert.assertEquals(new Uint(20), sp2.endHeight);
        Assert.assertEquals(new Fixed8(30000), sp2.output.value);

        tx.delete(minerTransaction.hash());
        spent.delete(minerTransaction.hash());
        snapshot.commit();
    }

    @Test
    public void calculateBonus() {
        for (int i = 0; i < 20; i++) {
            switch (i) {
                case 0:
                    blockchainDemo.headerIndex.add(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01"));
                    break;
                case 9:
                    blockchainDemo.headerIndex.add(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02"));
                    break;
                case 19:
                    blockchainDemo.headerIndex.add(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff03"));
                    break;
                default:
                    blockchainDemo.headerIndex.add(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00"));
            }
        }

        // construct tx
        MinerTransaction minerTransaction = new MinerTransaction();
        DataCache<UInt256, TransactionState> tx = snapshot.getTransactions();
        minerTransaction.outputs = new TransactionOutput[]{
                new TransactionOutput() {{
                    scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                    assetId = Blockchain.GoverningToken.hash();
                    value = new Fixed8(100000000000000l);
                }},
                new TransactionOutput() {{
                    scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff02");
                    assetId = Blockchain.GoverningToken.hash();
                    value = new Fixed8(200000000000000l);
                }},
                new TransactionOutput() {{
                    scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff03");
                    assetId = Blockchain.GoverningToken.hash();
                    value = new Fixed8(300000000000000l);
                }}
        };
        TransactionState txState = new TransactionState() {{
            blockIndex = new Uint(0);
            transaction = minerTransaction;
        }};
        tx.add(minerTransaction.hash(), txState);

        // add spent coin
        DataCache<UInt256, SpentCoinState> spent = snapshot.getSpentCoins();
        SpentCoinState spentState = new SpentCoinState() {{
            transactionHash = minerTransaction.hash();
            transactionHeight = new Uint(0);
            items = new HashMap<Ushort, Uint>() {{
                put(new Ushort(0), new Uint(10)); // 10000
                put(new Ushort(2), new Uint(20)); // 30000
            }};
        }};
        spent.add(spentState.transactionHash, spentState);
        snapshot.commit();

        ArrayList<CoinReference> unclaimReference = new ArrayList<>(2);
        unclaimReference.add(new CoinReference() {{
            prevHash = minerTransaction.hash();
            prevIndex = new Ushort(0);
        }});
        unclaimReference.add(new CoinReference() {{
            prevHash = minerTransaction.hash();
            prevIndex = new Ushort(1);
        }});
        unclaimReference.add(new CoinReference() {{
            prevHash = minerTransaction.hash();
            prevIndex = new Ushort(2);
        }});

//        Fixed8 bonus = snapshot.calculateBonus(unclaimReference);
//        Assert.assertEquals(new Fixed8(80000000), bonus);
        // TODO waiting for check....
    }

    @Test
    public void getValidatorPubkeys() {

    }

    @Test
    public void containsBlock() {

    }


    @Test
    public void containsTransaction() {

    }

    @Test
    public void getBlock() {

    }


    @Test
    public void getEnrollments() {

    }

    @Test
    public void getHeader() {

    }

    @Test
    public void getNextBlockHash() {

    }

    @Test
    public void getSysFeeAmount() {

    }

    @Test
    public void getTransaction() {

    }

    @Test
    public void getUnspent() {

    }

    @Test
    public void isDoubleSpend() {

    }
}