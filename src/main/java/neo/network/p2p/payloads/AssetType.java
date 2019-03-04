package neo.network.p2p.payloads;


import neo.common.ByteEnum;

/**
 * 资产类型
 */
public enum AssetType implements ByteEnum {
    /**
     * 带有信任类型资产
     */
    CreditFlag((byte) 0x40),
    /**
     * 带有权益类型资产, 转账时还需要收款人进行签名
     */
    DutyFlag((byte) 0x80),

    /**
     * NEO 资产
     */
    GoverningToken((byte) 0x00),

    /**
     * GAS 资产
     */
    UtilityToken((byte) 0x01),

    /**
     * 未使用（保留）
     */
    Currency((byte) 0x08),

    /**
     * 股权类资产
     */
    Share((byte) (DutyFlag.value | (byte) 0x10)),

    /**
     * 票据类资产（保留）
     */
    Invoice((byte) (DutyFlag.value | (byte) 0x18)),


    /**
     * Token类资产
     */
    Token((byte) (DutyFlag.value | (byte) 0x20));

    /**
     * 占用字节数大小
     */
    public static final int BYTES = 1;

    private byte value;

    AssetType(byte val) {
        this.value = val;
    }

    /**
     * 查询资产类型的具体byte值
     */
    @Override
    public byte value() {
        return this.value;
    }


    /**
     * 从byte中解析资产类型
     *
     * @param type 待解析的资产类型
     * @return AssetType
     * @throws IllegalArgumentException 当类型不存在时，抛出该异常
     */
    public static AssetType parse(byte type) {
        return ByteEnum.parse(AssetType.values(), type);
    }

}
