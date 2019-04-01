package neo.smartcontract.enumerators;

import org.junit.Assert;
import org.junit.Test;

import neo.vm.StackItem;

import static org.junit.Assert.*;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: ConcatenatedEnumeratorTest
 * @Package neo.smartcontract.enumerators
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 13:54 2019/3/29
 */
public class ConcatenatedEnumeratorTest {
    @Test
    public void next() throws Exception {
        ConcatenatedEnumerator concatenatedEnumerator=new ConcatenatedEnumerator(new IEnumerator() {
            @Override
            public void dispose() {

            }

            @Override
            public boolean next() {
                return false;
            }

            @Override
            public StackItem value() {
                return null;
            }
        }, new IEnumerator() {
            @Override
            public void dispose() {

            }

            @Override
            public boolean next() {
                return true;
            }

            @Override
            public StackItem value() {
                return null;
            }
        });
        Assert.assertEquals(true,concatenatedEnumerator.next());

    }

    @Test
    public void value() throws Exception {
        ConcatenatedEnumerator concatenatedEnumerator=new ConcatenatedEnumerator(new IEnumerator() {
            @Override
            public void dispose() {

            }

            @Override
            public boolean next() {
                return false;
            }

            @Override
            public StackItem value() {
                return null;
            }
        }, new IEnumerator() {
            @Override
            public void dispose() {

            }

            @Override
            public boolean next() {
                return true;
            }

            @Override
            public StackItem value() {
                return null;
            }
        });
        Assert.assertEquals(null,concatenatedEnumerator.value());
    }

    @Test
    public void dispose() throws Exception {
        ConcatenatedEnumerator concatenatedEnumerator=new ConcatenatedEnumerator(new IEnumerator() {
            @Override
            public void dispose() {

            }

            @Override
            public boolean next() {
                return false;
            }

            @Override
            public StackItem value() {
                return null;
            }
        }, new IEnumerator() {
            @Override
            public void dispose() {

            }

            @Override
            public boolean next() {
                return true;
            }

            @Override
            public StackItem value() {
                return null;
            }
        });
        concatenatedEnumerator.dispose();
    }

}