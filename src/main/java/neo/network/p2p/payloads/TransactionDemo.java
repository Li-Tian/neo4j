package neo.network.p2p.payloads;

import java.io.IOException;

import neo.UInt256;
import neo.io.BinaryReader;
import neo.io.BinaryWriter;
import neo.io.ICloneable;
import neo.io.ISerializable;

public class TransactionDemo implements IInventory, ICloneable<TransactionDemo>, ISerializable {

    private UInt256 value;

    public TransactionDemo(UInt256 value) {
        this.value = value;
    }

    @Override
    public UInt256 hash() {
        return value;
    }

    @Override
    public InventoryType inventoryType() {
        return InventoryType.TR;
    }

    @Override
    public TransactionDemo copy() {
        return new TransactionDemo(value);
    }

    @Override
    public void fromReplica(TransactionDemo replica) {
        this.value = replica.value;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public void serialize(BinaryWriter writer) {

    }

    @Override
    public void deserialize(BinaryReader reader) {

    }

}
