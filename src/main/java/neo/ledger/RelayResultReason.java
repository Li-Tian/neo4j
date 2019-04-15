package neo.ledger;


import java.util.HashMap;

import neo.csharp.common.ByteEnum;
import neo.network.p2p.payloads.InventoryType;

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
    UnableToVerify((byte) 3),

    /**
     * 非法数据
     */
    Invalid((byte) 4),

    /**
     * 策略失效
     */
    PolicyFail((byte) 5),

    /**
     * 未知
     */
    Unknown((byte) 6);


    RelayResultReason(byte value) {
        this.value = value;
    }

    private byte value;

    @Override
    public byte value() {
        return value;
    }


    private static final HashMap<Byte, RelayResultReason> map = new HashMap<>();

    static {
        for (RelayResultReason type : RelayResultReason.values()) {
            map.put(type.value, type);
        }
    }

    /**
     * 从byte中解析类型
     *
     * @param type 待解析的RelayResultReason类型值
     * @return RelayResultReason
     * @throws IllegalArgumentException 当类型不存在时，抛出该异常
     */
    public static RelayResultReason parse(byte type) {
        if (map.containsKey(type)) {
            return map.get(type);
        }
        throw new IllegalArgumentException();
    }

}
