package neo.smartcontract.enumerators;

import org.junit.Assert;
import org.junit.Test;

import neo.smartcontract.iterators.IIterator;
import neo.vm.StackItem;

import static org.junit.Assert.*;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: IteratorKeysWrapperTest
 * @Package neo.smartcontract.enumerators
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 14:16 2019/3/29
 */
public class IteratorKeysWrapperTest {
    @Test
    public void next() throws Exception {
        IIterator iterator=new IIterator() {
            @Override
            public StackItem key() {
                return null;
            }

            @Override
            public boolean next() {
                return false;
            }

            @Override
            public StackItem value() {
                return null;
            }

            @Override
            public void dispose() {

            }
        };
        IteratorKeysWrapper keysWrapper=new IteratorKeysWrapper(iterator);
        Assert.assertEquals(false,keysWrapper.next());
    }

    @Test
    public void value() throws Exception {
        IIterator iterator=new IIterator() {
            @Override
            public StackItem key() {
                return null;
            }

            @Override
            public boolean next() {
                return false;
            }

            @Override
            public StackItem value() {
                return null;
            }

            @Override
            public void dispose() {

            }
        };
        IteratorKeysWrapper keysWrapper=new IteratorKeysWrapper(iterator);
        Assert.assertEquals(null,keysWrapper.value());
    }

    @Test
    public void dispose() throws Exception {
        IIterator iterator=new IIterator() {
            @Override
            public StackItem key() {
                return null;
            }

            @Override
            public boolean next() {
                return false;
            }

            @Override
            public StackItem value() {
                return null;
            }

            @Override
            public void dispose() {

            }
        };
        IteratorKeysWrapper keysWrapper=new IteratorKeysWrapper(iterator);
        keysWrapper.dispose();
    }

}