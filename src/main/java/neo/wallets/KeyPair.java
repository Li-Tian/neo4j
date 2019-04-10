package neo.wallets;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Arrays;

import neo.UInt160;
import neo.cryptography.Helper;
import neo.cryptography.SCrypt;
import neo.cryptography.ecc.ECC;
import neo.cryptography.ecc.ECPoint;
import neo.log.notr.TR;
import neo.smartcontract.Contract;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: KeyPair
 * @Package neo.wallets
 * @Description: 密钥对
 * @date Created in 11:00 2019/3/14
 */
public class KeyPair {
    //私钥
    public byte[] privateKey;
    //公钥
    public ECPoint publicKey;

    /**
     * @Author:doubi.liu
     * @description:获取公钥哈希
     * @date:2019/4/8
     */
    public UInt160 getPublicKeyHash() {
        TR.enter();
        return TR.exit(neo.smartcontract.Helper.toScriptHash(publicKey.getEncoded(true)));
    }

    public KeyPair(byte[] privateKey) {
        TR.enter();
        if (privateKey.length != 32 && privateKey.length != 96 && privateKey.length != 104)
            throw TR.exit(new IllegalArgumentException());
        this.privateKey = new byte[32];
        System.arraycopy(privateKey, privateKey.length - 32, this.privateKey, 0, 32);
        if (privateKey.length == 32) {
            this.publicKey = new ECPoint(ECC.Secp256r1.getG().multiply(new BigInteger(1, privateKey)).normalize());
        } else {
            this.publicKey = ECPoint.fromBytes(privateKey, ECC.Secp256r1.getCurve());
        }
        TR.exit();
    }

    public boolean equals(KeyPair other) {
        TR.enter();
        if (this == other) {
            return TR.exit(true);
        }
        if (other == null) {
            return TR.exit(false);
        }
        return TR.exit(publicKey.equals(other.publicKey));
    }

    @Override
    public boolean equals(Object obj) {
        TR.enter();
        return TR.exit(equals((KeyPair) obj));
    }

    public String export() {
        TR.enter();
        byte[] data = new byte[34];
        data[0] = (byte) 0x80;
        System.arraycopy(privateKey, 0, data, 1, 32);
        data[33] = 0x01;
        String wif = neo.cryptography.Helper.base58CheckEncode(data);
        Arrays.fill(data, 0, data.length, (byte) 0x00);
        return TR.exit(wif);
    }

    public String export(String passphrase, int N, int r, int p) {
        TR.enter();
        UInt160 script_hash = neo.smartcontract.Helper.toScriptHash(Contract.createSignatureRedeemScript
                (publicKey));
        String address = script_hash.toAddress();
        byte[] addresshash = new byte[0];
        try {
            byte[] tempByteArray = new byte[4];
            System.arraycopy(Helper.sha256(Helper.sha256(address.getBytes("ASCII"))), 0,
                    tempByteArray, 0, 4);
            addresshash = tempByteArray;
            byte[] derivedkey = SCrypt.deriveKey(passphrase.getBytes("UTF8"), addresshash,
                    N, r, p, 64);

            byte[] derivedhalf1 = new byte[32];
            System.arraycopy(derivedkey, 0, derivedhalf1, 0, 32);
            byte[] derivedhalf2 = new byte[32];
            System.arraycopy(derivedkey, 32, derivedhalf2, 0, 32);
            byte[] encryptedkey = Helper.aes256Encrypt(XOR(privateKey, derivedhalf1), derivedhalf2);
            byte[] buffer = new byte[39];
            buffer[0] = 0x01;
            buffer[1] = 0x42;
            buffer[2] = (byte) 0xe0;
            System.arraycopy(addresshash, 0, buffer, 3, addresshash.length);
            System.arraycopy(encryptedkey, 0, buffer, 7, encryptedkey.length);
            return TR.exit(Helper.base58CheckEncode(buffer));
        } catch (UnsupportedEncodingException e) {
            throw TR.exit(new RuntimeException(e));
        }
    }

    public String export(String passphrase) {
        TR.enter();
        return TR.exit(export(passphrase, 16384, 8, 8));
    }

    @Override
    public int hashCode() {
        TR.enter();
        return TR.exit(publicKey.getHashCode());
    }

    @Override
    public String toString() {
        TR.enter();
        return TR.exit(publicKey.toString());
    }

    private static byte[] XOR(byte[] x, byte[] y) {
        TR.enter();
        if (x.length != y.length) {
            throw TR.exit(new IllegalArgumentException());
        }
        //LINQ START
        //return x.zip(y, (a, b) =>(byte) (a ^ b)).ToArray();
        byte[] tempArray = new byte[x.length];
        for (int i = 0; i < x.length; i++) {
            tempArray[i] = (byte) (x[i] ^ y[i]);
        }
        return TR.exit(tempArray);
        //LINQ END
    }
}