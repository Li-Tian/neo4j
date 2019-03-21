package neo.cryptography;

import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.BigIntegers;

import java.math.BigInteger;
import java.security.Security;

import neo.cryptography.ecc.ECC;
import neo.vm.ICrypto;

import neo.log.notr.TR;

import static neo.cryptography.Helper.ripeMD160;
import static neo.cryptography.Helper.sha256;

/**
 * Crypto implement the ICrypto interface in NVM, provides hash160, hash256, sign and verifySign
 * methods.
 */
public class Crypto implements ICrypto {

    /**
     * default instance of Crypto
     */
    public static final Crypto Default = new Crypto();

    static {
        //add BouncyCastleProvider support
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * hash160 method
     */
    @Override
    public byte[] hash160(byte[] message) {
        TR.enter();
        try {
            byte[] hash256 = sha256(message);
            byte[] ripeMD160 = ripeMD160(hash256);
            return TR.exit(ripeMD160);
        } catch (Exception e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * hash256 method
     */
    @Override
    public byte[] hash256(byte[] message) {
        TR.enter();
        try {
            return TR.exit(sha256(sha256(message)));
        } catch (Exception e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
    }


    /**
     * sign the message with ECC ecdsa, using Secp256r1 curve
     *
     * @param message    message to be signed
     * @param privateKey private key
     * @return signature data
     */
    public byte[] sign(byte[] message, byte[] privateKey, byte[] publicKey) {
        TR.enter();
        ECDSASigner signer = new ECDSASigner();
        BigInteger d = new BigInteger(1, privateKey);
        ECPrivateKeyParameters privateKeyParameters = new ECPrivateKeyParameters(d, ECC.Secp256r1);
        signer.init(true, privateKeyParameters);

        BigInteger[] bi = signer.generateSignature(sha256(message));
        byte[] signature = new byte[64];
        System.arraycopy(BigIntegers.asUnsignedByteArray(32, bi[0]), 0, signature, 0, 32);
        System.arraycopy(BigIntegers.asUnsignedByteArray(32, bi[1]), 0, signature, 32, 32);
        return TR.exit(signature);
    }

    /**
     * verify the signature
     *
     * @param message   the signed message
     * @param signature signature of the message
     * @param publicKey public key
     * @return true - verify success, otherwise false
     */
    @Override
    public boolean verifySignature(byte[] message, byte[] signature, byte[] publicKey) {
        TR.enter();
        ECDSASigner signer = new ECDSASigner();
        org.bouncycastle.math.ec.ECPoint publicPoint = ECC.Secp256r1.getCurve().decodePoint(publicKey);
        ECPublicKeyParameters publicKeyParameters = new ECPublicKeyParameters(publicPoint, ECC.Secp256r1);
        signer.init(false, publicKeyParameters);

        byte[] hash = sha256(message);
        BigInteger r = BigIntegers.fromUnsignedByteArray(signature, 0, 32);
        BigInteger s = BigIntegers.fromUnsignedByteArray(signature, 32, 32);
        return TR.exit(signer.verifySignature(hash, r, s));
    }

}
