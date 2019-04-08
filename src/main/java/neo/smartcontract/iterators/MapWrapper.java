package neo.smartcontract.iterators;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import neo.log.notr.TR;
import neo.vm.StackItem;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: MapWrapper
 * @Package neo.smartcontract.iterators
 * @Description: Map迭代器包装类
 * @date Created in 16:01 2019/3/12
 */
public class MapWrapper implements IIterator{

    //数据元素寄存器
    private Iterator<Map.Entry<StackItem,StackItem>> enumerator;
    //迭代器当前对象
    private Map.Entry<StackItem,StackItem> current;


    /**
      * @Author:doubi.liu
      * @description:构造函数
      * @param map 数据集合
      * @date:2019/4/8
    */
    public MapWrapper(Iterable<Map.Entry<StackItem, StackItem>> map) {
        TR.enter();
        this.enumerator = map.iterator();
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
            return TR.exit(current.getValue());
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
            return TR.exit(current.getKey());
        }
    }

    /**
      * @Author:doubi.liu
      * @description:资源释放函数
      * @date:2019/4/8
    */
    @Override
    public void dispose() {
        //enumerator.dispose();
        TR.enter();
        TR.exit();
    }
}
