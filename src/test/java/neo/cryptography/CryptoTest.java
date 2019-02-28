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
        String data = "hello world";
        String message = "";
        String signature = "";

//        Crypto.Default.verifySignature()
    }

    @Test
    public void sign() {
        String data = "hello world";
//        byte[] output = Crypto.Default.sign(data.getBytes(), BitConverter.hexToBytes(privateKey));
//        System.out.println(BitConverter.toHexString(output));
    }

    @Test
    public void ripeMD160() throws Exception {
        // http://tools.jb51.net/password/hash_md5_sha
        String input = "hello world";
        byte[] output = Crypto.Default.ripeMD160(input.getBytes());
        Assert.assertEquals("98c615784ccb5fe5936fbc0cbe9dfdb408d92f0f", BitConverter.toHexString(output));
    }

    @Test
    public void sha256() throws Exception {
        // http://encode.chahuo.com/
        String input = "hello world";
        byte[] output = Crypto.Default.sha256(input.getBytes());
        Assert.assertEquals("b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9", BitConverter.toHexString(output));
    }
}