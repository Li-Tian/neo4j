package neo.cryptography;

import org.junit.Assert;
import org.junit.Test;

import neo.csharp.BitConverter;


public class CryptoTest {
    private final static String privateKey = "f72b8fab85fdcc1bdd20b107e5da1ab4713487bc88fc53b5b134f5eddeaa1a19";
    private final static String publicKey = "031f64da8a38e6c1e5423a72ddd6d4fc4a777abe537e5cb5aa0425685cda8e063b";

    @Test
    public void hash160() {
        String publickey = "031f64da8a38e6c1e5423a72ddd6d4fc4a777abe537e5cb5aa0425685cda8e063b";
        String script = "21" + publickey + "ac";
        String scripthash = BitConverter.toHexString(Crypto.Default.hash160(BitConverter.hexToBytes(script)));
        Assert.assertEquals("8d3fcf1352ae46a93732b85f8331051ccd4de0a8", scripthash);
    }

    @Test
    public void hash256() {
        String input = "hello world";
        String output = BitConverter.toHexString(Crypto.Default.hash160(input.getBytes()));
        Assert.assertEquals("d7d5ee7824ff93f94c3055af9382c86c68b5ca92", output);
    }

    @Test
    public void verifySignature() {
        String message = "hello world";
        String cshop_signature = "261e894dd456a190f9e99e39cea9f64ca4f939b24cf47ee3498bf883967035b446f554753d5f76219397bc2abb281a13a3c3acce43978c02c510ccb91cb03f87";

        boolean result = Crypto.Default.verifySignature(message.getBytes(), BitConverter.hexToBytes(cshop_signature), BitConverter.hexToBytes(publicKey));
        Assert.assertTrue(result);

        byte[] output = Crypto.Default.sign(message.getBytes(), BitConverter.hexToBytes(privateKey));
        result = Crypto.Default.verifySignature(message.getBytes(), output, BitConverter.hexToBytes(publicKey));
        Assert.assertTrue(result);
    }

}