package neo.ledger;

import org.bouncycastle.math.ec.ECPoint;

import neo.Fixed8;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.io.ICloneable;

public class ValidatorState extends StateBase implements ICloneable<ValidatorState> {
    public ECPoint publicKey;
    public boolean registered;
    public Fixed8 votes;

    public ValidatorState() {
    }

    public ValidatorState(ECPoint pubkey) {
        this.publicKey = pubkey;
        this.registered = false;
        this.votes = Fixed8.ZERO;
    }

    @Override
    public ValidatorState copy() {
        ValidatorState state = new ValidatorState();
        state.votes = votes;
        state.registered = registered;
        state.publicKey = publicKey;
        return state;
    }

    @Override
    public void deserialize(BinaryReader reader) {
        super.deserialize(reader);
//        TODO ECPoint 序列化
//        publicKey = ECPoint.DeserializeFrom(reader, ECCurve.Secp256r1);
        registered = reader.readBoolean();
        votes = reader.readSerializable(Fixed8::new);
    }


    @Override
    public void fromReplica(ValidatorState replica) {
        publicKey = replica.publicKey;
        registered = replica.registered;
        votes = replica.votes;
    }

    @Override
    public void serialize(BinaryWriter writer) {
        super.serialize(writer);
        // TODO write ecpoint
//        writer.writeSerializable(PublicKey);
        writer.writeBoolean(registered);
        writer.writeSerializable(votes);
    }


}
