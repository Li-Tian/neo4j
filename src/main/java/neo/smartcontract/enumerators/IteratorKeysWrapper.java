package neo.smartcontract.enumerators;

import neo.smartcontract.iterators.IIterator;
import neo.vm.StackItem;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: IteratorKeysWrapper
 * @Package neo.smartcontract.enumerators
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 16:26 2019/3/12
 */
public class IteratorKeysWrapper implements IEnumerator{

    private IIterator iterator;

    public IteratorKeysWrapper(IIterator iterator) {
        this.iterator = iterator;
    }

    @Override
    public boolean next() {
        return iterator.next();
    }

    @Override
    public StackItem value() {
        return iterator.key();
    }

    @Override
    public void dispose() {
         iterator.dispose();
    }
}