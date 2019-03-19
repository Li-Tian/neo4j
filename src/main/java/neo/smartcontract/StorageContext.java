package neo.smartcontract;

import neo.UInt160;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: StorageContext
 * @Package neo.smartcontract
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 17:46 2019/3/13
 */
public class StorageContext {
    public UInt160 scriptHash;
    public boolean isReadOnly;

    public byte[] toArray()
    {
        return scriptHash.toArray();
    }
}