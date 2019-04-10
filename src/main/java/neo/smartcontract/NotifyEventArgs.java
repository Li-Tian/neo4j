package neo.smartcontract;

import neo.UInt160;
import neo.log.notr.TR;
import neo.vm.IScriptContainer;
import neo.vm.StackItem;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: NotifyEventArgs
 * @Package neo.smartcontract
 * @Description: 通知事件参数
 * @date Created in 17:50 2019/3/13
 */
public class NotifyEventArgs {
    //脚本容器
    public IScriptContainer scriptContainer;
    //脚本哈希
    public UInt160 scriptHash;
    //状态
    public StackItem state;

    /**
      * @Author:doubi.liu
      * @description:构造函数
      * @param container 脚本容器 script_hash 脚本哈希 state 状态
      * @date:2019/4/8
    */
    public NotifyEventArgs(IScriptContainer container, UInt160 script_hash, StackItem state)
    {
        TR.enter();
        this.scriptContainer = container;
        this.scriptHash = script_hash;
        this.state = state;
        TR.exit();
    }

    /**
      * @Author:doubi.liu
      * @description:获取脚本容器
      * @date:2019/4/8
    */
    public IScriptContainer getScriptContainer() {
        TR.enter();
        return TR.exit(scriptContainer);
    }

    /**
      * @Author:doubi.liu
      * @description:获取脚本哈希
      * @date:2019/4/8
    */
    public UInt160 getScriptHash() {
        TR.enter();
        return TR.exit(scriptHash);
    }

    /**
      * @Author:doubi.liu
      * @description:获取状态
      * @date:2019/4/8
    */
    public StackItem getState() {
        TR.enter();
        return TR.exit(state);
    }
}