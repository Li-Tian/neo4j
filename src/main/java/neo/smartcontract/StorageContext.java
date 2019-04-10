package neo.smartcontract;

import neo.UInt160;
import neo.log.notr.TR;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: StorageContext
 * @Package neo.smartcontract
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 17:46 2019/3/13
 */
public class StorageContext {
    //脚本哈希
    public UInt160 scriptHash;
    //只读标志位
    public boolean isReadOnly;

    /**
      * @Author:doubi.liu
      * @description:转数组
      * @date:2019/4/8
    */
    public byte[] toArray()
    {
        TR.enter();
        return TR.exit(scriptHash.toArray());
    }
}