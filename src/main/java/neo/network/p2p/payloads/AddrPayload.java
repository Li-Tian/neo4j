package neo.network.p2p.payloads;

import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;

/**
 * 节点收到其它节点发来的 getaddr 消息以后，回复 addr 消息时的传输数据包。 addr 消息里包含本地节点已知的其它节点 IP 地址。
 */
public class AddrPayload implements ISerializable {

    /**
     * 一次最多发送记录数。固定值200。
     */
    public static int MaxCountToSend = 200;

    /**
     * 已知的其它节点地址信息。包括这些节点的IP地址，监听端口，上次活动时间。
     */
    public NetworkAddressWithTime[] addressList;


    /**
     * 存储大小
     */
    @Override
    public int size() {
        return BitConverter.getVarSize(addressList);
    }

    /**
     * 序列化
     *
     * @param writer 二进制输出器
     */
    @Override
    public void serialize(BinaryWriter writer) {
        writer.writeArray(addressList);
    }

    /**
     * 反序列化
     *
     * @param reader 二进制读入器
     */
    @Override
    public void deserialize(BinaryReader reader) {
        addressList = reader.readArray(NetworkAddressWithTime[]::new, NetworkAddressWithTime::new);
    }

    /**
     * 创建 AddrPayload 数据结构
     *
     * @param addresses 已知节点的信息列表
     * @return 传输时使用的数据结构
     */
    public static AddrPayload Create(NetworkAddressWithTime[] addresses) {
        AddrPayload payload = new AddrPayload();
        payload.addressList = addresses;
        return payload;
    }
}
