package neo.network.p2p.payloads;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import neo.Fixed8;
import neo.ProtocolSettings;
import neo.UInt160;
import neo.UInt256;
import neo.cryptography.Crypto;
import neo.csharp.BitConverter;
import neo.csharp.Ushort;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.FormatException;
import neo.exception.InvalidOperationException;
import neo.ledger.Blockchain;
import neo.persistence.Snapshot;

/**
 * 所有交易的父类
 */
public abstract class Transaction implements IInventory {

    /**
     * 交易最大存储字节数。如果收到的交易数超过这个限制将被直接抛弃。
     */
    public static final int MaxTransactionSize = 102400;

    /**
     * Maximum number of attributes that can be contained within a transaction
     */
    private static final int MaxTransactionAttributes = 16;


    /**
     * 交易类型
     */
    public final TransactionType type;

    /**
     * 交易版本号。在各个子类中定义。
     */
    public byte version;

    /**
     * 交易属性
     */
    public TransactionAttribute[] attributes = {};

    /**
     * 交易输入
     */
    public CoinReference[] inputs = {};

    /**
     * 交易输出
     */
    public TransactionOutput[] outputs = {};

    /**
     * 验证脚本的数组
     */
    public Witness[] witnesses = {};

    private Fixed8 feePerByte = Fixed8.negate(Fixed8.SATOSHI);
    private Fixed8 networkFee = Fixed8.negate(Fixed8.SATOSHI);
    private HashMap<CoinReference, TransactionOutput> references;
    private UInt256 hash = null;

    /**
     * 创建交易
     */
    public Transaction(TransactionType type) {
        this.type = type;
    }


    /**
     * 每字节手续费
     */
    public Fixed8 getFeePerByte() {
        if (feePerByte.equals(Fixed8.negate(Fixed8.SATOSHI))) {
            feePerByte = Fixed8.divide(getNetworkFee(), size());
        }
        return feePerByte;
    }

    /**
     * 获取交易的hash值。是将交易信息数据做2次Sha256运算，这个过程被称为Hash256。
     */
    @Override
    public UInt256 hash() {
        if (hash == null) {
            hash = new UInt256(Crypto.Default.hash256(this.getHashData()));
        }
        return hash;
    }

    /**
     * 获取InventoryType。
     */
    @Override
    public InventoryType inventoryType() {
        return InventoryType.Tr;
    }

    /**
     * 获取见证人
     */
    @Override
    public Witness[] getWitnesses() {
        return this.witnesses;
    }

    /**
     * 设置见证人
     *
     * @param witnesses 见证人
     */
    @Override
    public void setWitnesses(Witness[] witnesses) {
        this.witnesses = witnesses;
    }

    /**
     * 存储大小。包括交易类型、版本号、属性、输入、输出和签名的总字节数。
     */
    @Override
    public int size() {
        return TransactionType.BYTES + Byte.BYTES + BitConverter.getVarSize(attributes)
                + BitConverter.getVarSize(inputs) + BitConverter.getVarSize(witnesses);
    }


    /**
     * 序列化
     *
     * @param writer 二进制输出器
     */
    @Override
    public void serialize(BinaryWriter writer) {
        serializeUnsigned(writer);
        writer.writeArray(witnesses);
    }

    /**
     * 序列化扩展数据。因子类的不同而实现不同。
     *
     * @param writer 序列化的输出对象
     */
    protected void serializeExclusiveData(BinaryWriter writer) {

    }

    /**
     * 序列化待签名的数据
     *
     * @param writer 2进制输出器
     */
    @Override
    public void serializeUnsigned(BinaryWriter writer) {
        writer.writeByte(type.value());
        writer.writeByte(version);
        serializeExclusiveData(writer);
        writer.writeArray(attributes);
        writer.writeArray(inputs);
        writer.writeArray(outputs);
    }

    /**
     * 从 byte数组中，解析出对应的交易
     *
     * @param value 待解析的字节数组
     * @return Transaction
     * @throws IllegalArgumentException 当解析的交易类型不存在时，抛出异常
     */
    public static Transaction deserializeFrom(byte[] value) {
        return deserializeFrom(value, 0);
    }

    /**
     * 从 byte数组中，解析出对应的交易
     *
     * @param value  待解析的字节数组
     * @param offset 偏移量，从offset位置开始解析
     * @return Transaction
     * @throws IllegalArgumentException 当解析的交易类型不存在时，抛出异常
     */
    public static Transaction deserializeFrom(byte[] value, int offset) {
        byte[] sub = BitConverter.subBytes(value, offset, value.length - offset - 1);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(sub);
        return deserializeFrom(new BinaryReader(inputStream));
    }

