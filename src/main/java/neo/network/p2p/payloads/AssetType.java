package neo.network.p2p.payloads;


import neo.common.ByteEnum;

/**
 * asset type
 */
public enum AssetType implements ByteEnum {
    /**
     * Asset with Credit type
     */
    CreditFlag((byte) 0x40),
    /**
     * Duty type asset, the payee is also required to sign the transfer.
     */
    DutyFlag((byte) 0x80),

    /**
     * NEO asset
     */
    GoverningToken((byte) 0x00),

    /**
     * GAS asset
     */
    UtilityToken((byte) 0x01),

    /**
     * Not used (reserved)
     */
    Currency((byte) 0x08),

    /**
     * Equity type assets
     */
    Share((byte) (DutyFlag.value | (byte) 0x10)),

    /**
     * Invoice type assets(reserved)
     */
    Invoice((byte) (DutyFlag.value | (byte) 0x18)),


    /**
     * Token type assets
     */
    Token((byte) (DutyFlag.value | (byte) 0x20));

    /**
     * Storage size
     */
    public static final int BYTES = 1;

    private byte value;

    AssetType(byte val) {
        this.value = val;
    }

    /**
     * get the byte value of the asset
     */
    @Override
    public byte value() {
        return this.value;
    }


    /**
     * parse AssetTye from byte value
     *
     * @param type asset type's byte value
     * @return AssetType
     * @throws IllegalArgumentException throw it if the type not exist.
     */
    public static AssetType parse(byte type) {
        return ByteEnum.parse(AssetType.values(), type);
    }

}
