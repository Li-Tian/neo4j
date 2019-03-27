package neo.network.p2p.payloads;

import java.io.ByteArrayOutputStream;

import neo.Fixed8;
import neo.UInt160;
import neo.Wallets.KeyPair;
import neo.cryptography.Crypto;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;
import neo.exception.InvalidOperationException;
import neo.log.tr.TR;
import neo.persistence.Snapshot;
import neo.smartcontract.ApplicationEngine;
import neo.smartcontract.TriggerType;
import neo.vm.IScriptContainer;
import neo.vm.ScriptBuilder;

/**
 * An interface for signature verification
 */
public interface IVerifiable extends ISerializable, IScriptContainer {

    /**
     * Get witnesses
     */
    Witness[] getWitnesses();

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
     * sign the verifiable object
     *
     * @param verifiable object to be sign
     * @param key        key pair
     * @return signature
     */
    static byte[] sign(IVerifiable verifiable, KeyPair key) {
        TR.enter();
        byte[] tempByteArray = new byte[20];
        System.arraycopy(key.publicKey.getEncoded(false), 1, tempByteArray, 0, 20);
        return TR.exit(Crypto.Default.sign(IVerifiable.getHashData(verifiable), key.privateKey, tempByteArray));
    }

    /**
     * verify witness
     *
     * @param verifiable the verifiable object
     * @param snapshot   database snapshot
     * @return true if verify success, otherwise false.
     */
    static boolean verifyWitnesses(IVerifiable verifiable, Snapshot snapshot) {
        TR.enter();

        UInt160[] hashes;
        try {
            hashes = verifiable.getScriptHashesForVerifying(snapshot);
        } catch (InvalidOperationException e) {
            // just return false
            TR.error(e);
            return TR.exit(false);
        }

        Witness[] witnesses = verifiable.getWitnesses();

        if (hashes.length != witnesses.length) {
            return TR.exit(false);
        }

        for (int i = 0; i < hashes.length; i++) {
            byte[] verification = witnesses[i].verificationScript;
            if (verification.length == 0) {
                ScriptBuilder sb = new ScriptBuilder();
                sb.emitAppCall(hashes[i].toArray());
                verification = sb.toArray();
            } else {
                if (hashes[i] != witnesses[i].scriptHash()) {
                    return TR.exit(false);
                }
            }
            ApplicationEngine engine = new ApplicationEngine(TriggerType.Verification, verifiable, snapshot, Fixed8.ZERO);
            engine.loadScript(verification);
            engine.loadScript(witnesses[i].invocationScript);

            if (!engine.execute2()) {
                return TR.exit(false);
            }
            if (engine.resultStack.getCount() != 1 || !engine.resultStack.pop().getBoolean()) {
                return TR.exit(false);
            }

        }
        return TR.exit(true);
    }

    /**
     * get the serialized data for the specified object
     *
     * @param verifiable specified object
     * @return serialized data
     */
    static byte[] getHashData(IVerifiable verifiable) {
        TR.enter();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BinaryWriter writer = new BinaryWriter(outputStream);
        verifiable.serializeUnsigned(writer);
        writer.flush();
        return TR.exit(outputStream.toByteArray());
    }

}
