package neo.consensus;


import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.FormatException;
import neo.log.notr.TR;

/**
 * PrepareResponse message, which only contains a signature
 */
public class PrepareResponse extends ConsensusMessage {

    /**
     * Signature of the proposal block
     */
    public byte[] signature;

    /**
     * Contractor, create a PrepareResponse message
     */
    public PrepareResponse() {
        super(ConsensusMessageType.PrepareResponse, PrepareResponse::new);
    }

    /**
     * size for storage
     */
    @Override
    public int size() {
        TR.enter();
        return TR.exit(super.size() + signature.length);
    }

    /**
     * Deserialize from reader
     *
     * @param reader binary reader
     * @throws FormatException if the newViewNumber is zero.
     */
    @Override
    public void deserialize(BinaryReader reader) {
        TR.enter();
        super.deserialize(reader);
        // TODO hard code 64
        signature = reader.readFully(64);
        TR.exit();
    }

    /**
     * Serialize the message
     *
     * <p>fields:</p>
     * <ul>
     * <li>Type: consensual message type</li>
     * <li>ViewNumber: view number</li>
     * <li>signature: block signature</li>
     * </ul>
     *
     * @param writer binary writer
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        super.serialize(writer);
        writer.write(signature);
        TR.exit();
    }


}
