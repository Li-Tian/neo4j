package neo.network.p2p.payloads;

import org.junit.Assert;
import org.junit.Test;

import java.util.BitSet;

import neo.UInt160;
import neo.UInt256;
import neo.Utils;
import neo.csharp.Uint;
import neo.csharp.Ulong;

import static org.junit.Assert.*;

public class MerkleBlockPayloadTest {

    @Test
    public void create() {
        Block block = new Block() {{
            version = new Uint(1);
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
            timestamp = new Uint(123885824);
            index = new Uint(10);
            consensusData = new Ulong(0);
            nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            witness = new Witness() {{
                verificationScript = new byte[]{0x00};
                invocationScript = new byte[]{0x00};
            }};
            transactions = new Transaction[]{
                    new MinerTransaction()
            };
        }};

        BitSet flags = new BitSet(16);
        MerkleBlockPayload payload = MerkleBlockPayload.create(block, flags);
        Assert.assertEquals(block.version, payload.version);
        Assert.assertEquals(block.prevHash, payload.prevHash);
        Assert.assertEquals(block.merkleRoot, payload.merkleRoot);
        Assert.assertEquals(block.consensusData, payload.consensusData);
        Assert.assertEquals(1, payload.hashes.length);
        Assert.assertEquals(block.transactions[0].hash(), payload.hashes[0]);
    }

    @Test
    public void size() {
        MerkleBlockPayload payload = new MerkleBlockPayload() {{
            version = new Uint(1);
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
            timestamp = new Uint(123885824);
            index = new Uint(10);
            consensusData = new Ulong(0);
            nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            witness = new Witness() {{
                verificationScript = new byte[]{0x00};
                invocationScript = new byte[]{0x00};
            }};
            txCount = 10;
            hashes = new UInt256[]{UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff03")};
            flags = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        }};
        // (105 + 2 + 2) + 4 + hashes + flags = 109 + 4+ 33 + 9 =155
        Assert.assertEquals(155, payload.size());
    }

    @Test
    public void serialize() {
        MerkleBlockPayload payload = new MerkleBlockPayload() {{
            version = new Uint(1);
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
            timestamp = new Uint(123885824);
            index = new Uint(10);
            consensusData = new Ulong(0);
            nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            witness = new Witness() {{
                verificationScript = new byte[]{0x00};
                invocationScript = new byte[]{0x00};
            }};
            txCount = 10;
            hashes = new UInt256[]{UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff03")};
            flags = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        }};
        MerkleBlockPayload copy = Utils.copyFromSerialize(payload, MerkleBlockPayload::new);

        Assert.assertEquals(payload.version, copy.version);
        Assert.assertEquals(payload.prevHash, copy.prevHash);
        Assert.assertEquals(payload.merkleRoot, copy.merkleRoot);
        Assert.assertEquals(payload.timestamp, copy.timestamp);
        Assert.assertEquals(payload.index, copy.index);
        Assert.assertEquals(payload.consensusData, copy.consensusData);
        Assert.assertEquals(payload.nextConsensus, copy.nextConsensus);
        Assert.assertEquals(payload.txCount, copy.txCount);
        Assert.assertEquals(payload.hashes, copy.hashes);
        Assert.assertArrayEquals(payload.flags, copy.flags);
        Assert.assertArrayEquals(payload.witness.verificationScript, copy.witness.verificationScript);
        Assert.assertArrayEquals(payload.witness.invocationScript, copy.witness.invocationScript);
    }
}