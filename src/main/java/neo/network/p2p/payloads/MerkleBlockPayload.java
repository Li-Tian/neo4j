package neo.network.p2p.payloads;

import sun.security.util.BitArray;

import java.util.BitSet;

import neo.UInt256;
import neo.cryptography.MerkleTree;
import neo.csharp.BitConverter;
import neo.csharp.Ulong;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;

/**
 * SPV钱包所需的数据块的实体类, BlockBase的子类
 */
public class MerkleBlockPayload extends BlockBase {

    /**
     * 交易数量
     */
    public int txCount;

    /**
     * 数组形式的梅克尔树
     */
    public UInt256[] hashes;

    /**
     * 标志位，表示梅克尔树中哪些节点可以省略，哪些节点不可以省略，用于梅克尔树与数组的相互转换。(little endian)
     */
    public byte[] flags;

    public static MerkleBlockPayload Create(Block block, BitSet flags) {
        UInt256[] txHashs = new UInt256[block.transactions.length];
        for (int i = 0; i < block.transactions.length; i++) {
            txHashs[i] = block.transactions[i].hash();
        }

        MerkleTree tree = new MerkleTree(txHashs);
        tree.trim(flags);
// TODO waiting for bitset or bitmap
//        byte[] buffer = new byte[(flags.size() + 7) / 8];
//        flags.toByteArray(buffer, 0);
        byte[] buffer = flags.toByteArray();

        MerkleBlockPayload payload = new MerkleBlockPayload();
        payload.version = block.version;
        payload.prevHash = block.prevHash;
        payload.merkleRoot = block.merkleRoot;
        payload.timestamp = block.timestamp;
        payload.index = block.index;
        payload.consensusData = block.consensusData;
        payload.nextConsensus = block.nextConsensus;
        payload.witness = block.witness;
        payload.txCount = block.transactions.length;
        payload.hashes = tree.toHashArray();
        payload.flags = buffer;
        return payload;
    }

    /**
     * 数据块的大小
     */
    @Override
    public int size() {
        return super.size() + Integer.BYTES + BitConverter.getVarSize(hashes) + BitConverter.getVarSize(flags);
    }

    /**
     * 反序列化方法
     *
     * @param reader 二进制输入流
     */
    @Override
    public void deserialize(BinaryReader reader) {
        super.deserialize(reader);
        txCount = reader.readVarInt(new Ulong(Integer.MAX_VALUE)).intValue();
        hashes = reader.readArray(UInt256[]::new, UInt256::new);
        flags = reader.readVarBytes();
    }

    /**
     * 序列化方法
     *
     * @param writer 二进制输出流
     */
    @Override
    public void serialize(BinaryWriter writer) {
        super.serialize(writer);
        writer.writeVarInt(txCount);
        writer.writeArray(hashes);
        writer.writeVarBytes(flags);
    }
}
