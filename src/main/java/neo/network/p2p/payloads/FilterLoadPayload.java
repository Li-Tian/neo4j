package neo.network.p2p.payloads;

import neo.cryptography.BloomFilter;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;
import neo.exception.FormatException;
import neo.log.notr.TR;

/**
 * The payload which is loaded from filter
 */
public class FilterLoadPayload implements ISerializable {

    /**
     * The initial byteArray of filter
     */
    public byte[] filter;

    /**
     * The number of independent hash functions
     */
    public byte k;


    /**
     * The tweak parameter
     */
    public Uint tweak;


    /**
     * The size of payload of filter
     */
    @Override
    public int size() {
        TR.enter();
        return TR.exit(BitConverter.getVarSize(filter) + Byte.BYTES + Uint.BYTES);
    }

    /**
     * Serialization
     *
     * @param writer The binary output writer
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        writer.writeVarBytes(filter);
        writer.writeByte(k);
        writer.writeUint(tweak);
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
        filter = reader.readVarBytes(36000);
        k = (byte) reader.readByte();
        if (k > 50) throw new FormatException();
        tweak = reader.readUint();
        TR.exit();
    }


    /**
     * create a filterpayload which is load from the bloomfilter
     *
     * @param filter The bloomfilter
     * @return The payload load from filter
     */
    public static FilterLoadPayload create(BloomFilter filter) {
        TR.enter();
        byte[] buffer = new byte[filter.getM() / 8];
        filter.getBits(buffer);
        FilterLoadPayload payload = new FilterLoadPayload();
        payload.filter = buffer;
        payload.k = (byte) filter.getK();
        payload.tweak = filter.getTweak();
        return TR.exit(payload);
    }

}
