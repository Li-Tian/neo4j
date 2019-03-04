package neo.ledger;

import com.google.gson.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import neo.UInt256;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;

import static org.junit.Assert.*;

public class HeaderHashListTest {

    @Test
    public void size() {
        HeaderHashList hashList = new HeaderHashList();
        hashList.hashes = new UInt256[]{UInt256.Zero, UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01")};

        Assert.assertEquals(66, hashList.size());
    }

    @Test
    public void copy() {
        HeaderHashList hashList = new HeaderHashList();
        hashList.hashes = new UInt256[]{UInt256.Zero, UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01")};

        HeaderHashList copy = hashList.copy();

        Assert.assertEquals(2, copy.hashes.length);
        Assert.assertEquals(UInt256.Zero, copy.hashes[0]);
        Assert.assertEquals(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01"), copy.hashes[1]);
    }

    @Test
    public void fromReplica() {
        HeaderHashList hashList = new HeaderHashList();
        hashList.hashes = new UInt256[]{UInt256.Zero, UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01")};

        HeaderHashList copy = new HeaderHashList();
        copy.fromReplica(hashList);

        Assert.assertEquals(2, copy.hashes.length);
        Assert.assertEquals(UInt256.Zero, copy.hashes[0]);
        Assert.assertEquals(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01"), copy.hashes[1]);
    }

    @Test
    public void serialize() {
        HeaderHashList hashList = new HeaderHashList();
        hashList.hashes = new UInt256[]{UInt256.Zero, UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01")};

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BinaryWriter writer = new BinaryWriter(byteArrayOutputStream);
        hashList.serialize(writer);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        HeaderHashList tmp = new HeaderHashList();
        tmp.deserialize(new BinaryReader(inputStream));

        Assert.assertEquals(2, tmp.hashes.length);
        Assert.assertEquals(UInt256.Zero, tmp.hashes[0]);
        Assert.assertEquals(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01"), tmp.hashes[1]);
    }

    @Test
    public void toJson() {
        HeaderHashList hashList = new HeaderHashList();
        hashList.hashes = new UInt256[]{UInt256.Zero, UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01")};

        JsonObject jsonObject = hashList.toJson();
        Assert.assertEquals("0x0000000000000000000000000000000000000000000000000000000000000000", jsonObject.getAsJsonArray("hashes").get(0).getAsString());
        Assert.assertEquals("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01", jsonObject.getAsJsonArray("hashes").get(1).getAsString());
    }
}