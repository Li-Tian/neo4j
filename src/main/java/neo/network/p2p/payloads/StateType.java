package neo.network.p2p.payloads;


import neo.csharp.common.ByteEnum;

/**
 * StateTransaction type
 */
public enum StateType implements ByteEnum {
    /**
     * The acount of votes, value is 0x40
     */
    Account((byte) 0x40),


    /**
     * The validator applicant, value is 0x48
     */
    Validator((byte) 0x48);

    private byte value;

    StateType(byte val) {
        this.value = val;
    }

    /**
     * get the value of type
     */
    @Override
    public byte value() {
        return this.value;
    }


    /**
     * parse StateType from type's value
     *
     * @param type type's value
     * @return StateType
     * @throws IllegalArgumentException throws this exception when the type is not exist.
     */
    public static StateType parse(byte type) {
        return ByteEnum.parse(StateType.values(), type);
    }
}
