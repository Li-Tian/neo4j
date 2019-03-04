package neo.network.p2p.payloads;

import neo.common.ByteEnum;

/**
 * StateTransaction类型
 */
public enum StateType implements ByteEnum {
    /**
     * 投票, 值 0x40
     */
    Account((byte) 0x40),


    /**
     * 申请验证人，值0x48
     */
    Validator((byte) 0x48);


    /**
     * 占用字节数大小
     */
    public static final int BYTES = Byte.BYTES;

    private byte value;

    StateType(byte val) {
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
     * 从byte中解析StateTransaction类型
     *
     * @param type StateTransaction类型
     * @return StateType
     * @throws IllegalArgumentException 当类型不存在时，抛出该异常
     */
    public static StateType parse(byte type) {
        return ByteEnum.parse(StateType.values(), type);
    }
}
