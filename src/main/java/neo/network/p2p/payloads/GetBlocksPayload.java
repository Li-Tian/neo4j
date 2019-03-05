package neo.network.p2p.payloads;

import neo.UInt256;
import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;

/**
 * 获取区块的传输数据包
 */
public class GetBlocksPayload implements ISerializable {

    /**
     * 开始区块的哈希值列表。固定长度为1
     */
    public UInt256[] hashStart;

    /**
     * 结束区块的哈希值
     */
    public UInt256 hashStop;

    /**
     * 存储大小
     */
    @Override
    public int size() {
        return BitConverter.getVarSize(hashStart) + hashStop.size();
    }

    /**
     * 序列化
     *
     * @param writer 二进制输出器
     */
    @Override
    public void serialize(BinaryWriter writer) {
        writer.writeArray(hashStart);
        writer.writeSerializable(hashStop);
    }

    /**
     * 反序列化
     *
     * @param reader 二进制读入器
     */
    @Override
    public void deserialize(BinaryReader reader) {
        hashStart = reader.readArray(UInt256[]::new, UInt256::new);
        hashStop = reader.readSerializable(UInt256::new);
    }

    /**
     * 创建一个获取区块的数据包
     *
     * @param hashStart 开始区块的哈希值
     * @param hashStop  结束区块的哈希值。不指定时，自动设置为0。将最多获取500个区块
     * @return 创建完成的获取区块的数据包
     */
    public static GetBlocksPayload create(UInt256 hashStart, UInt256 hashStop) {
        GetBlocksPayload payload = new GetBlocksPayload();
        payload.hashStart = new UInt256[]{hashStart};
        payload.hashStop = hashStop == null ? UInt256.Zero : hashStop;
        return payload;
    }

    /**
     * 创建一个获取区块的数据包
     *
     * @param hashStart 开始区块的哈希值
     * @return 创建完成的获取区块的数据包
     */
    public static GetBlocksPayload create(UInt256 hashStart) {
        return create(hashStart, null);
    }
}
