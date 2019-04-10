package neo.smartcontract;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.FormatException;
import neo.exception.InvalidOperationException;
import neo.network.p2p.payloads.Witness;
import neo.UInt160;
import neo.cryptography.ecc.ECC;
import neo.cryptography.ecc.ECPoint;
import neo.csharp.BitConverter;
import neo.ledger.Blockchain;
import neo.log.tr.TR;
import neo.network.p2p.payloads.IVerifiable;
import neo.persistence.Snapshot;
import neo.vm.ScriptBuilder;

public class ContractParametersContext {
    private static class ContextItem {
        public byte[] script;
        public ContractParameter[] parameters;
        public ConcurrentHashMap<ECPoint, byte[]> signatures;

        private ContextItem() {
            TR.enter();
            TR.exit();
        }

        public ContextItem(Contract contract) {
            TR.enter();
            this.script = contract.script;
            //this.Parameters = contract.ParameterList.Select(p => new ContractParameter { Type = p }).ToArray();
            ArrayList<ContractParameter> parameterList = new ArrayList<ContractParameter>();
            for (ContractParameterType type : contract.parameterList) {
                ContractParameter parameter = new ContractParameter();
                parameter.type = type;
                parameterList.add(parameter);
            }
            this.parameters = parameterList.toArray(new ContractParameter[contract.parameterList.length]);
            TR.exit();
        }

        public static ContextItem fromJson(JsonObject json) {
            TR.enter();
            ContextItem item = new ContextItem();
            item.script = json.get("script") == null ? null : BitConverter.hexToBytes(json.get("script").getAsString());
            //Parameters = ((JArray)json["parameters"]).Select(p => ContractParameter.FromJson(p)).ToArray(),
            ArrayList<ContractParameter> parameterList = new ArrayList<ContractParameter>();
            JsonArray array = (JsonArray) json.get("parameters");
            array.forEach(p -> parameterList.add(ContractParameter.fromJson((JsonObject) p)));
            item.parameters = parameterList.toArray(new ContractParameter[array.size()]);
            //Signatures = json["signatures"]?.Properties.Select(p => new
            //{
            //    PublicKey = ECPoint.Parse(p.Key, ECCurve.Secp256r1),
            //            Signature = p.Value.AsString().HexToBytes()
            //}).ToDictionary(p => p.PublicKey, p => p.Signature)
            if (json.get("signatures") == null) {
                item.signatures = null;
            } else {
                JsonObject tmp = new JsonObject();
                tmp.entrySet().forEach(p -> p.getKey());
                item.signatures = new ConcurrentHashMap<ECPoint, byte[]>();
                for (JsonElement object : json.get("signatures").getAsJsonArray()) {
                    ((JsonObject) object).entrySet().forEach(p ->
                            item.signatures.put(ECPoint.fromBytes(BitConverter.hexToBytes(p.getKey().toString()), ECC.Secp256r1.getCurve()),
                                    BitConverter.hexToBytes(p.getValue().getAsString())));
                }
            }
            return TR.exit(item);
        }

        public JsonObject toJson() {
            TR.enter();
            JsonObject json = new JsonObject();
            if (script != null)
                json.addProperty("script", BitConverter.toHexString(script));
            //json["parameters"] = new JArray(Parameters.Select(p => p.ToJson()));
            JsonArray array = new JsonArray();
            for (ContractParameter parameter : parameters) {
                array.add(parameter.toJson());
            }
            json.add("parameters", array);
            if (signatures != null) {
                json.add("signatures", new JsonObject());
                //foreach (var signature in Signatures)
                //    json["signatures"][signature.Key.ToString()] = signature.Value.ToHexString();
                signatures.forEach((p, q) -> json.get("signatures").getAsJsonObject().addProperty(p.toString(), BitConverter.toHexString(q)));
            }
            return TR.exit(json);
        }
    }

    public IVerifiable verifiable;
    private ConcurrentHashMap<UInt160, ContextItem> contextItems;

