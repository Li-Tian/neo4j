package neo.network.p2p.payloads;

import neo.UInt256;
import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;
import neo.log.tr.TR;

/**
 * A payload for getting block
 */
public class GetBlocksPayload implements ISerializable {

    /**
     * The hash list of start blocks. The fixed length is 1
     */
    public UInt256[] hashStart;

    /**
     * The hash value of the end block
     */
    public UInt256 hashStop;

    /**
     * The size of this payload
     */
    @Override
    public int size() {
        TR.enter();
        return TR.exit(BitConverter.getVarSize(hashStart) + hashStop.size());
    }

    /**
     * Serialization
     *
     * @param writer >The binary output writer
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        writer.writeArray(hashStart);
        writer.writeSerializable(hashStop);
        TR.exit();
    }

    /**
     * Deserialization
     *
     * @param reader The binary input reader
     */
    @Override
    public void deserialize(BinaryReader reader) {
        TR.enter();
        hashStart = reader.readArray(UInt256[]::new, UInt256::new);
        hashStop = reader.readSerializable(UInt256::new);
        TR.exit();
    }

    /**
     * Create a payload for geting blocks
     *
     * @param hashStart The hash value of the start block
     * @param hashStop  The hash value of the stop block. If not specified, set to 0 automatically.
     *                  The most block number is 500
     * @return The playload of the complete hash blocks with the hash start and hash stop
     */
    public static GetBlocksPayload create(UInt256 hashStart, UInt256 hashStop) {
        TR.enter();
        GetBlocksPayload payload = new GetBlocksPayload();
        payload.hashStart = new UInt256[]{hashStart};
        payload.hashStop = hashStop == null ? UInt256.Zero : hashStop;
        return TR.exit(payload);
    }

    /**
     * Create a payload for geting blocks
     *
     * @param hashStart The hash value of the start block
     * @return The playload of the complete hash blocks with the hash start and hash stop
     */
    public static GetBlocksPayload create(UInt256 hashStart) {
        TR.enter();
        return TR.exit(create(hashStart, null));
    }
}
