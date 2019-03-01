package neo.network.p2p.payloads;


import com.google.gson.JsonObject;

import java.math.BigDecimal;

import neo.Fixed8;
import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;
import neo.exception.FormatException;
import neo.exception.InvalidOperationException;
import neo.exception.TypeNotExistException;
import neo.persistence.Snapshot;

public class StateDescriptor implements ISerializable {

    public byte type;
    public byte[] key;
    public String field;
    public byte[] value;

    @Override
    public int size() {
//        sizeof(StateType) + Key.GetVarSize() + Field.GetVarSize() + Value.GetVarSize();
        // TODO waiting getvarsize
        return 0;
    }

    @Override
    public void serialize(BinaryWriter writer) {
        writer.writeByte(type);
        writer.writeVarBytes(key);
        writer.writeVarString(field);
        writer.writeVarBytes(value);
    }

    @Override
    public void deserialize(BinaryReader reader) {
        type = (byte) reader.readByte();
        if (!StateType.contain(type))
            throw new TypeNotExistException();

        key = reader.readVarBytes(100);
        field = reader.readVarString(32);
        value = reader.readVarBytes(65535);
        switch (type) {
            case StateType.Account:
                checkAccountState();
                break;
            case StateType.Validator:
                checkValidatorState();
                break;
        }
    }

    public Fixed8 getSystemFee() {
        switch (type) {
            case StateType.Validator:
                return getSystemFeeValidator();
            default:
                return Fixed8.ZERO;
        }
    }

    private Fixed8 getSystemFeeValidator() {
        switch (field) {
            case "Registered":
                for (byte b : value) {
                    if (b != 0) {
                        return Fixed8.fromDecimal(new BigDecimal(1000));
                    }
                }
                return Fixed8.ZERO;
            default:
                throw new InvalidOperationException();
        }
    }

    private void checkAccountState() {
        if (key.length != 20) throw new FormatException();
        if (!"Votes".equals(field)) throw new FormatException();
    }

    private void checkValidatorState() {
        if (key.length != 33) throw new FormatException();
        if (!"Registered".equals(field)) throw new FormatException();
    }

    protected boolean verify(Snapshot snapshot) {
        switch (type) {
            case StateType.Account:
                return verifyAccountState(snapshot);
            case StateType.Validator:
                return verifyValidatorState();
            default:
                return false;
        }
    }

    private boolean verifyAccountState(Snapshot snapshot) {
//        switch (field) {
//            case "Votes":
//                ECPoint[] pubkeys;
//                try {
//                    pubkeys = value.AsSerializableArray < ECPoint > ((int) Blockchain.MaxValidators);
//                } catch (FormatException) {
//                    return false;
//                }
//                UInt160 hash = new UInt160(Key);
//                AccountState account = snapshot.Accounts.TryGet(hash);
//                if (account ?.IsFrozen != false)return false;
//            if (pubkeys.Length > 0) {
//                if (account.GetBalance(Blockchain.GoverningToken.Hash).Equals(Fixed8.Zero))
//                    return false;
//                HashSet<ECPoint> sv = new HashSet<ECPoint>(Blockchain.StandbyValidators);
//                foreach(ECPoint pubkey in pubkeys)
//                if (!sv.Contains(pubkey) && snapshot.Validators.TryGet(pubkey) ?.Registered != true)
//                return false;
//            }
//            return true;
//            default:
//                return false;
//        }
        // TODO waiting ECPoint
        return true;
    }

    private boolean verifyValidatorState() {
        switch (field) {
            case "Registered":
                return true;
            default:
                return false;
        }
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type);
        json.addProperty("key", BitConverter.toHexString(key));
        json.addProperty("field", field);
        json.addProperty("value", BitConverter.toHexString(value));
        return json;
    }


}
