package neo.Wallets;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Arrays;

import neo.UInt160;
import neo.cryptography.Helper;
import neo.cryptography.SCrypt;
import neo.cryptography.ecc.ECC;
import neo.cryptography.ecc.ECPoint;
import neo.smartcontract.Contract;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: KeyPair
 * @Package neo.Wallets
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 11:00 2019/3/14
 */
public class KeyPair {
    public byte[] privateKey;
    public ECPoint publicKey;

    public UInt160 getPublicKeyHash() {
        return neo.smartcontract.Helper.toScriptHash(publicKey.getEncoded(true));
    }

    public KeyPair(byte[] privateKey) {
        if (privateKey.length != 32 && privateKey.length != 96 && privateKey.length != 104)
            throw new IllegalArgumentException();
        this.privateKey = new byte[32];
        if (privateKey.length == 32) {
            this.publicKey = new ECPoint(ECC.Secp256r1.getG().multiply(new BigInteger(1, privateKey)).normalize());
        } else {
            this.publicKey = ECPoint.fromBytes(privateKey, ECC.Secp256r1.getCurve());
        }
    }

    public boolean equals(KeyPair other) {
        if (this == other) return true;
        if (other == null) return false;
        return publicKey.equals(other.publicKey);
    }

    @Override
    public boolean equals(Object obj) {
        return equals((KeyPair) obj);
    }

    public String export() {
        byte[] data = new byte[34];
        data[0] = (byte) 0x80;
        System.arraycopy(privateKey, 0, data, 1, 32);
        data[33] = 0x01;
        String wif = neo.cryptography.Helper.base58CheckEncode(data);
        Arrays.fill(data, 0, data.length, (byte) 0x00);
        return wif;
    }

    public String export(String passphrase, int N, int r, int p) {
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
            return Helper.base58CheckEncode(buffer);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public String export(String passphrase) {
        int N = 16384;
        int r = 8;
        int p = 8;
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
            return Helper.base58CheckEncode(buffer);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int hashCode() {
        return publicKey.getHashCode();
    }

    @Override
    public String toString() {
        return publicKey.toString();
    }

    private static byte[] XOR(byte[] x, byte[] y) {
        if (x.length != y.length) throw new IllegalArgumentException();
        //LINQ START
        //return x.zip(y, (a, b) =>(byte) (a ^ b)).ToArray();
        byte[] tempArray = new byte[x.length];
        for (int i = 0; i < x.length; i++) {
            tempArray[i] = (byte) (x[i] ^ y[i]);
        }
        return tempArray;
        //LINQ END
    }
}