package neo.smartcontract.iterators;

import neo.smartcontract.enumerators.IEnumerator;
import neo.vm.StackItem;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: IIterator
 * @Package neo.smartcontract.iterators
 * @Description: 迭代器接口
 * @date Created in 15:56 2019/3/12
 */
public interface IIterator extends IEnumerator{
    StackItem key();
}
