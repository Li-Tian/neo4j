package neo.network.p2p.payloads;

import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;

/**
 * 过滤器增加元素的传输数据包
 */
public class FilterAddPayload implements ISerializable {

    /**
     * 需要添加的新元素数据
     */
    public byte[] data;


    /**
     * 负载大小
     */
    @Override
    public int size() {
        return BitConverter.getVarSize(data);
    }

    /**
     * 序列化
     *
     * @param writer 二进制输出器
     */
    @Override
    public void serialize(BinaryWriter writer) {
        writer.writeVarBytes(data);
    }

    /**
     * 反序列化
     *
     * @param reader 二进制读入器
     */
    @Override
    public void deserialize(BinaryReader reader) {
        data = reader.readVarBytes();
    }
}
