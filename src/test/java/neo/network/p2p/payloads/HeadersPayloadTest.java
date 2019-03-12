package neo.network.p2p.payloads;

import com.sun.net.httpserver.Headers;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

import neo.UInt160;
import neo.UInt256;
import neo.Utils;
import neo.csharp.Uint;
import neo.csharp.Ulong;

import static org.junit.Assert.*;

public class HeadersPayloadTest {

    @Test
    public void size() {
        HeadersPayload payload = new HeadersPayload() {{
            headers = new Header[]{
                    new Header() {{
                        version = new Uint(1);
                        prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                        timestamp = new Uint(14858584);
                        index = new Uint(10);
                        consensusData = new Ulong(100);
                        nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                        witness = new Witness() {{
                            invocationScript = new byte[]{0x01, 0x02};
                            verificationScript = new byte[]{0x03, 0x04};
                        }};
                        merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
                    }}
            };
        }};

        Assert.assertEquals(113, payload.size());
    }

    @Test
    public void serialize() {
        HeadersPayload payload = new HeadersPayload() {{
            headers = new Header[]{
                    new Header() {{
                        version = new Uint(1);
                        prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                        timestamp = new Uint(14858584);
                        index = new Uint(10);
                        consensusData = new Ulong(100);
                        nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
                        witness = new Witness() {{
                            invocationScript = new byte[]{0x01, 0x02};
                            verificationScript = new byte[]{0x03, 0x04};
                        }};
                        merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
                    }}
            };
        }};

        HeadersPayload tmp = Utils.copyFromSerialize(payload, HeadersPayload::new);

        Header header = payload.headers[0];
        Header copy = tmp.headers[0];

        Assert.assertEquals(header.version, copy.version);
        Assert.assertEquals(header.prevHash, copy.prevHash);
        Assert.assertEquals(header.timestamp, copy.timestamp);
        Assert.assertEquals(header.index, copy.index);
        Assert.assertEquals(header.consensusData, copy.consensusData);
        Assert.assertEquals(header.nextConsensus, copy.nextConsensus);
        Assert.assertEquals(header.merkleRoot, copy.merkleRoot);
        Assert.assertArrayEquals(header.witness.verificationScript, copy.witness.verificationScript);
        Assert.assertArrayEquals(header.witness.invocationScript, copy.witness.invocationScript);
    }

    @Test
    public void create() {
        Header header = new Header() {{
            version = new Uint(1);
            prevHash = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            timestamp = new Uint(14858584);
            index = new Uint(10);
            consensusData = new Ulong(100);
            nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            witness = new Witness() {{
                invocationScript = new byte[]{0x01, 0x02};
                verificationScript = new byte[]{0x03, 0x04};
            }};
            merkleRoot = UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02");
        }};
        HeadersPayload payload = HeadersPayload.create(Collections.singleton(header));
        Assert.assertEquals(1, payload.headers.length);
    }
}