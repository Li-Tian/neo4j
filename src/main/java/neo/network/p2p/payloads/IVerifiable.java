package neo.network.p2p.payloads;

import neo.UInt160;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;
import neo.persistence.Snapshot;
import neo.vm.IScriptContainer;

public interface IVerifiable extends ISerializable, IScriptContainer {

    Witness[] getWitnesses();

    byte[] getHashData();

    void setWitnesses(Witness[] witnesses);

    void deserializeUnsigned(BinaryReader reader);

    UInt160[] getScriptHashesForVerifying(Snapshot snapshot);

    void serializeUnsigned(BinaryWriter writer);

}
