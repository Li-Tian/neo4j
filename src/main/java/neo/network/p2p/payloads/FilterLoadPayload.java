package neo.network.p2p.payloads;

import neo.cryptography.BloomFilter;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;
import neo.exception.FormatException;

/**
 * 过滤器加载的传输数据包
 */
public class FilterLoadPayload implements ISerializable {

    /**
     * 过滤器初始化的位阵列数据
     */
    public byte[] filter;

    /**
     * 互相独立的哈希函数的个数
     */
    public byte k;


    /**
     * 微调参数
     */
    public Uint tweak;


    /**
     * 过滤器加载负载大小
     */
    @Override
    public int size() {
        return BitConverter.getVarSize(filter) + Byte.BYTES + Uint.BYTES;
    }

    /**
     * 序列化
     *
     * @param writer 二进制输出器
     */
    @Override
    public void serialize(BinaryWriter writer) {
        writer.writeVarBytes(filter);
        writer.writeByte(k);
        writer.writeUint(tweak);
    }

    /**
     * 反序列化
     *
     * @param reader 二进制读入器
     */
    @Override
    public void deserialize(BinaryReader reader) {
        filter = reader.readVarBytes(36000);
        k = (byte) reader.readByte();
        if (k > 50) throw new FormatException();
        tweak = reader.readUint();
    }


    /**
     * 根据一个布隆过滤器创建对应的过滤器加载传输数据包
     *
     * @param filter 布隆过滤器
     * @return 对应的过滤器加载的传输数据包
     */
    public static FilterLoadPayload create(BloomFilter filter) {
        byte[] buffer = new byte[filter.getM() / 8];
        filter.getBits(buffer);
        FilterLoadPayload payload = new FilterLoadPayload();
        payload.filter = buffer;
        payload.k = (byte) filter.getK();
        payload.tweak = filter.getTweak();
        return payload;
    }

}
