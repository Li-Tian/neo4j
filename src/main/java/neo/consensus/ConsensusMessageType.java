package neo.consensus;

import java.util.HashMap;

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


    private static final HashMap<Byte, ConsensusMessageType> map = new HashMap<>();

    static {
        for (ConsensusMessageType type : ConsensusMessageType.values()) {
            map.put(type.value, type);
        }
    }

    /**
     * parse ConsensusMessageType by the value of type
     *
     * @param type value of type
     * @return ConsensusMessageType
     */
    public static ConsensusMessageType parse(byte type) {
        if (map.containsKey(type)) {
            return map.get(type);
        }
        throw new IllegalArgumentException();
    }

}
