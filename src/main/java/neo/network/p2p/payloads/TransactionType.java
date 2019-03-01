package neo.network.p2p.payloads;


import neo.exception.TypeNotExistException;

public enum TransactionType {

    MinerTransaction((byte) 0x00),
    IssueTransaction((byte) 0x01),
    ClaimTransaction((byte) 0x02),
    EnrollmentTransaction((byte) 0x20),
    RegisterTransaction((byte) 0x40),
    ContractTransaction((byte) 0x80),
    StateTransaction((byte) 0x90),
    PublishTransaction((byte) 0xd0),
    InvocationTransaction((byte) 0xd1);

    private byte value;

    TransactionType(byte val) {
        this.value = val;
    }

    public byte value() {
        return this.value;
    }

    public static TransactionType parse(byte type) {
        for (TransactionType t : TransactionType.values()) {
            if (t.value == type) {
                return t;
            }
        }
        throw new TypeNotExistException();
    }

    /**
     * 占用字节数大小
     */
    public static final int BYTES = 1;
}
