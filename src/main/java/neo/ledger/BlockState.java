package neo.ledger;

import com.google.gson.JsonObject;

import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.io.ICloneable;
import neo.log.tr.TR;

/**
 * 区块状态
 */
public class BlockState extends StateBase implements ICloneable<BlockState> {

    /**
     * 截止当前块（包括当前块）所有系统手续费总和
     */
    public long systemFeeAmount;

    /**
     * 简化版的block
     */
    public TrimmedBlock trimmedBlock;

    /**
     * 存储大小
     */
    @Override
    public int size() {
        TR.enter();
        return TR.exit(super.size() + trimmedBlock.size() + Long.BYTES);
    }


    /**
     * 克隆
     *
     * @return 克隆的对象
     */
    @Override
    public BlockState copy() {
        TR.enter();
        BlockState state = new BlockState();
        state.systemFeeAmount = systemFeeAmount;
        state.trimmedBlock = trimmedBlock;
        return TR.exit(state);
    }

    /**
     * 从副本复制
     *
     * @param replica 副本
     */
    @Override
    public void fromReplica(BlockState replica) {
        TR.enter();
        this.systemFeeAmount = replica.systemFeeAmount;
        this.trimmedBlock = replica.trimmedBlock;
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
        systemFeeAmount = reader.readLong();
        trimmedBlock = reader.readSerializable(TrimmedBlock::new);
        TR.exit();
    }

    /**
     * 序列化
     * <p>序列化字段</p>
     * <ul>
     * <li>StateVersion: 状态版本号</li>
     * <li>systemFeeAmount: 截止当前块（包括当前块）所有系统手续费总和</li>
     * <li>trimmedBlock: 简化版的block</li>
     * </ul>
     *
     * @param writer 二进制输出流
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        super.serialize(writer);
        writer.writeLong(systemFeeAmount);
        writer.writeSerializable(trimmedBlock);
        TR.exit();
    }

    /**
     * 转成json对象
     */
    @Override
    public JsonObject toJson() {
        TR.enter();
        JsonObject json = super.toJson();
        json.addProperty("sysfee_amount", systemFeeAmount);
        json.add("trimmed", trimmedBlock.toJson());
        return TR.exit(json);
    }


}
