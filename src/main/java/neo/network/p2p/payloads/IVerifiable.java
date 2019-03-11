package neo.network.p2p.payloads;

import neo.UInt160;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;
import neo.log.tr.TR;
import neo.persistence.Snapshot;
import neo.vm.IScriptContainer;

/**
 * An interface for signature verification
 */
public interface IVerifiable extends ISerializable, IScriptContainer {

    /**
     * Get witnesses
     */
    Witness[] getWitnesses();

    /**
     * get the serialized data for the specified object
     *
     * @return serialized data
     */
    byte[] getHashData();

    /**
     * set witnesses
     */
    void setWitnesses(Witness[] witnesses);

    /**
     * Deserialize unsigned data
     *
     * @param reader BinaryReader
     */
    void deserializeUnsigned(BinaryReader reader);

    /**
     * Get the script hash collection for validation.
     *
     * @param snapshot Database Snapshot
     * @return script hash collection
     */
    UInt160[] getScriptHashesForVerifying(Snapshot snapshot);

    /**
     * Serialize unsigned data
     *
     * @param writer BinaryWriter
     */
    void serializeUnsigned(BinaryWriter writer);

    /**
     * verify witness
     *
     * @param verifiable the verifiable object
     * @param snapshot   database snapshot
     * @return true if verify success, otherwise false.
     */
    static boolean verifyWitnesses(IVerifiable verifiable, Snapshot snapshot) {
        // TODO waiting for smartcontract
        TR.fixMe("waiting for smartcontract....");

        return true;

        // C# code:
        //        UInt160[] hashes;
        //        try
        //        {
        //            hashes = verifiable.GetScriptHashesForVerifying(snapshot);
        //        }
        //        catch (InvalidOperationException)
        //        {
        //            return false;
        //        }
        //        if (hashes.Length != verifiable.Witnesses.Length) return false;
        //        for (int i = 0; i < hashes.Length; i++)
        //        {
        //            byte[] verification = verifiable.Witnesses[i].VerificationScript;
        //            if (verification.Length == 0)
        //            {
        //                using (ScriptBuilder sb = new ScriptBuilder())
        //                {
        //                    sb.EmitAppCall(hashes[i].ToArray());
        //                    verification = sb.ToArray();
        //                }
        //            }
        //            else
        //            {
        //                if (hashes[i] != verifiable.Witnesses[i].ScriptHash) return false;
        //            }
        //            using (ApplicationEngine engine = new ApplicationEngine(TriggerType.Verification, verifiable, snapshot, Fixed8.Zero))
        //            {
        //                engine.LoadScript(verification);
        //                engine.LoadScript(verifiable.Witnesses[i].InvocationScript);
        //                if (!engine.Execute()) return false;
        //                if (engine.ResultStack.Count != 1 || !engine.ResultStack.Pop().GetBoolean()) return false;
        //            }
        //        }
        //        return true;
    }

}