    public boolean completed() {
        TR.enter();
        if (contextItems.size() < scriptHashes().length) {
            return TR.exit(false);
        }
        //return ContextItems.Values.All(p => p != null && p.Parameters.All(q => q.Value != null));
        for (ContextItem p : contextItems.values()) {
            if (p == null) {
                return TR.exit(false);
            } else {
                for (ContractParameter q : p.parameters) {
                    if (q.value == null) {
                        return TR.exit(false);
                    }
                }
            }
        }
        return TR.exit(true);
    }

    private UInt160[] _scriptHashes = null;

    public UInt160[] scriptHashes() {
        TR.enter();
        if (_scriptHashes == null) {
            Snapshot snapshot = Blockchain.singleton().getSnapshot();
            _scriptHashes = verifiable.getScriptHashesForVerifying(snapshot);
        }
        return TR.exit(_scriptHashes);
    }

    public ContractParametersContext(IVerifiable verifiable) {
        TR.enter();
        this.verifiable = verifiable;
        this.contextItems = new ConcurrentHashMap<UInt160, ContextItem>();
        TR.exit();
    }

    public boolean add(Contract contract, int index, Object parameter) {
        TR.enter();
        ContextItem item = createItem(contract);
        if (item == null) return TR.exit(false);
        item.parameters[index].value = parameter;
        return TR.exit(true);
    }

    public boolean addSignature(Contract contract, ECPoint pubkey, byte[] signature) {
        TR.enter();
        if (Helper.isMultiSigContract(contract.script)) {
            ContextItem item = createItem(contract);
            if (item == null) {
                return TR.exit(false);
            }
            //if (item.Parameters.All(p => p.Value != null)) return false;
            boolean areAllNotNull = true;
            for (ContractParameter parameter : item.parameters) {
                if (parameter.value == null) {
                    areAllNotNull = false;
                    break;
                }
            }
            if (areAllNotNull) {
                return TR.exit(false);
            }
            if (item.signatures == null)
                item.signatures = new ConcurrentHashMap<ECPoint, byte[]>();
            else if (item.signatures.containsKey(pubkey)) {
                return TR.exit(false);
            }
            ArrayList<ECPoint> points = new ArrayList<ECPoint>();
            {
                int i = 0;
                switch (contract.script[i++]) {
                    case 1:
                        ++i;
                        break;
                    case 2:
                        i += 2;
                        break;
                }
                while (contract.script[i++] == 33) {
                    points.add(ECPoint.fromBytes(Arrays.copyOfRange(contract.script, i, 33 + i), ECC.Secp256r1.getCurve()));
                    i += 33;
                }
            }
            if (!points.contains(pubkey)) {
                return TR.exit(false);
            }
            item.signatures.put(pubkey, signature);
            if (item.signatures.size() == contract.parameterList.length) {
                // Dictionary<ECPoint, int> dic = points.Select((p, i) => new
                //                    {
                //                        PublicKey = p,
                //                        Index = i
                //                    }).ToDictionary(p => p.PublicKey, p => p.Index);
                ConcurrentHashMap<ECPoint, Integer> dic = new ConcurrentHashMap<ECPoint, Integer>();
                for (int i = 0; i < points.size(); i++) {
                    dic.put(points.get(i), i);
                }
                //byte[][] sigs = item.Signatures.Select(p => new
                //                    {
                //                        Signature = p.Value,
                //                        Index = dic[p.Key]
                //                    }).OrderByDescending(p => p.Index).Select(p => p.Signature).ToArray();
                byte[][] sigs = new byte[item.signatures.size()][];
                ArrayList<ECPoint> rankedPoints = new ArrayList<ECPoint>();
                item.signatures.forEach((p, q) -> rankedPoints.add(p));
                Collections.sort(rankedPoints, new Comparator<ECPoint>() {
                    public int compare(ECPoint a, ECPoint b) {
                        return dic.get(b) - dic.get(a);
                    }
                });
                for (int i = 0; i < item.signatures.size(); i++) {
                    sigs[i] = item.signatures.get(rankedPoints.get(i));
                }
                for (int i = 0; i < sigs.length; i++) {
                    if (!add(contract, i, sigs[i])) {
                        TR.exit();
                        throw new InvalidOperationException();
                    }
                }
                item.signatures = null;
            }
            return TR.exit(true);
        } else {
            int index = -1;
            for (int i = 0; i < contract.parameterList.length; i++) {
                if (contract.parameterList[i] == ContractParameterType.Signature) {
                    if (index >= 0) {
                        TR.exit();
                        throw new UnsupportedOperationException();
                    } else {
                        index = i;
                    }
                }
            }

            if (index == -1) {
                // unable to find ContractParameterType.Signature in contract.ParameterList
                // return now to prevent array index out of bounds exception
                return TR.exit(false);
            }
            return TR.exit(add(contract, index, signature));
        }
    }

