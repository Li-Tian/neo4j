package neo.smartcontract;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;

import neo.UInt160;
import neo.UInt256;
import neo.cryptography.ecc.ECC;
import neo.cryptography.ecc.ECPoint;
import neo.csharp.BitConverter;
import neo.exception.FormatException;
import neo.exception.InvalidOperationException;
import neo.log.tr.TR;

public class ContractParameter {
    public ContractParameterType type;
    public Object value = null;

    public ContractParameter() {
        TR.enter();
        TR.exit();
    }

    public ContractParameter(ContractParameterType type) {
        TR.enter();
        this.type = type;
        switch (type) {
            case Signature:
                this.value = new byte[64];
                break;
            case Boolean:
                this.value = false;
                break;
            case Integer:
                this.value = 0;
                break;
            case Hash160:
                this.value = new UInt160();
                break;
            case Hash256:
                this.value = new UInt256();
                break;
            case ByteArray:
                this.value = new byte[0];
                break;
            case PublicKey:
                this.value = new ECPoint(ECC.Secp256r1.getG());
                break;
            case String:
                this.value = "";
                break;
            case Array:
                this.value = new ContractParameterList<ContractParameter>();
                break;
            case Map:
                this.value = new MapList<AbstractMap.SimpleEntry<ContractParameter, ContractParameter>>();
                break;
            default:
                TR.exit();
                throw new IllegalArgumentException();
        }
        TR.exit();
    }

    public static ContractParameter fromJson(JsonObject json) {
        TR.enter();
        ContractParameter parameter = new ContractParameter();
        parameter.type = ContractParameterType.valueOf(json.get("type").getAsString());
        if (json.get("value") != null) {
            switch (parameter.type) {
                case Signature:
                case ByteArray:
                    parameter.value = BitConverter.hexToBytes(json.get("value").getAsString());
                    break;
                case Boolean:
                    parameter.value = json.get("value").getAsBoolean();
                    break;
                case Integer:
                    parameter.value = new BigInteger(json.get("value").getAsString());
                    break;
                case Hash160:
                    parameter.value = UInt160.parse(json.get("value").getAsString());
                    break;
                case Hash256:
                    parameter.value = UInt256.parse(json.get("value").getAsString());
                    break;
                case PublicKey:
                    parameter.value = ECPoint.fromBytes(BitConverter.hexToBytes(json.get("value").getAsString()), ECC.Secp256r1.getCurve());
                    break;
                case String:
                    parameter.value = json.get("value").getAsString();
                    break;
                case Array:
                    //parameter.Value = ((JArray)json["value"]).Select(p => FromJson(p)).ToList();
                    ContractParameterList<ContractParameter> list = new ContractParameterList<ContractParameter>();
                    ((JsonArray) json.get("value")).forEach(p -> list.add(fromJson((JsonObject) p)));
                    parameter.value = list;
                    break;
                case Map:
                    //parameter.Value = ((JArray)json["value"]).Select(p => new KeyValuePair<ContractParameter, ContractParameter>(FromJson(p["key"]), FromJson(p["value"]))).ToList();
                    MapList<AbstractMap.SimpleEntry<ContractParameter, ContractParameter>> entryList = new MapList<AbstractMap.SimpleEntry<ContractParameter, ContractParameter>>();
                    ((JsonArray) json.get("value")).forEach(p -> {
                        AbstractMap.SimpleEntry<ContractParameter, ContractParameter> entry = new AbstractMap.SimpleEntry<ContractParameter, ContractParameter>(fromJson((JsonObject) ((JsonObject) p).get("key")), fromJson((JsonObject) ((JsonObject) p).get("value")));
                        entryList.add(entry);
                    });
                    parameter.value = entryList;
                    break;
                default:
                    TR.exit();
                    throw new IllegalArgumentException();
            }
        }
        return TR.exit(parameter);
    }

    public void setValue(String text) {
        TR.enter();
        switch (type) {
            case Signature:
                byte[] signature = BitConverter.hexToBytes(text);
                if (signature.length != 64) {
                    TR.exit();
                    throw new FormatException();
                }
                value = signature;
                break;
            case Boolean:
                value = text.toLowerCase().equals(Boolean.TRUE.toString().toLowerCase());
                break;
            case Integer:
                value = new BigInteger(text);
                break;
            case Hash160:
                value = UInt160.parse(text);
                break;
            case Hash256:
                value = UInt256.parse(text);
                break;
            case ByteArray:
                value = BitConverter.hexToBytes(text);
                break;
            case PublicKey:
                value = ECPoint.fromBytes(BitConverter.hexToBytes(text), ECC.Secp256r1.getCurve());
                break;
            case String:
                value = text;
                break;
            default:
                TR.exit();
                throw new IllegalArgumentException();
        }
        TR.exit();
    }

