package neo.ledger;

import com.google.gson.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import neo.UInt256;
import neo.csharp.Uint;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;

import static org.junit.Assert.*;

public class HashIndexStateTest {

    @Test
    public void size() {
        HashIndexState indexState = new HashIndexState();
        indexState.hash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        indexState.index = new Uint(10);

        Assert.assertEquals(37, indexState.size());
    }

    @Test
    public void copy() {
        HashIndexState indexState = new HashIndexState();
        indexState.hash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        indexState.index = new Uint(10);

        HashIndexState copy = indexState.copy();
        Assert.assertEquals(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01"), copy.hash);
        Assert.assertEquals(new Uint(10), copy.index);
    }

    @Test
    public void fromReplica() {
        HashIndexState indexState = new HashIndexState();
        indexState.hash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        indexState.index = new Uint(10);

        HashIndexState copy = new HashIndexState();
        copy.fromReplica(indexState);
        Assert.assertEquals(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01"), copy.hash);
        Assert.assertEquals(new Uint(10), copy.index);
    }

    @Test
    public void serialize() {
        HashIndexState indexState = new HashIndexState();
        indexState.hash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        indexState.index = new Uint(10);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BinaryWriter writer = new BinaryWriter(byteArrayOutputStream);
        indexState.serialize(writer);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        HashIndexState tmp = new HashIndexState();
        tmp.deserialize(new BinaryReader(inputStream));

        Assert.assertEquals(UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01"), tmp.hash);
        Assert.assertEquals(new Uint(10), tmp.index);
    }

    @Test
    public void toJson() {
        HashIndexState indexState = new HashIndexState();
        indexState.hash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        indexState.index = new Uint(10);

        JsonObject jsonObject = indexState.toJson();
        Assert.assertEquals("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01", jsonObject.get("hash").getAsString());
    }
}