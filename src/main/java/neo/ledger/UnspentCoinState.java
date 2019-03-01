package neo.ledger;

import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.io.ICloneable;

public class UnspentCoinState extends StateBase implements ICloneable<UnspentCoinState> {

    public byte[] items;

    @Override
    public int size() {
        return super.size() + items.length;
    }

    @Override
    public UnspentCoinState copy() {
        UnspentCoinState state = new UnspentCoinState();
        state.items = items;
        return state;
    }

    @Override
    public void fromReplica(UnspentCoinState replica) {
        this.items = replica.items;
    }

    @Override
    public void deserialize(BinaryReader reader) {
        super.deserialize(reader);
        items = reader.readVarBytes();
    }

    @Override
    public void serialize(BinaryWriter writer) {
        super.serialize(writer);
        writer.writeVarBytes(items);
    }

}
