package neo.network.p2p.payloads;

import org.junit.Assert;
import org.junit.Test;

import neo.Utils;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.csharp.Ushort;
import neo.network.p2p.LocalNode;

import static org.junit.Assert.*;

public class VersionPayloadTest {

    private VersionPayload payload = new VersionPayload() {{
        version = Uint.ZERO;
        services = Ulong.ZERO;
        timestamp = new Uint(128484428);
        port = new Ushort(8080);
        nonce = Uint.ZERO;
        userAgent = "test";
        startHeight = new Uint(10);
        relay = true;
    }};

    @Test
    public void size() {
        // 4 + 8 + 4 + 2 + 4 + usergent + 4 + 1 = 27 + 5 = 32
        Assert.assertEquals(32, payload.size());
    }

    @Test
    public void serialize() {
        VersionPayload copy = Utils.copyFromSerialize(payload, VersionPayload::new);

        Assert.assertEquals(payload.version, copy.version);
        Assert.assertEquals(payload.services, copy.services);
        Assert.assertEquals(payload.timestamp, copy.timestamp);
        Assert.assertEquals(payload.port, copy.port);
        Assert.assertEquals(payload.nonce, copy.nonce);
        Assert.assertEquals(payload.userAgent, copy.userAgent);
        Assert.assertEquals(payload.startHeight, copy.startHeight);
        Assert.assertEquals(payload.relay, copy.relay);
    }

    @Test
    public void create() {
        VersionPayload payload2 = VersionPayload.create(payload.port.intValue(), payload.nonce, payload.userAgent, payload.startHeight);

        Assert.assertEquals(LocalNode.ProtocolVersion, payload2.version);
        Assert.assertEquals(NetworkAddressWithTime.NODE_NETWORK, payload2.services);
//        Assert.assertEquals(payload.timestamp, payload2.timestamp);
        Assert.assertEquals(payload.port, payload2.port);
        Assert.assertEquals(payload.nonce, payload2.nonce);
        Assert.assertEquals(payload.userAgent, payload2.userAgent);
        Assert.assertEquals(payload.startHeight, payload2.startHeight);
        Assert.assertEquals(true, payload2.relay);
    }
}