    private ContextItem createItem(Contract contract) {
        TR.enter();
        ContextItem item = contextItems.get(contract.scriptHash());
        if (item != null) {
            return TR.exit(item);
        }
        if (Arrays.binarySearch(scriptHashes(), contract.scriptHash()) < 0) {
            return TR.exit(null);
        }
        item = new ContextItem(contract);
        contextItems.put(contract.scriptHash(), item);
        return TR.exit(item);
    }

    public static ContractParametersContext fromJson(JsonObject json) {
        TR.enter();
        IVerifiable verifiable;
        try {
            verifiable = (IVerifiable) Class.forName(json.get("type").getAsString()).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
        if (verifiable == null) {
            TR.exit();
            throw new FormatException();
        }
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(BitConverter.hexToBytes(json.get("hex").getAsString()));
        BinaryReader reader = new BinaryReader(byteArrayInputStream);
        verifiable.deserializeUnsigned(reader);
        ContractParametersContext context = new ContractParametersContext(verifiable);
        for (Map.Entry property : json.getAsJsonObject("items").entrySet()) {
            context.contextItems.put(UInt160.parse(property.getKey().toString()), ContextItem.fromJson((JsonObject) property.getValue()));
        }
        return TR.exit(context);
    }

    /**
     * 获取指定脚本哈希和索引对应的合约参数
     */
    public ContractParameter getParameter(UInt160 scriptHash, int index) {
        TR.enter();
        ContractParameter[] parameters = getParameters(scriptHash);
        return TR.exit(parameters != null ? parameters[index] : null);
    }

    public ContractParameter[] getParameters(UInt160 scriptHash) {
        TR.enter();
        ContextItem item = contextItems.get(scriptHash);
        if (item == null) {
            return TR.exit(null);
        }
        return TR.exit(item.parameters);
    }

    /**
     * 获取见证人列表
     */
    public Witness[] getWitnesses() {
        TR.enter();
        if (!completed()) {
            TR.exit();
            throw new InvalidOperationException();
        }
        Witness[] witnesses = new Witness[scriptHashes().length];
        for (int i = 0; i < scriptHashes().length; i++) {
            ContextItem item = contextItems.get(scriptHashes()[i]);
            ScriptBuilder sb = new ScriptBuilder();
            List<ContractParameter> reversedArray = Arrays.asList(item.parameters);
            Collections.reverse(reversedArray);
            for (ContractParameter parameter : reversedArray.toArray(new ContractParameter[reversedArray.size()])) {
                neo.vm.Helper.emitPush(sb, parameter);
            }
            witnesses[i] = new Witness();
            witnesses[i].invocationScript = sb.toArray();
            witnesses[i].verificationScript = item.script != null ? item.script : new byte[0];
        }
        return TR.exit(witnesses);
    }

    public static ContractParametersContext parse(String value) {
        TR.enter();
        return TR.exit(fromJson((JsonObject) new JsonParser().parse(value)));
    }

    /**
     * 将本对象转化为相应的Json格式
     */
    public JsonObject toJson() {
        TR.enter();
        JsonObject json = new JsonObject();
        json.addProperty("type", verifiable.getClass().getName());
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BinaryWriter writer = new BinaryWriter(byteArrayOutputStream);
        verifiable.serializeUnsigned(writer);
        writer.flush();
        json.addProperty("hex", BitConverter.toHexString(byteArrayOutputStream.toByteArray()));
        json.add("items", new JsonObject());
        contextItems.forEach((p, q) -> json.get("items").getAsJsonObject().add(p.toString(), q.toJson()));
        return TR.exit(json);
    }

    @Override
    public String toString() {
        TR.enter();
        return TR.exit(toJson().toString());
    }
}