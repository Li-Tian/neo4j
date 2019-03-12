package neo.network.p2p.payloads;


import java.util.BitSet;

import neo.UInt256;
import neo.cryptography.MerkleTree;
import neo.csharp.BitConverter;
import neo.csharp.Ulong;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.log.notr.TR;

/**
 * a class descript data required by SPV wallet <br/> a subclass of BlockBase
 */
public class MerkleBlockPayload extends BlockBase {

    /**
     * The number of transactions
     */
    public int txCount;

    /**
     * The merkle tree in array format
     */
    public UInt256[] hashes;

    /**
     * The flags stands for which nodes are ignored, and which nodes are not ignored.<br/>   This
     * flag is used for transferring merkle tree to array.(little endian)<br/>
     */
    public byte[] flags;


    /**
     * create the data for SPV wallet to verify the transaction
     *
     * @param block block data
     * @param flags flags
     * @return MerkleBlockPayload
     */
    public static MerkleBlockPayload create(Block block, BitSet flags) {
        TR.enter();
        UInt256[] txHashs = new UInt256[block.transactions.length];
        for (int i = 0; i < block.transactions.length; i++) {
            txHashs[i] = block.transactions[i].hash();
        }

        MerkleTree tree = new MerkleTree(txHashs);
        tree.trim(flags);
        // C# code:
        //    byte[] buffer = new byte[(flags.size() + 7) / 8];
        //    flags.toByteArray(buffer, 0);
        byte[] buffer = flags.toByteArray();

        MerkleBlockPayload payload = new MerkleBlockPayload();
        payload.version = block.version;
        payload.prevHash = block.prevHash;
        payload.merkleRoot = block.merkleRoot;
        payload.timestamp = block.timestamp;
        payload.index = block.index;
        payload.consensusData = block.consensusData;
        payload.nextConsensus = block.nextConsensus;
        payload.witness = block.witness;
        payload.txCount = block.transactions.length;
        payload.hashes = tree.toHashArray();
        payload.flags = buffer;
        return TR.exit(payload);
    }

    /**
     * The size of data blocks
     */
    @Override
    public int size() {
        TR.enter();
        // C# code: Size => base.Size + sizeof(int) + Hashes.GetVarSize() + Flags.GetVarSize();
        // 105 + witness + 4 + hashes + flags
        return TR.exit(super.size() + Integer.BYTES + BitConverter.getVarSize(hashes) + BitConverter.getVarSize(flags));
    }

    /**
     * Deserialization
     *
     * @param reader The binary input reader
     */
    @Override
    public void deserialize(BinaryReader reader) {
        TR.enter();
        super.deserialize(reader);
        txCount = reader.readVarInt(new Ulong(Integer.MAX_VALUE)).intValue();
        hashes = reader.readArray(UInt256[]::new, UInt256::new);
        flags = reader.readVarBytes();
        TR.exit();
    }

    /**
     * Serialization
     *
     * @param writer The binary output writer
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        super.serialize(writer);
        writer.writeVarInt(txCount);
        writer.writeArray(hashes);
        writer.writeVarBytes(flags);
        TR.exit();
    }
}