    /**
     * 解析交易
     *
     * @param reader 二进制输入流
     * @return 返回对应类型的交易
     * @throws neo.exception.FormatException 解析失败时，抛出该异常
     */
    public static Transaction deserializeFrom(BinaryReader reader) {
        TransactionType type = TransactionType.parse((byte) reader.readByte());
        Transaction transaction = TransactionBuilder.build(type);
        transaction.deserializeUnsignedWithoutType(reader);
        transaction.witnesses = reader.readArray(Witness[]::new, Witness::new);
        transaction.onDeserialized();
        return transaction;
    }


    /**
     * 反序列化
     *
     * @param reader 二进制读入器
     */
    @Override
    public void deserialize(BinaryReader reader) {
        this.deserializeUnsigned(reader);
        witnesses = reader.readArray(Witness[]::new, Witness::new);
        onDeserialized();
    }

    /**
     * 反序列化待签名数据
     *
     * @param reader 2进制读取器
     * @throws FormatException 解析失败时，抛出该异常
     */
    @Override
    public void deserializeUnsigned(BinaryReader reader) {
        if (TransactionType.parse((byte) reader.readByte()) != type)
            throw new FormatException();
        deserializeUnsignedWithoutType(reader);
    }

    private void deserializeUnsignedWithoutType(BinaryReader reader) {
        version = (byte) reader.readByte();
        deserializeExclusiveData(reader);
        attributes = reader.readArray(TransactionAttribute[]::new, TransactionAttribute::new, MaxTransactionAttributes);
        inputs = reader.readArray(CoinReference[]::new, CoinReference::new);
        outputs = reader.readArray(TransactionOutput[]::new, TransactionOutput::new, Ushort.MAX_VALUE + 1);
    }

    /**
     * 反序列化扩展数据
     *
     * @param reader 二进制输入流
     */
    protected void deserializeExclusiveData(BinaryReader reader) {
    }

    /**
     * 反序列化。因子类的不同而实现不同。
     */
    protected void onDeserialized() {
    }

    /**
     * 判断两笔交易是否相等
     *
     * @param obj 相比较的另一个交易
     * @return <ul>
     * <li>如果参数other是null则返回false</li>
     * <li>如果参数 obj 是null或者不是Transaction则返回false，否则按照哈希值比较。</li>
     * </ul>
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null) return false;
        if (!(obj instanceof Transaction)) return false;

        Transaction other = (Transaction) obj;
        return this.hash().equals(other.hash());
    }

    /**
     * 获取交易哈希的hash code
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return hash().hashCode();
    }

    /**
     * 获取哈希数据
     *
     * @return 哈希数据
     */
    @Override
    public byte[] GetMessage() {
        return this.getHashData();
    }

    /**
     * 是否是低优先级交易。若是claim交易或网络费用低于阈值时，则为低优先级交易。<br/>优先级阈值在配置文件 protocol.json
     * 中指定，如果不指定，则使用默认值(0.001GAS)。
     */
    public boolean isLowPriority() {
        return getNetworkFee().compareTo(ProtocolSettings.Default.lowPriorityThreshold) < 0;
    }

    /**
     * 系统手续费。因交易种类不同而不同。
     */
    public Fixed8 getSystemFee() {
        Fixed8 fee = ProtocolSettings.Default.systemFee.get(type);
        return fee == null ? Fixed8.ZERO : fee;
    }

    /**
     * 网络手续费。值为交易的Input中的GAS总和减去Output中的GAS总和，再减去系统手续费。
     */
    public Fixed8 getNetworkFee() {
        Fixed8 amountIn = Fixed8.ZERO;
        Fixed8 amountOut = Fixed8.ZERO;
        if (networkFee.equals(Fixed8.negate(Fixed8.SATOSHI))) {
            for (TransactionOutput input : getReferences().values()) {
                if (input.assetId.equals(Blockchain.UtilityToken.hash())) {
                    amountIn = Fixed8.add(amountIn, input.value);
                }
            }
            for (TransactionOutput output : outputs) {
                if (output.assetId.equals(Blockchain.UtilityToken.hash())) {
                    amountOut = Fixed8.add(amountOut, output.value);
                }
            }
            networkFee = Fixed8.subtract(Fixed8.subtract(amountIn, amountOut), getSystemFee());
            // C# code
            // Fixed8 input = References.Values.Where(p = > p.AssetId.Equals(Blockchain.UtilityToken.Hash)).
            // Sum(p = > p.Value);
            // Fixed8 output = Outputs.Where(p = > p.AssetId.Equals(Blockchain.UtilityToken.Hash)).
            // Sum(p = > p.Value);
            // networkFee = input - output - SystemFee;
        }
        return networkFee;
    }

