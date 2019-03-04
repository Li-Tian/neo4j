package neo.ledger;

import com.google.gson.JsonObject;

import neo.UInt256;
import neo.csharp.Uint;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.io.ICloneable;

/**
 * 区块索引
 */
public class HashIndexState extends StateBase implements ICloneable<HashIndexState> {

    /**
     * 区块hash
     */
    public UInt256 hash = UInt256.Zero;

    /**
     * 区块高度
     */
    public Uint index = Uint.MAX_VALUE_2;

    /**
     * 存储大小
     */
    @Override
    public int size() {
        return super.size() + hash.size() + Uint.BYTES;
    }


    /**
     * 克隆
     *
     * @return 克隆对象
     */
    @Override
    public HashIndexState copy() {
        HashIndexState state = new HashIndexState();
        state.hash = hash;
        state.index = index;
        return state;
    }

    /**
     * 从副本复制
     *
     * @param replica 副本对象
     */
    @Override
    public void fromReplica(HashIndexState replica) {
        hash = replica.hash;
        index = replica.index;
    }

    /**
     * 反序列化
     *
     * @param reader 二进制输入流
     */
    @Override
    public void deserialize(BinaryReader reader) {
        super.deserialize(reader);
        hash = reader.readSerializable(UInt256::new);
        index = reader.readUint();
    }

    /**
     * 序列化
     * <p>序列化字段</p>
     * <ul>
     * <li>StateVersion: 状态版本号</li>
     * <li>hash: 区块hash</li>
     * <li>Index: 区块高度</li>
     * </ul>
     *
     * @param writer 二进制输出流
     */
    @Override
    public void serialize(BinaryWriter writer) {
        super.serialize(writer);
        writer.writeSerializable(hash);
        writer.writeUint(index);
    }

    /**
     * 转成json对象
     *
     * @return 返回一个包含了Hash和Index的json对象
     */
    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("hash", hash.toString());
        json.addProperty("index", index);
        return json;
    }
}
