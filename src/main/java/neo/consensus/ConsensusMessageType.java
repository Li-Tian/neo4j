package neo.consensus;

import neo.csharp.common.ByteEnum;

/**
 * Consensus message type
 */
public enum ConsensusMessageType implements ByteEnum {

    /**
     * ChangeView message
     */
    ChangeView((byte) 0x00),

    /**
     * PrepareRequest message
     */
    PrepareRequest((byte) 0x20),

    /**
     * PrepareResponse message
     */
    PrepareResponse((byte) 0x21);

    ConsensusMessageType(byte value) {
        this.value = value;
    }

    private byte value;

    /**
     * get the value of ConsensusMessageType
     *
     * @return byte value
     */
    @Override
    public byte value() {
        return value;
    }

    /**
     * parse ConsensusMessageType by the value of type
     *
     * @param type value of type
     * @return ConsensusMessageType
     */
    public static ConsensusMessageType parse(byte type) {
        return ByteEnum.parse(ConsensusMessageType.values(), type);
    }

}
