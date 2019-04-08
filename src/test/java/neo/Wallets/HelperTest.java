package neo.Wallets;

import org.junit.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

import neo.UInt160;
import neo.cryptography.Crypto;
import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.network.p2p.payloads.IVerifiable;
import neo.network.p2p.payloads.Witness;
import neo.persistence.Snapshot;

import static org.junit.Assert.*;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: HelperTest
 * @Package neo.Wallets
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 15:57 2019/4/4
 */
public class HelperTest {
    @Test
    public void sign() throws Exception {
        IVerifiable iv= new IVerifiable() {
            @Override
            public byte[] getMessage() {
                return new byte[0];
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public void serialize(BinaryWriter binaryWriter) {

            }

            @Override
            public void deserialize(BinaryReader binaryReader) {

            }

            @Override
            public Witness[] getWitnesses() {
                return new Witness[0];
            }

            @Override
            public void setWitnesses(Witness[] witnesses) {

            }

            @Override
            public void deserializeUnsigned(BinaryReader reader) {

            }

            @Override
            public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
                return new UInt160[0];
            }

            @Override
            public void serializeUnsigned(BinaryWriter writer) {
                try {
                    writer.write("a".getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        };
        KeyPair key = new KeyPair(Wallet.getPrivateKeyFromNEP2
                ("6PYNaeA3WyZJJNnFqq3PbXszry5hL7bbageAmN3sc4tziXmPzpcQSEqHjr", "1234567890"));
        System.out.println(BitConverter.toHexString(key.privateKey));
        byte[] t=IVerifiable.getHashData(iv);
        byte[] temp=Helper.sign(iv,key);
        boolean result=Crypto.Default.verifySignature(new byte[]{0x61},temp,key.publicKey.getEncoded(true));
        Assert.assertEquals(true,result);
    }

    @Test
    public void toAddress() throws Exception {
        UInt160 uInt160=Helper.toScriptHash("AZnqQgpVvFKJF6gxComwDTUqZzWg8vyv12");
        Assert.assertEquals("AZnqQgpVvFKJF6gxComwDTUqZzWg8vyv12",uInt160.toAddress());
    }

    @Test
    public void toScriptHash() throws Exception {
        UInt160 uInt160=Helper.toScriptHash("AZnqQgpVvFKJF6gxComwDTUqZzWg8vyv12");
        Assert.assertEquals("AZnqQgpVvFKJF6gxComwDTUqZzWg8vyv12",uInt160.toAddress());
    }

}