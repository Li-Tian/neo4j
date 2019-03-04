package neo.ledger;


import neo.Fixed8;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.io.ICloneable;
import neo.cryptography.ECC.ECPoint;
import neo.log.tr.TR;

/**
 * 验证人状态
 */
public class ValidatorState extends StateBase implements ICloneable<ValidatorState> {

    /**
     * 验证人公钥
     */
    public ECPoint publicKey;

    /**
     * 是否注册
     */
    public boolean registered;

    /**
     * 投票数
     */
    public Fixed8 votes;

    public ValidatorState() {
    }

    /**
     * 构造函数
     *
     * @param pubkey 验证人公钥
     */
    public ValidatorState(ECPoint pubkey) {
        this.publicKey = pubkey;
        this.registered = false;
        this.votes = Fixed8.ZERO;
    }


    /**
     * 存储大小
     */
    @Override
    public int size() {
        TR.enter();
        return TR.exit(super.size() + publicKey.size() + Byte.BYTES + votes.size());
    }

    /**
     * 克隆
     */
    @Override
    public ValidatorState copy() {
        TR.enter();
        ValidatorState state = new ValidatorState();
        state.votes = votes;
        state.registered = registered;
        state.publicKey = publicKey;
        return TR.exit(state);
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
        publicKey = ECPoint.deserializeFrom(reader, ECPoint.secp256r1.getCurve());
        registered = reader.readBoolean();
        votes = reader.readSerializable(Fixed8::new);
        TR.exit();
    }


    /**
     * 从副本复制
     *
     * @param replica 副本
     */
    @Override
    public void fromReplica(ValidatorState replica) {
        TR.enter();
        publicKey = replica.publicKey;
        registered = replica.registered;
        votes = replica.votes;
        TR.exit();
    }

    /**
     * 序列化
     * <p>序列化字段</p>
     * <ul>
     * <li>stateVersion: 状态版本号</li>
     * <li>publicKey: 验证人公钥</li>
     * <li>registered: 是否注册</li>
     * <li>votes: 投票数</li>
     * </ul>
     *
     * @param writer 二进制输出流
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        super.serialize(writer);
        writer.writeSerializable(publicKey);
        writer.writeBoolean(registered);
        writer.writeSerializable(votes);
        TR.exit();
    }


}
