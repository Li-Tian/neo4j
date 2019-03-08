package neo.network.p2p.payloads;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


import java.util.Arrays;
import java.util.Collection;

import neo.Fixed8;
import neo.UInt160;
import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.FormatException;
import neo.ledger.Blockchain;
import neo.persistence.Snapshot;
import neo.smartcontract.Contract;
import neo.cryptography.ecc.ECPoint;


/**
 * 资产登记交易【已弃用】
 */
@Deprecated
public class RegisterTransaction extends Transaction {

    /**
     * 资产类型
     */
    public AssetType assetType;

    /**
     * 资产名字
     */
    public String name;

    /**
     * 资产总量
     */
    public Fixed8 amount;

    /**
     * 精度
     */
    public byte precision;

    /**
     * 所有者公钥
     */
    public ECPoint owner;

    /**
     * 管理员地址脚本hash
     */
    public UInt160 admin;

    private UInt160 scriptHash = null;

    /**
     * 创建智能合约发布交易
     */
    public RegisterTransaction() {
        super(TransactionType.RegisterTransaction);
    }

    /**
     * 获取所有者脚本hash
     */
    public UInt160 getOwnerScriptHash() {
        if (scriptHash == null) {
            scriptHash = UInt160.parseToScriptHash(Contract.createSignatureRedeemScript(owner));
        }
        return scriptHash;
    }

    /**
     * 存储大小
     */
    @Override
    public int size() {
        // C# code: Size => base.Size + sizeof(AssetType) + Name.GetVarSize() + Amount.Size + sizeof(byte) + Owner.Size + Admin.Size;
        return super.size() + AssetType.BYTES + BitConverter.getVarSize(name) + amount.size() + Byte.BYTES + owner.size() + admin.size();
    }

    /**
     * 系统手续费
     *
     * @return 若资产是NEO，GAS则费用为0
     */
    @Override
    public Fixed8 getSystemFee() {
        if (assetType == AssetType.GoverningToken || assetType == AssetType.UtilityToken) {
            return Fixed8.ZERO;
        }
        return super.getSystemFee();
    }

    /**
     * 获取待验证签名的脚本hash集合
     *
     * @param snapshot 数据库快照
     * @return 交易的其他验证脚本 和 资产所有者地址脚本hash
     */
    @Override
    public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
        // C# code
        // return super.getScriptHashesForVerifying(snapshot).Union(new[] { owner }).OrderBy(p => p).ToArray();
        UInt160 ownerHash = UInt160.parseToScriptHash(Contract.createSignatureRedeemScript(owner));
        UInt160[] hashes = super.getScriptHashesForVerifying(snapshot);

        UInt160[] results = new UInt160[hashes.length + 1];
        results[0] = ownerHash;
        System.arraycopy(hashes, 0, results, 1, hashes.length);
        Arrays.sort(results);
        return results;
    }

    /**
     * 反序列化后的处理
     *
     * @throws FormatException 若资产是NEO，GAS，但是hash值不对应时，抛出该异常
     */
    @Override
    protected void onDeserialized() {
        super.onDeserialized();
        if (assetType == AssetType.GoverningToken && !hash().equals(Blockchain.GoverningToken.hash()))
            throw new FormatException();
        if (assetType == AssetType.UtilityToken && !hash().equals(Blockchain.UtilityToken.hash()))
            throw new FormatException();
    }

    /**
     * 反序列化非data数据
     *
     * @param reader 二进制输入流
     * @throws FormatException 1. 如果版本号不为0. 2. 如果资产不是NEO/GAS且未指定Owner
     */
    @Override
    protected void deserializeExclusiveData(BinaryReader reader) {
        if (version != 0) {
            throw new FormatException();
        }
        assetType = AssetType.parse((byte) reader.readByte());
        name = reader.readVarString(1024);
        amount = reader.readSerializable(Fixed8::new);
        precision = (byte) reader.readByte();
        owner = ECPoint.deserializeFrom(reader, ECPoint.secp256r1.getCurve());
        if (owner.isInfinity() && assetType != AssetType.GoverningToken && assetType != AssetType.UtilityToken)
            throw new FormatException();
        admin = reader.readSerializable(UInt160::new);
    }


    /**
     * 序列化非data数据
     *
     * <p>序列化字段包括：</p>
     * <ul>
     * <li>AssetType: 资产类型</li>
     * <li>Name: 名字</li>
     * <li>Amount: 总量</li>
     * <li>Precision: 精度</li>
     * <li>Owner: 所有者</li>
     * <li>Admin: 管理员</li>
     * </ul>
     *
     * @param writer 二进制输出流
     */
    @Override
    protected void serializeExclusiveData(BinaryWriter writer) {
        writer.writeByte(assetType.value());
        writer.writeVarString(name);
        writer.writeSerializable(amount);
        writer.writeByte(precision);
        writer.writeSerializable(owner); // TODO
        writer.writeSerializable(admin);
    }


    /**
     * 转成json对象
     *
     * @return json对象
     */
    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        JsonObject asset = new JsonObject();
        json.add("asset", asset);

        asset.addProperty("type", assetType.value());
        try {
            asset.add("name", name == "" ? null : new JsonParser().parse(name));
        } catch (FormatException e) {
            asset.addProperty("name", name);
        }
        asset.addProperty("amount", amount.toString());
        asset.addProperty("precision", precision);
        asset.addProperty("owner", owner.toString());
        asset.addProperty("admin", admin.toAddress());
        return json;
    }


    /**
     * 校验交易。已经弃用。禁止注册新的资产。
     *
     * @param snapshot 数据库快照
     * @param mempool  内存池交易
     * @return 固定值false，已弃用
     */
    @Override
    public boolean verify(Snapshot snapshot, Collection<Transaction> mempool) {
        return false;
    }
}
