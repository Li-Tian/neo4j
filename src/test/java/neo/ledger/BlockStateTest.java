package neo.ledger;

import com.google.gson.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import neo.UInt160;
import neo.UInt256;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.network.p2p.payloads.MinerTransaction;
import neo.network.p2p.payloads.Witness;

import static org.junit.Assert.*;

public class BlockStateTest {

    @Test
    public void size() {
        BlockState blockState = new BlockState();
        blockState.systemFeeAmount = 100;
        blockState.trimmedBlock = new TrimmedBlock();
        blockState.trimmedBlock.consensusData = new Ulong(10);
        blockState.trimmedBlock.version = new Uint(1);
        blockState.trimmedBlock.prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        blockState.trimmedBlock.timestamp = new Uint(1568489959);
        blockState.trimmedBlock.index = new Uint(10);
        blockState.trimmedBlock.nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.witness = new Witness();
        blockState.trimmedBlock.witness.verificationScript = new byte[]{0x01, 0x02};
        blockState.trimmedBlock.witness.invocationScript = new byte[]{0x3, 0x04};
        blockState.trimmedBlock.hashes = new UInt256[]{new MinerTransaction().hash()};

        Assert.assertEquals(153, blockState.size());
    }

    @Test
    public void copy() {
        BlockState blockState = new BlockState();
        blockState.systemFeeAmount = 100;
        blockState.trimmedBlock = new TrimmedBlock();
        blockState.trimmedBlock.consensusData = new Ulong(10);
        blockState.trimmedBlock.version = new Uint(1);
        blockState.trimmedBlock.prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        blockState.trimmedBlock.timestamp = new Uint(1568489959);
        blockState.trimmedBlock.index = new Uint(10);
        blockState.trimmedBlock.nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.witness = new Witness();
        blockState.trimmedBlock.witness.verificationScript = new byte[]{0x01, 0x02};
        blockState.trimmedBlock.witness.invocationScript = new byte[]{0x3, 0x04};
        blockState.trimmedBlock.hashes = new UInt256[]{new MinerTransaction().hash()};

        BlockState copy = blockState.copy();

        Assert.assertEquals(100, copy.systemFeeAmount);
        Assert.assertEquals(new Ulong(10), copy.trimmedBlock.consensusData);
        Assert.assertEquals(new Uint(1), copy.trimmedBlock.version);
        Assert.assertEquals("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01", copy.trimmedBlock.prevHash.toString());
        Assert.assertEquals("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02", copy.trimmedBlock.merkleRoot.toString());
        Assert.assertEquals(new Uint(1568489959), copy.trimmedBlock.timestamp);
        Assert.assertEquals(new Uint(10), copy.trimmedBlock.index);
        Assert.assertEquals("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01", copy.trimmedBlock.nextConsensus.toString());
        Assert.assertArrayEquals(new byte[]{0x01, 0x02}, copy.trimmedBlock.witness.verificationScript);
        Assert.assertArrayEquals(new byte[]{0x3, 0x4}, copy.trimmedBlock.witness.invocationScript);
    }

    @Test
    public void fromReplica() {
        BlockState blockState = new BlockState();
        blockState.systemFeeAmount = 100;
        blockState.trimmedBlock = new TrimmedBlock();
        blockState.trimmedBlock.consensusData = new Ulong(10);
        blockState.trimmedBlock.version = new Uint(1);
        blockState.trimmedBlock.prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        blockState.trimmedBlock.timestamp = new Uint(1568489959);
        blockState.trimmedBlock.index = new Uint(10);
        blockState.trimmedBlock.nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.witness = new Witness();
        blockState.trimmedBlock.witness.verificationScript = new byte[]{0x01, 0x02};
        blockState.trimmedBlock.witness.invocationScript = new byte[]{0x3, 0x04};
        blockState.trimmedBlock.hashes = new UInt256[]{new MinerTransaction().hash()};

        BlockState copy = new BlockState();
        copy.fromReplica(blockState);

        Assert.assertEquals(100, copy.systemFeeAmount);
        Assert.assertEquals(new Ulong(10), copy.trimmedBlock.consensusData);
        Assert.assertEquals(new Uint(1), copy.trimmedBlock.version);
        Assert.assertEquals("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01", copy.trimmedBlock.prevHash.toString());
        Assert.assertEquals("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02", copy.trimmedBlock.merkleRoot.toString());
        Assert.assertEquals(new Uint(1568489959), copy.trimmedBlock.timestamp);
        Assert.assertEquals(new Uint(10), copy.trimmedBlock.index);
        Assert.assertEquals("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01", copy.trimmedBlock.nextConsensus.toString());
        Assert.assertArrayEquals(new byte[]{0x01, 0x02}, copy.trimmedBlock.witness.verificationScript);
        Assert.assertArrayEquals(new byte[]{0x3, 0x4}, copy.trimmedBlock.witness.invocationScript);
    }

