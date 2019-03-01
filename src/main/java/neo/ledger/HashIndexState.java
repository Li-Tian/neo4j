package neo.ledger;

import com.google.gson.JsonObject;

import neo.UInt256;
import neo.csharp.Uint;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.io.ICloneable;

public class HashIndexState extends StateBase implements ICloneable<HashIndexState> {

    public UInt256 hash = UInt256.Zero;
    public Uint index = Uint.MAX_VALUE_2;

    @Override
    public int size() {
        return super.size() + hash.size() + Uint.BYTES;
    }

    @Override
    public HashIndexState copy() {
        HashIndexState state = new HashIndexState();
        state.hash = hash;
        state.index = index;
        return state;
    }

    @Override
    public void fromReplica(HashIndexState replica) {
        hash = replica.hash;
        index = replica.index;
    }

    @Override
    public void deserialize(BinaryReader reader) {
        super.deserialize(reader);
        hash = reader.readSerializable(UInt256::new);
        index = reader.readUint();
    }

    @Override
    public void serialize(BinaryWriter writer) {
        super.serialize(writer);
        writer.writeSerializable(hash);
        writer.writeUint(index);
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("hash", hash.toString());
        json.addProperty("index", index);
        return json;
    }
}
