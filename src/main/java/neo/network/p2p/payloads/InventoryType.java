package neo.network.p2p.payloads;


import java.util.HashMap;

import neo.csharp.BitConverter;
import neo.csharp.common.ByteEnum;
import neo.smartcontract.ContractParameterType;

/**
 * The type of inventory
 */
public enum InventoryType implements ByteEnum {

    /**
     * Transaction
     */
    Tx((byte) 0x01),

    /**
     * Transaction
     */
    Block((byte) 0x02),

    /**
     * Consensus data
     */
    Consensus((byte) 0xe0);

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

    private static final HashMap<Byte, InventoryType> map = new HashMap<>();

    static {
        for (InventoryType type : InventoryType.values()) {
            map.put(type.value, type);
        }
    }

    /**
     * parse type from value
     *
     * @param type type's value
     * @return InventoryType
     * @throws IllegalArgumentException throws this exception when the type is not exist.
     */
    public static InventoryType parse(byte type) {
        if (map.containsKey(type)) {
            return map.get(type);
        }
        throw new IllegalArgumentException();
    }

}
