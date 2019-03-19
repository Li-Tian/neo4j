package neo.smartcontract;

import neo.UInt160;
import neo.vm.IScriptContainer;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: LogEventArgs
 * @Package neo.smartcontract
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 17:48 2019/3/13
 */
public class LogEventArgs {
    public IScriptContainer scriptContainer;
    public UInt160 scriptHash;
    public String message;

    public LogEventArgs(IScriptContainer container, UInt160 script_hash, String message)
    {
        this.scriptContainer = container;
        this.scriptHash = script_hash;
        this.message = message;
    }

    public IScriptContainer getScriptContainer() {
        return scriptContainer;
    }

    public UInt160 getScriptHash() {
        return scriptHash;
    }

    public String getMessage() {
        return message;
    }
}