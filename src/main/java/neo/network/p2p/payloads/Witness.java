package neo.network.p2p.payloads;


import com.google.gson.JsonObject;

import neo.UInt160;
import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;
import neo.log.notr.TR;

/**
 * Witness <br/>
 *
 * @doc When verifying, first read the verification script (VerificationScript)and push it onto the
 * stack. Then read the execution script (InvocationScript) and push it onto the stack. Then execute
 * and determine the result. The length of script cannot over 65536.
 */
public class Witness implements ISerializable {

    /**
     * Invocation script，push the required data into the stack
     */
    public byte[] invocationScript;

    /**
     * Verification scripts
     */
    public byte[] verificationScript;

    private UInt160 scriptHash;

    /**
     * Verification scripts hash
     */
    public UInt160 scriptHash() {
        TR.enter();
        if (scriptHash == null) {
            scriptHash = UInt160.parseToScriptHash(verificationScript);
        }
        return TR.exit(scriptHash);
    }

    /**
     * size for storage
     */
    @Override
    public int size() {
        TR.enter();
        // InvocationScript.GetVarSize() + VerificationScript.GetVarSize();
        return TR.exit(BitConverter.getVarSize(invocationScript) + BitConverter.getVarSize(verificationScript));
    }

    /**
     * serialize
     *
     * @param writer BinaryWriter
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        writer.writeVarBytes(invocationScript);
        writer.writeVarBytes(verificationScript);
        TR.exit();
    }

    /**
     * Deserialize
     *
     * @param reader BinaryReader
     */
    @Override
    public void deserialize(BinaryReader reader) {
        TR.enter();
        invocationScript = reader.readVarBytes(65536);
        verificationScript = reader.readVarBytes(65536);
        TR.exit();
    }

    /**
     * Convert to JObject object
     */
    public JsonObject toJson() {
        TR.enter();
        JsonObject json = new JsonObject();
        json.addProperty("invocation", BitConverter.toHexString(invocationScript));
        json.addProperty("verification", BitConverter.toHexString(verificationScript));
        return TR.exit(json);
    }
}
