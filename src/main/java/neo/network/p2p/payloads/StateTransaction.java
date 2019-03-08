package neo.network.p2p.payloads;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import neo.Fixed8;
import neo.UInt160;
import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.InvalidOperationException;
import neo.persistence.Snapshot;
import neo.smartcontract.Contract;
import neo.cryptography.ecc.ECPoint;


/**
 * 投票或申请验证人交易
 */
public class StateTransaction extends Transaction {

    /**
     * 交易描述
     */
    public StateDescriptor[] descriptors;

    /**
     * 构造函数
     */
    public StateTransaction() {
        super(TransactionType.StateTransaction);
    }

    /**
     * 存储大小
     */
    @Override
    public int size() {
        // C# code Size => base.Size + Descriptors.GetVarSize();
        return super.size() + BitConverter.getVarSize(descriptors);
    }


    /**
     * 交易手续费
     */
    @Override
    public Fixed8 getSystemFee() {
        Fixed8 fee = Fixed8.ZERO;
        for (StateDescriptor descriptor : descriptors) {
            fee = Fixed8.add(fee, descriptor.getSystemFee());
        }
        return fee;
    }

    /**
     * 反序列化非data数据
     *
     * @param reader 二进制输入流
     */
    @Override
    protected void deserializeExclusiveData(BinaryReader reader) {
        descriptors = reader.readArray(StateDescriptor[]::new, StateDescriptor::new, 16);
    }

    /**
     * 获取验证脚本hash
     *
     * @param snapshot 数据库快照
     * @return <ul>
     * <li>若 StateDescriptor.Field = "Votes"时, 包含投票人地址地址</li>
     * <li>若 Field="Registered"时，包含申请人的地址脚本hash</li>
     * </ul>
     */
    @Override
    public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
        UInt160[] hashes = super.getScriptHashesForVerifying(snapshot);
        List<UInt160> list = Arrays.asList(hashes);

        /* C# code
        HashSet<UInt160> hashes = new HashSet<UInt160>(base.GetScriptHashesForVerifying(snapshot));
        foreach (StateDescriptor descriptor in Descriptors)
        {
            switch (descriptor.Type)
            {
                case StateType.Account:
                    hashes.UnionWith(GetScriptHashesForVerifying_Account(descriptor));
                    break;
                case StateType.Validator:
                    hashes.UnionWith(GetScriptHashesForVerifying_Validator(descriptor));
                    break;
                default:
                    throw new InvalidOperationException();
            }
        }
        return hashes.OrderBy(p => p).ToArray();
        */

        for (StateDescriptor descriptor : descriptors) {
            switch (descriptor.type) {
                case Account:
                    list.addAll(getScriptHashesForVerifyingAccount(descriptor));
                    break;
                case Validator:
                    list.addAll(getScriptHashesForVerifying_Validator(descriptor));
                    break;
                default:
                    throw new InvalidOperationException();
            }
        }
        return (UInt160[]) list.stream().distinct().sorted().toArray();
    }

    private Collection<UInt160> getScriptHashesForVerifyingAccount(StateDescriptor descriptor) {
        switch (descriptor.field) {
            case "Votes":
                return Collections.singleton(new UInt160(descriptor.key));
            default:
                throw new InvalidOperationException();
        }
    }

    private Collection<UInt160> getScriptHashesForVerifying_Validator(StateDescriptor descriptor) {
        switch (descriptor.field) {
            case "Registered":
                //  Collections.singleton(UInt160.parseToScriptHash(Contract.createSignatureRedeemScript()));
                //  yield return Contract.CreateSignatureRedeemScript(ECPoint.DecodePoint(descriptor.Key, ECCurve.Secp256r1)).ToScriptHash();
                ECPoint publicKey = ECPoint.fromBytes(descriptor.key, ECPoint.secp256r1.getCurve());
                byte[] scripts = Contract.createSignatureRedeemScript(publicKey);
                UInt160 scriptHash = UInt160.parseToScriptHash(scripts);
                return Collections.singleton(scriptHash);
            default:
                throw new InvalidOperationException();
        }
    }

    /**
     * 序列化非data数据
     *
     * @param writer 二进制输出流
     */
    @Override
    protected void serializeExclusiveData(BinaryWriter writer) {
        writer.writeArray(descriptors);
    }

    /**
     * 转成json对象
     *
     * @return json对象
     */
    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();

        JsonArray array = new JsonArray(descriptors.length);
        for (StateDescriptor descriptor : descriptors) {
            array.add(descriptor.toJson());
        }
        json.add("descriptors", array);
        return json;
    }

    /**
     * 校验交易
     *
     * @param snapshot 数据库快照
     * @param mempool  内存池交易
     * @return <ul>
     * <li>1. 对每个StateDescriptor进行验证
     * <ul>
     * <li>1.1 若 descriptor.Type 是 StateType.Validator 时, 若 descriptor.Field
     * 不等于`Registered`时，返回false </li>
     * <li>1.2 若 descriptor.Type 是 StateType.Account 时
     * <ul>
     * <li>1.2.1 若投票账户持有的NEO数量为0，或者投票账户冻结时，返回false</li>
     * <li>1.2.2 若被投账户在备用共识节点列表或尚未申请为验证人时，返回false</li>
     * </ul></li>
     * </ul>
     * </li>
     * <li>2. 进行交易的基本验证，若验证失败，则返回false</li>
     * </ul>
     */
    @Override
    public boolean verify(Snapshot snapshot, Collection<Transaction> mempool) {
        for (StateDescriptor descriptor : descriptors)
            if (!descriptor.verify(snapshot))
                return false;
        return super.verify(snapshot, mempool);
    }
}
