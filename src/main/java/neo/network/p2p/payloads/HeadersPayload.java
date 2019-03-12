package neo.network.p2p.payloads;

import java.util.Collection;

import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;
import neo.log.notr.TR;

/**
 * The payload of the headers, which define the struct of block headers
 */
public class HeadersPayload implements ISerializable {

    /**
     * The max number of headers
     */
    public static final int MaxHeadersCount = 2000;

    /**
     * The array of headers
     */
    public Header[] headers;

    /**
     * The size of block header array
     */
    @Override
    public int size() {
        TR.enter();
        return TR.exit(BitConverter.getVarSize(headers));
    }


    /**
     * Serialization
     *
     * @param writer The binary output writer
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        writer.writeArray(headers);
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
        headers = reader.readArray(Header[]::new, Header::new, MaxHeadersCount);
        TR.exit();
    }

    /**
     * Create a payload for headers accoring to enumerable headers
     *
     * @param headers The enumerable headers
     * @return The payload for headers
     */
    public static HeadersPayload create(Collection<Header> headers) {
        TR.enter();
        HeadersPayload headersPayload = new HeadersPayload();
        headersPayload.headers = new Header[headers.size()];
        headers.toArray(headersPayload.headers);
        return TR.exit(headersPayload);
    }
}
