package neo.network.p2p.payloads;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import neo.Fixed8;
import neo.UInt160;
import neo.cryptography.ecc.ECC;
import neo.cryptography.ecc.ECPoint;
import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.InvalidOperationException;
import neo.log.notr.TR;
import neo.persistence.Snapshot;
import neo.smartcontract.Contract;


/**
 * The transaction for voting or application for validators
 */
public class StateTransaction extends Transaction {

    /**
     * The descriptor for transactions, max 16 descriptors.
     */
    public StateDescriptor[] descriptors;

    /**
     * Constructor of transaction for voting and validator application
     */
    public StateTransaction() {
        super(TransactionType.StateTransaction, StateTransaction::new);
    }

    /**
     * The storage of size
     */
    @Override
    public int size() {
        TR.enter();
        // C# code Size => base.Size + Descriptors.GetVarSize();
        return TR.exit(super.size() + BitConverter.getVarSize(descriptors));
    }


    /**
     * The transaction system fee
     */
    @Override
    public Fixed8 getSystemFee() {
        TR.enter();
        Fixed8 fee = Fixed8.ZERO;
        for (StateDescriptor descriptor : descriptors) {
            fee = Fixed8.add(fee, descriptor.getSystemFee());
        }
        return TR.exit(fee);
    }

    /**
     * Deserialize exclusive data
     *
     * @param reader The binary input reader
     */
    @Override
    protected void deserializeExclusiveData(BinaryReader reader) {
        TR.enter();
        descriptors = reader.readArray(StateDescriptor[]::new, StateDescriptor::new, 16);
        TR.exit();
    }

    /**
     * Get the script hashes for verifying
     *
     * @param snapshot The snapshot for database
     * @return <ul>
     * <li>If the stateDescriptor field is "Votes", it includes the address of the votes </li>
     * <li>If the stateDescriptor field is "Registered", it includes the address script hash of
     * applicant</li>
     * </ul>
     */
    @Override
    public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
        TR.enter();
        UInt160[] hashes = super.getScriptHashesForVerifying(snapshot);
        HashSet<UInt160> hashSet = new HashSet<>(Arrays.asList(hashes));

        /* C# code
        HashSet<UInt160> hashes = new HashSet<UInt160>(base.GetScriptHashesForVerifying(snapshot));
        foreach (StateDescriptor descriptor in Descriptors)
        {
            switch (descriptor.Type)
            {
                case StateType.Account:
                    hashes.UnionWith(GetScriptHashesForVerifying_Account(descriptor));
                    break;
                case StateType.Validator:
                    hashes.UnionWith(GetScriptHashesForVerifying_Validator(descriptor));
                    break;
                default:
                    throw new InvalidOperationException();
            }
        }
        return hashes.OrderBy(p => p).ToArray();
        */

        for (StateDescriptor descriptor : descriptors) {
            switch (descriptor.type) {
                case Account:
                    Collection<UInt160> addrHashList = getScriptHashesForVerifyingAccount(descriptor);
                    System.out.println(addrHashList.size());
                    hashSet.addAll(addrHashList);
                    break;
                case Validator:
                    hashSet.addAll(getScriptHashesForVerifying_Validator(descriptor));
                    break;
                default:
                    throw new InvalidOperationException();
            }
        }
        return TR.exit(hashSet.stream().sorted().toArray(UInt160[]::new));
    }

    private Collection<UInt160> getScriptHashesForVerifyingAccount(StateDescriptor descriptor) {
        TR.enter();
        switch (descriptor.field) {
            case "Votes":
                return TR.exit(Collections.singleton(new UInt160(descriptor.key)));
            default:
                throw new InvalidOperationException();
        }
    }

    private Collection<UInt160> getScriptHashesForVerifying_Validator(StateDescriptor descriptor) {
        TR.enter();
        switch (descriptor.field) {
            case "Registered":
                //  Collections.singleton(UInt160.parseToScriptHash(Contract.createSignatureRedeemScript()));
                //  yield return Contract.CreateSignatureRedeemScript(ECPoint.DecodePoint(descriptor.Key, ECCurve.Secp256r1)).ToScriptHash();
                ECPoint publicKey = ECPoint.fromBytes(descriptor.key, ECC.Secp256r1.getCurve());
                byte[] scripts = Contract.createSignatureRedeemScript(publicKey);
                UInt160 scriptHash = UInt160.parseToScriptHash(scripts);
                return TR.exit(Collections.singleton(scriptHash));
            default:
                throw new InvalidOperationException();
        }
    }

    /**
     * Serialize exclusive data
     * <p>fields:</p>
     * <ul>
     * <li>Descriptors: transaction description</li>
     * </ul>
     *
     * @param writer BinaryWriter
     */
    @Override
    protected void serializeExclusiveData(BinaryWriter writer) {
        TR.enter();
        writer.writeArray(descriptors);
        TR.exit();
    }

    /**
     * Transfer to json object
     *
     * @return Json object
     */
    @Override
    public JsonObject toJson() {
        TR.enter();
        JsonObject json = super.toJson();

        JsonArray array = new JsonArray(descriptors.length);
        for (StateDescriptor descriptor : descriptors) {
            array.add(descriptor.toJson());
        }
        json.add("descriptors", array);
        return TR.exit(json);
    }

    /**
     * The transaction verification
     *
     * @param snapshot database snapshot
     * @param mempool  transaction in mempool
     * @return <ul>
     * <li>1. Verify each stateDescriptor
     * <ul>
     * <li>1.1 When the descriptor.Type is StateType.Validator:if descriptor.Field is not equal to
     * Registered, return false </li>
     * <li>1.2 When the descriptor.Type is StateType.Account:
     * <ul>
     * <li>1.2.1 If NEO hold by the the voting accuntis 0, or when the voting acount is frozen,
     * return false.</li>
     * <li> 1.2.2 If the voted account is in the backup list or is not registered as validators,
     * return false.</li>
     * </ul></li>
     * </ul>
     * </li>
     * <li>2. The basic transaction verification. If verified failed, return false</li>
     * </ul>
     */
    @Override
    public boolean verify(Snapshot snapshot, Collection<Transaction> mempool) {
        TR.enter();
        for (StateDescriptor descriptor : descriptors)
            if (!descriptor.verify(snapshot))
                return TR.exit(false);
        return TR.exit(super.verify(snapshot, mempool));
    }
}
