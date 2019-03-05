package neo.network.p2p.payloads;


import com.google.gson.JsonObject;

import java.math.BigDecimal;

import neo.Fixed8;
import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;
import neo.exception.FormatException;
import neo.exception.InvalidOperationException;
import neo.persistence.Snapshot;

/**
 * 投票状态描述：投票，申请
 */
public class StateDescriptor implements ISerializable {

    /**
     * 类型：投票或者登记成为候选人。
     */
    public StateType type;

    /**
     * <ul>
     * <li>当Field = "Votes"时， 存放投票人地址的脚本hash， Key代表投票人;</li>
     * <li>当Field = "Registered"时， 存放公钥， Key代表申请人</li>
     * </ul>
     */
    public byte[] key;

    /**
     * <ul>
     * <li>当Type = 0x40时， Field = "Votes";</li>
     * <li>当Type = 0x48时， Field = "Registered";</li>
     * </ul>
     */
    public String field;

    /**
     * <ul>
     * <li>当Type = 0x40时， 代表投票地址列表；</li>
     * <li>当Type = 0x48时， 代表取消或申请验证人的布尔值</li>
     * </ul>
     */
    public byte[] value;

    /**
     * 存储大小
     */
    @Override
    public int size() {
        //  C# code sizeof(StateType) + Key.GetVarSize() + Field.GetVarSize() + Value.GetVarSize();
        return StateType.BYTES + BitConverter.getVarSize(key) + BitConverter.getVarSize(field) + BitConverter.getVarSize(value);
    }

    /**
     * 序列化
     *
     * @param writer 二进制输出器
     */
    @Override
    public void serialize(BinaryWriter writer) {
        writer.writeByte(type.value());
        writer.writeVarBytes(key);
        writer.writeVarString(field);
        writer.writeVarBytes(value);
    }


    /**
     * 反序列化
     *
     * @param reader 二进制读入器
     */
    @Override
    public void deserialize(BinaryReader reader) {
        type = StateType.parse((byte) reader.readByte());
        key = reader.readVarBytes(100);
        field = reader.readVarString(32);
        value = reader.readVarBytes(65535);
        switch (type) {
            case Account:
                checkAccountState();
                break;
            case Validator:
                checkValidatorState();
                break;
        }
    }

    /**
     * 交易手续费  若是申请见证人，需要1000个GAS， 否则为0
     */
    public Fixed8 getSystemFee() {
        switch (type) {
            case Validator:
                return getSystemFeeValidator();
            default:
                return Fixed8.ZERO;
        }
    }

    private Fixed8 getSystemFeeValidator() {
        switch (field) {
            case "Registered":
                for (byte b : value) {
                    if (b != 0) {
                        return Fixed8.fromDecimal(new BigDecimal(1000));
                    }
                }
                return Fixed8.ZERO;
            default:
                throw new InvalidOperationException();
        }
    }

    private void checkAccountState() {
        if (key.length != 20) throw new FormatException();
        if (!"Votes".equals(field)) throw new FormatException();
    }

    private void checkValidatorState() {
        if (key.length != 33) throw new FormatException();
        if (!"Registered".equals(field)) throw new FormatException();
    }

    protected boolean verify(Snapshot snapshot) {
        switch (type) {
            case Account:
                return verifyAccountState(snapshot);
            case Validator:
                return verifyValidatorState();
            default:
                return false;
        }
    }

    private boolean verifyAccountState(Snapshot snapshot) {
//        switch (field) {
//            case "Votes":
//                ECPoint[] pubkeys;
//                try {
//                    pubkeys = value.AsSerializableArray < ECPoint > ((int) Blockchain.MaxValidators);
//                } catch (FormatException) {
//                    return false;
//                }
//                UInt160 hash = new UInt160(Key);
//                AccountState account = snapshot.Accounts.TryGet(hash);
//                if (account ?.IsFrozen != false)return false;
//            if (pubkeys.Length > 0) {
//                if (account.GetBalance(Blockchain.GoverningToken.Hash).Equals(Fixed8.Zero))
//                    return false;
//                HashSet<ECPoint> sv = new HashSet<ECPoint>(Blockchain.StandbyValidators);
//                foreach(ECPoint pubkey in pubkeys)
//                if (!sv.Contains(pubkey) && snapshot.Validators.TryGet(pubkey) ?.Registered != true)
//                return false;
//            }
//            return true;
//            default:
//                return false;
//        }
        // TODO waiting db
        return true;
    }

    private boolean verifyValidatorState() {
        switch (field) {
            case "Registered":
                return true;
            default:
                return false;
        }
    }

    /**
     * 转成json对象
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type.value());
        json.addProperty("key", BitConverter.toHexString(key));
        json.addProperty("field", field);
        json.addProperty("value", BitConverter.toHexString(value));
        return json;
    }


}
