package neo.network.p2p.payloads;

public enum InventoryType {

    TR((byte)0x01),
    BLOCK((byte)0x02),
    CONSENSUS((byte)0x03);

    private byte value;

    InventoryType(byte val) {
        this.value = val;
    }

    public byte value() {
        return this.value;
    }
};
