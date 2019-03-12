package neo.network.p2p.payloads;

import com.google.gson.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import neo.UInt256;
import neo.Utils;
import neo.csharp.Ushort;

public class CoinReferenceTest {

    @Test
    public void size() {
        CoinReference reference = new CoinReference() {{
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            prevIndex = new Ushort(0);
        }};
        Assert.assertEquals(34, reference.size());
    }

    @Test
    public void serialize() {
        CoinReference reference1 = new CoinReference() {{
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            prevIndex = new Ushort(0);
        }};

        CoinReference reference2 = Utils.copyFromSerialize(reference1, CoinReference::new);

        Assert.assertTrue(reference1.equals(reference2));
    }

    @Test
    public void testHashCode() {
        CoinReference reference1 = new CoinReference() {{
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            prevIndex = new Ushort(0);
        }};
        CoinReference reference2 = new CoinReference() {{
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            prevIndex = new Ushort(0);
        }};
        Assert.assertEquals(reference1.hashCode(), reference2.hashCode());
    }

    @Test
    public void equals() {
        CoinReference reference1 = new CoinReference() {{
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            prevIndex = new Ushort(0);
        }};
        CoinReference reference2 = new CoinReference() {{
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            prevIndex = new Ushort(0);
        }};
        Assert.assertTrue(reference1.equals(reference2));
    }

    @Test
    public void toJson() {
        CoinReference reference = new CoinReference() {{
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            prevIndex = new Ushort(0);
        }};
        JsonObject jsonObject = reference.toJson();
        Assert.assertEquals(reference.prevHash.toString(), jsonObject.get("txid").getAsString());
        Assert.assertEquals(reference.prevIndex.intValue(), jsonObject.get("vout").getAsInt());
    }

}