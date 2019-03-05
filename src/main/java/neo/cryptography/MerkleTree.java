package neo.cryptography;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;

import neo.UInt256;
import neo.log.tr.TR;

public class MerkleTree {
    private MerkleTreeNode root;

    private int depth;

    public int getDepth() {
        TR.enter();
        return TR.exit(depth);
    }

    public MerkleTree(UInt256[] hashes) {
        TR.enter();
        if (hashes.length == 0) {
            TR.exit();
            throw new IllegalArgumentException();
        }
        //this.root = Build(hashes.Select(p => new MerkleTreeNode { Hash = p }).ToArray());
        this.root = build((MerkleTreeNode[]) Arrays.stream(hashes).map(x -> new MerkleTreeNode(x)).toArray());
        int depth = 1;
        for (MerkleTreeNode i = root; i.leftChild != null; i = i.leftChild) {
            depth++;
        }
        this.depth = depth;
        TR.exit();
    }

    private static MerkleTreeNode build(MerkleTreeNode[] leaves) {
        TR.enter();
        if (leaves.length == 0) {
            TR.exit();
            throw new IllegalArgumentException();
        }
        if (leaves.length == 1) {
            return TR.exit(leaves[0]);
        }
        MerkleTreeNode[] parents = new MerkleTreeNode[(leaves.length + 1) / 2];
        for (int i = 0; i < parents.length; i++) {
            parents[i] = new MerkleTreeNode();
            parents[i].leftChild = leaves[i * 2];
            leaves[i * 2].parent = parents[i];
            if (i * 2 + 1 == leaves.length) {
                parents[i].rightChild = parents[i].leftChild;
            } else {
                parents[i].rightChild = leaves[i * 2 + 1];
                leaves[i * 2 + 1].parent = parents[i];
            }

            byte[] leftData = parents[i].leftChild.hash.toArray();
            byte[] rightData = parents[i].rightChild.hash.toArray();
            byte[] overralData = (byte[]) java.lang.reflect.Array.newInstance(leftData.getClass().getComponentType(), leftData.length + rightData.length);
            System.arraycopy(leftData, 0, overralData, 0, leftData.length);
            System.arraycopy(rightData, 0, overralData, leftData.length, rightData.length);
            parents[i].hash = new UInt256(Crypto.Default.hash256(overralData));
        }
        return TR.exit(build(parents)); //TailCall
    }

    public static UInt256 computeRoot(UInt256[] hashes) {
        TR.enter();
        if (hashes.length == 0) {
            TR.exit();
            throw new IllegalArgumentException();
        }
        if (hashes.length == 1) {
            return TR.exit(hashes[0]);
        }
        MerkleTree tree = new MerkleTree(hashes);
        return TR.exit(tree.root.hash);
    }

    private static void depthFirstSearch(MerkleTreeNode node, ArrayList<UInt256> hashes) {
        TR.enter();
        if (node.leftChild == null) {
            // if left is null, then right must be null
            hashes.add(node.hash);
        } else {
            depthFirstSearch(node.leftChild, hashes);
            depthFirstSearch(node.rightChild, hashes);
        }
        TR.exit();
    }

    // depth-first order
    public UInt256[] toHashArray() {
        TR.enter();
        ArrayList<UInt256> hashes = new ArrayList<UInt256>();
        depthFirstSearch(root, hashes);
        return TR.exit((UInt256[]) hashes.toArray());
    }

    public void trim(BitSet flags) {
        TR.enter();
        trim(root, 0, depth, flags);
        TR.exit();
    }

    private static void trim(MerkleTreeNode node, int index, int depth, BitSet flags) {
        TR.enter();
        if (depth == 1) {
            TR.exit();
            return;
        }
        if (node.leftChild == null) {
            TR.exit();
            return; // if left is null, then right must be null
        }
        if (depth == 2) {
            if (!flags.get(index * 2) && !flags.get(index * 2 + 1)) {
                node.leftChild = null;
                node.rightChild = null;
            }
        } else {
            trim(node.leftChild, index * 2, depth - 1, flags);
            trim(node.rightChild, index * 2 + 1, depth - 1, flags);
            if (node.leftChild.leftChild == null && node.rightChild.rightChild == null) {
                node.leftChild = null;
                node.rightChild = null;
            }
        }
        TR.exit();
    }
}