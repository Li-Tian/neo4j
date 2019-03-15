package neo.io.wrappers;

import org.junit.Assert;
import org.junit.Test;

import neo.UInt32;
import neo.Utils;
import neo.csharp.Uint;

import static org.junit.Assert.*;

public class UInt32WrapperTest {

    @Test
    public void parseFrom() {
        Uint value = new Uint(2);
        UInt32Wrapper wrapper = UInt32Wrapper.parseFrom(value);

        Assert.assertEquals(value, wrapper.toUint32());
    }

    @Test
    public void size() {
        Uint value = new Uint(2);
        UInt32Wrapper wrapper = UInt32Wrapper.parseFrom(value);

        Assert.assertEquals(4, wrapper.size());
    }

    @Test
    public void serialize() {
        Uint value = new Uint(2);
        UInt32Wrapper wrapper = UInt32Wrapper.parseFrom(value);

        UInt32Wrapper copy = Utils.copyFromSerialize(wrapper, UInt32Wrapper::new);
        Assert.assertEquals(wrapper.value, copy.value);
    }

    @Test
    public void toUint32() {
        Uint value = new Uint(2);
        UInt32Wrapper wrapper = UInt32Wrapper.parseFrom(value);

        Assert.assertEquals(value, wrapper.toUint32());
    }

    @Test
    public void equals() {
        Uint value = new Uint(2);
        UInt32Wrapper wrapper = UInt32Wrapper.parseFrom(value);

        UInt32Wrapper copy = Utils.copyFromSerialize(wrapper, UInt32Wrapper::new);
        Assert.assertTrue(wrapper.equals(copy));
    }
}