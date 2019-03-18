package neo.consensus;

import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.FormatException;

/**
 * ChangeView message, used in view change processing, extends ConsensusMessage.
 */
public class ChangeView extends ConsensusMessage {

    /**
     * New view number
     */
    public byte newViewNumber;

    /**
     * Contractor, create a ChangeView message
     */
    public ChangeView() {
        super(ConsensusMessageType.ChangeView, ChangeView::new);
    }

    /**
     * size for storage
     */
    @Override
    public int size() {
        return super.size() + Byte.BYTES;
    }

    /**
     * Deserialize from reader
     *
     * @param reader binary reader
     * @throws FormatException if the newViewNumber is zero.
     */
    @Override
    public void deserialize(BinaryReader reader) {
        super.deserialize(reader);
        newViewNumber = (byte) reader.readByte();
        if (newViewNumber == 0) throw new FormatException();
    }

    /**
     * Serialize the message
     *
     * <p>fields:</p>
     * <ul>
     * <li>Type: consensual message type</li>
     * <li>ViewNumber: view number</li>
     * <li>NewViewNumber: new view number</li>
     * </ul>
     *
     * @param writer binary writer
     */
    @Override
    public void serialize(BinaryWriter writer) {
        super.serialize(writer);
        writer.writeByte(newViewNumber);
    }


}
