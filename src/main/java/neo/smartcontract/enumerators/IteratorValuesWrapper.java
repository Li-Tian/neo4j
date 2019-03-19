package neo.smartcontract.enumerators;

import neo.smartcontract.iterators.IIterator;
import neo.vm.StackItem;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: IteratorValuesWrapper
 * @Package neo.smartcontract.enumerators
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 16:28 2019/3/12
 */
public class IteratorValuesWrapper implements IEnumerator{

    private IIterator iterator;

    public IteratorValuesWrapper(IIterator iterator) {
        this.iterator = iterator;
    }

    @Override
    public boolean next() {
        return iterator.next();
    }

    @Override
    public StackItem value() {
        return iterator.value();
    }

    @Override
    public void dispose() {
        iterator.dispose();
    }
}