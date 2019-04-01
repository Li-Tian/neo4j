package neo.smartcontract.iterators;

import java.util.Iterator;
import java.util.Map;

import neo.ledger.StorageItem;
import neo.ledger.StorageKey;
import neo.log.notr.TR;
import neo.vm.StackItem;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: StorageIterator
 * @Package neo.smartcontract.iterators
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 17:10 2019/3/12
 */
public class StorageIterator implements IIterator{
    private Iterator<Map.Entry<StorageKey, StorageItem>> enumerator;
    private Map.Entry<StorageKey, StorageItem> current;


    public StorageIterator(Iterator<Map.Entry<StorageKey, StorageItem>> enumerator) {
        this.enumerator = enumerator;
    }

    @Override
    public boolean next() {
        if (enumerator.hasNext()){
            current=enumerator.next();
            return true;
        }else{
            return false;
        }
    }

    @Override
    public StackItem key() {
        if (current==null){
            TR.fixMe("行为未定义");
            throw new RuntimeException("迭代器行为未定义");
        }else {
            return StackItem.getStackItem(current.getKey().key);
        }
    }

    @Override
    public StackItem value() {
        if (current==null){
            TR.fixMe("行为未定义");
            throw new RuntimeException("迭代器行为未定义");
        }else {
            return StackItem.getStackItem(current.getValue().value);
        }
    }

    @Override
    public void dispose() {

    }
}