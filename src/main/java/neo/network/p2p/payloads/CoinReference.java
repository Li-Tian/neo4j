package neo.network.p2p.payloads;

import com.google.gson.JsonObject;

import neo.UInt256;
import neo.csharp.Ushort;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;

/**
 * CoinReference
 */
public class CoinReference implements ISerializable {

    /**
     * The transaction hash in which UTXO  point to
     */
    public UInt256 prevHash;

    /**
     * The transaction output index in which UTXO point to. Starting from 0
     */
    public Ushort prevIndex;

    /**
     * size for storage
     */
    @Override
    public int size() {
        return prevHash.size() + Ushort.BYTES; // ushort 2个字节
    }

    /**
     * Serialize
     *
     * @param writer BinaryWriter
     */
    @Override
    public void serialize(BinaryWriter writer) {
        writer.writeSerializable(prevHash);
        writer.writeUshort(prevIndex);
    }


    /**
     * Deserialize
     *
     * @param reader BinaryReader
     */
    @Override
    public void deserialize(BinaryReader reader) {
        prevHash = reader.readSerializable(UInt256::new);
        prevIndex = reader.readUshort();
    }

    /**
     * get hash code
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return prevHash.hashCode() + prevIndex.hashCode();
    }

    /**
     * Determine if two CoinReference object are equal
     *
     * @param obj Object to be compared
     * @return return as following：
     * <ul>
     * <li>returns false if the object to be compared is null or not a CoinReference object</li>
     * <li>If the other CoinReference object is null, it returns false.</li>
     * <li>Otherwise compare the transaction hash  and the output index of the transaction in which
     * UTXO point to</li>
     * </ul>
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null) return false;
        if (!(obj instanceof CoinReference)) return false;

        CoinReference other = (CoinReference) obj;
        return prevIndex.equals(other.prevIndex) && prevHash.equals(other.prevHash);
    }

    /**
     * Convert to JObject object
     *
     * @return JObject object
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("txid", prevHash.toString());
        json.addProperty("vout", prevIndex);
        return json;
    }
}
