package neo.ledger;

import neo.Fixed8;
import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.io.ICloneable;
import neo.log.notr.TR;

/**
 * The state of validators's count
 */
public class ValidatorsCountState extends StateBase implements ICloneable<ValidatorsCountState> {

    /**
     * The list of votes whose index stands for the count of validators
     */
    public Fixed8[] votes;

    /**
     * Constructor of creating the state of validators's count
     */
    public ValidatorsCountState() {
        this.votes = new Fixed8[Blockchain.MaxValidators];
        for (int i = 0; i < Blockchain.MaxValidators; i++) {
            this.votes[i] = Fixed8.ZERO;
        }
    }

    /**
     * The size of storage
     */
    @Override
    public int size() {
        TR.enter();
        return TR.exit(super.size() + BitConverter.getVarSize(votes));
    }

    /**
     * clone
     */
    @Override
    public ValidatorsCountState copy() {
        TR.enter();
        ValidatorsCountState state = new ValidatorsCountState();
        for (int i = 0; i < votes.length; i++) {
            state.votes[i] = votes[i];
        }
        return TR.exit(state);
    }

    /**
     * Copy from Replication
     *
     * @param replica replication
     */
    @Override
    public void fromReplica(ValidatorsCountState replica) {
        TR.enter();
        votes = replica.votes;
        TR.exit();
    }

    /**
     * Deserialization
     *
     * @param reader The binary input reader
     */
    @Override
    public void deserialize(BinaryReader reader) {
        TR.enter();
        super.deserialize(reader);
        votes = reader.readArray(Fixed8[]::new, Fixed8::new);
        TR.exit();
    }

    /**
     * Serialization
     * <p>fields</p>
     * <ul>
     * <li>stateVersion: The VERSION of the state</li>
     * <li>votes: The status of votings</li>
     * </ul>
     *
     * @param writer The binary output writer
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        super.serialize(writer);
        writer.writeArray(votes);
        TR.exit();
    }
}
