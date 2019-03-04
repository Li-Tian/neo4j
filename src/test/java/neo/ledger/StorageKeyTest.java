package neo.ledger;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import neo.UInt160;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;

import static org.junit.Assert.*;

public class StorageKeyTest {

    @Test
    public void size() {
        StorageKey key = new StorageKey();
        key.scriptHash = UInt160.Zero;
        key.key = new byte[]{0x01, 0x02, 0x03, 0x04};

        Assert.assertEquals(20 + 17, key.size());
    }


    @Test
    public void equals() {
        StorageKey key1 = new StorageKey();
        key1.scriptHash = UInt160.Zero;
        key1.key = new byte[]{0x01, 0x02, 0x03, 0x04};

        StorageKey key2 = new StorageKey();
        key2.scriptHash = UInt160.Zero;
        key2.key = new byte[]{0x01, 0x02, 0x03, 0x03};

        StorageKey key3 = new StorageKey();
        key3.scriptHash = UInt160.Zero;
        key3.key = new byte[]{0x01, 0x02, 0x03, 0x04};

        Assert.assertFalse(key1.equals(key2));
        Assert.assertTrue(key1.equals(key3));
    }

    @Test
    public void hashcode() {
        StorageKey key1 = new StorageKey();
        key1.scriptHash = UInt160.Zero;
        key1.key = new byte[]{0x01, 0x02, 0x03, 0x04};

        Assert.assertEquals(-1516424130, key1.hashCode());
    }

    @Test
    public void serialize() {
        StorageKey key = new StorageKey();
        key.scriptHash = UInt160.Zero;
        key.key = new byte[]{0x01, 0x02, 0x03, 0x04};

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BinaryWriter writer = new BinaryWriter(byteArrayOutputStream);
        key.serialize(writer);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        StorageKey tmp = new StorageKey();
        tmp.deserialize(new BinaryReader(inputStream));

        Assert.assertEquals(key.scriptHash, tmp.scriptHash);
        Assert.assertEquals((byte) 0x01, tmp.key[0]);
        Assert.assertEquals((byte) 0x02, tmp.key[1]);
        Assert.assertEquals((byte) 0x03, tmp.key[2]);
        Assert.assertEquals((byte) 0x04, tmp.key[3]);
    }
}