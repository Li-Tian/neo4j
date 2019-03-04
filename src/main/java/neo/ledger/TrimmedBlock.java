package neo.ledger;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Arrays;

import neo.UInt256;
import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.io.caching.DataCache;
import neo.network.p2p.payloads.Block;
import neo.network.p2p.payloads.BlockBase;
import neo.network.p2p.payloads.Header;
import neo.network.p2p.payloads.Transaction;

/**
 * 简化版本的block，主要方便存储在leveldb中
 */
public class TrimmedBlock extends BlockBase {

    /**
     * 交易hash列表
     */
    public UInt256 hashes[];

    private Header header;

    /**
     * 是否是从完整的block简化而来。从区块头对象简化而来的版本没有交易的哈希。
     */
    public boolean isBlock() {
        return hashes.length > 0;
    }

    /**
     * 从缓存中获取完整的交易，构建出完整的block
     *
     * @param cache 缓存的交易
     * @return 完整的block
     */
    public Block getBlock(DataCache<UInt256, TransactionState> cache) {
        Block block = new Block();
        block.version = version;
        block.prevHash = prevHash;
        block.merkleRoot = merkleRoot;
        block.timestamp = timestamp;
        block.index = index;
        block.consensusData = consensusData;
        block.nextConsensus = nextConsensus;
        block.witness = witness;
        // C# code:  Transactions = Hashes.Select(p => cache[p].Transaction).ToArray()
        block.transactions = (Transaction[]) Arrays.stream(hashes).map(p -> cache.get(p).transaction).toArray();
        return block;
    }

    /**
     * 获取区块头
     */
    public Header getHeader() {
        if (header == null) {
            header = new Header();
            header.version = version;
            header.prevHash = prevHash;
            header.merkleRoot = merkleRoot;
            header.timestamp = timestamp;
            header.index = index;
            header.consensusData = consensusData;
            header.nextConsensus = nextConsensus;
            header.witness = witness;
        }
        return header;
    }

    /**
     * 存储大小
     */
    @Override
    public int size() {
        // 111 + 32 +1 => 144
        return super.size() + BitConverter.getVarSize(hashes);
    }

    /**
     * 反序列化
     *
     * @param reader 二进制输入流
     */
    @Override
    public void deserialize(BinaryReader reader) {
        super.deserialize(reader);
        hashes = reader.readArray(UInt256[]::new, UInt256::new);
    }

    /**
     * 序列化
     * <p>序列化字段</p>
     * <ul>
     * <li>StateVersion: 状态版本号</li>
     * <li>Hashes: 交易hash列表</li>
     * </ul>
     *
     * @param writer 二进制输出流
     */
    @Override
    public void serialize(BinaryWriter writer) {
        super.serialize(writer);
        writer.writeArray(hashes);
    }


    /**
     * 转成json对象
     *
     * @return json对象
     */
    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        JsonArray array = new JsonArray(hashes.length);
        Arrays.stream(hashes).forEach(p -> array.add(p.toString()));
        json.add("hashes", array);
        return json;
    }

}
