package neo.cryptography.ecc;

import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;

public class ECC {
    /**
     * 曲线secp256k1
     */
    private static final X9ECParameters secp256r1nc = ECNamedCurveTable.getByName("secp256r1");

    /**
     * 曲线secp256r1
     */
    public static final ECDomainParameters secp256r1 = new ECDomainParameters(secp256r1nc.getCurve(), secp256r1nc.getG(), secp256r1nc.getN(), secp256r1nc.getH(), secp256r1nc.getSeed());

}
