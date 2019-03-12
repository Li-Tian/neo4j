package neo.network.p2p.payloads;


import neo.common.ByteEnum;

/**
 * The type of Inventory
 */
public enum InventoryType implements ByteEnum {

    /**
     * Transaction
     */
    Tr((byte) 0x01),

    /**
     * Transaction
     */
    Block((byte) 0x02),

    /**
     * Consensus data
     */
    Consensus((byte) 0x03);

    private byte value;

    InventoryType(byte val) {
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
     * parse type from value
     *
     * @param type type's value
     * @return InventoryType
     * @throws IllegalArgumentException throws this exception when the type is not exist.
     */
    public static InventoryType parse(byte type) {
        return ByteEnum.parse(InventoryType.values(), type);
    }
}
