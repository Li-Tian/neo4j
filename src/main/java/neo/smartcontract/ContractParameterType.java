package neo.smartcontract;


import neo.csharp.common.ByteEnum;

/**
 * 合约参数类型，为byte类型
 */
public enum ContractParameterType implements ByteEnum {

    /**
     * 签名类型
     */
    Signature((byte) 0x00),

    /**
     * 布尔类型
     */
    Boolean((byte) 0x01),

    /**
     * 整型
     */
    Integer((byte) 0x02),

    /**
     * Hash160类型
     */
    Hash160((byte) 0x03),

    /**
     * Hash256类型
     */
    Hash256((byte) 0x04),

    /**
     * 字节数组类型
     */
    ByteArray((byte) 0x05),

    /**
     * 公钥类型
     */
    PublicKey((byte) 0x06),

    /**
     * 字符串类型
     */
    String((byte) 0x07),

    /**
     * 数组类型
     */
    Array((byte) 0x10),

    /**
     * Map类型
     */
    Map((byte) 0x12),

    /**
     * 互操作接口类型
     */
    InteropInterface((byte) 0xf0),

    /**
     * Void类型
     */
    Void((byte) 0xff);


    private byte value;

    ContractParameterType(byte value) {
        this.value = value;
    }

    /**
     * 获取类型值
     */
    @Override
    public byte value() {
        return value;
    }

    /**
     * 从byte值中，解析出对应的类型
     *
     * @param type 类型值
     * @return ContractParameterType
     * @throws IllegalArgumentException 不存在的类型，当type值不存在时
     */
    public static ContractParameterType parse(byte type) {
        return ByteEnum.parse(ContractParameterType.values(), type);
    }

    /**
     * 从byte值中，解析出对应的类型列表
     *
     * @param types 类型值列表
     * @return ContractParameterType[]
     * @throws IllegalArgumentException 不存在的类型，当type值不存在时
     */
    public static ContractParameterType[] parse(byte[] types) {
        ContractParameterType[] values = new ContractParameterType[types.length];
        for (int i = 0; i < types.length; i++) {
            values[i] = parse(types[i]);
        }
        return values;
    }

    /**
     * 将类型转化成byte值数组
     *
     * @param types 类型数组
     * @return byte[]
     */
    public static byte[] toBytes(ContractParameterType[] types) {
        return ByteEnum.toBytes(types);
    }
}
