package neo.network.p2p.payloads;

import com.google.gson.JsonObject;

import neo.Fixed8;
import neo.UInt160;
import neo.UInt256;
import neo.csharp.Ushort;
import neo.io.BinaryReader;
import neo.io.BinaryWriter;
import neo.io.ISerializable;

public class TransactionOutput implements ISerializable {

    public UInt256 assetId;
    public Fixed8 value;
    public UInt160 scriptHash;

    @Override
    public int size() {
        return assetId.size() + value.size() + scriptHash.size();
    }

    @Override
    public void serialize(BinaryWriter writer) {
        writer.writeSerializable(assetId);
        writer.writeSerializable(value);
        writer.writeSerializable(scriptHash);
    }

    @Override
    public void deserialize(BinaryReader reader) {
        this.assetId = reader.readSerializable(() -> new UInt256());
        this.value = reader.readSerializable(() -> new Fixed8());
        if (value.compareTo(Fixed8.ZERO) <= 0) throw new IllegalArgumentException();
        this.scriptHash = reader.readSerializable(() -> new UInt160());
    }

    public JsonObject toJson(Ushort index) {
        JsonObject json = new JsonObject();
        json.addProperty("n", index);
        json.addProperty("asset", assetId.toString());
        json.addProperty("value", value.toString());
        json.addProperty("address", scriptHash.toString()); // TODO ScriptHash.ToAddress();
        return json;
    }
}
