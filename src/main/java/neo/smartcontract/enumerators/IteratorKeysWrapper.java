package neo.smartcontract.enumerators;

import neo.log.notr.TR;
import neo.smartcontract.iterators.IIterator;
import neo.vm.StackItem;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: IteratorKeysWrapper
 * @Package neo.smartcontract.enumerators
 * @Description: 迭代器键包装类
 * @date Created in 16:26 2019/3/12
 */
public class IteratorKeysWrapper implements IEnumerator {

    //迭代器
    private IIterator iterator;

    /**
     * @param iterator 迭代器
     * @Author:doubi.liu
     * @description:构造器
     * @date:2019/4/8
     */
    public IteratorKeysWrapper(IIterator iterator) {
        TR.enter();
        this.iterator = iterator;
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
        return TR.exit(iterator.next());
    }

    /**
     * @Author:doubi.liu
     * @description:迭代器当前对象的值
     * @date:2019/4/8
     */
    @Override
    public StackItem value() {
        TR.enter();
        return TR.exit(iterator.key());
    }

    /**
     * @Author:doubi.liu
     * @description:资源释放函数
     * @date:2019/4/8
     */
    @Override
    public void dispose() {
        TR.enter();
        iterator.dispose();
        TR.exit();
    }
}