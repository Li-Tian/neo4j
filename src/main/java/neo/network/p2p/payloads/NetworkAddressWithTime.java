package neo.network.p2p.payloads;

import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.csharp.Ushort;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;
import neo.exception.FormatException;

/**
 * 节点地址信息和最近活动时间
 */
public class NetworkAddressWithTime implements ISerializable {

    /**
     * 节点类型常量：普通网络节点。 比特币网络中节点类型很多，与之相比 NEO 网络目前只有一种节点。
     */
    public static Ulong NODE_NETWORK = new Ulong(1);

    /**
     * 最近一次的活动时间。从EPOCH(1970/1/1 00:00:00)开始，单位秒。
     */
    public Uint timestamp;

    /**
     * 节点类型。目前 NEO 只有普通网络节点。
     */
    public Ulong services;

    /**
     * 节点地址信息。包括IP地址和端口
     */
    // TODO java
//    public IPEndPoint EndPoint;

    /**
     * 获取传输时的长度（字节）
     */
    @Override
    public int size() {
        return Uint.BYTES + Ulong.BYTES + 16 + Ushort.BYTES;
    }

    /**
     * 序列化
     *
     * @param writer 二进制输出器
     */
    @Override
    public void serialize(BinaryWriter writer) {
        writer.writeUint(timestamp);
        writer.writeUlong(services);
//        writer.Write(EndPoint.Address.MapToIPv6().GetAddressBytes());
//        writer.write(BitConverter.getBytes((ushort) EndPoint.Port).Reverse().ToArray());
    }


    /**
     * 反序列化
     *
     * @param reader 二进制读入器
     */
    @Override
    public void deserialize(BinaryReader reader) {
        timestamp = reader.readUint();
        services = reader.readUlong();
        byte[] data = reader.readFully(16);
        if (data.length != 16) throw new FormatException();
//        IPAddress address = new IPAddress(data).Unmap();
//        data = reader.ReadBytes(2);
//        if (data.Length != 2) throw new FormatException();
//        ushort port = data.Reverse().ToArray().ToUInt16(0);
//        EndPoint = new IPEndPoint(address, port);
    }

    /**
     * 创建一个地址与活动时间信息对象
     *
     * @param endpoint  地址信息
     * @param services  服务类型
     * @param timestamp 最近活动时间
     * @return 地址与活动时间信息对象
     */
    public static NetworkAddressWithTime create(Ulong services, Uint timestamp) {
        NetworkAddressWithTime time = new NetworkAddressWithTime();
        time.timestamp = timestamp;
        time.services = services;
//        time.endpoint = endpoint;
        return time;
    }
}
