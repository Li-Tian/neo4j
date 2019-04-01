package neo.smartcontract.iterators;

import org.junit.Assert;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import neo.vm.StackItem;
import neo.vm.Types.Boolean;
import neo.vm.Types.Integer;

import static org.junit.Assert.*;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: MapWrapperTest
 * @Package neo.smartcontract.iterators
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 14:21 2019/3/29
 */
public class MapWrapperTest {
    @Test
    public void next() throws Exception {
        List<Map.Entry<StackItem, StackItem>> map=new ArrayList<>();
        map.add(new AbstractMap.SimpleEntry<StackItem, StackItem>(new Integer(1),new Boolean
                (true)));
        map.add(new AbstractMap.SimpleEntry<StackItem, StackItem>(new Integer(2),new Boolean
                (true)));
        map.add(new AbstractMap.SimpleEntry<StackItem, StackItem>(new Integer(3),new Boolean
                (true)));
        MapWrapper mapWrapper=new MapWrapper(map);
        Assert.assertEquals(true,mapWrapper.next());
    }

    @Test
    public void value() throws Exception {
        List<Map.Entry<StackItem, StackItem>> map=new ArrayList<>();
        map.add(new AbstractMap.SimpleEntry<StackItem, StackItem>(new Integer(1),new Boolean
                (true)));
        map.add(new AbstractMap.SimpleEntry<StackItem, StackItem>(new Integer(2),new Boolean
                (true)));
        map.add(new AbstractMap.SimpleEntry<StackItem, StackItem>(new Integer(3),new Boolean
                (true)));
        MapWrapper mapWrapper=new MapWrapper(map);
        mapWrapper.next();

        Assert.assertEquals(true,mapWrapper.value().getBoolean());
    }

    @Test
    public void key() throws Exception {
        List<Map.Entry<StackItem, StackItem>> map=new ArrayList<>();
        map.add(new AbstractMap.SimpleEntry<StackItem, StackItem>(new Integer(1),new Boolean
                (true)));
        map.add(new AbstractMap.SimpleEntry<StackItem, StackItem>(new Integer(2),new Boolean
                (true)));
        map.add(new AbstractMap.SimpleEntry<StackItem, StackItem>(new Integer(3),new Boolean
                (true)));
        MapWrapper mapWrapper=new MapWrapper(map);
        mapWrapper.next();

        Assert.assertEquals(1,mapWrapper.value().getBigInteger().intValue());
    }

    @Test
    public void dispose() throws Exception {
        List<Map.Entry<StackItem, StackItem>> map=new ArrayList<>();
        map.add(new AbstractMap.SimpleEntry<StackItem, StackItem>(new Integer(1),new Boolean
                (true)));
        map.add(new AbstractMap.SimpleEntry<StackItem, StackItem>(new Integer(2),new Boolean
                (true)));
        map.add(new AbstractMap.SimpleEntry<StackItem, StackItem>(new Integer(3),new Boolean
                (true)));
        MapWrapper mapWrapper=new MapWrapper(map);
        mapWrapper.dispose();
    }

}