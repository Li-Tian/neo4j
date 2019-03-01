package neo.ledger;

import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.io.ICloneable;

public class StorageItem extends StateBase implements ICloneable<StorageItem> {

    public byte[] value;
    public boolean isConstant;

    @Override
    public int size() {
        return super.size() + value.length + 1;
    }

    @Override
    public StorageItem copy() {
        StorageItem item = new StorageItem();
        item.isConstant = isConstant;
        item.value = value;
        return item;
    }

    @Override
    public void fromReplica(StorageItem replica) {
        this.value = replica.value;
        this.isConstant = replica.isConstant;
    }

    @Override
    public void serialize(BinaryWriter writer) {
        super.serialize(writer);
        writer.writeVarBytes(value);
        writer.writeBoolean(isConstant);
    }

    @Override
    public void deserialize(BinaryReader reader) {
        super.deserialize(reader);
        value = reader.readVarBytes();
        isConstant = reader.readBoolean();
    }
}
