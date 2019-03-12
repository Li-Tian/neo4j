package neo.ledger;

import org.junit.Assert;
import org.junit.Test;

import neo.Utils;

public class StorageItemTest {

    @Test
    public void size() {
        StorageItem item = new StorageItem();
        item.isConstant = false;
        item.value = new byte[]{0x01, 0x02, 0x03, 0x04};

        Assert.assertEquals(7, item.size());
    }

    @Test
    public void copy() {
        StorageItem item = new StorageItem();
        item.isConstant = false;
        item.value = new byte[]{0x01, 0x02, 0x03, 0x04};

        StorageItem copy = item.copy();
        Assert.assertEquals(false, copy.isConstant);
        Assert.assertEquals((byte) 0x01, copy.value[0]);
        Assert.assertEquals((byte) 0x02, copy.value[1]);
        Assert.assertEquals((byte) 0x03, copy.value[2]);
        Assert.assertEquals((byte) 0x04, copy.value[3]);
    }

    @Test
    public void fromReplica() {
        StorageItem item = new StorageItem();
        item.isConstant = false;
        item.value = new byte[]{0x01, 0x02, 0x03, 0x04};

        StorageItem copy = new StorageItem();
        copy.fromReplica(item);
        Assert.assertEquals(false, copy.isConstant);
        Assert.assertEquals((byte) 0x01, copy.value[0]);
        Assert.assertEquals((byte) 0x02, copy.value[1]);
        Assert.assertEquals((byte) 0x03, copy.value[2]);
        Assert.assertEquals((byte) 0x04, copy.value[3]);
    }

    @Test
    public void serialize() {
        StorageItem item = new StorageItem();
        item.isConstant = false;
        item.value = new byte[]{0x01, 0x02, 0x03, 0x04};

        StorageItem tmp = Utils.copyFromSerialize(item, StorageItem::new);

        Assert.assertEquals(false, tmp.isConstant);
        Assert.assertEquals((byte) 0x01, tmp.value[0]);
        Assert.assertEquals((byte) 0x02, tmp.value[1]);
        Assert.assertEquals((byte) 0x03, tmp.value[2]);
        Assert.assertEquals((byte) 0x04, tmp.value[3]);
    }
}