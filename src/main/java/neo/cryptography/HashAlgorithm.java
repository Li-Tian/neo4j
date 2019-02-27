package neo.cryptography;

import java.util.Arrays;

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
        if (disposing) {
            if (hashValue != null)
                Arrays.fill(hashValue, (byte) 0);
            hashValue = null;
            m_bDisposed = true;
        }
    }

    public byte[] computeHash(byte[] buffer) {
        if (m_bDisposed) {
            throw new IllegalStateException();
        }

        // Do some validation
        if (buffer == null) {
            throw new NullPointerException("buffer");
        }

        hashCore(buffer, 0, buffer.length);
        hashValue = hashFinal();
        byte[] tmp = Arrays.copyOf(hashValue, hashValue.length);
        initialize();
        return (tmp);
    }

    //
    // abstract public methods
    //

    public abstract void initialize();

    protected abstract void hashCore(byte[] array, int ibStart, int cbSize);

    protected abstract byte[] hashFinal();

    protected abstract int getHashSize();
}
