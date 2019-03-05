package neo.network.p2p.payloads;

import neo.UInt160;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;
import neo.persistence.Snapshot;
import neo.vm.IScriptContainer;

/**
 * 封装的待签名验证接口
 */
public interface IVerifiable extends ISerializable, IScriptContainer {

    /**
     * 见证人列表
     */
    Witness[] getWitnesses();

    /**
     * 获取hash数据
     */
    byte[] getHashData();

    /**
     * 设置见者人
     *
     * @param witnesses 见证人
     */
    void setWitnesses(Witness[] witnesses);

    /**
     * 反序列化待签名数据
     *
     * @param reader 2进制读取器
     */
    void deserializeUnsigned(BinaryReader reader);

    /**
     * 获取等待签名验证的脚本hash集合
     *
     * @param snapshot 数据库快照
     * @return 验证脚本hash集合
     */
    UInt160[] getScriptHashesForVerifying(Snapshot snapshot);

    /**
     * 序列化待签名数据
     *
     * @param writer 2进制输出器
     */
    void serializeUnsigned(BinaryWriter writer);

}
