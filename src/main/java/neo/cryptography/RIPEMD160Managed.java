package neo.cryptography;

import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import java.util.Arrays;

import neo.log.tr.TR;

public class RIPEMD160Managed extends HashAlgorithm {
    private byte[] _buffer;

    @Override
    public int getHashSize() {
        TR.enter();
        return TR.exit(160);
    }

    //
    // public constructors
    //
    public RIPEMD160Managed() {
        TR.enter();
        TR.exit();
    }

    //
    // public methods
    //
    @Override
    public void initialize() {
        TR.enter();
        TR.exit();
    }

    //SecuritySafeCritical
    @Override
    protected void hashCore(byte[] rgb, int ibStart, int cbSize) {
        TR.enter();
        _buffer = Arrays.copyOfRange(rgb, ibStart, cbSize);
        TR.exit();
    }

    //SecuritySafeCritical
    @Override
    protected byte[] hashFinal() {
        TR.enter();
        RIPEMD160Digest digest = new RIPEMD160Digest();
        digest.update(_buffer, 0, _buffer.length);
        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);
        return TR.exit(result);
    }
}