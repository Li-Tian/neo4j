package neo.network.p2p.payloads;

import com.google.gson.JsonObject;

import neo.UInt256;
import neo.csharp.Ushort;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;

public class CoinReference implements ISerializable {

    public UInt256 prevHash;
    public Ushort prevIndex;

    @Override
    public int size() {
        return prevHash.size() + Ushort.BYTES; // ushort 1个字节
    }

    @Override
    public void serialize(BinaryWriter writer) {
        writer.writeSerializable(prevHash);
        writer.writeUshort(prevIndex);
    }

    @Override
    public void deserialize(BinaryReader reader) {
        prevHash = reader.readSerializable(() -> new UInt256());
        prevIndex = reader.readUshort();
    }

    @Override
    public int hashCode() {
        return prevHash.hashCode() + prevIndex.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null) return false;
        if (!(obj instanceof CoinReference)) return false;

        CoinReference other = (CoinReference) obj;
        return prevIndex.equals(other.prevIndex) && prevHash.equals(other.prevHash);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("txid", prevHash.toString());
        json.addProperty("vout", prevIndex);
        return json;
    }
}
