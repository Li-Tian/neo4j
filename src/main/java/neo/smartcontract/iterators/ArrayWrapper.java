package neo.smartcontract.iterators;

import java.math.BigInteger;
import java.util.List;

import neo.exception.InvalidOperationException;
import neo.vm.StackItem;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: ArrayWrapper
 * @Package neo.smartcontract.iterators
 * @Description: 非线程安全
 * @date Created in 15:57 2019/3/12
 */
public class ArrayWrapper implements IIterator {

    private List<StackItem> array;
    private int index = -1;


    public ArrayWrapper(List<StackItem> array) {
        this.array = array;
    }

    @Override
    public boolean next() {
        int next = index + 1;
        if (next >= array.size()) {
            return false;
        }
        index = next;
        return true;
    }

    @Override
    public StackItem value() {
        if (index < 0)
            throw new InvalidOperationException();
        return array.get(index);
    }

    @Override
    public StackItem key() {
        if (index < 0)
            throw new InvalidOperationException();
        return StackItem.getStackItem(new BigInteger(String.valueOf(index)));
    }

    @Override
    public void dispose() {

    }
}