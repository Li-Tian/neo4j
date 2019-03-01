package neo.network.p2p.payloads;

import com.google.gson.JsonObject;

import neo.csharp.Uint;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.FormatException;
import neo.ledger.Blockchain;

public class MinerTransaction extends Transaction {

    public Uint nonce;

    public MinerTransaction() {
        super(TransactionType.MinerTransaction);
    }

    @Override
    public int size() {
        return super.size() + Uint.BYTES;
    }

    @Override
    protected void deserializeExclusiveData(BinaryReader reader) {
        if (version != 0) throw new FormatException();
        this.nonce = reader.readUint();
    }

    @Override
    protected void onDeserialized() {
        super.onDeserialized();
        if (inputs.length != 0)
            throw new FormatException();
        for (TransactionOutput output : outputs) {
            if (output.assetId == Blockchain.UtilityToken.hash()) {
                throw new FormatException();
            }
        }
    }

    @Override
    protected void serializeExclusiveData(BinaryWriter writer) {
        writer.writeUint(nonce);
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("nonce", nonce);
        return json;
    }
}
