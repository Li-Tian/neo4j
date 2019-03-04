package neo.network.p2p.payloads;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import neo.Fixed8;
import neo.UInt160;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.InvalidOperationException;
import neo.persistence.Snapshot;


public class StateTransaction extends Transaction {

    public StateDescriptor[] descriptors;

    public StateTransaction() {
        super(TransactionType.StateTransaction);
    }

    @Override
    public int size() {
        // TODO C#
//        Size => base.Size + Descriptors.GetVarSize();
        return super.size();
    }

    @Override
    public Fixed8 getSystemFee() {
        Fixed8 fee = Fixed8.ZERO;
        for (StateDescriptor descriptor : descriptors) {
            fee = Fixed8.add(fee, descriptor.getSystemFee());
        }
        return fee;
    }

    @Override
    protected void deserializeExclusiveData(BinaryReader reader) {
        descriptors = reader.readArray(StateDescriptor[]::new, StateDescriptor::new, 16);
    }

    @Override
    public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
        UInt160[] hashes = super.getScriptHashesForVerifying(snapshot);
        List<UInt160> list = Arrays.asList(hashes);

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
                    list.addAll(getScriptHashesForVerifyingAccount(descriptor));
                    break;
                case Validator:
                    list.addAll(getScriptHashesForVerifying_Validator(descriptor));
                    break;
                default:
                    throw new InvalidOperationException();
            }
        }
        return (UInt160[]) list.stream().distinct().sorted().toArray();
    }

    private Collection<UInt160> getScriptHashesForVerifyingAccount(StateDescriptor descriptor) {
        switch (descriptor.field) {
            case "Votes":
                return Collections.singleton(new UInt160(descriptor.key));
            default:
                throw new InvalidOperationException();
        }
    }

    private Collection<UInt160> getScriptHashesForVerifying_Validator(StateDescriptor descriptor) {
        switch (descriptor.field) {
            case "Registered":
//                Collections.singleton(UInt160.parseToScriptHash(Contract.createSignatureRedeemScript()));
//                yield return Contract.CreateSignatureRedeemScript(ECPoint.DecodePoint(descriptor.Key, ECCurve.Secp256r1)).ToScriptHash();
                return Collections.emptyList();// TODO waiting ecpint
            default:
                throw new InvalidOperationException();
        }
    }

    @Override
    protected void serializeExclusiveData(BinaryWriter writer) {
        writer.writeArray(descriptors);
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();

        JsonArray array = new JsonArray(descriptors.length);
        for (StateDescriptor descriptor : descriptors) {
            array.add(descriptor.toJson());
        }
        json.add("descriptors", array);
        return json;
    }

    @Override
    public boolean verify(Snapshot snapshot, Collection<Transaction> mempool) {
        for (StateDescriptor descriptor : descriptors)
            if (!descriptor.verify(snapshot))
                return false;
        return super.verify(snapshot, mempool);
    }
}
