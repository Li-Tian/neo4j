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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import neo.Fixed8;
import neo.ProtocolSettings;
import neo.UInt160;
import neo.UInt256;
import neo.cryptography.Crypto;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.Ushort;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.FormatException;
import neo.exception.InvalidOperationException;
import neo.ledger.AssetState;
import neo.ledger.Blockchain;
import neo.persistence.Snapshot;

/**
 * The parent class of all the transactions
 */
public abstract class Transaction implements IInventory {

    /**
     * The maximum stored bytes of transaction. If the received transaction exceeds than this
     * limitation will be abandoned
     */
    public static final int MaxTransactionSize = 102400;

    /**
     * Maximum number of attributes that can be contained within a transaction
     */
    private static final int MaxTransactionAttributes = 16;


    /**
     * The type of transaction
     */
    public final TransactionType type;

    /**
     * The version of transaction, which defined in the subclass
     */
    public byte version;

    /**
     * The attribute of transaction
     */
    public TransactionAttribute[] attributes = {};

    /**
     * The input of transaction
     */
    public CoinReference[] inputs = {};

    /**
     * The output of transaction
     */
    public TransactionOutput[] outputs = {};

    /**
     * The array of witness
     */
    public Witness[] witnesses = {};

    private Fixed8 feePerByte = Fixed8.negate(Fixed8.SATOSHI);
    private Fixed8 networkFee = Fixed8.negate(Fixed8.SATOSHI);
    private HashMap<CoinReference, TransactionOutput> references;
    private UInt256 hash = null;

    /**
     * Create a transaction
     *
     * @param type Transaction type
     */
    public Transaction(TransactionType type) {
        this.type = type;
    }


    /**
     * The <c>NetworkFee</c> for the transaction divided by its <c>Size</c>.<para>Note that this
     * property must be used with care. Getting the value of this property multiple times will
     * return the same result. The value of this property can only be obtained after the transaction
     * has been completely built (no longer modified).</para>
     */
    public Fixed8 getFeePerByte() {
        if (feePerByte.equals(Fixed8.negate(Fixed8.SATOSHI))) {
            feePerByte = Fixed8.divide(getNetworkFee(), size());
        }
        return feePerByte;
    }

    /**
     * Get the hash of transactions. Do twice sha256 operation  to get hash of transaction data.
     * This process is called Hash256
     */
    @Override
    public UInt256 hash() {
        if (hash == null) {
            hash = new UInt256(Crypto.Default.hash256(this.getHashData()));
        }
        return hash;
    }

    /**
     * get InventoryType
     */
    @Override
    public InventoryType inventoryType() {
        return InventoryType.Tr;
    }

    /**
     * get witnesses
     */
    @Override
    public Witness[] getWitnesses() {
        return this.witnesses;
    }

    /**
     * set witness
     */
    @Override
    public void setWitnesses(Witness[] witnesses) {
        this.witnesses = witnesses;
    }

    /**
     * storage size. value=sizeof(TransactionType) + sizeof(byte) + Attributes.GetVarSize() +
     * Inputs.GetVarSize() + Outputs.GetVarSize() + Witnesses.GetVarSize()
     */
    @Override
    public int size() {
        // 6
        return TransactionType.BYTES + Byte.BYTES + BitConverter.getVarSize(attributes)
                + BitConverter.getVarSize(inputs) + BitConverter.getVarSize(outputs)
                + BitConverter.getVarSize(witnesses);
    }


    /**
     * Serialize
     *
     * @param writer BinaryWriter
     */
    @Override
    public void serialize(BinaryWriter writer) {
        serializeUnsigned(writer);
        writer.writeArray(witnesses);
    }

    /**
     * Serialize exclusive data��Depending on the type of transaction.
     *
     * @param writer BinaryWriter
     */
    protected void serializeExclusiveData(BinaryWriter writer) {

    }