    /**
     * 获取当前交易所有输入(input)与其所指向的之前某个交易的一个输出(output)之间的只读关系映射(Dictionary)。<br/>
     * 这个关系映射的每个 key 都是当前交易的
     * input，而 value 则是之前某个交易的 output。<br/>
     * 如果当前交易的某个 input 所指向的 output 在过去的交易中不存在，那么返回 null。
     */
    public HashMap<CoinReference, TransactionOutput> getReferences() {
        if (references == null) {
            references = new HashMap<>();
            // C# code
            //            foreach (var group in Inputs.GroupBy(p => p.PrevHash))
            //            {
            //                Transaction tx = Blockchain.Singleton.Store.GetTransaction(group.Key);
            //                if (tx == null) return null;
            //                foreach (var reference in group.Select(p => new
            //                {
            //                    Input = p,
            //                            Output = tx.Outputs[p.PrevIndex]
            //                }))
            //                {
            //                    dictionary.Add(reference.Input, reference.Output);
            //                }
            //            }
            //            _references = dictionary;
        }
        return references;
    }

    /**
     * 获取交易的 input 与 output 的比较结果。
     *
     * @return <p>
     * 如果当前交易的某个 input 所指向的 output 在过去的交易中不存在，那么返回 null。 <br/>
     * 否则按照资产种类归档，返回每种资产的所有 input
     * 之和减去对应资产的所有 output 之和。<br/>
     * 归档以后，资产比较结果为 0 的资产会从归档列表中除去。<br/>
     * 如果所有的资产比较结果都被除去，则返回一个长度为0的IEnumerable对象。
     * </p>
     */
    public Collection<TransactionResult> getTransactionResults() {
        if (getReferences() == null) return null;
        /*
        // C# code
        return References.Values.Select(p => new
        {
            p.AssetId,
                    p.Value
        }).Concat(Outputs.Select(p => new
        {
            p.AssetId,
                    Value = -p.Value
        })).GroupBy(p => p.AssetId, (k, g) => new TransactionResult
        {
            AssetId = k,
                    Amount = g.Sum(p => p.Value)
        }).Where(p => p.Amount != Fixed8.Zero);
        */
        HashMap<UInt256, Fixed8> map = new HashMap<>();
        for (TransactionOutput input : getReferences().values()) {
            Fixed8 tmp = input.value;
            if (map.containsKey(input.assetId)) {
                tmp = Fixed8.add(tmp, map.get(input.assetId));
            }
            map.put(input.assetId, tmp);
        }
        for (TransactionOutput ouput : outputs) {
            Fixed8 tmp = Fixed8.negate(ouput.value); // -output.value
            if (map.containsKey(ouput.assetId)) {
                tmp = Fixed8.add(map.get(ouput.assetId), tmp);
            }
            map.put(ouput.assetId, tmp);
        }
        ArrayList<TransactionResult> result = new ArrayList<>();
        for (Map.Entry<UInt256, Fixed8> entry : map.entrySet()) {
            if (!entry.getValue().equals(Fixed8.ZERO)) {
                TransactionResult txResult = new TransactionResult();
                txResult.amount = entry.getValue();
                txResult.assetId = entry.getKey();
                result.add(txResult);
            }
        }
        return result;
    }


    /**
     * 通过数据库快照验证交易
     * @param snapshot 数据库快照
     * @return 验证结果
     */
    public boolean verify(Snapshot snapshot) {
        return verify(snapshot, Collections.emptyList());
    }

