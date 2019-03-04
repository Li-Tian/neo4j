package neo.ledger;

import neo.common.ByteEnum;

/**
 * 一个代表了当前NEO状态的enum类
 */
public enum CoinState implements ByteEnum {

    /**
     * 未确认的
     */
    Unconfirmed((byte) 0),

    /**
     * 已确认的
     */
    Confirmed((byte) (1 << 0)),

    /**
     * 已经被支付给他人的
     */
    Spent((byte) (1 << 1)),

    /**
     * 已经被Claimed
     */
    Claimed((byte) (1 << 3)),

    /**
     * 锁仓中的
     */
    Frozen((byte) (1 << 5));


    private byte value;

    CoinState(byte val) {
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
     * @param type 待解析的CoinState类型值
     * @return CoinState
     * @throws IllegalArgumentException 当类型不存在时，抛出该异常
     */
    public static CoinState parse(byte type) {
        return ByteEnum.parse(CoinState.values(), type);
    }
}
