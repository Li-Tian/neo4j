package neo.smartcontract.iterators;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import neo.vm.StackItem;
import neo.vm.Types.Boolean;

import static org.junit.Assert.*;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: ArrayWrapperTest
 * @Package neo.smartcontract.iterators
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 14:21 2019/3/29
 */
public class ArrayWrapperTest {
    @Test
    public void next() throws Exception {
        List<StackItem> list=new ArrayList<>();
        list.add(new Boolean(true));
        list.add(new Boolean(true));
        list.add(new Boolean(true));
        ArrayWrapper wrapper=new ArrayWrapper(list);
        Assert.assertEquals(true,wrapper.next());
    }

    @Test
    public void value() throws Exception {
        List<StackItem> list=new ArrayList<>();
        list.add(new Boolean(true));
        list.add(new Boolean(true));
        list.add(new Boolean(true));
        ArrayWrapper wrapper=new ArrayWrapper(list);
        wrapper.next();
        Assert.assertEquals(true,wrapper.value().equals(new Boolean(true)));
    }

    @Test
    public void key() throws Exception {
        List<StackItem> list=new ArrayList<>();
        list.add(new Boolean(true));
        list.add(new Boolean(true));
        list.add(new Boolean(true));
        ArrayWrapper wrapper=new ArrayWrapper(list);
        wrapper.next();
        Assert.assertEquals(0,wrapper.key().getBigInteger().intValue());
    }

    @Test
    public void dispose() throws Exception {
        List<StackItem> list=new ArrayList<>();
        list.add(new Boolean(true));
        list.add(new Boolean(true));
        list.add(new Boolean(true));
        ArrayWrapper wrapper=new ArrayWrapper(list);
        wrapper.dispose();
    }

}