package neo.network.p2p.payloads;


import com.google.gson.JsonObject;


import neo.UInt160;
import neo.UInt256;
import neo.cryptography.Crypto;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.InvalidOperationException;
import neo.persistence.Snapshot;

import neo.log.notr.TR;

/**
 * Block base class
 */
public abstract class BlockBase implements IVerifiable {

    /**
     * Block VERSION
     */
    public Uint version = Uint.ZERO;

    /**
     * Previous block hash
     */
    public UInt256 prevHash;

    /**
     * Merkle root
     */
    public UInt256 merkleRoot;

    /**
     * Timestamp
     */
    public Uint timestamp;

    /**
     * Block height
     */
    public Uint index;

    /**
     * Consensus additional data, the default is block NONCE. a pseudo-random number generated by
     * the speaker when the block is released
     */
    public Ulong consensusData;

    /**
     * Next block consensus address，two-thirds multiparty signed contract address of consensus
     * nodes
     */
    public UInt160 nextConsensus;

    /**
     * Witness
     */
    public Witness witness;

    private UInt256 hash = null;

    /**
     * Block hash
     *
     * @return UInt256
     */
    public UInt256 hash() {
        TR.enter();
        if (hash == null) {
            hash = new UInt256(Crypto.Default.hash256(this.getMessage()));
        }
        return TR.exit(hash);
    }


    /**
     * Get Witness list
     *
     * @return Witness[]
     */
    @Override
    public Witness[] getWitnesses() {
        TR.enter();
        return TR.exit(new Witness[]{witness});
    }

    /**
     * Set Witness list
     */
    @Override
    public void setWitnesses(Witness[] witnesses) {
        TR.enter();
        if (witnesses.length != 1) throw new IllegalArgumentException();
        this.witness = witnesses[0];
        TR.exit();
    }


    /**
     * the size of storage
     */
    @Override
    public int size() {
        TR.enter();
        // C# code  Size => sizeof(uint) + PrevHash.Size + MerkleRoot.Size + sizeof(uint) + sizeof(uint) + sizeof(ulong) + NextConsensus.Size + 1 + Witness.Size;
        // 4 + 32 + 32 + 4 + 4 + 8 + 20 + 1 + （1+2+1+2） =105 + 6 => 111
        return TR.exit(Uint.BYTES + prevHash.size() + merkleRoot.size() + Uint.BYTES
                + Uint.BYTES + Ulong.BYTES + nextConsensus.size() + 1 + witness.size());
    }

    /**
     * Deserialize method
     *
     * @param reader BinaryReader
     */
    @Override
    public void deserialize(BinaryReader reader) {
        TR.enter();
        this.deserializeUnsigned(reader);
        if (reader.readByte() != 1) throw new IllegalArgumentException();
        witness = reader.readSerializable(Witness::new);
        TR.exit();
    }


    /**
     * Deserialize method（Block header）
     *
     * @param reader BinaryReader
     */
    @Override
    public void deserializeUnsigned(BinaryReader reader) {
        TR.enter();
        version = reader.readUint();
        prevHash = reader.readSerializable(UInt256::new);
        merkleRoot = reader.readSerializable(UInt256::new);
        timestamp = reader.readUint();
        index = reader.readUint();
        consensusData = reader.readUlong();
        nextConsensus = reader.readSerializable(UInt160::new);
        TR.exit();
    }

    /**
     * Serialize
     *
     * <p>fields</p>
     * <ul>
     * <li>Version: Version</li>
     * <li>PrevHash: Previous block hash</li>
     * <li>MerkleRoot: Merkle root</li>
     * <li>Timestamp: Timestamp</li>
     * <li>Index: Block height</li>
     * <li>ConsensusData: Consensus Data.The default is block nonce.A pseudo-random number
     * generated by the speaker when the block is released</li>
     * <li>NextConsensus: Next block consensus address</li>
     * </ul>
     *
     * @param writer BinaryWriter
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        this.serializeUnsigned(writer);
        writer.writeByte((byte) 1);
        writer.writeSerializable(witness);
        TR.exit();
    }

    /**
     * Serialize method（block header）
     *
     * @param writer BinaryWriter
     */
    @Override
    public void serializeUnsigned(BinaryWriter writer) {
        TR.enter();
        writer.writeUint(version);
        writer.writeSerializable(prevHash);
        writer.writeSerializable(merkleRoot);
        writer.writeUint(timestamp);
        writer.writeUint(index);
        writer.writeUlong(consensusData);
        writer.writeSerializable(nextConsensus);
        TR.exit();
    }

    /**
     * Get the script hash collection for validation. Actually, it is two-thirds of the multi-party
     * signed contract address of the current block consensus node.
     *
     * @param snapshot Database Snapshot
     * @return script hash collection
     */
    @Override
    public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
        TR.enter();
        if (prevHash == UInt256.Zero) {
            return new UInt160[]{witness.scriptHash()};
        }

        Header prevHeader = snapshot.getHeader(prevHash);
        if (prevHeader == null) {
            throw new InvalidOperationException();
        }
        return TR.exit(new UInt160[]{prevHeader.nextConsensus});
        // C# code
        //        Header prev_header = snapshot.GetHeader(PrevHash);
        //        if (prev_header == null) throw new InvalidOperationException();
        //        return new UInt160[]{prev_header.NextConsensus};
    }

    /**
     * Get hash data
     *
     * @return hash data
     */
    @Override
    public byte[] getMessage() {
        TR.enter();
        return TR.exit(this.getHashData());
    }

    /**
     * get the serialized data for the specified object
     *
     * @return serialized data
     */
    public byte[] getHashData() {
        TR.enter();
        return TR.exit(IVerifiable.getHashData(this));
    }

    /**
     * Convert to a JObject object
     *
     * @return JObject object
     */
    public JsonObject toJson() {
        TR.enter();
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
        return TR.exit(jsonObject);
    }

    /**
     * Verify the block based on the current block snapshot
     *
     * @param snapshot block snapshot
     * @return If the following four conditions are met, the verification result is false.<br/>
     * <ul>
     * <li>1) If the previous block does not exist</li>
     * <li>2) If the previous block height plus 1 is not equal to the current block height</li>
     * <li>3) If the previous block timestamp is greater than or equal to the current block
     * timestamp</li>
     * <li>4) If witness verification fails</li>
     * </ul>
     */
    public boolean verify(Snapshot snapshot) {
        TR.enter();
        Header prevHeader = snapshot.getHeader(prevHash);
        if (prevHeader == null) {
            return TR.exit(false);
        }
        if (!prevHeader.index.add(new Uint(1)).equals(index)) {
            return TR.exit(false);
        }
        if (prevHeader.timestamp.compareTo(timestamp) >= 0) {
            return TR.exit(false);
        }
        if (!IVerifiable.verifyWitnesses(this, snapshot)) {
            return TR.exit(false);
        }
        return TR.exit(true);
    }

}
