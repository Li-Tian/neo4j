package neo.ledger;

import com.google.gson.JsonObject;

import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;
import neo.log.tr.TR;

public abstract class StateBase implements ISerializable {
    public final byte stateVersion = 0;

    @Override
    public int size() {
        return TR.exit(Byte.BYTES);
    }
    @Override
    public void deserialize(BinaryReader reader) {
        TR.enter();
        if (reader.readByte() != stateVersion) {
            TR.exit();
            throw new NumberFormatException();
        }
        TR.exit();
    }

    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        writer.writeByte(this.stateVersion);
        TR.exit();
    }

    public JsonObject toJson() {
        TR.enter();
        JsonObject json = new JsonObject();
        json.addProperty("version", stateVersion);
        return TR.exit(json);
    }
}