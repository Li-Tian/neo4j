package neo.ledger;

import com.google.gson.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import neo.UInt256;
import neo.Utils;
import neo.csharp.Uint;

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

        HashIndexState tmp = Utils.copyFromSerialize(indexState, HashIndexState::new);

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