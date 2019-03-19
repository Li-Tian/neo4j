package neo.smartcontract.enumerators;

import neo.csharp.common.IDisposable;
import neo.vm.StackItem;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: IEnumerator
 * @Package neo.smartcontract.enumerators
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 15:55 2019/3/12
 */
public interface IEnumerator extends IDisposable{
    boolean next();

    StackItem value();
}
