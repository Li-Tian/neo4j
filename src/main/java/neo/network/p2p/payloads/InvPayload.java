package neo.network.p2p.payloads;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import neo.UInt256;
import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;


/**
 * 实体类，记录Inventory数据的哈希
 */
public class InvPayload implements ISerializable {

    /**
     * 最大哈希的个数
     */
    public static final int MaxHashesCount = 500;

    /**
     * Inventory数据的类型
     */
    public InventoryType type;

    /**
     * 存储哈希的数组
     */
    public UInt256[] hashes;

    /**
     * 存储大小
     */
    @Override
    public int size() {
        // C# code Size => sizeof(InventoryType) + Hashes.GetVarSize();
        return InventoryType.BYTES + BitConverter.getVarSize(hashes);
    }

    /**
     * 序列化方法
     *
     * @param writer 二进制输出器
     */
    @Override
    public void serialize(BinaryWriter writer) {
        writer.writeByte(type.value());
        writer.writeArray(hashes);
    }

    /**
     * 反序列化方法
     *
     * @param reader 二进制读入器
     */
    @Override
    public void deserialize(BinaryReader reader) {
        type = InventoryType.parse((byte) reader.readByte());
        hashes = reader.readArray(UInt256[]::new, UInt256::new, MaxHashesCount);
    }

    /**
     * 构建一个InvPayload对象
     *
     * @param type   Inventory数据的类型
     * @param hashes 哈希数据
     * @return InvPayload对象
     */
    public static InvPayload create(InventoryType type, UInt256[] hashes) {
        InvPayload payload = new InvPayload();
        payload.type = type;
        payload.hashes = hashes;
        return payload;
    }

    /**
     * 构建一组InvPayload对象。
     *
     * @param type   Inventory数据的类型
     * @param hashes 哈希数据
     * @return InvPayload对象数组, 每次返回最多500个哈希值。
     */
    public static Collection<InvPayload> createGroup(InventoryType type, UInt256[] hashes) {
        ArrayList<InvPayload> payloadArrayList = new ArrayList<>();
        for (int i = 0; i < hashes.length; i += MaxHashesCount) {
            UInt256[] arrs = (UInt256[]) Arrays.stream(hashes).skip(i).limit(MaxHashesCount).toArray();
            InvPayload invPayload = create(type, arrs);
            payloadArrayList.add(invPayload);
        }
        return payloadArrayList;
    }

}
