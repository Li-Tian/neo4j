package neo.network.p2p.payloads;

import org.junit.Assert;
import org.junit.Test;

import neo.Utils;

public class AddrPayloadTest {

    @Test
    public void size() {
        AddrPayload addrPayload = new AddrPayload() {{
            addressList = new NetworkAddressWithTime[0];
        }};

        Assert.assertEquals(1, addrPayload.size());
    }

    @Test
    public void serialize() {
        AddrPayload addrPayload = new AddrPayload() {{
            addressList = new NetworkAddressWithTime[0];
        }};

        AddrPayload tmp = Utils.copyFromSerialize(addrPayload, AddrPayload::new);

        Assert.assertEquals(1, tmp.size());
    }

    @Test
    public void create() {
        AddrPayload addrPayload = AddrPayload.create(new NetworkAddressWithTime[0]);
        Assert.assertEquals(1, addrPayload.size());
    }
}