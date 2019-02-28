package neo.network.p2p.payloads;

import neo.exception.TypeNotExistException;

public enum TransactionType {
    TR((byte) 0x01);

    private byte value;

    TransactionType(byte val) {
        this.value = val;
    }

    public byte value() {
        return this.value;
    }

    public static TransactionType parse(byte type) {
        if (type == TR.value) return TR;
        throw new TypeNotExistException();
    }

}
