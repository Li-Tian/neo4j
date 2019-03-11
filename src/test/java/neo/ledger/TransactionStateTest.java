package neo.ledger;

import com.google.gson.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import neo.csharp.Uint;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.network.p2p.payloads.MinerTransaction;


public class TransactionStateTest {

    @Test
    public void size() {
        TransactionState state = new TransactionState();
        state.blockIndex = new Uint(10);
        state.transaction = new MinerTransaction();

        Assert.assertEquals(15, state.size());
    }

    @Test
    public void copy() {
        TransactionState state = new TransactionState();
        state.blockIndex = new Uint(10);
        state.transaction = new MinerTransaction();

        TransactionState copy = state.copy();
        Assert.assertEquals(state.blockIndex, copy.blockIndex);
        Assert.assertEquals(state.transaction, copy.transaction);
    }

    @Test
    public void fromReplica() {
        TransactionState state = new TransactionState();
        state.blockIndex = new Uint(10);
        state.transaction = new MinerTransaction();

        TransactionState replica = new TransactionState();
        replica.fromReplica(state);
        Assert.assertEquals(state.blockIndex, replica.blockIndex);
        Assert.assertEquals(state.transaction, replica.transaction);
    }

    @Test
    public void toJson() {
        TransactionState state = new TransactionState();
        state.blockIndex = new Uint(10);
        state.transaction = new MinerTransaction();

        JsonObject jsonObject = state.toJson();
        Assert.assertEquals("0", jsonObject.get("version").getAsString());
        Assert.assertEquals(10, jsonObject.get("height").getAsInt());
    }

    @Test
    public void serialize() {
        TransactionState state = new TransactionState();
        state.blockIndex = new Uint(10);
        state.transaction = new MinerTransaction();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BinaryWriter writer = new BinaryWriter(byteArrayOutputStream);
        state.serialize(writer);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        TransactionState tmp = new TransactionState();
        tmp.deserialize(new BinaryReader(inputStream));

        Assert.assertEquals(state.blockIndex, tmp.blockIndex);
        Assert.assertEquals(state.transaction, tmp.transaction);
    }
}