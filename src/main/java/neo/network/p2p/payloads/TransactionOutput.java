package neo.network.p2p.payloads;

import com.google.gson.JsonObject;

import neo.Fixed8;
import neo.UInt160;
import neo.UInt256;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;
import neo.exception.FormatException;

/**
 * The transaction Output
 */
public class TransactionOutput implements ISerializable {

    /**
     * The asset Id
     */
    public UInt256 assetId;

    /**
     * The amount to be transfer
     */
    public Fixed8 value;

    /**
     * The recipient address script hash
     */
    public UInt160 scriptHash;

    /**
     * The size of storage
     */
    @Override
    public int size() {
        return assetId.size() + value.size() + scriptHash.size();
    }

    /**
     * serialize
     *
     * @param writer BinaryWriter
     */
    @Override
    public void serialize(BinaryWriter writer) {
        writer.writeSerializable(assetId);
        writer.writeSerializable(value);
        writer.writeSerializable(scriptHash);
    }

    /**
     * deserialize
     *
     * @param reader BinaryReader
     * @throws FormatException if value less than 0, throw this exception
     */
    @Override
    public void deserialize(BinaryReader reader) {
        this.assetId = reader.readSerializable(UInt256::new);
        this.value = reader.readSerializable(Fixed8::new);
        if (value.compareTo(Fixed8.ZERO) <= 0) throw new FormatException();
        this.scriptHash = reader.readSerializable(UInt160::new);
    }

    /**
     * Transfer to json object
     *
     * @param index The index of UTXO in the transaction list, begin from 0
     * @return json object
     */
    public JsonObject toJson(int index) {
        JsonObject json = new JsonObject();
        json.addProperty("n", index);
        json.addProperty("asset", assetId.toString());
        json.addProperty("value", value.toString());
        json.addProperty("address", scriptHash.toAddress());
        return json;
    }
}