    /**
     * Serialize unsigned data
     *
     * @param writer BinaryWriter
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
     * Deserialize from byte array
     *
     * @param value init data
     * @return Transaction object
     */
    public static Transaction deserializeFrom(byte[] value) {
        return deserializeFrom(value, 0);
    }

    /**
     * Deserialize from byte array
     *
     * @param value  init data
     * @param offset offset
     * @return Transaction
     */
    public static Transaction deserializeFrom(byte[] value, int offset) {
        byte[] sub = BitConverter.subBytes(value, offset, value.length - offset - 1);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(sub);
        return deserializeFrom(new BinaryReader(inputStream));
    }

    /**
     * Deserialize from binary reader
     *
     * @param reader BinaryReader
     * @return Transaction object
     * @throws neo.exception.FormatException when parse failed, throw this exception.
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
     * Deserialize
     *
     * @param reader BinaryReader
     */
    @Override
    public void deserialize(BinaryReader reader) {
        this.deserializeUnsigned(reader);
        witnesses = reader.readArray(Witness[]::new, Witness::new);
        onDeserialized();
    }

    /**
     * Deserialize unsigned data
     *
     * @param reader BinaryReader
     * @throws FormatException when the transaction type not exist, throw this exception.
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
     * Deserialize exclusive data。Depending on the type of transaction.
     *
     * @param reader BinaryReader
     */
    protected void deserializeExclusiveData(BinaryReader reader) {
    }

    /**
     * Handling deserialized transactions.Depending on the type of transaction.
     */
    protected void onDeserialized() {
    }

    /**
     * Determine if it equal to another object;
     *
     * @param obj another object to be compared
     * @return If another object is null,return false.Otherwise,compare hash
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
     * Get hash code
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return hash().hashCode();
    }

    /**
     * get hash data
     *
     * @return hash data
     */
    @Override
    public byte[] getMessage() {
        return this.getHashData();
    }

    /**
     * Is it a low priority transaction.If network free is less than low priority threshold,it is a
     * low priority transaction. Low priority threshold is set in protocol.json.If not
     * config，default value is 0.001GAS。
     */
    public boolean isLowPriority() {
        return getNetworkFee().compareTo(ProtocolSettings.Default.lowPriorityThreshold) < 0;
    }

    /**
     * SystemFee. Depending on the type of transaction.
     */
    public Fixed8 getSystemFee() {
        Fixed8 fee = ProtocolSettings.Default.systemFee.get(type);
        return fee == null ? Fixed8.ZERO : fee;
    }

