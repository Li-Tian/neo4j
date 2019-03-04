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
import java.util.HashSet;
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
import neo.exception.InvalidOperationException;
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

    public Transaction(TransactionType type) {
        this.type = type;
    }


    public Fixed8 getFeePerByte() {
        if (feePerByte == Fixed8.negate(Fixed8.SATOSHI)) {
            feePerByte = Fixed8.divide(getNetworkFee(), size());
        }
        return feePerByte;
    }

    @Override
    public UInt256 hash() {
        if (hash == null) {
            hash = new UInt256(Crypto.Default.hash256(this.getHashData()));
        }
        return hash;
    }

    @Override
    public InventoryType inventoryType() {
        return InventoryType.Tr;
    }

    @Override
    public Witness[] getWitnesses() {
        return this.witnesses;
    }

    @Override
    public void setWitnesses(Witness[] witnesses) {
        this.witnesses = witnesses;
    }

    /**
     * 存储大小
     */
    @Override
    public int size() {
        return TransactionType.BYTES + Byte.BYTES + BitConverter.getVarSize(attributes)
                + BitConverter.getVarSize(inputs) + BitConverter.getVarSize(witnesses);
    }

    @Override
    public void serialize(BinaryWriter writer) {
        serializeUnsigned(writer);
        writer.writeArray(witnesses);
    }

    protected void serializeExclusiveData(BinaryWriter writer) {

    }

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

    public static Transaction deserializeFrom(BinaryReader reader) {
        TransactionType type = TransactionType.parse((byte) reader.readByte());
        Transaction transaction = TransactionBuilder.build(type);
        transaction.deserializeUnsignedWithoutType(reader);
        transaction.witnesses = reader.readArray(Witness[]::new, Witness::new);
        transaction.onDeserialized();
        return transaction;
    }


    @Override
    public void deserialize(BinaryReader reader) {
        this.deserializeUnsigned(reader);
        witnesses = reader.readArray(Witness[]::new, Witness::new);
        onDeserialized();
    }

    @Override
    public void deserializeUnsigned(BinaryReader reader) {
        if (TransactionType.parse((byte) reader.readByte()) != type)
            throw new IllegalArgumentException();
        deserializeUnsignedWithoutType(reader);
    }

    private void deserializeUnsignedWithoutType(BinaryReader reader) {
        version = (byte) reader.readByte();
        deserializeExclusiveData(reader);
        attributes = reader.readArray(TransactionAttribute[]::new, TransactionAttribute::new, MaxTransactionAttributes);
        inputs = reader.readArray(CoinReference[]::new, CoinReference::new);
        outputs = reader.readArray(TransactionOutput[]::new, TransactionOutput::new, Ushort.MAX_VALUE + 1);
    }

    protected void deserializeExclusiveData(BinaryReader reader) {
    }

    protected void onDeserialized() {
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null) return false;
        if (!(obj instanceof Transaction)) return false;

        Transaction other = (Transaction) obj;
        return this.hash().equals(other.hash());
    }

    @Override
    public int hashCode() {
        return hash().hashCode();
    }

    @Override
    public byte[] GetMessage() {
        return this.getHashData();
    }

    public boolean isLowPriority() {
        //
        return false;
    }

    public Fixed8 getSystemFee() {
        Fixed8 fee = ProtocolSettings.Default.systemFee.get(type);
        return fee == null ? Fixed8.ZERO : fee;
    }

    public Fixed8 getNetworkFee() {
        if (networkFee == Fixed8.negate(Fixed8.SATOSHI)) {
//            Fixed8 input = References.Values.Where(p = > p.AssetId.Equals(Blockchain.UtilityToken.Hash)).
//            Sum(p = > p.Value);
//            Fixed8 output = Outputs.Where(p = > p.AssetId.Equals(Blockchain.UtilityToken.Hash)).
//            Sum(p = > p.Value);
//            networkFee = input - output - SystemFee;
        }
        return networkFee;
    }

    public HashMap<CoinReference, TransactionOutput> getReferences() {
        if (references == null) {
            references = new HashMap<>();
//            Arrays.stream(inputs).collect(Collectors.groupingBy(p -> p.prevHash)).forEach();
        }
        return references;
    }

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
                result.add(new TransactionResult(entry.getKey(), entry.getValue()));
            }
        }
        return result;
    }


    boolean verify(Snapshot snapshot) {
        return verify(snapshot, Collections.emptyList());
    }

    public boolean verify(Snapshot snapshot, Collection<Transaction> mempool) {


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

    @Override
    public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
        if (getReferences() == null) throw new InvalidOperationException();

        Set<UInt160> set1 = Arrays.stream(inputs).map(p -> references.get(p).scriptHash).collect(Collectors.toSet());
        Set<UInt160> set2 = Arrays.stream(attributes).filter(p -> p.usage == TransactionAttributeUsage.Script).map(p -> new UInt160(p.data)).collect(Collectors.toSet());
        set1.addAll(set2);

//        Arrays.stream(outputs).collect(Collectors.groupingBy(p -> p.assetId)).forEach(p -> sna);

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

    @Override
    public byte[] getHashData() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BinaryWriter writer = new BinaryWriter(outputStream);
        serializeUnsigned(writer);
        writer.flush();
        return outputStream.toByteArray();
    }

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
