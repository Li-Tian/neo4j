package neo.ledger;

import com.google.gson.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.Map;

import neo.UInt160;
import neo.UInt256;
import neo.Utils;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.io.caching.DataCache;
import neo.network.p2p.payloads.Block;
import neo.network.p2p.payloads.Header;
import neo.network.p2p.payloads.MinerTransaction;
import neo.network.p2p.payloads.Witness;

public class TrimmedBlockTest {

    @Test
    public void isBlock() {
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
        trimmedBlock.hashes = new UInt256[]{new MinerTransaction().hash()};

        Assert.assertEquals(true, trimmedBlock.isBlock());
    }

    @Test
    public void getBlock() {
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
        trimmedBlock.hashes = new UInt256[]{new MinerTransaction().hash()};
        DataCache<UInt256, TransactionState> cache = new MyDataCache();
        TransactionState state = new TransactionState();
        state.transaction = new MinerTransaction();
        state.blockIndex = new Uint(10);
        cache.add(new MinerTransaction().hash(), state);
        Block block = trimmedBlock.getBlock(cache);

        Assert.assertEquals(1, block.transactions.length);
        Assert.assertEquals(new MinerTransaction(), block.transactions[0]);
    }

    @Test
    public void getHeader() {
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
        trimmedBlock.hashes = new UInt256[]{new MinerTransaction().hash()};

        Header header = trimmedBlock.getHeader();
        Assert.assertEquals(trimmedBlock.consensusData, header.consensusData);
        Assert.assertEquals(trimmedBlock.version, header.version);
        Assert.assertEquals(trimmedBlock.prevHash, header.prevHash);
        Assert.assertEquals(trimmedBlock.merkleRoot, header.merkleRoot);
    }

    @Test
    public void size() {
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
        trimmedBlock.hashes = new UInt256[]{new MinerTransaction().hash()};

        Assert.assertEquals(144, trimmedBlock.size());
    }

    @Test
    public void serialize() {
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
        trimmedBlock.hashes = new UInt256[]{new MinerTransaction().hash()};

        TrimmedBlock tmp = Utils.copyFromSerialize(trimmedBlock, TrimmedBlock::new);

        Assert.assertEquals(trimmedBlock.consensusData, tmp.consensusData);
        Assert.assertEquals(trimmedBlock.version, tmp.version);
        Assert.assertEquals(trimmedBlock.prevHash, tmp.prevHash);
        Assert.assertEquals(trimmedBlock.merkleRoot, tmp.merkleRoot);
    }

    @Test
    public void toJson() {
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
        trimmedBlock.hashes = new UInt256[]{new MinerTransaction().hash()};

        JsonObject jsonObject = trimmedBlock.toJson();
        Assert.assertEquals("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01", jsonObject.get("previousblockhash").getAsString());
    }

    private class MyDataCache extends DataCache<UInt256, TransactionState> {
        @Override
        protected TransactionState getInternal(UInt256 uInt256) {
            return null;
        }

        @Override
        protected void addInternal(UInt256 uInt256, TransactionState transactionState) {

        }

        @Override
        protected TransactionState tryGetInternal(UInt256 uInt256) {
            return null;
        }

        @Override
        protected void updateInternal(UInt256 uInt256, TransactionState transactionState) {

        }

        @Override
        public void deleteInternal(UInt256 uInt256) {

        }

        @Override
        protected Collection<Map.Entry<UInt256, TransactionState>> findInternal(byte[] keyPrefix) {
            return null;
        }
    }
}