package neo.ledger;


import neo.Fixed8;
import neo.cryptography.ecc.ECC;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.io.ICloneable;
import neo.cryptography.ecc.ECPoint;
import neo.log.tr.TR;

/**
 * The state of validator
 */
public class ValidatorState extends StateBase implements ICloneable<ValidatorState> {

    /**
     * The public key of validators
     */
    public ECPoint publicKey;

    /**
     * Is it registered
     */
    public boolean registered;

    /**
     * The votes on it
     */
    public Fixed8 votes;

    /**
     * Empty constructor
     */
    public ValidatorState() {
    }

    /**
     * The constructor
     *
     * @param pubkey The public key of validators
     */
    public ValidatorState(ECPoint pubkey) {
        this.publicKey = pubkey;
        this.registered = false;
        this.votes = Fixed8.ZERO;
    }


    /**
     * 存储大小
     */
    @Override
    public int size() {
        TR.enter();
        return TR.exit(super.size() + publicKey.size() + Byte.BYTES + votes.size());
    }

    /**
     * The clone method
     */
    @Override
    public ValidatorState copy() {
        TR.enter();
        ValidatorState state = new ValidatorState();
        state.votes = votes;
        state.registered = registered;
        state.publicKey = publicKey;
        return TR.exit(state);
    }

    /**
     * Deserialization
     *
     * @param reader The binary input stream
     */
    @Override
    public void deserialize(BinaryReader reader) {
        TR.enter();
        super.deserialize(reader);
        publicKey = ECPoint.deserializeFrom(reader, ECC.Secp256r1.getCurve());
        registered = reader.readBoolean();
        votes = reader.readSerializable(Fixed8::new);
        TR.exit();
    }


    /**
     * Copy from replication
     *
     * @param replica Replication of other validator state
     */
    @Override
    public void fromReplica(ValidatorState replica) {
        TR.enter();
        publicKey = replica.publicKey;
        registered = replica.registered;
        votes = replica.votes;
        TR.exit();
    }

    /**
     * Serialization
     * <p>fields</p>
     * <ul>
     * <li>stateVersion: The verison of state</li>
     * <li>publicKey: The public key of validator</li>
     * <li>registered: Is it registerd</li>
     * <li>votes: The number of votes</li>
     * </ul>
     *
     * @param writer The binary output stream writer
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        super.serialize(writer);
        writer.writeSerializable(publicKey);
        writer.writeBoolean(registered);
        writer.writeSerializable(votes);
        TR.exit();
    }

    /**
     * Get votes
     */
    public Fixed8 getVotes() {
        return votes;
    }

}
