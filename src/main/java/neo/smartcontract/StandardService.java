package neo.smartcontract;

import neo.csharp.Uint;
import neo.csharp.common.IDisposable;
import neo.vm.ExecutionEngine;
import neo.vm.IInteropService;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: StandardService
 * @Package neo.smartcontract
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 14:10 2019/3/12
 */
public class StandardService implements IInteropService,IDisposable{
    @Override
    public boolean invoke(byte[] bytes, ExecutionEngine executionEngine) {
        return false;
    }

    @Override
    public void dispose() {

    }

    public long getPrice(Uint hash)
    {
        /*prices.TryGetValue(hash, out long price);*/
        return 0L;
    }
}