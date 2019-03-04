package neo.ledger;

import neo.Fixed8;
import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.io.ICloneable;
import neo.log.tr.TR;

/**
 * 验证人个数投票状态
 */
public class ValidatorsCountState extends StateBase implements ICloneable<ValidatorsCountState> {

    /**
     * 投票数组， 数组下标(index)即验证人投票个数
     */
    public Fixed8[] votes;

    /**
     * 创建验证人个数投票状态
     */
    public ValidatorsCountState() {
        this.votes = new Fixed8[Blockchain.MaxValidators];
        for (int i = 0; i < Blockchain.MaxValidators; i++) {
            this.votes[i] = Fixed8.ZERO;
        }
    }

    /**
     * 存储大小
     */
    @Override
    public int size() {
        TR.enter();
        return TR.exit(super.size() + BitConverter.getVarSize(votes));
    }

    /**
     * 克隆
     */
    @Override
    public ValidatorsCountState copy() {
        TR.enter();
        ValidatorsCountState state = new ValidatorsCountState();
        for (int i = 0; i < votes.length; i++) {
            state.votes[i] = votes[i];
        }
        return TR.exit(state);
    }

    /**
     * 从副本复制
     *
     * @param replica 副本
     */
    @Override
    public void fromReplica(ValidatorsCountState replica) {
        TR.enter();
        votes = replica.votes;
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
        votes = reader.readArray(Fixed8[]::new, Fixed8::new);
        TR.exit();
    }

    /**
     * 序列化
     * <p>序列化字段</p>
     * <ul>
     * <li>stateVersion: 状态版本号</li>
     * <li>votes: 验证人个数投票情况</li>
     * </ul>
     *
     * @param writer 二进制输出流
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        super.serialize(writer);
        writer.writeArray(votes);
        TR.exit();
    }
}
