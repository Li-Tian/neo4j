package neo.ledger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import neo.UInt160;
import neo.UInt256;
import neo.Fixed8;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.FormatException;
import neo.io.ICloneable;

import neo.log.tr.TR;
import neo.network.p2p.payloads.AssetType;
import neo.cryptography.ECC.ECPoint;

/**
 * 资产状态
 */
public class AssetState extends StateBase implements ICloneable<AssetState> {
    /**
     * 资产Id
     */
    public UInt256 assetId;

    /**
     * 资产类型
     */
    public AssetType assetType;

    /**
     * 资产名称
     */
    public String name;

    /**
     * 资产总量
     */
    public Fixed8 amount;

    /**
     * 资产可用额度
     */
    public Fixed8 available;

    /**
     * 精度
     */
    public byte precision;

    /**
     * 收费模式
     */
    public final byte feeMode = 0;

    /**
     * 费用
     */
    public Fixed8 fee;

    /**
     * 收费地址
     */
    public UInt160 feeAddress;

    /**
     * 所有者地址
     */
    public ECPoint owner;

    /**
     * 管理员地址
     */
    public UInt160 admin;

    /**
     * 发行者地址
     */
    public UInt160 issuer;

    /**
     * 资产过期时间（允许上链的最后区块高度）
     */
    public Uint expiration;

    /**
     * 资产是否冻结
     */
    public boolean isFrozen;

    /**
     * 存储大小
     */
    @Override
    public int size() {
        TR.enter();
        // 1 + 32 + 1 +  1+ name.length + 8 + 8 + 1 +1 + 8 + 20 + 1 + 20 + 20 + 4 +1
        return TR.exit(super.size() + assetId.size() + AssetType.BYTES + BitConverter.getVarSize(name) +
                amount.size() + available.size() + Byte.BYTES + Byte.BYTES + fee.size() +
                feeAddress.size() + owner.size() + admin.size() + issuer.size() + Uint.BYTES + Byte.BYTES);
    }

    /**
     * 克隆
     *
     * @return 克隆对象
     */
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

    /**
     * 反序列化
     *
     * @param reader 二进制输入流
     */
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
        owner = ECPoint.deserializeFrom(reader, ECPoint.secp256r1.getCurve());
        admin = reader.readSerializable(UInt160::new);
        issuer = reader.readSerializable(UInt160::new);
        expiration = reader.readUint();
        isFrozen = reader.readBoolean();
        TR.exit();
    }

    /**
     * 从指定参数的副本复制信息到将当前资产
     *
     * @param replica 资产的拷贝副本
     */
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

    /**
     * 查询资产名称
     *
     * @return 资产名
     */
    public String getName() {
        TR.enter();
        return TR.exit(getName(null));
    }

    /**
     * 查询资产名称
     *
     * @param culture 语言环境
     * @return 资产名
     */
    public String getName(Locale culture) {
        TR.enter();
        if (assetType == AssetType.GoverningToken) return "NEO";
        if (assetType == AssetType.UtilityToken) return "NeoGas";
        if (nameMap == null) {
            nameMap = new ConcurrentHashMap<>();
            JsonElement nameJsonElement;
            try {
                nameJsonElement = new JsonParser().parse(name);
                System.out.println(name);
                System.out.println(nameJsonElement);

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
                } else {
                    nameMap.put(Locale.ENGLISH, nameJsonElement.getAsString());
                }
            } catch (Exception e) {
                TR.error(e);
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

    /**
     * 序列化
     * <p>序列化字段</p>
     * <ul>
     * <li>stateVersion: 状态版本号</li>
     * <li>assetId: 资产id</li>
     * <li>assetType: 资产类型</li>
     * <li>name: 资产名称</li>
     * <li>amount: 总量</li>
     * <li>available: 可用量</li>
     * <li>precision: 精度</li>
     * <li>feeMode: 费用模式，目前为0</li>
     * <li>fee: 费用</li>
     * <li>feeAddress: 收费地址</li>
     * <li>owner: 所有者地址</li>
     * <li>admin: 管理员地址</li>
     * <li>issuer: 发行者地址</li>
     * <li>expiration: 资产过期时间</li>
     * <li>isFrozen: 资产是否冻结</li>
     * </ul>
     *
     * @param writer 二进制输出流
     */
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
        writer.writeSerializable(owner);
        writer.writeSerializable(admin);
        writer.writeSerializable(issuer);
        writer.writeUint(expiration);
        writer.writeBoolean(isFrozen);
        TR.exit();
    }

    /**
     * 将这个AssetState转成json对象返回
     *
     * @return 转换好的json对象
     */
    @Override
    public JsonObject toJson() {
        TR.enter();
        JsonObject json = super.toJson();
        json.addProperty("id", assetId.toString());
        json.addProperty("type", assetType.value());
        try {
            JsonElement element = name == "" ? null : new JsonParser().parse(name);
            json.add("name", element);
        } catch (FormatException e) {
            json.addProperty("name", name);
        }
        json.addProperty("amount", amount.toString());
        json.addProperty("available", available.toString());
        json.addProperty("precision", precision);
        json.addProperty("owner", owner.toString());
        json.addProperty("admin", admin.toAddress());
        json.addProperty("issuer", issuer.toAddress());
        json.addProperty("expiration", expiration);
        json.addProperty("frozen", isFrozen);
        return TR.exit(json);
    }

    /**
     * 转成String
     *
     * @return 返回资产名字
     */
    @Override
    public String toString() {
        return getName();
    }
}