package neo.network.p2p.payloads;


import neo.common.ByteEnum;

/**
 * Inventory类型
 */
public enum InventoryType implements ByteEnum {

    /**
     * 交易
     */
    Tr((byte) 0x01),

    /**
     * 区块类
     */
    Block((byte) 0x02),

    /**
     * 共识类
     */
    Consensus((byte) 0x03);

    private byte value;

    InventoryType(byte val) {
        this.value = val;
    }

    /**
     * 获取类别存储的byte值
     */
    @Override
    public byte value() {
        return this.value;
    }

    /**
     * 从byte中解析类型
     *
     * @param type 待解析的Inventory类型值
     * @return InventoryType
     * @throws IllegalArgumentException 当类型不存在时，抛出该异常
     */
    public static InventoryType parse(byte type) {
        return ByteEnum.parse(InventoryType.values(), type);
    }
}
