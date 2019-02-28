package neo.network.p2p.payloads;


import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;

import neo.UInt160;
import neo.UInt256;
import neo.cryptography.Crypto;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.io.BinaryReader;
import neo.io.BinaryWriter;
import neo.persistence.Snapshot;


public abstract class BlockBase implements IVerifiable {

    public Uint version;
    public UInt256 prevHash;
    public UInt256 merkleRoot;
    public Uint timestamp;
    public Uint index;
    public Ulong consensusData;
    public UInt160 nextConsensus;
    public Witness witness;

    private UInt256 hash = null;

    public UInt256 hash() {
        if (hash == null) {
            hash = new UInt256(Crypto.Default.hash256(this.GetMessage()));
        }
        return hash;
    }


    @Override
    public Witness[] getWitnesses() {
        return new Witness[]{witness};
    }

    @Override
    public void setWitnesses(Witness[] witnesses) {
        if (witnesses.length != 1) throw new IllegalArgumentException();

    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public void deserialize(BinaryReader reader) {
        this.deserializeUnsigned(reader);
        if (reader.readByte() != 1) throw new IllegalArgumentException();
        witness = reader.readSerializable(() -> new Witness());
    }


    @Override
    public void deserializeUnsigned(BinaryReader reader) {
        version = reader.readUint();
        prevHash = reader.readSerializable(() -> new UInt256());
        merkleRoot = reader.readSerializable(() -> new UInt256());
        timestamp = reader.readUint();
        index = reader.readUint();
        consensusData = reader.readUlong();
        nextConsensus = reader.readSerializable(() -> new UInt160());
    }


    @Override
    public void serialize(BinaryWriter writer) {
        this.serializeUnsigned(writer);
        writer.writeByte((byte) 1);
        writer.writeSerializable(witness);
    }

    @Override
    public void serializeUnsigned(BinaryWriter writer) {
        writer.writeUint(version);
        writer.writeSerializable(prevHash);
        writer.writeSerializable(merkleRoot);
        writer.writeUint(timestamp);
        writer.writeUint(index);
        writer.writeUlong(consensusData);
        writer.writeSerializable(nextConsensus);
    }

    @Override
    public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
        if (prevHash == UInt256.Zero) {
            return new UInt160[]{witness.scriptHash()};
        }
        // TODO
        return new UInt160[0];
//        Header prev_header = snapshot.GetHeader(PrevHash);
//        if (prev_header == null) throw new InvalidOperationException();
//        return new UInt160[]{prev_header.NextConsensus};
    }

    @Override
    public byte[] GetMessage() {
        return this.getHashData();
    }

    @Override
    public byte[] getHashData() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BinaryWriter writer = new BinaryWriter(outputStream);
        serializeUnsigned(writer);
        writer.flush();
        return outputStream.toByteArray();
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("hash", hash().toString());
        jsonObject.addProperty("size", size());
        jsonObject.addProperty("version", version);
        jsonObject.addProperty("previousblockhash", prevHash.toString());
        jsonObject.addProperty("merkleroot", merkleRoot.toString());
        jsonObject.addProperty("time", timestamp);
        jsonObject.addProperty("index", index);
        jsonObject.addProperty("nonce", consensusData.toString());
        jsonObject.addProperty("nextconsensus", nextConsensus.toString());
        jsonObject.add("script", witness.toJson());
        return jsonObject;
    }

    public boolean verify(Snapshot snapshot) {
//        Header prev_header = snapshot.GetHeader(PrevHash);
//        if (prev_header == null) return false;
//        if (prev_header.Index + 1 != Index) return false;
//        if (prev_header.Timestamp >= Timestamp) return false;
//        if (!this.VerifyWitnesses(snapshot)) return false;
        return true;
    }

}
