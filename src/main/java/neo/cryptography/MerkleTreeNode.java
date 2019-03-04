package neo.cryptography;

import neo.UInt256;
import neo.log.tr.TR;

class MerkleTreeNode {
    public UInt256 hash;
    public MerkleTreeNode parent;
    public MerkleTreeNode leftChild;
    public MerkleTreeNode rightChild;

    public MerkleTreeNode() {
        this(null);
        TR.enter();
        TR.exit();
    }

    public MerkleTreeNode(UInt256 input) {
        TR.enter();
        hash = input;
        parent = null;
        leftChild = null;
        rightChild = null;
        TR.exit();
    }

    public boolean IsLeaf() {
        TR.enter();
        return TR.exit(leftChild == null && rightChild == null);
    }

    public boolean IsRoot() {
        TR.enter();
        return TR.exit(parent == null);
    }
}