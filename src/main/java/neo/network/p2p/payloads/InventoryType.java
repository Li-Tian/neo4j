package neo.network.p2p.payloads;


import neo.exception.TypeNotExistException;

public enum InventoryType {

    Tr((byte) 0x01),
    // TODO
    Block((byte) 0x02),
    // TODO
    Consensus((byte) 0x03);

    private byte value;

    InventoryType(byte val) {
        this.value = val;
    }

    public byte value() {
        return this.value;
    }


    public static InventoryType parse(byte type) {
        if (type == Tr.value) return Tr;
        if (type == Block.value) return Block;
        if (type == Consensus.value) return Consensus;

        throw new TypeNotExistException();
    }
}