    public JsonObject toJson() {
        TR.enter();
        return TR.exit(toJson(this, null));
    }

    private static JsonObject toJson(ContractParameter parameter, HashSet<ContractParameter> context) {
        TR.enter();
        JsonObject json = new JsonObject();
        json.addProperty("type", parameter.type.toString());
        if (parameter.value != null)
            switch (parameter.type) {
                case Signature:
                case ByteArray:
                    json.addProperty("value", BitConverter.toHexString((byte[]) parameter.value));
                    break;
                case Boolean:
                    json.addProperty("value", (boolean) parameter.value);
                    break;
                case Integer:
                case Hash160:
                case Hash256:
                case PublicKey:
                case String:
                    json.addProperty("value", parameter.value.toString());
                    break;
                case Array:
                    if (context == null) {
                        context = new HashSet<ContractParameter>();
                    } else if (context.contains(parameter)) {
                        TR.exit();
                        throw new InvalidOperationException();
                    }
                    context.add(parameter);
                    //json["value"] = new JArray(((IList<ContractParameter>)parameter.Value).Select(p => ToJson(p, context)));
                    JsonArray array = new JsonArray();
                    for (ContractParameter input : (ContractParameterList<ContractParameter>) parameter.value) {
                        array.add(toJson(input, context));
                    }
                    json.add("value", array);
                    break;
                case Map:
                    if (context == null) {
                        context = new HashSet<ContractParameter>();
                    } else if (context.contains(parameter)) {
                        TR.exit();
                        throw new InvalidOperationException();
                    }
                    context.add(parameter);
                    array = new JsonArray();
                    for (AbstractMap.SimpleEntry<ContractParameter, ContractParameter> p : (MapList<AbstractMap.SimpleEntry<ContractParameter, ContractParameter>>) parameter.value) {
                        JsonObject t = new JsonObject();
                        t.add("key", toJson(p.getKey(), context));
                        t.add("value", toJson(p.getValue(), context));
                        array.add(t);
                    }
                    json.add("value", array);
                    break;
            }
        return TR.exit(json);
    }

    @Override
    public String toString() {
        TR.enter();
        return TR.exit(toString(this, null));
    }

    private static String toString(ContractParameter parameter, HashSet<ContractParameter> context) {
        TR.enter();
        if (parameter.value == null) {
            return TR.exit("(null)");
        } else if (parameter.value instanceof byte[]) {
            return TR.exit(BitConverter.toHexString((byte[]) parameter.value));
        } else if (parameter.value instanceof ContractParameterList) {
            ContractParameterList<ContractParameter> data = (ContractParameterList<ContractParameter>) parameter.value;
            if (context == null) {
                context = new HashSet<ContractParameter>();
            }
            if (context.contains(parameter)) {
                return TR.exit("(array)");
            } else {
                context.add(parameter);
                StringBuilder sb = new StringBuilder();
                sb.append('[');
                for (ContractParameter item : data) {
                    sb.append(toString(item, context));
                    sb.append(", ");
                }
                if (data.size() > 0) {
                    sb.delete(sb.length() - 2, sb.length());
                }
                sb.append(']');
                return TR.exit(sb.toString());
            }
        } else if (parameter.value instanceof MapList) {
            MapList<AbstractMap.SimpleEntry> data = new MapList<AbstractMap.SimpleEntry>();
            if (context == null) {
                context = new HashSet<ContractParameter>();
            }
            if (context.contains(parameter)) {
                return TR.exit("(map)");
            } else {
                context.add(parameter);
                StringBuilder sb = new StringBuilder();
                sb.append('[');
                for (AbstractMap.SimpleEntry<ContractParameter, ContractParameter> item : data) {
                    sb.append('{');
                    sb.append(toString(item.getKey(), context));
                    sb.append(',');
                    sb.append(toString(item.getValue(), context));
                    sb.append('}');
                    sb.append(", ");
                }
                if (data.size() > 0) {
                    sb.delete(sb.length() - 2, sb.length());
                }
                sb.append(']');
                return sb.toString();
            }
        } else {
            return parameter.value.toString();
        }
    }

    static public class ContractParameterList<E> extends ArrayList<E> {
    }

    static public class MapList<E> extends ArrayList<E> {
    }
}