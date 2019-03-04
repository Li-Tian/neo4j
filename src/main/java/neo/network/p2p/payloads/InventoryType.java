package neo.network.p2p.payloads;


import neo.exception.TypeNotExistException;

/**
 * Inventory类型
 */
public enum InventoryType {

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
    public byte value() {
        return this.value;
    }


    /**
     * 占用字节数大小
     */
    public static final int BYTES = 1;

    /**
     * 从byte中解析类型
     *
     * @param type 待解析的Inventory类型值
     * @return InventoryType
     * @throws TypeNotExistException 当类型不存在时，抛出该异常
     */
    public static InventoryType parse(byte type) {
        for (InventoryType t : InventoryType.values()) {
            if (t.value == type) {
                return t;
            }
        }
        throw new TypeNotExistException();
    }
}
