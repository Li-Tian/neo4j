package neo.network.p2p.payloads;

import org.junit.Assert;
import org.junit.Test;

import neo.UInt160;
import neo.UInt256;
import neo.Utils;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.ledger.TrimmedBlock;

import static org.junit.Assert.*;

public class HeaderTest {

    @Test
    public void size() {
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

        Assert.assertEquals(112, header.size());
    }

    @Test
    public void serialize() {
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


        Header copy = Utils.copyFromSerialize(header, Header::new);

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
    public void testHashCode() {
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

        Assert.assertEquals(header.hash().hashCode(), header.hashCode());
    }

    @Test
    public void equals() {
        Header header1 = new Header() {{
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

        Header header2 = new Header() {{
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


        Assert.assertEquals(header1, header2);
    }

    @Test
    public void trim() {
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

        TrimmedBlock trimmedBlock = header.trim();

        Assert.assertEquals(header.version, trimmedBlock.version);
        Assert.assertEquals(header.prevHash, trimmedBlock.prevHash);
        Assert.assertEquals(header.timestamp, trimmedBlock.timestamp);
        Assert.assertEquals(header.index, trimmedBlock.index);
        Assert.assertEquals(header.consensusData, trimmedBlock.consensusData);
        Assert.assertEquals(header.nextConsensus, trimmedBlock.nextConsensus);
        Assert.assertEquals(header.merkleRoot, trimmedBlock.merkleRoot);
        Assert.assertEquals(0, trimmedBlock.hashes.length);
    }
}