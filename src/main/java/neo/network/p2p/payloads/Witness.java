package neo.network.p2p.payloads;


import com.google.gson.JsonObject;

import neo.UInt160;
import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;

/**
 * 见证人。<br/>
 *
 * @doc 验证时先读取验证脚本(VerificationScript)压入堆栈， 然后再读取执行脚本(InvocationScript)并压入堆栈， 然后执行并判定结果。
 */
public class Witness implements ISerializable {

    /**
     * 执行脚本，补全参数
     */
    public byte[] invocationScript;

    /**
     * 验证脚本
     */
    public byte[] verificationScript;

    private UInt160 scriptHash;

    /**
     * 验证脚本的哈希
     */
    public UInt160 scriptHash() {
        if (scriptHash == null) {
            scriptHash = UInt160.parseToScriptHash(verificationScript);
        }
        return scriptHash;
    }

    /**
     * 存储大小
     */
    @Override
    public int size() {
        // InvocationScript.GetVarSize() + VerificationScript.GetVarSize();
        return BitConverter.getVarSize(invocationScript) + BitConverter.getVarSize(verificationScript);
    }

    /**
     * 序列化
     *
     * @param writer 二进制输出器
     */
    @Override
    public void serialize(BinaryWriter writer) {
        writer.writeVarBytes(invocationScript);
        writer.writeVarBytes(verificationScript);
    }

    /**
     * 反序列化
     *
     * @param reader 二进制读入器
     */
    @Override
    public void deserialize(BinaryReader reader) {
        invocationScript = reader.readVarBytes(65536);
        verificationScript = reader.readVarBytes(65536);
    }

    /**
     * 转成json对象
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("invocation", BitConverter.toHexString(invocationScript));
        json.addProperty("invocation", BitConverter.toHexString(verificationScript));
        return json;
    }
}
