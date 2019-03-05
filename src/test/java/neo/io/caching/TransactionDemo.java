package neo.io.caching;

import neo.UInt160;
import neo.UInt256;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.io.ICloneable;
import neo.network.p2p.payloads.IInventory;
import neo.network.p2p.payloads.InventoryType;
import neo.network.p2p.payloads.Witness;
import neo.persistence.Snapshot;

public class TransactionDemo implements IInventory, ICloneable<TransactionDemo> {

    public UInt256 value;

    public TransactionDemo(UInt256 value) {
        this.value = value;
    }

    public UInt256 hash() {
        return value;
    }

    @Override
    public InventoryType inventoryType() {
        return null;
    }

    @Override
    public boolean verify(Snapshot snapshot) {
        return false;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public void serialize(neo.csharp.io.BinaryWriter writer) {
        writer.writeSerializable(value);
    }

    @Override
    public void deserialize(neo.csharp.io.BinaryReader reader) {
        this.value = reader.readSerializable(() -> new UInt256());
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
    public Witness[] getWitnesses() {
        return new Witness[0];
    }

    @Override
    public byte[] getHashData() {
        return new byte[0];
    }

    @Override
    public void setWitnesses(Witness[] witnesses) {

    }

    @Override
    public void deserializeUnsigned(BinaryReader reader) {

    }

    @Override
    public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
        return new UInt160[0];
    }

    @Override
    public void serializeUnsigned(BinaryWriter writer) {

    }

    @Override
    public byte[] GetMessage() {
        return new byte[0];
    }
}
