package neo.network.p2p.payloads;

import neo.exception.TypeNotExistException;

/**
 * StateTransaction类型
 */
public enum StateType {
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
    public static final int BYTES = 1;

    private byte value;

    StateType(byte val) {
        this.value = val;
    }

    /**
     * 查询资产类型的具体byte值
     */
    public byte value() {
        return this.value;
    }


    /**
     * 从byte中解析StateTransaction类型
     *
     * @param type StateTransaction类型
     * @return StateType
     * @throws TypeNotExistException 当类型不存在时，抛出该异常
     */
    public static StateType parse(byte type) {
        for (StateType t : StateType.values()) {
            if (t.value == type) {
                return t;
            }
        }
        throw new TypeNotExistException();
    }
}
