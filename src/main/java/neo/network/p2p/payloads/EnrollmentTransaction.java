package neo.network.p2p.payloads;


import com.google.gson.JsonObject;


import java.util.Arrays;
import java.util.Collection;

import neo.UInt160;
import neo.cryptography.ecc.ECC;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.FormatException;
import neo.persistence.Snapshot;
import neo.smartcontract.Contract;
import neo.cryptography.ecc.ECPoint;

/**
 * Registered Verifier【Abandoned, please use StateTransaction】
 */
@Deprecated
public class EnrollmentTransaction extends Transaction {

    /**
     * Applicant public key
     */
    public ECPoint publicKey;

    private UInt160 scriptHash = null;

    /**
     * Constructor：create a EnrollmentTransaction object
     */
    public EnrollmentTransaction() {
        super(TransactionType.EnrollmentTransaction);
    }

    /**
     * get scripthash
     */
    public UInt160 getScriptHash() {
        if (scriptHash == null) {
            scriptHash = UInt160.parseToScriptHash(Contract.createSignatureRedeemScript(publicKey));
        }
        return scriptHash;
    }

    /**
     * size for storage
     */
    @Override
    public int size() {
        return super.size() + publicKey.size();
    }

    /**
     * Deserialize method，read publickey from binary reader
     *
     * @param reader BinaryReader
     * @throws FormatException the transaction version number is not 0
     */
    @Override
    protected void deserializeExclusiveData(BinaryReader reader) {
        if (version != 0) throw new FormatException();
        publicKey = ECPoint.deserializeFrom(reader, ECC.Secp256r1.getCurve());
    }

    /**
     * Get the hash of the transaction that needs to be signed. This includes transaction input
     * address and the applicant's public key.
     *
     * @param snapshot snapshot
     * @return This includes transaction input address and the applicant's public key.
     */
    @Override
    public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
        // C# code
        // base.GetScriptHashesForVerifying(snapshot).Union(new UInt160[] { ScriptHash })
        // .OrderBy(p => p).ToArray();
        UInt160 ownerHash = getScriptHash();
        UInt160[] hashes = super.getScriptHashesForVerifying(snapshot);

        UInt160[] results = new UInt160[hashes.length + 1];
        results[0] = ownerHash;
        System.arraycopy(hashes, 0, results, 1, hashes.length);
        Arrays.sort(results);
        return results;
    }

    /**
     * Serialize
     * <p>fields:</p>
     * <ul>
     * <li>PublicKey: applicant's public key</li>
     * </ul>
     *
     * @param writer BinaryWriter
     */
    @Override
    protected void serializeExclusiveData(BinaryWriter writer) {
        writer.writeSerializable(publicKey);
    }

    /**
     * applicant's public key
     *
     * @return JObject object
     */
    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("pubkey", publicKey.toString());
        return json;
    }

    /**
     * Verify the transaction. This class has been deprecated.Return false by default.
     *
     * @param snapshot database snapshot
     * @param mempool  mempool
     * @return return false by default.
     */
    @Override
    public boolean verify(Snapshot snapshot, Collection<Transaction> mempool) {
        return false;
    }
}
