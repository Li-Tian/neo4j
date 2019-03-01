package neo.ledger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Arrays;

import neo.UInt256;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.io.ICloneable;

public class HeaderHashList extends StateBase implements ICloneable<HeaderHashList> {

    public UInt256[] hashes;

    @Override
    public int size() {
        // TODO getvarsize
//        Size => base.Size + Hashes.GetVarSize();
        return super.size();
    }

    @Override
    public HeaderHashList copy() {
        HeaderHashList list = new HeaderHashList();
        list.hashes = hashes;
        return list;
    }

    @Override
    public void fromReplica(HeaderHashList replica) {
        this.hashes = replica.hashes;
    }

    @Override
    public void deserialize(BinaryReader reader) {
        super.deserialize(reader);
        hashes = reader.readArray(UInt256[]::new, UInt256::new);
    }

    @Override
    public void serialize(BinaryWriter writer) {
        super.serialize(writer);
        writer.writeArray(hashes);
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        JsonArray array = new JsonArray(hashes.length);
        Arrays.stream(hashes).forEach(p -> array.add(p.toString()));
        json.add("hashes", array);
        return json;
    }

}
