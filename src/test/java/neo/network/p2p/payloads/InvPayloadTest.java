package neo.network.p2p.payloads;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import neo.UInt256;
import neo.Utils;

public class InvPayloadTest {

    @Test
    public void size() {
        InvPayload payload = new InvPayload() {{
            type = InventoryType.Tx;
            hashes = new UInt256[]{
                    UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01")
            };
        }};
        Assert.assertEquals(34, payload.size());
    }

    @Test
    public void serialize() {
        InvPayload payload = new InvPayload() {{
            type = InventoryType.Tx;
            hashes = new UInt256[]{
                    UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01")
            };
        }};

        InvPayload copy = Utils.copyFromSerialize(payload, InvPayload::new);

        Assert.assertEquals(payload.type, copy.type);
        Assert.assertEquals(payload.hashes[0], copy.hashes[0]);
    }

    @Test
    public void create() {
        UInt256 hash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        InvPayload payload = InvPayload.create(InventoryType.Tx, new UInt256[]{hash});

        Assert.assertEquals(hash, payload.hashes[0]);
    }

    @Test
    public void createGroup() {
        UInt256 hash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
        UInt256[] hashes = new UInt256[InvPayload.MaxHashesCount * 2 + 10];
        Arrays.fill(hashes, hash);

        Collection<InvPayload> payloads = InvPayload.createGroup(InventoryType.Tx, hashes);
        Assert.assertEquals(3, payloads.size());
        Iterator<InvPayload> iterator = payloads.iterator();
        iterator.next();
        iterator.next();
        InvPayload payload = iterator.next();
        Assert.assertEquals(10, payload.hashes.length);

        ArrayList<UInt256> list = new ArrayList<>();
        for (int i = 0; i < InvPayload.MaxHashesCount * 2 + 10; i++) {
            list.add(hash);
        }
        payloads = InvPayload.createGroup(InventoryType.Tx, list);
        Assert.assertEquals(3, payloads.size());
        iterator = payloads.iterator();
        iterator.next();
        iterator.next();
        payload = iterator.next();
        Assert.assertEquals(10, payload.hashes.length);
    }
}