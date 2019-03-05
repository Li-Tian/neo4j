package neo.network.p2p.payloads;

import java.util.Collection;

import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;

/**
 * 区块头的传输数据包，定义了区块头的结构等
 */
public class HeadersPayload implements ISerializable {

    /**
     * 最大区块头数量
     */
    public static final int MaxHeadersCount = 2000;

    /**
     * 区块头数组
     */
    public Header[] headers;

    /**
     * 区块头数组的大小
     */
    @Override
    public int size() {
        return BitConverter.getVarSize(headers);
    }


    /**
     * 序列化
     *
     * @param writer 二进制输出器
     */
    @Override
    public void serialize(BinaryWriter writer) {
        writer.writeArray(headers);
    }

    /**
     * 反序列化
     *
     * @param reader 二进制读入器
     */
    @Override
    public void deserialize(BinaryReader reader) {
        headers = reader.readArray(Header[]::new, Header::new, MaxHeadersCount);
    }

    /**
     * 根据可枚举区块头集合创建一个区块头传输数据包
     *
     * @param headers 可枚举区块头集合
     * @return 创建的区块头传输数据包
     */
    public static HeadersPayload create(Collection<Header> headers) {
        HeadersPayload headersPayload = new HeadersPayload();
        headersPayload.headers = (Header[]) headers.toArray();
        return headersPayload;
    }
}
