package neo.ledger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.bouncycastle.math.ec.ECPoint;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import neo.UInt160;
import neo.UInt256;
import neo.Fixed8;
import neo.csharp.Uint;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.FormatException;
import neo.io.ICloneable;

import neo.log.tr.TR;
import neo.network.p2p.payloads.AssetType;

public class AssetState extends StateBase implements ICloneable<AssetState> {
    public UInt256 assetId;
    public AssetType assetType;
    public String name;
    public Fixed8 amount;
    public Fixed8 available;
    public byte precision;
    public final byte feeMode = 0;
    public Fixed8 fee;
    public UInt160 feeAddress;
    public ECPoint owner;
    public UInt160 admin;
    public UInt160 issuer;
    public Uint expiration;
    public boolean isFrozen;

    @Override
    public int size() {
        TR.enter();
        return super.size();
        // TODO waiting ECPoint
//        return TR.exit(super.size() + assetId.size() + Byte.BYTES + name.length() +
//                amount.size() + available.size() + Byte.BYTES + Byte.BYTES + fee.size() +
//                feeAddress.size() + owner.size() + admin.size() + issuer.size() + Uint.BYTES + Byte.BYTES);
    }

    @Override
    public AssetState copy() {
        TR.enter();
        AssetState result = new AssetState();
        result.assetId = assetId;
        result.assetType = assetType;
        result.name = name;
        result.amount = amount;
        result.available = available;
        result.precision = precision;
        //result.feeMode = feeMode;
        result.fee = fee;
        result.feeAddress = feeAddress;
        result.owner = owner;
        result.admin = admin;
        result.issuer = issuer;
        result.expiration = expiration;
        result.isFrozen = isFrozen;
        result.nameMap = nameMap;
        return TR.exit(result);
    }

    @Override
    public void deserialize(BinaryReader reader) {
        TR.enter();
        super.deserialize(reader);
        assetId = reader.readSerializable(UInt256::new);
        assetType = AssetType.parse((byte) reader.readByte());
        name = reader.readVarString();
        amount = reader.readSerializable(Fixed8::new);
        available = reader.readSerializable(Fixed8::new);
        precision = (byte) reader.readByte();
        reader.readByte(); //FeeMode
        fee = reader.readSerializable(Fixed8::new); //Fee
        feeAddress = reader.readSerializable(UInt160::new);
        //TODO
        //owner = ECPoint.deserializeFrom(reader, ECCurve.secp256r1);
        admin = reader.readSerializable(UInt160::new);
        issuer = reader.readSerializable(UInt160::new);
        expiration = reader.readUint();
        isFrozen = reader.readBoolean();
        TR.exit();
    }

    @Override
    public void fromReplica(AssetState replica) {
        TR.enter();
        assetId = replica.assetId;
        assetType = replica.assetType;
        name = replica.name;
        amount = replica.amount;
        available = replica.available;
        precision = replica.precision;
        //FeeMode = replica.FeeMode;
        fee = replica.fee;
        feeAddress = replica.feeAddress;
        owner = replica.owner;
        admin = replica.admin;
        issuer = replica.issuer;
        expiration = replica.expiration;
        isFrozen = replica.isFrozen;
        nameMap = replica.nameMap;
        TR.exit();
    }

    private ConcurrentHashMap<Locale, String> nameMap;

    public String getName() {
        TR.enter();
        return TR.exit(getName(null));
    }

    public String getName(Locale culture) {
        // TODO 有待验证
        TR.enter();
        if (assetType == AssetType.GoverningToken) return "NEO";
        if (assetType == AssetType.UtilityToken) return "NeoGas";
        if (nameMap == null) {
            JsonElement nameJsonElement;
            try {
                nameJsonElement = new JsonParser().parse(name);
                if (nameJsonElement.isJsonArray()) {
                    JsonArray array = nameJsonElement.getAsJsonArray();
                    for (int i = 0; i < array.size(); i++) {
                        JsonObject jsonObject = array.get(i).getAsJsonObject();
                        if (jsonObject.has("lang") && jsonObject.has("name")) {
                            String lang = jsonObject.get("lang").getAsString();
                            String realName = jsonObject.get("name").getAsString();
                            Locale langType = Locale.forLanguageTag(lang);
                            nameMap.put(langType, realName);
                        }
                    }
                } else if (nameJsonElement.isJsonObject()) {
                    nameMap.put(Locale.ENGLISH, nameJsonElement.getAsString());
                }
            } catch (Exception e) {
                nameMap.put(Locale.ENGLISH, name);
            }
        }

        if (culture == null) culture = Locale.getDefault();
        String name = nameMap.get(culture);
        if (name != null) {
            return TR.exit(name);
        }
        name = nameMap.get(en);
        if (name != null) {
            return TR.exit(name);
        }
        return TR.exit(nameMap.elements().nextElement());
    }

    private static final Locale en = Locale.ENGLISH;

    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        super.serialize(writer);
        writer.writeSerializable(assetId);
        writer.writeByte(assetType.value());
        writer.writeVarString(name);
        writer.writeSerializable(amount);
        writer.writeSerializable(available);
        writer.writeByte(precision);
        writer.writeByte(feeMode);
        writer.writeSerializable(fee);
        writer.writeSerializable(feeAddress);
//        writer.writeSerializable(owner); // TODO waiting ECPoint
        writer.writeSerializable(admin);
        writer.writeSerializable(issuer);
        writer.writeUint(expiration);
        writer.writeBoolean(isFrozen);
        TR.exit();
    }

    @Override
    public JsonObject toJson() {
        TR.enter();
        JsonObject json = super.toJson();
        json.addProperty("id", assetId.toString());
        json.addProperty("type", assetType.value());
        try {
            // TODO waiting for Gson parse
//            json["name"] = name == "" ? null : JObject.Parse(name);
        } catch (FormatException e) {
            json.addProperty("name", name);
        }
        json.addProperty("amount", amount.toString());
        json.addProperty("available", available.toString());
        json.addProperty("precision", precision);
        // TODO waiting ECPoint
//        json.addProperty("owner", owner); owner.ToString();
//        json.addProperty("admin", admin); admin.ToAddress();
//        json.addProperty("issuer", issuer); issuer.ToAddress();
        json.addProperty("expiration", expiration);
        json.addProperty("frozen", isFrozen);

        return TR.exit(json);
    }

    @Override
    public String toString() {
        return getName();
    }
}