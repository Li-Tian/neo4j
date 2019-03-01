package neo.network.p2p.payloads;

public final class StateType {
    public static final byte Account = 0x40;
    public static final byte Validator = 0x48;

    /**
     * 存储大小一个字节
     */
    public static final int BYTES = 1;

    public static boolean contain(byte type) {
        return type == Account || type == Validator;
    }
}
