package neo.cryptography;

import java.util.Arrays;
import java.util.BitSet;
import java.util.stream.IntStream;

import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.log.notr.TR;

public class BloomFilter {
    private Uint[] seeds;
    private BitSet bits;
    private Uint tweak;

    public BloomFilter(int m, int k, Uint nTweak) {
        this(m, k, nTweak, null);
    }

    public BloomFilter(int m, int k, Uint nTweak, byte[] elements/*= null*/) {
        TR.enter();
        if (m % 8 != 0) {
            TR.exit();
            throw new IllegalArgumentException("m must be divisible by 8");
        }
        //this.seeds = Enumerable.Range(0, k).Select(p = > (uint) p * 0xFBA4C795 + nTweak).ToArray();
        this.seeds = IntStream.range(0, k).mapToObj(p->new Uint(p * 0xFBA4C795)
                .add(nTweak)).toArray(Uint[]::new);
        //this.bits = elements == null ? new BitArray(m) : new BitArray(elements);
        //this.bits.Length = m;
        this.tweak = nTweak;
        this.bits = new BitSet(m);
        if (elements != null) {
            if (elements.length * 8 == m) {
                bits.or(BitSet.valueOf(elements));
            } else {//elements.length * 8 != m
                byte[] temp = new byte[m / 8];
                System.arraycopy(elements, 0, temp, 0,
                        Math.min(elements.length, temp.length));
                bits.or(BitSet.valueOf(temp));
            }
        }
        TR.exit();
    }

    public int getK() {
        return seeds.length;
    }

    public int getM() {
        return bits.size();
    }

    public Uint getTweak() {
        return tweak;
    }

    public void add(byte[] element) {
        TR.enter();
        //foreach(uint i in seeds.AsParallel().Select(s = > element.Murmur32(s)))
        //bits.Set((int) (i % (uint) bits.Length), true);
        Arrays.stream(seeds).map(s->Helper.murmur32(element, s)).forEach(
                i->bits.set(i.remainder(new Uint(bits.size())).intValue(), true));
        TR.exit();
    }

    public boolean check(byte[] element) {
        TR.enter();
        //foreach(uint i in seeds.AsParallel().Select(s = > element.Murmur32(s)))
        //if (!bits.Get((int) (i % (uint) bits.Length)))
        //    return false;
        //return true;
        return TR.exit((Arrays.stream(seeds).map(s->Helper.murmur32(element, s)).allMatch(
                i->bits.get(i.remainder(new Uint(bits.size())).intValue())
        )));
    }

    public void getBits(byte[] newBits) {
        TR.enter();
        byte[] temp = bits.toByteArray();
        if (newBits.length * 8 < bits.size()) {
            TR.exit();
            throw new IllegalArgumentException("space not large enough");
        }
        System.arraycopy(temp, 0, newBits, 0, temp.length);
        TR.exit();
    }
}
