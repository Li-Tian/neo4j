package neo.network.p2p.payloads;

import com.google.gson.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import neo.UInt160;
import neo.Utils;
import neo.csharp.BitConverter;

import static org.junit.Assert.*;

public class TransactionAttributeTest {

    private TransactionAttribute attribute = new TransactionAttribute() {{
        usage = TransactionAttributeUsage.Script;
        data = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01").toArray();
    }};

    @Test
    public void size() {
        Assert.assertEquals(21, attribute.size());
    }

    @Test
    public void serialize() {
        TransactionAttribute copy = Utils.copyFromSerialize(attribute, TransactionAttribute::new);

        Assert.assertEquals(attribute.usage, copy.usage);
        Assert.assertArrayEquals(attribute.data, copy.data);
    }

    @Test
    public void toJson() {
        JsonObject jsonObject = attribute.toJson();
        Assert.assertEquals(attribute.usage.value(), jsonObject.get("usage").getAsByte());
        Assert.assertEquals(BitConverter.toHexString(attribute.data), jsonObject.get("data").getAsString());
    }
}