package neo.consensus;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import neo.UInt160;
import neo.UInt256;
import neo.Utils;
import neo.csharp.Ulong;
import neo.network.p2p.payloads.MinerTransaction;

import static org.junit.Assert.*;

public class PrepareRequestTest {

    @Test
    public void size() {
        PrepareRequest request = new PrepareRequest() {{
            nonce = Ulong.ZERO;
            nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            transactionHashes = new UInt256[]{new MinerTransaction().hash(), UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01")};
            minerTransaction = new MinerTransaction();
            signature = new byte[64];
        }};
        Arrays.fill(request.signature, (byte) 0x01);

        Assert.assertEquals(169, request.size());
    }

    @Test
    public void serialize() {
        PrepareRequest request = new PrepareRequest() {{
            nonce = Ulong.ZERO;
            nextConsensus = UInt160.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff01");
            transactionHashes = new UInt256[]{new MinerTransaction().hash(), UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01")};
            minerTransaction = new MinerTransaction();
            signature = new byte[64];
        }};
        Arrays.fill(request.signature, (byte) 0x01);

        PrepareRequest copy = Utils.copyFromSerialize(request, PrepareRequest::new);
        Assert.assertEquals(request.nonce, copy.nonce);
        Assert.assertEquals(request.nextConsensus, copy.nextConsensus);
        Assert.assertArrayEquals(request.transactionHashes, copy.transactionHashes);
        Assert.assertEquals(request.minerTransaction, copy.minerTransaction);
        Assert.assertArrayEquals(request.signature, copy.signature);
    }
}