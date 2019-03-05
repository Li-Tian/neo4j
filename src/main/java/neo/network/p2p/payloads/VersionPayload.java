package neo.network.p2p.payloads;

import neo.TimeProvider;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.csharp.Ushort;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;

/**
 * 记录版本数据和区块高度的数据对象
 */
public class VersionPayload implements ISerializable {

    public Uint version;
    public Ulong services;
    public Uint timestamp;
    public Ushort port;
    public Uint nonce;
    public String userAgent;
    public Uint startHeight;
    public boolean relay;

    /**
     * 存储大小
     */
    @Override
    public int size() {
        //C# code Size => sizeof(uint) + sizeof(ulong) + sizeof(uint) + sizeof(ushort) + sizeof(uint) + UserAgent.GetVarSize() + sizeof(uint) + sizeof(bool);
        return Uint.BYTES + Ulong.BYTES + Uint.BYTES + Ushort.BYTES + Uint.BYTES + BitConverter.getVarSize(userAgent) + Uint.BYTES + Byte.BYTES;
    }

    /**
     * 序列化
     *
     * @param writer 二进制输出器
     */
    @Override
    public void serialize(BinaryWriter writer) {
        writer.writeUint(version);
        writer.writeUlong(services);
        writer.writeUint(timestamp);
        writer.writeUshort(port);
        writer.writeUint(nonce);
        writer.writeVarString(userAgent);
        writer.writeUint(startHeight);
        writer.writeBoolean(relay);
    }

    /**
     * 发序列化
     *
     * @param reader 二进制读入器
     */
    @Override
    public void deserialize(BinaryReader reader) {
        version = reader.readUint();
        services = reader.readUlong();
        timestamp = reader.readUint();
        port = reader.readUshort();
        nonce = reader.readUint();
        userAgent = reader.readVarString(1024);
        startHeight = reader.readUint();
        relay = reader.readBoolean();
    }

    /**
     * 构建一个VersionPayload对象
     *
     * @param port        接收端监听的端口
     * @param nonce       本地节点的一个随机数
     * @param userAgent   节点软件的名称和版本的描述信息
     * @param startHeight 区块高度
     * @return 生成的VersionPayload对象
     */
    public static VersionPayload create(int port, Uint nonce, String userAgent, Uint startHeight) {
        VersionPayload payload = new VersionPayload();
        // TODO waiting for LocalNode
//        payload.version = LocalNode.ProtocolVersion;
//        payload.services = NetworkAddressWithTime.NODE_NETWORK;
        payload.timestamp = new Uint(Long.valueOf(TimeProvider.current().utcNow().getTime()).intValue());
        payload.port = new Ushort(port);
        payload.nonce = nonce;
        payload.userAgent = userAgent;
        payload.startHeight = startHeight;
        payload.relay = true;
        return payload;
    }
}