    /**
     * 校验交易
     * @param snapshot  数据库快照
     * @param mempool 内存池交易
     * @return
     * <ul>
     *     <li>1. 交易数据大小大于最大交易数据大小时，则返回false</li>
     *     <li>2. 若Input存在重复，则返回false</li>
     *     <li>3. 若内存池交易包含Input交易时，返回false</li>
     *     <li>4. 若Input是已经花费的交易，则返回false</li>
     *     <li>5. 若转账资产不存在，则返回false</li>
     *     <li>6. 若资产是非NEO或非GAS时，且资产过期时，返回false</li>
     *     <li>7. 若转账金额不能整除对应资产的最小精度时，返回false</li>
     *     <li>8. 检查金额关系:
     *      <ul>
     *          <li>8.1 若当前交易的某个 input 所指向的 output 在过去的交易中不存在时，返回false</li>
     *          <li>8.2 若 Input.Asset &gt; Output.Asset 时，且资金种类大于一种时，返回false</li>
     *          <li>8.3 若 Input.Asset &gt; Output.Asset 时，资金种类不是GAS时，返回false</li>
     *          <li>8.4 若 交易手续费 大于 （Input.GAS - output.GAS） 时， 返回false</li>
     *          <li>8.5 若 Input.Asset &lt; Output.Asset 时：
     *                <ul>
     *                    <li><8.5.1 若交易类型是 MinerTransaction 或 ClaimTransaction，且资产不是 GAS 时，返回false/li>
     *                    <li>8.5.2 若交易类型时 IssueTransaction时，且资产是GAS时，返回false</li>
     *                    <li>8.5.3 若是其他交易类型，且存在增发资产时，返回false</li>
     *                </ul>
     *          </li>
     *      </ul>
     *     </li>
     *     <li>9. 若交易属性，包含类型是 TransactionAttributeUsage.ECDH02 或 TransactionAttributeUsage.ECDH03 时，返回false </li>
     *     <li>10.若 VerifyReceivingScripts 验证返回false时（VerificationR触发器验证），返回false。(目前，VerifyReceivingScripts 返回永真）</li>
     *     <li>11.若 VerifyWitnesses 验证返回false时（对验证脚本进行验证），则返回false</li>
     * </ul>
     *
     */
    public boolean verify(Snapshot snapshot, Collection<Transaction> mempool) {
        //TODO waiting for db
//        if (Size > MaxTransactionSize) return false;
//        for (int i = 1; i < Inputs.Length; i++)
//            for (int j = 0; j < i; j++)
//                if (Inputs[i].PrevHash == Inputs[j].PrevHash && Inputs[i].PrevIndex == Inputs[j].PrevIndex)
//                    return false;
//        if (mempool.Where(p => p != this).SelectMany(p => p.Inputs).Intersect(Inputs).Count() > 0)
//        return false;
//        if (snapshot.IsDoubleSpend(this))
//            return false;
//        foreach (var group in Outputs.GroupBy(p => p.AssetId))
//        {
//            AssetState asset = snapshot.Assets.TryGet(group.Key);
//            if (asset == null) return false;
//            if (asset.Expiration <= snapshot.Height + 1 && asset.AssetType != AssetType.GoverningToken && asset.AssetType != AssetType.UtilityToken)
//                return false;
//            foreach (TransactionOutput output in group)
//            if (output.Value.GetData() % (long)Math.Pow(10, 8 - asset.Precision) != 0)
//                return false;
//        }
//        TransactionResult[] results = GetTransactionResults()?.ToArray();
//        if (results == null) return false;
//        TransactionResult[] results_destroy = results.Where(p => p.Amount > Fixed8.Zero).ToArray();
//        if (results_destroy.Length > 1) return false;
//        if (results_destroy.Length == 1 && results_destroy[0].AssetId != Blockchain.UtilityToken.Hash)
//            return false;
//        if (SystemFee > Fixed8.Zero && (results_destroy.Length == 0 || results_destroy[0].Amount < SystemFee))
//            return false;
//        TransactionResult[] results_issue = results.Where(p => p.Amount < Fixed8.Zero).ToArray();
//        switch (Type)
//        {
//            // TODO 移植到 java 时仔细检查这段逻辑是否正确
//            case TransactionType.MinerTransaction:
//            case TransactionType.ClaimTransaction:
//                if (results_issue.Any(p => p.AssetId != Blockchain.UtilityToken.Hash))
//                return false;
//            break;
//            case TransactionType.IssueTransaction:
//                if (results_issue.Any(p => p.AssetId == Blockchain.UtilityToken.Hash))
//                return false;
//            break;
//            default:
//                if (results_issue.Length > 0)
//                    return false;
//                break;
//        }
//        if (Attributes.Count(p => p.Usage == TransactionAttributeUsage.ECDH02 || p.Usage == TransactionAttributeUsage.ECDH03) > 1)
//        return false;
//        if (!VerifyReceivingScripts()) return false;
//        return this.VerifyWitnesses(snapshot);

        return true;
    }

