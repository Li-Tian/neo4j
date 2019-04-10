package neo.smartcontract.enumerators;

import neo.log.notr.TR;
import neo.vm.StackItem;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: ConcatenatedEnumerator
 * @Package neo.smartcontract.enumerators
 * @Description: 联合迭代器
 * @date Created in 16:30 2019/3/12
 */
public class ConcatenatedEnumerator implements IEnumerator {

    //第一个迭代器
    private IEnumerator first;
    //第二个迭代器
    private IEnumerator second;
    //当前迭代器
    private IEnumerator current;

    /**
      * @Author:doubi.liu
      * @description:构造函数
      * @param first 第一个迭代器 second 第二个迭代器
      * @date:2019/4/8
    */
    public ConcatenatedEnumerator(IEnumerator first, IEnumerator second) {
        TR.enter();
        this.current = first;
        this.first = first;
        this.second = second;
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
        if (current.next()) {
            return TR.exit(true);
        }
        current = second;
        return TR.exit(current.next());
    }

    /**
      * @Author:doubi.liu
      * @description:迭代器当前对象的值
      * @date:2019/4/8
    */
    @Override
    public StackItem value() {
        TR.enter();
        return TR.exit(current.value());
    }

    /**
      * @Author:doubi.liu
      * @description:资源释放函数
      * @date:2019/4/8
    */
    @Override
    public void dispose() {
        TR.enter();
        first.dispose();
        second.dispose();
        TR.exit();
    }
}