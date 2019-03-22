package neo.network.p2p.payloads;


import neo.UInt256;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.FormatException;
import neo.ledger.TrimmedBlock;
import neo.log.notr.TR;

/**
 * The block header
 */
public class Header extends BlockBase {

    /**
     * The storage size for this block header object
     */
    @Override
    public int size() {
        TR.enter();
        return TR.exit(super.size() + 1);
    }

    /**
     * The deserialization
     *
     * @param reader The binary output stream
     * @throws FormatException Thrown when the binary data format does not match the format of the
     *                         header after serialization
     */
    @Override
    public void deserialize(BinaryReader reader) {
        TR.enter();
        super.deserialize(reader);
        if (reader.readByte() != 0) {
            throw new FormatException();
        }
        TR.exit();
    }


    /**
     * Serialization, with a fixed 0 at trim
     * <p>fields:</p>
     * <ul>
     * <li>Version: The VERSION of the state</li>
     * <li>PrevHash: The hash of previous block</li>
     * <li>MerkleRoot: The root of Merkle tree</li>
     * <li>Timestamp: The timestamp</li>
     * <li>Index: The height of block</li>
     * <li>ConsensusData: The consensus data, default is block NONCE</li>
     * <li>NextConsensus: The next consensus node address</li>
     * <li>0: Fixed 0</li>
     * </ul>
     *
     * @param writer The binary output writer
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        super.serialize(writer);
        writer.writeByte((byte) 0);
        TR.exit();
    }

    /**
     * Get the hash code
     *
     * @return The hash code of block
     */
    @Override
    public int hashCode() {
        TR.enter();
        return TR.exit(hash().hashCode());
    }

    /**
     * Determine if the block header is equal to other object
     *
     * @param other the object to be compared
     * @return If it is equal to other object returns true, otherwise return false
     */
    @Override
    public boolean equals(Object other) {
        TR.enter();
        if (other == this) {
            return TR.exit(true);
        }
        if (other == null) {
            return TR.exit(false);
        }
        if (!(other instanceof Header)) {
            return TR.exit(false);
        }
        return TR.exit(hash().equals(((Header) other).hash()));
    }

    /**
     * Transfer to trimmed block. The trimmed block instance transferd from header does not include
     * the hash value of transactions
     *
     * @return the trimmed block
     */
    public TrimmedBlock trim() {
        TR.enter();
        TrimmedBlock trimmedBlock = new TrimmedBlock();
        trimmedBlock.version = version;
        trimmedBlock.prevHash = prevHash;
        trimmedBlock.merkleRoot = merkleRoot;
        trimmedBlock.timestamp = timestamp;
        trimmedBlock.index = index;
        trimmedBlock.consensusData = consensusData;
        trimmedBlock.nextConsensus = nextConsensus;
        trimmedBlock.witness = witness;
        trimmedBlock.hashes = new UInt256[0];
        return TR.exit(trimmedBlock);
    }
}
