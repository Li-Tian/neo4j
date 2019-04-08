package neo.smartcontract;

import neo.UInt160;
import neo.log.notr.TR;
import neo.vm.IScriptContainer;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: LogEventArgs
 * @Package neo.smartcontract
 * @Description: 日志事件参数
 * @date Created in 17:48 2019/3/13
 */
public class LogEventArgs {
    //脚本容器
    public IScriptContainer scriptContainer;
    //脚本哈希
    public UInt160 scriptHash;
    //消息
    public String message;

    /**
      * @Author:doubi.liu
      * @description:构造函数
      * @param container 脚本容器  script_hash 脚本哈希 message 消息
      * @date:2019/4/8
    */
    public LogEventArgs(IScriptContainer container, UInt160 script_hash, String message)
    {
        TR.enter();
        this.scriptContainer = container;
        this.scriptHash = script_hash;
        this.message = message;
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
      * @description:获取消息
      * @date:2019/4/8
    */
    public String getMessage() {
        TR.enter();
        return TR.exit(message);
    }
}