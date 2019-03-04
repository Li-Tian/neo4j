package neo.ledger;

import neo.exception.TypeNotExistException;

/**
 * 一个代表了当前NEO状态的enum类
 */
public enum CoinState {

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
     * @param type 待解析的CoinState类型值
     * @return CoinState
     * @throws TypeNotExistException 当类型不存在时，抛出该异常
     */
    public static CoinState parse(byte type) {
        for (CoinState t : CoinState.values()) {
            if (t.value == type) {
                return t;
            }
        }
        throw new TypeNotExistException();
    }
}