    /**
     * Network Fee. Its value  = the sum of the gas in input of transaction  - the sum of the gas in
     * output of transaction -  system free.
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
     * Get all the TransactionOutputs referenced by inputs.
     *
     * @return the map CoinReference -> TransactionOutput. it returns null, if the
     * TransactionOutputs pointed by inputs are not exist.
     */
    public HashMap<CoinReference, TransactionOutput> getReferences() {
        if (references == null) {
            HashMap<CoinReference, TransactionOutput> map = new HashMap<>();
            for (Map.Entry<UInt256, List<CoinReference>> entry : Arrays.stream(inputs)
                    .collect(Collectors.groupingBy(p -> p.prevHash))
                    .entrySet()) {
                UInt256 key = entry.getKey();
                List<CoinReference> group = entry.getValue();

                Transaction tx = Blockchain.singleton().getStore().getTransaction(key);
                if (tx == null) {
                    return null;
                }
                for (CoinReference input : group) {
                    map.put(input, tx.outputs[input.prevIndex.intValue()]);
                }
            }
            references = map;

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
     * Get the comparison of the input and output of the transaction, which the transactions with
     * inputs' asset not equal with the outputs' asset.
     *
     * @return 1. it returns null, when the TransactionOutput referenced by inputs are not exist.
     * <br/> 2. it returns the result of transactions which the inputs' asset not equal with the
     * outputs' asset.
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
     * Verify transaction by database snapshot
     *
     * @param snapshot database snapshot
     * @return Verification result
     */
    public boolean verify(Snapshot snapshot) {
        return verify(snapshot, Collections.emptyList());
    }

    /**
     * Verify transaction
     *
     * @param snapshot database snapshot
     * @param mempool  transactions in mempool
     * @return <ul>
     * <li>1. If the size of transaction is larger than maximum transaction data size,return
     * false.</li>
     * <li>2. If inputs of transaction duplication exists,return false.</li>
     * <li>3. If a transaction contains the input exists in mempool, return false</li>
     * <li>4. If input is a spent transaction output,return false</li>
     * <li>5. If asset does not exist,return false</li>
     * <li>6. If asset is not NEO/GAS and is expired,return false</li>
     * <li>7. If amount can not be divided by asset precision,return false.</li>
     * <li>8. Check amount relationship:
     * <ul>
     * <li>8.1 If the output pointed to by an input of the current transaction does not exist,
     * return false</li>
     * <li>8.2 If Input.Asset &gt; Output.Asset and the number of asset type is more than
     * one,return false</li>
     * <li>8.3 If Input.Asset &gt; Output.Asset and asset type is not Gas,return false</li>
     * <li>8.4 If system fee is larger than Input.GAS - output.GAS,return false</li>
     * <li>8.5 When Input.Asset &lt; Output.Asset:
     * <ul>
     * <li>8.5.1 If transaction type is MinerTransaction or ClaimTransaction and asset type is not
     * GAS,return false</li>
     * <li>8.5.2 If transaction type is IssueTransaction and asset type is GAS,return false</li>
     * <li>8.5.3 If it is other type transaction and exist additional issuances,return false</li>
     * </ul>
     * </li>
     * </ul>
     * </li>
     * <li>9. If transaction attribute contains TransactionAttributeUsage.ECDH02 or
     * TransactionAttributeUsage.ECDH03,return false</li>
     * <li>10.If VerifyReceivingScripts() return false(VerificationR trigger return false
     * (Currently VerifyReceivingScripts() default return true</li>
     * <li>11.If VerifyWitnesses() return false Verify witness return false.</li>
     * </ul>
     */
    public boolean verify(Snapshot snapshot, Collection<Transaction> mempool) {
        // 1. check size
        if (size() > MaxTransactionSize) {
            return false;
        }
        // 2. check this tx's whether repeat input
        for (int i = 1; i < inputs.length; i++) {
            for (int j = 0; j < i; j++) {
                if (inputs[i].prevHash == inputs[j].prevHash
                        && inputs[i].prevIndex == inputs[j].prevIndex) {
                    return false;
                }
            }
        }

        // 3. check whether repeat input in mempool txs
        HashSet<CoinReference> inputSet = new HashSet<>(Arrays.asList(inputs));
        boolean isRepeatInput = mempool.stream()
                .filter(p -> p != this)
                .map(p -> p.inputs)
                .anyMatch(others -> {
                    for (int i = 0; i < others.length; i++) {
                        if (inputSet.contains(others[i])) {
                            return true;
                        }
                    }
                    return false;
                });
        if (isRepeatInput) {
            return false;
        }

        // 4. double spend check
        if (snapshot.isDoubleSpend(this)) {
            return false;
        }

        // 5. check asset
        for (Map.Entry<UInt256, List<TransactionOutput>> entry : Arrays.stream(outputs)
                .collect(Collectors.groupingBy(p -> p.assetId))
                .entrySet()) {
            UInt256 assetId = entry.getKey();
            List<TransactionOutput> group = entry.getValue();

            AssetState asset = snapshot.getAssets().tryGet(assetId);
            if (asset == null) {
                return false;
            }
            // check asset expiration
            if (asset.expiration.compareTo(snapshot.getHeight().add(new Uint(1))) <= 0
                    && asset.assetType != AssetType.GoverningToken
                    && asset.assetType != AssetType.UtilityToken) {
                return false;
            }
            // check value precision
            for (TransactionOutput output : group) {
                if (output.value.getData() % Math.pow(10, 8 - asset.precision) != 0) {
                    return false;
                }
            }
        }

        Collection<TransactionResult> results = getTransactionResults();
        // must one result is not equal zero, as the gas asset, which inputs' gas > outputs' gas
        if (results == null) {
            return false;
        }

        List<TransactionResult> results_destroy = results.stream()
                .filter(result -> Fixed8.bigger(result.amount, Fixed8.ZERO))
                .collect(Collectors.toList());
        if (results_destroy.size() > 1) { // only gas global asset can be destroyed.
            return false;
        }
        if (results_destroy.size() == 1
                && !results_destroy.get(0).assetId.equals(Blockchain.UtilityToken.hash())) {
            return false; // must be gas asset
        }
        // check system fee, systemfee must less than the amount of destroyed gas
        Fixed8 system_fee = getSystemFee();
        if (system_fee.compareTo(Fixed8.ZERO) > 0
                && (results_destroy.size() == 0
                || results_destroy.get(0).amount.compareTo(system_fee) < 0)) {
            return false;
        }

        // check issue asset:
        List<TransactionResult> results_issue = results.stream()
                .filter(p -> p.amount.compareTo(Fixed8.ZERO) < 0).collect(Collectors.toList());
        switch (type) {
            case MinerTransaction:
            case ClaimTransaction:
                // only gas can be claimed or as a bonus in minerTx
                if (results_issue.stream().anyMatch(p -> !p.assetId.equals(Blockchain.UtilityToken.hash()))) {
                    return false;
                }
                break;
            case IssueTransaction:
                // Gas asset can not be issue in IssueTx
                if (results_issue.stream().anyMatch(p -> p.assetId.equals(Blockchain.UtilityToken.hash()))) {
                    return false;
                }
                break;
            default:
                // otherwise cannot issue global asset
                if (results_issue.size() > 0) {
                    return false;
                }
                break;
        }

        // check attrs, can only have one of ECDH02, ECDH03
        if (Arrays.stream(attributes)
                .filter(attr -> attr.usage == TransactionAttributeUsage.ECDH02
                        || attr.usage == TransactionAttributeUsage.ECDH03)
                .count() > 1) {
            return false;
        }

        // check witness and scripts
        if (!verifyReceivingScripts()) return false;
        return IVerifiable.verifyWitnesses(this, snapshot);

        // C# code:
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
     * get script hash for verifying
     *
     * @param snapshot database snapshot
     * @return contains：
     * <ul>
     * <li>1. The payee address script hash pointed to by the transaction input</li>
     * <li>2. If attribute of transaction is script, it contains data of attribute</li>
     * <li>3. If asset type contains AssetType.DutyFlag, it contains address script hash of
     * payee.</li>
     * </ul>
     */
    @Override
    public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
        if (getReferences() == null) throw new InvalidOperationException();

        Set<UInt160> set1 = Arrays.stream(inputs)
                .map(p -> references.get(p).scriptHash)
                .collect(Collectors.toSet());

        Set<UInt160> set2 = Arrays.stream(attributes)
                .filter(p -> p.usage == TransactionAttributeUsage.Script)
                .map(p -> new UInt160(p.data))
                .collect(Collectors.toSet());

        set1.addAll(set2);

        // C# code:  Arrays.stream(outputs).collect(Collectors.groupingBy(p -> p.assetId)).forEach(p -> sna);
        UInt160[] results = new UInt160[set1.size()];
        set1.toArray(results);
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
     * get the serialized data for the specified object
     *
     * @return serialized data
     */
    public byte[] getHashData() {
        return IVerifiable.getHashData(this);
    }

    /**
     * Convert to JObject object
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
