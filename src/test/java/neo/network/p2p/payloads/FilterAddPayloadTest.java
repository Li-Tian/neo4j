package neo.network.p2p.payloads;

import org.junit.Assert;
import org.junit.Test;

import neo.Utils;

import static org.junit.Assert.*;

public class FilterAddPayloadTest {

    @Test
    public void size() {
        FilterAddPayload payload = new FilterAddPayload() {{
            data = new byte[]{0x00, 0x00, 0x00, 0x00};
        }};

        Assert.assertEquals(5, payload.size());
    }

    @Test
    public void serialize() {
        FilterAddPayload payload = new FilterAddPayload() {{
            data = new byte[]{0x00, 0x00, 0x00, 0x00};
        }};

        FilterAddPayload copy = Utils.copyFromSerialize(payload, FilterAddPayload::new);

        Assert.assertArrayEquals(payload.data, copy.data);
    }
}