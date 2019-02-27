package neo.cryptography;

import java.util.Arrays;

import neo.log.notr.TR;

abstract class HashAlgorithm {
    protected byte[] hashValue;
    private boolean m_bDisposed = false;

    public void dispose() {
        dispose(true);
    }

    public void clear() {
        dispose();
    }

    protected void dispose(boolean disposing) {
        TR.enter();
        if (disposing) {
            if (hashValue != null)
                Arrays.fill(hashValue, (byte) 0);
            hashValue = null;
            m_bDisposed = true;
        }
        TR.exit();
    }

    public byte[] computeHash(byte[] buffer) {
        TR.enter();
        if (m_bDisposed) {
            TR.exit();
            throw new IllegalStateException();
        }

        // Do some validation
        if (buffer == null) {
            TR.exit();
            throw new NullPointerException("buffer");
        }

        hashCore(buffer, 0, buffer.length);
        hashValue = hashFinal();
        byte[] tmp = Arrays.copyOf(hashValue, hashValue.length);
        initialize();
        return TR.exit(tmp);
    }

    //
    // abstract public methods
    //

    public abstract void initialize();

    protected abstract void hashCore(byte[] array, int ibStart, int cbSize);

    protected abstract byte[] hashFinal();

    protected abstract int getHashSize();
}
