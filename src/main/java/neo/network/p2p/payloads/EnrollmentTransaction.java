package neo.network.p2p.payloads;


import com.google.gson.JsonObject;

import org.bouncycastle.math.ec.ECPoint;

import java.util.Arrays;
import java.util.Collection;

import neo.UInt160;
import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.FormatException;
import neo.persistence.Snapshot;
import neo.smartcontract.Contract;

@Deprecated
public class EnrollmentTransaction extends Transaction {

    public ECPoint publicKey;

    private UInt160 scriptHash = null;

    public EnrollmentTransaction() {
        super(TransactionType.EnrollmentTransaction);
    }

    public UInt160 getScriptHash() {
        if (scriptHash == null) {
            scriptHash = UInt160.parseToScriptHash(Contract.createSignatureRedeemScript(publicKey));
        }
        return scriptHash;
    }

    @Override
    public int size() {
        // TODO 待核对
        return super.size() + publicKey.getEncoded(false).length;
    }

    @Override
    protected void deserializeExclusiveData(BinaryReader reader) {
        if (version != 0) throw new FormatException();
//        PublicKey = ECPoint.DeserializeFrom(reader, ECCurve.Secp256r1);
        // TODO 待确认 ECPoint序列化
    }

    @Override
    public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
        UInt160 ownerHash = getScriptHash();
        UInt160[] hashes = super.getScriptHashesForVerifying(snapshot);

        UInt160[] results = new UInt160[hashes.length + 1];
        results[0] = ownerHash;
        System.arraycopy(hashes, 0, results, 1, hashes.length);
        Arrays.sort(results);
        return results;
    }

    @Override
    protected void serializeExclusiveData(BinaryWriter writer) {
//        writer.writeSerializable(publicKey);
        // TODO 待确认
        writer.write(publicKey.getEncoded(false));
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("pubkey", BitConverter.toHexString(publicKey.getEncoded(false)));
        // TODO 待确认
        // json["pubkey"] = PublicKey.ToString();
        return json;
    }

    @Override
    public boolean verify(Snapshot snapshot, Collection<Transaction> mempool) {
        return false;
    }
}
