package neo.network.p2p.payloads;

import com.google.gson.JsonObject;

import org.bouncycastle.math.ec.ECPoint;

import java.util.Arrays;
import java.util.Collection;

import neo.Fixed8;
import neo.UInt160;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.FormatException;
import neo.ledger.Blockchain;
import neo.persistence.Snapshot;
import neo.smartcontract.Contract;


@Deprecated
public class RegisterTransaction extends Transaction {

    public byte assetType;
    public String name;
    public Fixed8 amount;
    public byte precision;
    public ECPoint owner;
    public UInt160 admin;

    private UInt160 scriptHash = null;

    public RegisterTransaction() {
        super(TransactionType.RegisterTransaction);
    }

    public UInt160 getOwnerScriptHash() {
        if (scriptHash == null) {
            scriptHash = UInt160.parseToScriptHash(Contract.createSignatureRedeemScript(owner));
        }
        return scriptHash;
    }

    @Override
    public int size() {
        // TODO
//        Size => base.Size + sizeof(AssetType) + Name.GetVarSize() + Amount.Size + sizeof(byte) + Owner.Size + Admin.Size;
        return super.size();
    }

    @Override
    public Fixed8 getSystemFee() {
        if (assetType == AssetType.GoverningToken || assetType == AssetType.UtilityToken) {
            return Fixed8.ZERO;
        }
        return super.getSystemFee();
    }

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

    @Override
    protected void onDeserialized() {
        super.onDeserialized();
        if (assetType == AssetType.GoverningToken && !hash().equals(Blockchain.GoverningToken.hash()))
            throw new FormatException();
        if (assetType == AssetType.UtilityToken && !hash().equals(Blockchain.UtilityToken.hash()))
            throw new FormatException();
    }

    @Override
    protected void deserializeExclusiveData(BinaryReader reader) {
        if (version != 0) {
            throw new FormatException();
        }
        assetType = (byte) reader.readByte();
        name = reader.readVarString(1024);
        amount = reader.readSerializable(Fixed8::new);
        precision = (byte) reader.readByte();
        // TODO 序列化
//        Owner = ECPoint.DeserializeFrom(reader, ECCurve.Secp256r1);
        if (owner.isInfinity() && assetType != AssetType.GoverningToken && assetType != AssetType.UtilityToken)
            throw new FormatException();
        admin = reader.readSerializable(UInt160::new);
    }


    @Override
    protected void serializeExclusiveData(BinaryWriter writer) {
        writer.writeByte(assetType);
        writer.writeVarString(name);
        writer.writeSerializable(amount);
        writer.writeByte(precision);
//        writer.Write(Owner); // TODO
        writer.writeSerializable(admin);
    }


    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        JsonObject asset = new JsonObject();
        json.add("asset", asset);

        asset.addProperty("type", assetType);
        try {
//            asset.addProperty("name", ); = name == "" ? null : JObject.Parse(name);
        } catch (FormatException e) {
            asset.addProperty("name", name);
        }
        asset.addProperty("amount", amount.toString());
        asset.addProperty("precision", precision);
        asset.addProperty("owner", owner.toString());
        asset.addProperty("admin", admin.toString());
// TODO
//        json["asset"]["amount"] = Amount.ToString();
//        json["asset"]["precision"] = precision;
//        json["asset"]["owner"] = Owner.ToString();
//        json["asset"]["admin"] = Admin.ToAddress();
        return json;
    }

    @Override
    public boolean verify(Snapshot snapshot, Collection<Transaction> mempool) {
        return false;
    }
}
