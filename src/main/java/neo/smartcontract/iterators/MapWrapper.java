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
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 16:01 2019/3/12
 */
public class MapWrapper implements IIterator{

    private Iterator<Map.Entry<StackItem,StackItem>> enumerator;
    private Map.Entry<StackItem,StackItem> current;


    public MapWrapper(Iterable<Map.Entry<StackItem, StackItem>> map) {
        this.enumerator = map.iterator();
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
    public StackItem value() {
        if (current==null){
            TR.fixMe("行为未定义");
            throw new RuntimeException("迭代器行为未定义");
        }else {
            return current.getValue();
        }
    }

    @Override
    public StackItem key() {
        if (current==null){
            TR.fixMe("行为未定义");
            throw new RuntimeException("迭代器行为未定义");
        }else {
            return current.getKey();
        }
    }

    @Override
    public void dispose() {
        //enumerator.dispose();
    }
}
