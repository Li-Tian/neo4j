package neo.network.p2p.payloads;

import com.google.gson.JsonObject;

import neo.UInt256;
import neo.csharp.Ushort;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;

/**
 * 交易引用
 */
public class CoinReference implements ISerializable {

    /**
     * 指向的UTXO所在的交易的hash值
     */
    public UInt256 prevHash;

    /**
     * 指向的UTXO所在的交易的output的位置。从0开始。
     */
    public Ushort prevIndex;

    /**
     * 存储大小
     */
    @Override
    public int size() {
        return prevHash.size() + Ushort.BYTES; // ushort 1个字节
    }

    /**
     * 序列化
     *
     * @param writer 二进制输出器
     */
    @Override
    public void serialize(BinaryWriter writer) {
        writer.writeSerializable(prevHash);
        writer.writeUshort(prevIndex);
    }


    /**
     * 反序列化
     *
     * @param reader 二进制读入器
     */
    @Override
    public void deserialize(BinaryReader reader) {
        prevHash = reader.readSerializable(UInt256::new);
        prevIndex = reader.readUshort();
    }

    /**
     * 获取hash code
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return prevHash.hashCode() + prevIndex.hashCode();
    }

    /**
     * 判断交易与该对象是否相等
     *
     * @param obj 待比较对象
     * @return 返回情况如下：
     * <ul>
     * <li>若待比较对象为null 或 不是CoinReference， 则返回false</li>
     * <li>若待比较交易为null, 则返回false。</li>
     * <li>否则按所指向的交易哈希和所指向的交易的output index比较</li>
     * </ul>
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null) return false;
        if (!(obj instanceof CoinReference)) return false;

        CoinReference other = (CoinReference) obj;
        return prevIndex.equals(other.prevIndex) && prevHash.equals(other.prevHash);
    }

    /**
     * 转json对象
     *
     * @return json对象
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("txid", prevHash.toString());
        json.addProperty("vout", prevIndex);
        return json;
    }
}
