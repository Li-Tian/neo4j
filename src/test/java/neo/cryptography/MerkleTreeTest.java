package neo.cryptography;

import org.junit.Assert;
import org.junit.Test;

import java.util.BitSet;

import neo.UInt256;

public class MerkleTreeTest {
    @Test
    public void MerkleTreeFromHash() {
        UInt256[] hashes = new UInt256[]{UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01")};
        MerkleTree tree = new MerkleTree(hashes);
        Assert.assertEquals(1, tree.getDepth());
    }

    @Test
    public void computeRoot() {
        UInt256[] hashes = new UInt256[]{UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01")};
        MerkleTree tree = new MerkleTree(hashes);
        Assert.assertEquals(hashes[0], tree.computeRoot(hashes));
        UInt256[] hashes2 = new UInt256[]{hashes[0], UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02")};
        tree = new MerkleTree(hashes2);
        Assert.assertEquals(UInt256.parse("0xf9cb225fab1d2ea151154e37e92b6486e7b6b268769f5d584f8df8ccd2050410"), tree.computeRoot(hashes2));
    }

    @Test
    public void toHashArray() {
        UInt256[] hashes = new UInt256[]{UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01"), UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02")};
        MerkleTree tree = new MerkleTree(hashes);
        UInt256[] points = tree.toHashArray();
        Assert.assertEquals(hashes[0], points[0]);
        Assert.assertEquals(hashes[1], points[1]);
    }

    @Test
    public void trim() {
        UInt256[] hashes = new UInt256[]{UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff01"), UInt256.parse("0xa400ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff00ff02")};
        MerkleTree tree = new MerkleTree(hashes);
        BitSet bitSet = new BitSet();
        bitSet.set(0, 1, true);
        tree.trim(bitSet);
        Assert.assertEquals(2, tree.toHashArray().length);
        bitSet.set(0, 1, false);
        tree.trim(bitSet);
        Assert.assertEquals(1, tree.toHashArray().length);
    }
}