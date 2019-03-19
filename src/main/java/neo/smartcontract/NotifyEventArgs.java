package neo.smartcontract;

import neo.UInt160;
import neo.vm.IScriptContainer;
import neo.vm.StackItem;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: NotifyEventArgs
 * @Package neo.smartcontract
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 17:50 2019/3/13
 */
public class NotifyEventArgs {
    public IScriptContainer scriptContainer;
    public UInt160 scriptHash;
    public StackItem state;

    public NotifyEventArgs(IScriptContainer container, UInt160 script_hash, StackItem state)
    {
        this.scriptContainer = container;
        this.scriptHash = script_hash;
        this.state = state;
    }

    public IScriptContainer getScriptContainer() {
        return scriptContainer;
    }

    public UInt160 getScriptHash() {
        return scriptHash;
    }

    public StackItem getState() {
        return state;
    }
}