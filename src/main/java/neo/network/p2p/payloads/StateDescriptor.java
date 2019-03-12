package neo.network.p2p.payloads;


import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.util.HashSet;

import neo.Fixed8;
import neo.UInt160;
import neo.cryptography.ecc.ECPoint;
import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;
import neo.exception.FormatException;
import neo.exception.InvalidOperationException;
import neo.io.SerializeHelper;
import neo.ledger.AccountState;
import neo.ledger.Blockchain;
import neo.ledger.ValidatorState;
import neo.log.notr.TR;
import neo.persistence.Snapshot;

/**
 * The description of vote state: vote and application
 *
 * @note Be careful with key, field, and value fields.
 */
public class StateDescriptor implements ISerializable {

    /**
     * The type: Vote or register as a applicant
     */
    public StateType type;

    /**
     * <ul>
     * <li>If the filed is Votes,it will save the script hash of current voters and represents
     * voters</li>
     * <li>If the field is Registered, it will save the public key and represents applicant</li>
     * </ul>
     */
    public byte[] key;

    /**
     * <ul>
     * <li>When the Type is 0x40, Field is equal to "Votes";</li>
     * <li>when the Type is 0x48, Field is equal to "Registered";</li>
     * </ul>
     */
    public String field;

    /**
     * <ul>
     * <li>When the Type is 0x40, which stands for the vote list </li>
     * <li>When the Type is 0x48, which stands for canceling or registers the validators</li>
     * </ul>
     */
    public byte[] value;

    /**
     * The size of storage
     */
    @Override
    public int size() {
        TR.enter();
        //  C# code sizeof(StateType) + Key.GetVarSize() + Field.GetVarSize() + Value.GetVarSize();
        // 1 + key(1 +) + field(1+) + value(1+)
        return TR.exit(StateType.BYTES + BitConverter.getVarSize(key) + BitConverter.getVarSize(field)
                + BitConverter.getVarSize(value));
    }

    /**
     * The serialization method
     *
     * @param writer The binary output writer
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        writer.writeByte(type.value());
        writer.writeVarBytes(key);
        writer.writeVarString(field);
        writer.writeVarBytes(value);
        TR.exit();
    }


    /**
     * Deserialization
     *
     * @param reader The binary output input
     */
    @Override
    public void deserialize(BinaryReader reader) {
        TR.enter();
        type = StateType.parse((byte) reader.readByte());
        key = reader.readVarBytes(100);
        field = reader.readVarString(32);
        value = reader.readVarBytes(65535);
        switch (type) {
            case Account:
                checkAccountState();
                break;
            case Validator:
                checkValidatorState();
                break;
        }
        TR.exit();
    }

    /**
     * The transaction fee. If apply for validators, need 1000 Gas, otherwose is 0.
     */
    public Fixed8 getSystemFee() {
        TR.enter();
        switch (type) {
            case Validator:
                return TR.exit(getSystemFeeValidator());
            default:
                return TR.exit(Fixed8.ZERO);
        }
    }

    private Fixed8 getSystemFeeValidator() {
        TR.enter();
        switch (field) {
            case "Registered":
                for (byte b : value) {
                    if (b != 0) {
                        return TR.exit(Fixed8.fromDecimal(new BigDecimal(1000)));
                    }
                }
                return TR.exit(Fixed8.ZERO);
            default:
                throw new InvalidOperationException();
        }
    }


    private void checkAccountState() {
        TR.enter();
        if (key.length != 20) throw new FormatException();
        if (!"Votes".equals(field)) throw new FormatException();
        TR.exit();
    }


    private void checkValidatorState() {
        TR.enter();
        if (key.length != 33) throw new FormatException();
        if (!"Registered".equals(field)) throw new FormatException();
        TR.exit();
    }

    protected boolean verify(Snapshot snapshot) {
        TR.enter();
        switch (type) {
            case Account:
                return TR.exit(verifyAccountState(snapshot));
            case Validator:
                return TR.exit(verifyValidatorState());
            default:
                return TR.exit(false);
        }
    }

    private boolean verifyAccountState(Snapshot snapshot) {
        TR.enter();
        // C# code
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

        switch (field) {
            case "Votes":
                ECPoint[] pubkeys;
                try {
                    pubkeys = SerializeHelper.asAsSerializableArray(value, ECPoint[]::new, ECPoint::new);
                } catch (FormatException e) {
                    // we just ignore this error, and return false
                    return TR.exit(false);
                }
                UInt160 hash = new UInt160(key);
                AccountState account = snapshot.getAccounts().tryGet(hash);
                // @TODO if account is null, we just return false.
                if (account == null || account.isFrozen != false) {
                    return TR.exit(false);
                }
                if (pubkeys.length > 0) {
                    if (account.getBalance(Blockchain.GoverningToken.hash()).equals(Fixed8.ZERO)) {
                        return TR.exit(false);
                    }
                    HashSet<ECPoint> sv = new HashSet<>(Blockchain.StandbyValidators.length);
                    for (ECPoint ecPoint : Blockchain.StandbyValidators) {
                        sv.add(ecPoint);
                    }
                    for (ECPoint pubkey : pubkeys) {
                        ValidatorState validator = snapshot.getValidators().tryGet(pubkey);
                        if (!sv.contains(pubkey) && (validator != null && validator.registered != true)) {
                            return TR.exit(false);
                        }
                    }
                }
                return TR.exit(true);
            default:
                return TR.exit(false);
        }
    }

    private boolean verifyValidatorState() {
        TR.enter();
        switch (field) {
            case "Registered":
                return TR.exit(true);
            default:
                return TR.exit(false);
        }
    }

    /**
     * Transfer to json object
     */
    public JsonObject toJson() {
        TR.enter();
        JsonObject json = new JsonObject();
        json.addProperty("type", type.value());
        json.addProperty("key", BitConverter.toHexString(key));
        json.addProperty("field", field);
        json.addProperty("value", BitConverter.toHexString(value));
        return TR.exit(json);
    }


}
