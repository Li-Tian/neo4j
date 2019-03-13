package neo.network.p2p.payloads;

import com.google.gson.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import neo.Fixed8;
import neo.UInt160;
import neo.UInt256;
import neo.Utils;

import static org.junit.Assert.*;

public class TransactionOutputTest {

    private TransactionOutput output = new TransactionOutput() {{
        assetId = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        value = new Fixed8(10000);
        scriptHash = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
    }};

    @Test
    public void size() {
        // 32 + 8 + 20 = 60
        Assert.assertEquals(60, output.size());
    }

    @Test
    public void serialize() {
        TransactionOutput copy = Utils.copyFromSerialize(output, TransactionOutput::new);

        Assert.assertEquals(output.assetId, copy.assetId);
        Assert.assertEquals(output.value, copy.value);
        Assert.assertEquals(output.scriptHash, copy.scriptHash);
    }

    @Test
    public void toJson() {
        JsonObject jsonObject = output.toJson(0);

        Assert.assertEquals(0, jsonObject.get("n").getAsInt());
        Assert.assertEquals(output.assetId.toString(), jsonObject.get("asset").getAsString());
        Assert.assertEquals(output.value.toString(), jsonObject.get("value").getAsString());
        Assert.assertEquals(output.scriptHash.toAddress(), jsonObject.get("address").getAsString());
    }
}