package neo.network.p2p.payloads;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.ledger.AssetState;

import static org.junit.Assert.*;

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

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BinaryWriter writer = new BinaryWriter(byteArrayOutputStream);
        addrPayload.serialize(writer);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        AddrPayload tmp = new AddrPayload();
        tmp.deserialize(new BinaryReader(inputStream));

        Assert.assertEquals(1, tmp.size());
    }

    @Test
    public void create() {
        AddrPayload addrPayload = AddrPayload.create(new NetworkAddressWithTime[0]);
        Assert.assertEquals(1, addrPayload.size());
    }
}