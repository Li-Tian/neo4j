package neo.network.p2p.payloads;


import neo.UInt256;
import neo.exception.TypeNotExistException;
import neo.function.FuncVoid2T;

public enum InventoryType {

    TR((byte)0x01, () -> new TransactionDemo(UInt256.Zero)),
    // TODO
    BLOCK((byte)0x02, ()-> new TransactionDemo(UInt256.Zero)),
    // TODO
    CONSENSUS((byte)0x03, ()-> new TransactionDemo(UInt256.Zero));

    private byte value;
    private FuncVoid2T factory;

    InventoryType(byte val, FuncVoid2T factory) {
        this.value = val;
        this.factory = factory;
    }

    public byte value() {
        return this.value;
    }

    public<T> T gen(){
        return (T) this.factory.gen();
    }

    public static InventoryType parse(byte type){
        if (type == TR.value) return TR;
        if (type == BLOCK.value) return BLOCK;
        if (type == CONSENSUS.value) return CONSENSUS;

        throw new TypeNotExistException();
    }
}
