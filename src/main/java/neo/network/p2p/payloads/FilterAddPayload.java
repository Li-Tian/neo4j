package neo.network.p2p.payloads;

import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;
import neo.log.notr.TR;

/**
 * The payload which is add to filter
 */
public class FilterAddPayload implements ISerializable {

    /**
     * The element need to be added
     */
    public byte[] data;


    /**
     * The size of the payload
     */
    @Override
    public int size() {
        TR.enter();
        return TR.exit(BitConverter.getVarSize(data));
    }

    /**
     * The serialization function
     *
     * @param writer The binary output Writer
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        writer.writeVarBytes(data);
        TR.exit();
    }

    /**
     * Deserialize
     *
     * @param reader The binary input Rnput
     */
    @Override
    public void deserialize(BinaryReader reader) {
        TR.enter();
        data = reader.readVarBytes();
        TR.exit();
    }
}
