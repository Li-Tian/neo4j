package neo.network.p2p.payloads;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


import java.util.Arrays;
import java.util.Collection;

import neo.Fixed8;
import neo.UInt160;
import neo.cryptography.ecc.ECC;
import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.FormatException;
import neo.ledger.Blockchain;
import neo.log.notr.TR;
import neo.persistence.Snapshot;
import neo.smartcontract.Contract;
import neo.cryptography.ecc.ECPoint;


/**
 * A transaction for registering asset(given up, please use InvocationTransaction)
 */
@Deprecated
public class RegisterTransaction extends Transaction {

    /**
     * The type of asset
     */
    public AssetType assetType;

    /**
     * The name of asset
     */
    public String name;

    /**
     * The total amount of asset
     */
    public Fixed8 amount;

    /**
     * The precision of asset
     */
    public byte precision;

    /**
     * The publickey of owner
     */
    public ECPoint owner;

    /**
     * The address hash of admin
     */
    public UInt160 admin;

    private UInt160 scriptHash = null;

    /**
     * The constructor method.Create a registration transaction
     */
    public RegisterTransaction() {
        super(TransactionType.RegisterTransaction, RegisterTransaction::new);
    }

    /**
     * get the owner's script hash
     */
    public UInt160 getOwnerScriptHash() {
        TR.enter();
        if (scriptHash == null) {
            scriptHash = UInt160.parseToScriptHash(Contract.createSignatureRedeemScript(owner));
        }
        return TR.exit(scriptHash);
    }

    /**
     * The size for storage
     */
    @Override
    public int size() {
        TR.enter();
        // C# code: Size => base.Size + sizeof(AssetType) + Name.GetVarSize() + Amount.Size
        // + sizeof(byte) + Owner.Size + Admin.Size;
        return TR.exit(super.size() + AssetType.BYTES + BitConverter.getVarSize(name) + amount.size()
                + Byte.BYTES + owner.size() + admin.size());
    }

    /**
     * The system fee.
     *
     * @return If the asset is NEO\Gas,the fee is 0
     */
    @Override
    public Fixed8 getSystemFee() {
        TR.enter();
        if (assetType == AssetType.GoverningToken || assetType == AssetType.UtilityToken) {
            return TR.exit(Fixed8.ZERO);
        }
        return TR.exit(super.getSystemFee());
    }

    /**
     * get a hash list which is waiting for verifying
     *
     * @param snapshot database snapshot
     * @return transaction verification script hash and asset owner address script hash
     */
    @Override
    public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
        TR.enter();
        // C# code
        // return super.getScriptHashesForVerifying(snapshot).Union(new[] { owner }).OrderBy(p => p).ToArray();
        UInt160 ownerHash = UInt160.parseToScriptHash(Contract.createSignatureRedeemScript(owner));
        UInt160[] hashes = super.getScriptHashesForVerifying(snapshot);

        UInt160[] results = new UInt160[hashes.length + 1];
        results[0] = ownerHash;
        System.arraycopy(hashes, 0, results, 1, hashes.length);
        Arrays.sort(results);
        return TR.exit(results);
    }

    /**
     * Handling deserialized transactions
     *
     * @throws FormatException If the asset is NEO/GAS, but the hash value does not correspond
     */
    @Override
    protected void onDeserialized() {
        TR.enter();
        super.onDeserialized();
        if (assetType == AssetType.GoverningToken && !hash().equals(Blockchain.GoverningToken.hash()))
            throw new FormatException();
        if (assetType == AssetType.UtilityToken && !hash().equals(Blockchain.UtilityToken.hash()))
            throw new FormatException();
        TR.exit();
    }

    /**
     * Deserialization exclusive data
     *
     * @param reader The binary input reader
     * @throws FormatException 1. If the VERSION number is not 0<br/> 2. If the asset is not NEO/GAS
     *                         and not specify owner.
     */
    @Override
    protected void deserializeExclusiveData(BinaryReader reader) {
        TR.enter();
        if (version != 0) {
            throw new FormatException();
        }
        assetType = AssetType.parse((byte) reader.readByte());
        name = reader.readVarString(1024);
        amount = reader.readSerializable(Fixed8::new);
        precision = (byte) reader.readByte();
        owner = ECPoint.deserializeFrom(reader, ECC.Secp256r1.getCurve());

        if (owner.isInfinity()
                && assetType != AssetType.GoverningToken
                && assetType != AssetType.UtilityToken) {
            throw new FormatException();
        }
        admin = reader.readSerializable(UInt160::new);
        TR.exit();
    }


    /**
     * Serialize exclusive data
     *
     * <p>Fields:</p>
     * <ul>
     * <li>AssetType: type of asset</li>
     * <li>Name: name of asset</li>
     * <li>Amount: total amount of asset</li>
     * <li>Precision: precision of asset</li>
     * <li>Owner: asset owner</li>
     * <li>Admin: asset admin</li>
     * </ul>
     *
     * @param writer The binary output writer
     */
    @Override
    protected void serializeExclusiveData(BinaryWriter writer) {
        TR.enter();
        writer.writeByte(assetType.value());
        writer.writeVarString(name);
        writer.writeSerializable(amount);
        writer.writeByte(precision);
        writer.writeSerializable(owner);
        writer.writeSerializable(admin);
        TR.exit();
    }


    /**
     * Convert to json object
     *
     * @return json object
     */
    @Override
    public JsonObject toJson() {
        TR.enter();
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
        return TR.exit(json);
    }


    /**
     * The transaction verification which is deprecated
     *
     * @param snapshot The snapshot of database
     * @param mempool  transactions in mempool
     * @return The fixed value is false.
     */
    @Override
    public boolean verify(Snapshot snapshot, Collection<Transaction> mempool) {
        TR.enter();
        return TR.exit(false);
    }
}
