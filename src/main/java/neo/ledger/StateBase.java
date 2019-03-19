package neo.ledger;

import com.google.gson.JsonObject;

import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;
import neo.log.notr.TR;

/**
 * 状态基类
 */
public abstract class StateBase implements ISerializable {

    /**
     * 状态版本号，固定为0
     */
    public final byte stateVersion = 0;

    /**
     * 存储大小
     */
    @Override
    public int size() {
        TR.enter();
        return TR.exit(Byte.BYTES);
    }

    /**
     * 反序列化
     *
     * @param reader 二进制输入流
     */
    @Override
    public void deserialize(BinaryReader reader) {
        TR.enter();
        if (reader.readByte() != stateVersion) {
            TR.exit();
            throw new NumberFormatException();
        }
        TR.exit();
    }

    /**
     * 序列化
     * <p>序列化字段</p>
     * <ul>
     * <li>StateVersion: 状态版本号</li>
     * </ul>
     *
     * @param writer 二进制输出流
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        writer.writeByte(this.stateVersion);
        TR.exit();
    }

    /**
     * 转成Json对象.添入状态版本号.
     *
     * @return 返回一个包含状态版本号的JObject
     */
    public JsonObject toJson() {
        TR.enter();
        JsonObject json = new JsonObject();
        json.addProperty("version", stateVersion);
        return TR.exit(json);
    }
}