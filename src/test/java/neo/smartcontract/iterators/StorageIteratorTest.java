package neo.smartcontract.iterators;

import org.junit.Assert;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import neo.ledger.StorageItem;
import neo.ledger.StorageKey;
import neo.smartcontract.EventHandler;
import neo.vm.StackItem;
import neo.vm.Types.Boolean;
import neo.vm.Types.Integer;

import static org.junit.Assert.*;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: StorageIteratorTest
 * @Package neo.smartcontract.iterators
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 14:21 2019/3/29
 */
public class StorageIteratorTest {
    @Test
    public void next() throws Exception {
        List<Map.Entry<StorageKey, StorageItem>> map=new ArrayList<>();
        StorageKey storageKey=new StorageKey();
        storageKey.key=new byte[]{0x00};

        StorageItem storageItem=new StorageItem();
        storageItem.value=new byte[]{0x00};
        map.add(new AbstractMap.SimpleEntry<StorageKey, StorageItem>(storageKey,storageItem));
        StorageIterator iterator=new StorageIterator(map.iterator());
        Assert.assertEquals(true,iterator.next());
    }

    @Test
    public void value() throws Exception {
        List<Map.Entry<StorageKey, StorageItem>> map=new ArrayList<>();
        StorageKey storageKey=new StorageKey();
        storageKey.key=new byte[]{0x00};

        StorageItem storageItem=new StorageItem();
        storageItem.value=new byte[]{0x00};
        map.add(new AbstractMap.SimpleEntry<StorageKey, StorageItem>(storageKey,storageItem));
        StorageIterator iterator=new StorageIterator(map.iterator());
        iterator.next();
        Assert.assertEquals(true,iterator.value().equals(new Integer(0)));
    }

    @Test
    public void key() throws Exception {
        List<Map.Entry<StorageKey, StorageItem>> map=new ArrayList<>();
        StorageKey storageKey=new StorageKey();
        storageKey.key=new byte[]{0x00};

        StorageItem storageItem=new StorageItem();
        storageItem.value=new byte[]{0x00};
        map.add(new AbstractMap.SimpleEntry<StorageKey, StorageItem>(storageKey,storageItem));
        StorageIterator iterator=new StorageIterator(map.iterator());
        iterator.next();
        Assert.assertEquals(true,iterator.key().equals(new Integer(0)));
    }

    @Test
    public void dispose() throws Exception {
        List<Map.Entry<StorageKey, StorageItem>> map=new ArrayList<>();
        StorageKey storageKey=new StorageKey();
        storageKey.key=new byte[]{0x00};

        StorageItem storageItem=new StorageItem();
        storageItem.value=new byte[]{0x00};
        map.add(new AbstractMap.SimpleEntry<StorageKey, StorageItem>(storageKey,storageItem));
        StorageIterator iterator=new StorageIterator(map.iterator());
        iterator.next();
        iterator.dispose();
    }

}