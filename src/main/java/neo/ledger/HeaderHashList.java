package neo.ledger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Arrays;

import neo.UInt256;
import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.io.ICloneable;
import neo.log.notr.TR;

/**
 * 批量存放区块头信息
 */
public class HeaderHashList extends StateBase implements ICloneable<HeaderHashList> {

    /**
     * 区块头hash列表，单次最多2000条
     */
    public UInt256[] hashes;

    /**
     * 存储大小
     */
    @Override
    public int size() {
        TR.enter();
        // C#  Size => base.Size + Hashes.GetVarSize();
        return TR.exit(super.size() + BitConverter.getVarSize(hashes));
    }

    /**
     * 克隆
     *
     * @return 克隆对象
     */
    @Override
    public HeaderHashList copy() {
        TR.enter();
        HeaderHashList list = new HeaderHashList();
        list.hashes = hashes;
        return TR.exit(list);
    }

    /**
     * 从副本复制
     *
     * @param replica 副本对象
     */
    @Override
    public void fromReplica(HeaderHashList replica) {
        TR.enter();
        this.hashes = replica.hashes;
        TR.exit();
    }

    /**
     * 反序列化
     *
     * @param reader 二进制输入流
     */
    @Override
    public void deserialize(BinaryReader reader) {
        TR.enter();
        super.deserialize(reader);
        hashes = reader.readArray(UInt256[]::new, UInt256::new);
        TR.exit();
    }

    /**
     * 序列化
     * <p>序列化字段</p>
     * <ul>
     * <li>StateVersion: 状态版本号</li>
     * <li>Hashes: 区块头hash列表</li>
     * </ul>
     *
     * @param writer 二进制输出流
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        super.serialize(writer);
        writer.writeArray(hashes);
        TR.exit();
    }

    /**
     * 转成json对象
     */
    @Override
    public JsonObject toJson() {
        TR.enter();
        JsonObject json = super.toJson();
        JsonArray array = new JsonArray(hashes.length);
        Arrays.stream(hashes).forEach(p -> array.add(p.toString()));
        json.add("hashes", array);
        return TR.exit(json);
    }

}
