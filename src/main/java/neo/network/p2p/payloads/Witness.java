package neo.network.p2p.payloads;


import com.google.gson.JsonObject;

import neo.UInt160;
import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;

public class Witness implements ISerializable {

    public byte[] invocationScript;
    public byte[] verificationScript;

    private UInt160 scriptHash;

    public UInt160 scriptHash() {
        if (scriptHash == null) {
            scriptHash = UInt160.parseToScriptHash(verificationScript);
        }
        return scriptHash;
    }

    @Override
    public int size() {
        // InvocationScript.GetVarSize() + VerificationScript.GetVarSize();
        return BitConverter.getVarSize(invocationScript) + BitConverter.getVarSize(verificationScript);
    }

    @Override
    public void serialize(BinaryWriter writer) {
        writer.writeVarBytes(invocationScript);
        writer.writeVarBytes(verificationScript);
    }

    @Override
    public void deserialize(BinaryReader reader) {
        invocationScript = reader.readVarBytes(65536);
        verificationScript = reader.readVarBytes(65536);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("invocation", BitConverter.toHexString(invocationScript));
        json.addProperty("invocation", BitConverter.toHexString(verificationScript));
        return json;
    }
}
