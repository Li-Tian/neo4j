package neo.cryptography;

import neo.log.notr.TR;

public class SCrypt {
    public static byte[] deriveKey(byte[] password, byte[] salt, int N, int r, int p, int derivedKeyLength) {
        TR.enter();
        return TR.exit(org.bouncycastle.crypto.generators.SCrypt.generate(password, salt, N, r, p, derivedKeyLength));
    }
}