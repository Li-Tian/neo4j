package neo.ledger;

import neo.Fixed8;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.io.ICloneable;

public class ValidatorsCountState extends StateBase implements ICloneable<ValidatorsCountState> {

    public Fixed8[] votes;

    public ValidatorsCountState() {
        this.votes = new Fixed8[Blockchain.MaxValidators];
    }

    @Override
    public int size() {
        // TODO getvarsize
        // Size => base.Size + Votes.GetVarSize();
        return super.size();
    }

    @Override
    public ValidatorsCountState copy() {
        ValidatorsCountState state = new ValidatorsCountState();
        state.votes = votes;
        return state;
    }

    @Override
    public void fromReplica(ValidatorsCountState replica) {
        votes = replica.votes;
    }

    @Override
    public void deserialize(BinaryReader reader) {
        super.deserialize(reader);
        votes = reader.readArray(Fixed8[]::new, Fixed8::new);
    }

    @Override
    public void serialize(BinaryWriter writer) {
        super.serialize(writer);
        writer.writeArray(votes);
    }
}
