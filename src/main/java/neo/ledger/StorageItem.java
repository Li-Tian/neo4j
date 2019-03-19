package neo.ledger;

import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.io.ICloneable;
import neo.log.notr.TR;

/**
 * 合约存储项目的数据结构
 */
public class StorageItem extends StateBase implements ICloneable<StorageItem> {

    /**
     * 存储的具体值
     */
    public byte[] value;

    /**
     * 是否是常量
     */
    public boolean isConstant;

    /**
     * 存储大小
     */
    @Override
    public int size() {
        TR.enter();
        return TR.exit(super.size() + BitConverter.getVarSize(value) + Byte.BYTES);
    }

    /**
     * 克隆
     *
     * @return 克隆对象
     */
    @Override
    public StorageItem copy() {
        TR.enter();
        StorageItem item = new StorageItem();
        item.isConstant = isConstant;
        item.value = value;
        return TR.exit(item);
    }

    /**
     * 从副本复制
     *
     * @param replica 副本
     */
    @Override
    public void fromReplica(StorageItem replica) {
        TR.enter();
        this.value = replica.value;
        this.isConstant = replica.isConstant;
        TR.exit();
    }

    /**
     * 序列化
     * <p>序列化字段</p>
     * <ul>
     * <li>StateVersion: 状态版本号</li>
     * <li>Value: 存储的具体值</li>
     * <li>IsConstant: 是否是常量</li>
     * </ul>
     *
     * @param writer 二进制输出流
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        super.serialize(writer);
        writer.writeVarBytes(value);
        writer.writeBoolean(isConstant);
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
        value = reader.readVarBytes();
        isConstant = reader.readBoolean();
        TR.exit();
    }
}