    private boolean verifyReceivingScripts() {
        //TODO: run ApplicationEngine
        //foreach (UInt160 hash in Outputs.Select(p => p.ScriptHash).Distinct())
        //{
        //    ContractState contract = Blockchain.Default.GetContract(hash);
        //    if (contract == null) continue;
        //    if (!contract.Payable) return false;
        //    using (StateReader service = new StateReader())
        //    {
        //        ApplicationEngine engine = new ApplicationEngine(TriggerType.VerificationR, this, Blockchain.Default, service, Fixed8.Zero);
        //        engine.LoadScript(contract.Script, false);
        //        using (ScriptBuilder sb = new ScriptBuilder())
        //        {
        //            sb.EmitPush(0);
        //            sb.Emit(OpCode.PACK);
        //            sb.EmitPush("receiving");
        //            engine.LoadScript(sb.ToArray(), false);
        //        }
        //        if (!engine.Execute()) return false;
        //        if (engine.EvaluationStack.Count != 1 || !engine.EvaluationStack.Pop().GetBoolean()) return false;
        //    }
        //}
        return true;
    }

    /**
     * 获取验证脚本hash
     * @param snapshot 数据库快照
     * @return 包含：
     * <ul>
     *     <li>1. 交易输入所指向的收款人地址脚本hash，</li>
     *     <li>2. 交易属性为script时，包含该Data</li>
     *     <li>3. 若资产类型包含AssetType.DutyFlag时，包含收款人地址脚本hash</li>
     * </ul>
     */
    @Override
    public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
        if (getReferences() == null) throw new InvalidOperationException();

        Set<UInt160> set1 = Arrays.stream(inputs).map(p -> references.get(p).scriptHash).collect(Collectors.toSet());
        Set<UInt160> set2 = Arrays.stream(attributes).filter(p -> p.usage == TransactionAttributeUsage.Script).map(p -> new UInt160(p.data)).collect(Collectors.toSet());
        set1.addAll(set2);

        //  Arrays.stream(outputs).collect(Collectors.groupingBy(p -> p.assetId)).forEach(p -> sna);

        UInt160[] results = (UInt160[]) set1.toArray();
        Arrays.sort(results);
        return results;

        //        C# code
        //        hashes.UnionWith(Attributes.Where(p = > p.Usage == TransactionAttributeUsage.Script).Select(p = > new UInt160(p.Data)))
        //        ;
        //        foreach(var group in Outputs.GroupBy(p = > p.AssetId))
        //        {
        //            AssetState asset = snapshot.Assets.TryGet(group.Key);
        //            if (asset == null) throw new InvalidOperationException();
        //            if (asset.AssetType.HasFlag(AssetType.DutyFlag)) {
        //                hashes.UnionWith(group.Select(p = > p.ScriptHash));
        //            }
        //        }
        //        return hashes.OrderBy(p = > p).ToArray();
    }

    /**
     * 获取指定对象序列化后的数据
     *
     * @return 序列化后的原始数据
     */
    @Override
    public byte[] getHashData() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BinaryWriter writer = new BinaryWriter(outputStream);
        serializeUnsigned(writer);
        writer.flush();
        return outputStream.toByteArray();
    }

    /**
     * 转成json对象
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("txid", hash().toString());
        json.addProperty("size", size());
        json.addProperty("type", type.value());
        json.addProperty("version", version);

        JsonArray attrArray = new JsonArray(attributes.length);
        Arrays.stream(attributes).map(p -> p.toJson()).forEach(p -> attrArray.add(p));
        json.add("attributes", attrArray);

        JsonArray vinArray = new JsonArray(inputs.length);
        Arrays.stream(inputs).map(p -> p.toJson()).forEach(p -> vinArray.add(p));
        json.add("vin", vinArray);

        JsonArray voutArray = new JsonArray(outputs.length);
        for (int i = 0; i < outputs.length; i++) {
            voutArray.add(outputs[i].toJson(i));
        }
        json.add("vout", voutArray);

        json.addProperty("sys_fee", getSystemFee().toString());
        json.addProperty("net_fee", getNetworkFee().toString());

        JsonArray scriptsArray = new JsonArray(inputs.length);
        Arrays.stream(inputs).map(p -> p.toJson()).forEach(p -> scriptsArray.add(p));
        json.add("vin", scriptsArray);
        return json;
    }
}
