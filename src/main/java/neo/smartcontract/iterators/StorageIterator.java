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
 * @Description: Storage迭代器包装类
 * @date Created in 17:10 2019/3/12
 */
public class StorageIterator implements IIterator{
    //数据元素寄存器
    private Iterator<Map.Entry<StorageKey, StorageItem>> enumerator;
    //迭代器当前对象
    private Map.Entry<StorageKey, StorageItem> current;


    /**
      * @Author:doubi.liu
      * @description:构造函数
      * @param enumerator 数据集合
      * @date:2019/4/8
    */
    public StorageIterator(Iterator<Map.Entry<StorageKey, StorageItem>> enumerator) {
        TR.enter();
        this.enumerator = enumerator;
        TR.exit();
    }

    /**
      * @Author:doubi.liu
      * @description:判断迭代器是否存在下一个对象
      * @date:2019/4/8
    */
    @Override
    public boolean next() {
        TR.enter();
        if (enumerator.hasNext()){
            current=enumerator.next();
            return TR.exit(true);
        }else{
            return TR.exit(false);
        }
    }

    /**
      * @Author:doubi.liu
      * @description:迭代器当前对象的键
      * @date:2019/4/8
    */
    @Override
    public StackItem key() {
        TR.enter();
        if (current==null){
            TR.fixMe("行为未定义");
            throw TR.exit(new RuntimeException("迭代器行为未定义"));
        }else {
            return TR.exit(StackItem.getStackItem(current.getKey().key));
        }
    }

    /**
      * @Author:doubi.liu
      * @description:迭代器当前对象的值
      * @date:2019/4/8
    */
    @Override
    public StackItem value() {
        TR.enter();
        if (current==null){
            TR.fixMe("行为未定义");
            throw TR.exit(new RuntimeException("迭代器行为未定义"));
        }else {
            return TR.exit(StackItem.getStackItem(current.getValue().value));
        }
    }

    /**
      * @Author:doubi.liu
      * @description:资源释放函数
      * @date:2019/4/8
    */
    @Override
    public void dispose() {
        TR.enter();
        TR.exit();
    }
}