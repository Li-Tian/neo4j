package neo.network.p2p.payloads;

import org.junit.Assert;
import org.junit.Test;

public class InventoryTypeTest {

    @Test
    public void value() {
        Assert.assertEquals(0x03, InventoryType.Consensus.value());
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse() {
        InventoryType type = InventoryType.parse((byte) 0x01);
        Assert.assertEquals(InventoryType.Tx, type);

        InventoryType.parse((byte) 0x05);
    }
}