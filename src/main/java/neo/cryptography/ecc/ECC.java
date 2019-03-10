package neo.cryptography.ecc;

import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;

import neo.csharp.BitConverter;

/**
 * ECC, provides secp256k1 and secp256r1 curves.
 */
public class ECC {

    /**
     * secp256k1 ecc curve, using in bitcoin
     */
    public static final X9ECParameters Secp256k1 = ECNamedCurveTable.getByName("secp256r1");

    /**
     * Secp256r1 ecc curve, using in neo
     */
    public static final ECDomainParameters Secp256r1 = new ECDomainParameters(Secp256k1.getCurve(), Secp256k1.getG(), Secp256k1.getN(), Secp256k1.getH(), Secp256k1.getSeed());


    /**
     * parse Secp256r1's eccpoint from hex string
     *
     * @param hex eccpoint hexstring
     * @return ECCPoint
     */
    public static ECPoint parseFromHexString(String hex) {
        return ECPoint.fromBytes(BitConverter.hexToBytes(hex), Secp256r1.getCurve());
    }

    /**
     * Get Secp256r1's infinity ecpoint
     *
     * @return infinity ecpoint
     */
    public static ECPoint getInfinityPoint() {
        return new ECPoint(Secp256r1.getCurve().getInfinity());
    }

}
