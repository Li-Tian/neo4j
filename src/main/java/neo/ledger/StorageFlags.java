package neo.ledger;


import neo.csharp.common.ByteEnum;

/**
 * 存储标记
 */
public enum StorageFlags implements ByteEnum {

    /**
     * 无特殊标记
     */
    None((byte) 0x00),

    /**
     * 常量（一次写入不可修改）
     */
    Constant((byte) 0x01);

    private byte value;

    StorageFlags(byte value) {
        this.value = value;
    }

    @Override
    public byte value() {
        return value;
    }

    /**
     * 从byte中解析类型
     *
     * @param type 待解析的StorageFlags类型值
     * @return StorageFlags
     * @throws IllegalArgumentException 当类型不存在时，抛出该异常
     */
    public static StorageFlags parse(byte type) {
        return ByteEnum.parse(StorageFlags.values(), type);
    }
}
