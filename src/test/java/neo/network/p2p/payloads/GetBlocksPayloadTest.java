package neo.network.p2p.payloads;

import org.junit.Assert;
import org.junit.Test;

import neo.UInt256;
import neo.Utils;

import static org.junit.Assert.*;

public class GetBlocksPayloadTest {

    @Test
    public void size() {
        GetBlocksPayload payload = new GetBlocksPayload() {{
            hashStart = new UInt256[]{
                    UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01")
            };
            hashStop = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        }};

        Assert.assertEquals(65, payload.size());
    }

    @Test
    public void serialize() {
        GetBlocksPayload payload = new GetBlocksPayload() {{
            hashStart = new UInt256[]{
                    UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01")
            };
            hashStop = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        }};

        GetBlocksPayload payload1 = Utils.copyFromSerialize(payload, GetBlocksPayload::new);

        Assert.assertEquals(payload.hashStart[0], payload1.hashStart[0]);
        Assert.assertEquals(payload.hashStop, payload1.hashStop);
    }

    @Test
    public void create() {
        UInt256 hash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        GetBlocksPayload payload = GetBlocksPayload.create(hash);

        Assert.assertEquals(hash, payload.hashStart[0]);
        Assert.assertEquals(UInt256.Zero, payload.hashStop);
    }
}