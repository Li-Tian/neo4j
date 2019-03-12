package neo.network.p2p.payloads;

import org.junit.Assert;
import org.junit.Test;

import neo.Utils;
import neo.cryptography.BloomFilter;
import neo.csharp.Uint;

import static org.junit.Assert.*;

public class FilterLoadPayloadTest {

    @Test
    public void size() {
        FilterLoadPayload payload = new FilterLoadPayload() {{
            filter = new byte[]{0x00, 0x00, 0x00, 0x00};
            k = (byte) 0x01;
            tweak = new Uint(1);
        }};
        Assert.assertEquals(10, payload.size());
    }

    @Test
    public void serialize() {
        FilterLoadPayload payload = new FilterLoadPayload() {{
            filter = new byte[]{0x00, 0x00, 0x00, 0x00};
            k = (byte) 0x01;
            tweak = new Uint(1);
        }};

        FilterLoadPayload copy = Utils.copyFromSerialize(payload, FilterLoadPayload::new);

        Assert.assertArrayEquals(payload.filter, copy.filter);
        Assert.assertEquals(payload.k, copy.k);
        Assert.assertEquals(payload.tweak, copy.tweak);
    }

    @Test
    public void create() {
        BloomFilter filter = new BloomFilter(8, 1, new Uint(1));
        FilterLoadPayload payload = FilterLoadPayload.create(filter);

        Assert.assertEquals(1, payload.k);
        Assert.assertEquals(new Uint(1), payload.tweak);
    }
}