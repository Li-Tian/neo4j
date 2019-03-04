package neo.ledger;

import neo.common.ByteEnum;

/**
 * 对接受到的转发消息的处理结果描述
 */
public enum RelayResultReason implements ByteEnum {

    /**
     * 处理成功
     */
    Succeed((byte) 0),

    /**
     * 已经存在
     */
    AlreadyExists((byte) 1),

    /**
     * OOM错误
     */
    OutOfMemory((byte) 2),

    /**
     * 不能进行验证
     */
    Invalid((byte) 3),

    /**
     * 非法数据
     */
    PolicyFail((byte) 4),

    /**
     * 未知
     */
    Unknown((byte) 5);


    RelayResultReason(byte value) {
        this.value = value;
    }

    private byte value;

    @Override
    public byte value() {
        return value;
    }

    /**
     * 从byte中解析类型
     *
     * @param type 待解析的RelayResultReason类型值
     * @return RelayResultReason
     * @throws IllegalArgumentException 当类型不存在时，抛出该异常
     */
    public static RelayResultReason parse(byte type) {
        return ByteEnum.parse(RelayResultReason.values(), type);
    }
}
