package neo.ledger;

import com.google.gson.JsonObject;

import neo.csharp.Uint;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.io.ICloneable;
import neo.network.p2p.payloads.Transaction;

public class TransactionState extends StateBase implements ICloneable<TransactionState> {

    public Uint blockIndex;
    public Transaction transaction;

    @Override
    public int size() {
        return super.size() + Uint.BYTES + transaction.size();
    }

    @Override
    public TransactionState copy() {
        TransactionState copy = new TransactionState();
        copy.blockIndex = blockIndex;
        copy.transaction = transaction;
        return copy;
    }

    @Override
    public void fromReplica(TransactionState replica) {
        this.blockIndex = replica.blockIndex;
        this.transaction = replica.transaction;
    }

    @Override
    public void deserialize(BinaryReader reader) {
        super.deserialize(reader);
        blockIndex = reader.readUint();
        transaction = Transaction.deserializeFrom(reader);
    }

    @Override
    public void serialize(BinaryWriter writer) {
        super.serialize(writer);
        writer.writeUint(blockIndex);
        writer.writeSerializable(transaction);
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("height", blockIndex);
        json.add("tx", transaction.toJson());
        return json;
    }
}
