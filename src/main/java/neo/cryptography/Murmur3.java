package neo.cryptography;

import neo.csharp.BitConverter;
import neo.csharp.Uint;

public class Murmur3 extends HashAlgorithm {

    private final Uint c1 = new Uint(0xcc9e2d51);
    private final Uint c2 = new Uint(0x1b873593);
    private final int r1 = 15;
    private final int r2 = 13;
    private final Uint m = new Uint(5);
    private final Uint n = new Uint(0xe6546b64);

    private final Uint seed;
    private Uint hash;
    private int length;

    public Murmur3(Uint seed) {
        this.seed = seed;
        initialize();
    }

    @Override
    public int getHashSize() {
        return 32;
    }

    @Override
    protected void hashCore(byte[] array, int ibStart, int cbSize) {
        length += cbSize;
        int remainder = cbSize & 3;
        int alignedLength = ibStart + (cbSize - remainder);
        for (int i = ibStart; i < alignedLength; i += 4) {
            Uint k = new Uint(0);//TODO array.ToUInt32(i);
            k = k.multiply(c1);
            k = k.rotateLeft(r1);
            k = k.multiply(c2);
            hash = hash.xor(k);
            hash = hash.rotateLeft(r2);
            hash = hash.multiply(m).add(n);
        }
        if (remainder > 0) {
            Uint remainingBytes = Uint.ZERO;
            switch (remainder) {
                case 3:
                    //remainingBytes ^= (uint)array[alignedLength + 2] << 16;
                    remainingBytes = remainingBytes.xor(
                            new Uint(array[alignedLength + 2] & 0xFF).shiftLeft(16));
                    // goto case 2;
                case 2:
                    //remainingBytes ^= (uint)array[alignedLength + 1] << 8;
                    remainingBytes = remainingBytes.xor(
                            new Uint(array[alignedLength + 1] & 0xFF).shiftLeft(8));
                    // goto case 1;
                case 1:
                    //remainingBytes ^= array[alignedLength];
                    remainingBytes = remainingBytes.xor(
                            new Uint(array[alignedLength] & 0xFF));
                    break;
                default:
                    // do nothing
            }
            remainingBytes = remainingBytes.multiply(c1);
            remainingBytes = remainingBytes.rotateLeft(r1);
            remainingBytes = remainingBytes.multiply(c2);
            hash = hash.xor(remainingBytes);
        }
    }

    @Override
    protected byte[] hashFinal() {
        hash = hash.xor(new Uint(length));
        hash = hash.xor(hash.shiftRight(16));
        hash = hash.multiply(new Uint(0x85ebca6b));
        hash = hash.xor(hash.shiftRight(13));
        hash = hash.multiply(new Uint(0xc2b2ae35));
        hash = hash.xor(hash.shiftRight(16));
        return BitConverter.getBytes(hash);
    }

    @Override
    public void initialize() {
        hash = seed;
        length = 0;
    }

}
