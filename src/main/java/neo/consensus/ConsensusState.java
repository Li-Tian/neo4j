package neo.consensus;

import neo.csharp.common.ByteFlag;

/**
 * Consensus state
 */
public class ConsensusState extends ByteFlag {

    /**
     * Initial
     */
    public static final ConsensusState Initial = new ConsensusState((byte) 0x00);

    /**
     * The Speaker
     */
    public static final ConsensusState Primary = new ConsensusState((byte) 0x01);

    /**
     * The Delegates
     */
    public static final ConsensusState Backup = new ConsensusState((byte) 0x02);

    /**
     * The prepare-request message has been send
     */
    public static final ConsensusState RequestSent = new ConsensusState((byte) 0x04);

    /**
     * The prepare-request message has been received
     */
    public static final ConsensusState RequestReceived = new ConsensusState((byte) 0x08);

    /**
     * The prepare-response message has been send
     */
    public static final ConsensusState SignatureSent = new ConsensusState((byte) 0x10);

    /**
     * The full block has been published before the next round start
     */
    public static final ConsensusState BlockSent = new ConsensusState((byte) 0x20);

    /**
     * View changing
     */
    public static final ConsensusState ViewChanging = new ConsensusState((byte) 0x40);


    public ConsensusState(byte value) {
        super(value);
    }

    /**
     * state A's value and state B's value
     *
     * @param other the target state
     * @return a new state with the value of A and B
     */
    public ConsensusState and(ConsensusState other) {
        return new ConsensusState((byte) (this.value & other.value));
    }

    /**
     * state A's value or state B's value
     *
     * @param other the target state
     * @return a new state with the value of A or B
     */
    public ConsensusState or(ConsensusState other) {
        return new ConsensusState((byte) (this.value | other.value));
    }
}