    @Test
    public void serialize() {
        BlockState blockState = new BlockState();
        blockState.systemFeeAmount = 100;
        blockState.trimmedBlock = new TrimmedBlock();
        blockState.trimmedBlock.consensusData = new Ulong(10);
        blockState.trimmedBlock.version = new Uint(1);
        blockState.trimmedBlock.prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        blockState.trimmedBlock.timestamp = new Uint(1568489959);
        blockState.trimmedBlock.index = new Uint(10);
        blockState.trimmedBlock.nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.witness = new Witness();
        blockState.trimmedBlock.witness.verificationScript = new byte[]{0x01, 0x02};
        blockState.trimmedBlock.witness.invocationScript = new byte[]{0x3, 0x04};
        blockState.trimmedBlock.hashes = new UInt256[]{new MinerTransaction().hash()};

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BinaryWriter writer = new BinaryWriter(byteArrayOutputStream);
        blockState.serialize(writer);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        BlockState copy = new BlockState();
        copy.deserialize(new BinaryReader(inputStream));

        Assert.assertEquals(100, copy.systemFeeAmount);
        Assert.assertEquals(new Ulong(10), copy.trimmedBlock.consensusData);
        Assert.assertEquals(new Uint(1), copy.trimmedBlock.version);
        Assert.assertEquals("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01", copy.trimmedBlock.prevHash.toString());
        Assert.assertEquals("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02", copy.trimmedBlock.merkleRoot.toString());
        Assert.assertEquals(new Uint(1568489959), copy.trimmedBlock.timestamp);
        Assert.assertEquals(new Uint(10), copy.trimmedBlock.index);
        Assert.assertEquals("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01", copy.trimmedBlock.nextConsensus.toString());
        Assert.assertArrayEquals(new byte[]{0x01, 0x02}, copy.trimmedBlock.witness.verificationScript);
        Assert.assertArrayEquals(new byte[]{0x3, 0x4}, copy.trimmedBlock.witness.invocationScript);
    }

    @Test
    public void toJson() {
        BlockState blockState = new BlockState();
        blockState.systemFeeAmount = 100;
        blockState.trimmedBlock = new TrimmedBlock();
        blockState.trimmedBlock.consensusData = new Ulong(10);
        blockState.trimmedBlock.version = new Uint(1);
        blockState.trimmedBlock.prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        blockState.trimmedBlock.timestamp = new Uint(1568489959);
        blockState.trimmedBlock.index = new Uint(10);
        blockState.trimmedBlock.nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        blockState.trimmedBlock.witness = new Witness();
        blockState.trimmedBlock.witness.verificationScript = new byte[]{0x01, 0x02};
        blockState.trimmedBlock.witness.invocationScript = new byte[]{0x3, 0x04};
        blockState.trimmedBlock.hashes = new UInt256[]{new MinerTransaction().hash()};

        JsonObject jsonObject = blockState.toJson();
        Assert.assertEquals("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01", jsonObject.get("script_hash").getAsString());
        Assert.assertEquals("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01", jsonObject.get("script_hash").getAsString());

        Assert.assertEquals(100, jsonObject.get("sysfee_amount").getAsLong());
        Assert.assertEquals("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01", jsonObject.getAsJsonObject("trimmed").get("previousblockhash").getAsString());
    }
}