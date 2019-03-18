package neo.common;

import org.junit.Assert;
import org.junit.Test;

import java.util.Objects;

import neo.Utils;

import static org.junit.Assert.*;

public class ByteFlagTest {

    @Test
    public void value() {
        ByteFlag flag = new ByteFlag((byte) 0x01);
        Assert.assertEquals(0x01, flag.value());
    }

    @Test
    public void hasFlag() {
        ByteFlag flag = new ByteFlag((byte) 0x11);
        Assert.assertTrue(flag.hasFlag(new ByteFlag((byte) 0x01)));
        Assert.assertFalse(flag.hasFlag(new ByteFlag((byte) 0x02)));
    }

    @Test
    public void equals() {
        ByteFlag flag1 = new ByteFlag((byte) 0x11);
        ByteFlag flag2 = new ByteFlag((byte) 0x12);
        ByteFlag flag3 = new ByteFlag((byte) 0x11);

        Assert.assertFalse(flag1.equals(flag2));
        Assert.assertTrue(flag1.equals(flag3));
    }

    @Test
    public void testHashCode() {
        ByteFlag flag1 = new ByteFlag((byte) 0x11);
        Assert.assertEquals(Objects.hash(0x11), flag1.hashCode());
    }

    @Test
    public void size() {
        Assert.assertEquals(1, new ByteFlag((byte) 0x11).size());
    }

    @Test
    public void serialize() {
        ByteFlag flag = new ByteFlag((byte) 0x11);

        ByteFlag copy = Utils.copyFromSerialize(flag, () -> new ByteFlag((byte) 0x00));
        Assert.assertEquals(flag.value(), copy.value());
    }
}