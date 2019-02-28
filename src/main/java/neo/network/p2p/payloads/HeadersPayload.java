package neo.network.p2p.payloads;

import java.util.Collection;

import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;

public class HeadersPayload implements ISerializable {

    public static final int MaxHeadersCount = 2000;

    public Header[] headers;

    @Override
    public int size() {
        // TODO headers vavrsize
        return 0;
    }

    @Override
    public void serialize(BinaryWriter writer) {
        writer.writeArray(headers);
    }

    @Override
    public void deserialize(BinaryReader reader) {
        headers = reader.readArray(Header[]::new, Header::new, MaxHeadersCount);
    }

    public static HeadersPayload create(Collection<Header> headers) {
        HeadersPayload headersPayload = new HeadersPayload();
        headersPayload.headers = (Header[]) headers.toArray();
        return headersPayload;
    }
}
