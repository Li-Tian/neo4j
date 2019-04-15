package neo.ledger;


import java.util.HashMap;

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

    private static final HashMap<Byte, StorageFlags> map = new HashMap<>();

    static {
        for (StorageFlags type : StorageFlags.values()) {
            map.put(type.value, type);
        }
    }

    /**
     * 从byte中解析类型
     *
     * @param type 待解析的StorageFlags类型值
     * @return StorageFlags
     * @throws IllegalArgumentException 当类型不存在时，抛出该异常
     */
    public static StorageFlags parse(byte type) {
        if (map.containsKey(type)) {
            return map.get(type);
        }
        throw new IllegalArgumentException();
    }


    /**
     * check whether has the specific flag
     *
     * @param flag the specific flag
     * @return true - has, else false
     */
    public boolean hasFlag(StorageFlags flag) {
        return (this.value & flag.value) == flag.value;
    }
}
