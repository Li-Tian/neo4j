package neo.ledger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Arrays;

import neo.UInt256;
import neo.csharp.Ulong;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.io.ICloneable;

public class BlockState extends StateBase implements ICloneable<BlockState> {

    public long systemFeeAmount;
    public TrimmedBlock trimmedBlock;

    @Override
    public int size() {
        return super.size() + trimmedBlock.size() + Ulong.BYTES;
    }

    @Override
    public BlockState copy() {
        BlockState state = new BlockState();
        state.systemFeeAmount = systemFeeAmount;
        state.trimmedBlock = trimmedBlock;
        return state;
    }

    @Override
    public void fromReplica(BlockState replica) {
        this.systemFeeAmount = replica.systemFeeAmount;
        this.trimmedBlock = replica.trimmedBlock;
    }

    @Override
    public void deserialize(BinaryReader reader) {
        super.deserialize(reader);
        systemFeeAmount = reader.readLong();
        trimmedBlock = reader.readSerializable(TrimmedBlock::new);
    }

    @Override
    public void serialize(BinaryWriter writer) {
        super.serialize(writer);
        writer.writeLong(systemFeeAmount);
        writer.writeSerializable(trimmedBlock);
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("sysfee_amount", systemFeeAmount);
        json.add("trimmed", trimmedBlock.toJson());
        return json;
    }


}
