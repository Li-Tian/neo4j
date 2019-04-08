package neo.ledger;

import neo.common.ByteFlag;

/**
 * 一个代表了当前NEO状态的enum类
 */
public class CoinState extends ByteFlag {

    /**
     * 未确认的
     */
    public static final CoinState Unconfirmed = new CoinState((byte) 0);

    /**
     * 已确认的
     */
    public static final CoinState Confirmed = new CoinState((byte) (1 << 0));

    /**
     * 已经被支付给他人的
     */
    public static final CoinState Spent = new CoinState((byte) (1 << 1));

    /**
     * 已经被Claimed
     */
    public static final CoinState Claimed = new CoinState((byte) (1 << 3));

    /**
     * 锁仓中的
     */
    public static final CoinState Frozen = new CoinState((byte) (1 << 5));


    /**
     * 构造函数
     *
     * @param value 标志位的值
     */
    public CoinState(byte value) {
        super(value);
    }


    /**
     * 状态或操作
     *
     * @param other 或操作对象
     * @return 新的状态
     */
    public CoinState OR(CoinState other) {
        return new CoinState((byte) (this.value | other.value));
    }
}
