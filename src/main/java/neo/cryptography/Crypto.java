package neo.cryptography;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import sun.security.ec.ECPrivateKeyImpl;
import sun.security.ec.ECPublicKeyImpl;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;

import neo.log.tr.TR;
import neo.vm.ICrypto;

public class Crypto implements ICrypto {

    public static final Crypto Default = new Crypto();

    private static final String SIGNATURE_ALGORITHM = "SHA256withECDSA";

    static {
        //加入BouncyCastleProvider的支持
        Security.addProvider(new BouncyCastleProvider());
    }

    @Override
    public byte[] hash160(byte[] message) {
        try {
            byte[] hash256 = sha256(message);
            byte[] ripeMD160 = ripeMD160(hash256);
            return ripeMD160;
        } catch (Exception e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] hash256(byte[] message) {
        try {
            return sha256(sha256(message));
        } catch (Exception e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean verifySignature(byte[] message, byte[] signature, byte[] pubkey) {
        // TODO 尚未验证
        TR.enter();
        if (pubkey.length == 33 && (pubkey[0] == 0x02 || pubkey[0] == 0x03)) {
            try {
                pubkey = neo.cryptography.ECC.ECPoint.secp256r1.getCurve().decodePoint(pubkey).getEncoded(false);
                byte[] tmp = new byte[pubkey.length - 1];
                System.arraycopy(pubkey, 1, tmp, 0, pubkey.length - 1);
                pubkey = tmp;
                // C# code
                //  pubkey = Cryptography.ECC.ECPoint.DecodePoint(pubkey, Cryptography.ECC.ECCurve.Secp256r1).EncodePoint(false).Skip(1).ToArray();
            } catch (Exception e) {
                return false;
            }
        } else if (pubkey.length == 65 && pubkey[0] == 0x04) {
            byte[] tmp = new byte[64];
            System.arraycopy(pubkey, 1, tmp, 0, 64);
            pubkey = tmp;
        } else if (pubkey.length != 64) {
            throw new IllegalArgumentException();
        }

        try {
            Signature sign = Signature.getInstance(SIGNATURE_ALGORITHM);
            PublicKey key = new ECPublicKeyImpl(pubkey);
            sign.initVerify(key);
            sign.update(message);
            return TR.exit(sign.verify(signature));
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 数据签名 (ECDSA 方法进行签名)
     *
     * @param message    待签名数据
     * @param privateKey 私钥
     * @return 签名数据
     */
    public byte[] sign(byte[] message, byte[] privateKey) {
        // TODO 尚未验证
        try {
            TR.enter();
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            PrivateKey key = new ECPrivateKeyImpl(privateKey);
            signature.initSign(key);
            signature.update(message);
            return TR.exit(signature.sign());
        } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }
}
