package neo.smartcontract.iterators;

import java.math.BigInteger;
import java.util.List;

import neo.exception.InvalidOperationException;
import neo.log.notr.TR;
import neo.vm.StackItem;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: ArrayWrapper
 * @Package neo.smartcontract.iterators
 * @Description: 数组迭代器包装类，非线程安全
 * @date Created in 15:57 2019/3/12
 */
public class ArrayWrapper implements IIterator {

    //数据元素寄存器
    private List<StackItem> array;
    //索引
    private int index = -1;
    
    /**
      * @Author:doubi.liu
      * @description:构造函数
      * @param array 数据集合
      * @date:2019/4/8
    */
    public ArrayWrapper(List<StackItem> array) {
        TR.enter();
        this.array = array;
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
        int next = index + 1;
        if (next >= array.size()) {
            return TR.exit(false);
        }
        index = next;
        return TR.exit(true);
    }

    /**
      * @Author:doubi.liu
      * @description:迭代器当前对象的值
      * @date:2019/4/8
    */
    @Override
    public StackItem value() {
        TR.enter();
        if (index < 0)
            throw TR.exit(new InvalidOperationException());
        return TR.exit(array.get(index));
    }

    /**
      * @Author:doubi.liu
      * @description:迭代器当前对象的键
      * @date:2019/4/8
    */
    @Override
    public StackItem key() {
        TR.enter();
        if (index < 0)
            throw TR.exit(new InvalidOperationException());
        return TR.exit(StackItem.getStackItem(new BigInteger(String.valueOf(index))));
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