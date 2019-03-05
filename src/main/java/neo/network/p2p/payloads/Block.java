package neo.network.p2p.payloads;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import neo.Fixed8;
import neo.UInt256;
import neo.cryptography.MerkleTree;
import neo.csharp.BitConverter;
import neo.csharp.Ulong;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.FormatException;
import neo.ledger.Blockchain;
import neo.ledger.TrimmedBlock;

/**
 * 区块数据类，BlockBase的子类
 */
public class Block extends BlockBase implements IInventory {

    /**
     * 交易集合
     */
    public Transaction[] transactions;

    private Header header = null;

    /**
     * 消息传输Inventory种类
     */
    @Override
    public InventoryType inventoryType() {
        return InventoryType.Block;
    }

    /**
     * 区块头
     */
    public Header getHeader() {
        if (header == null) {
            header = new Header();
            header.prevHash = this.prevHash;
            header.merkleRoot = this.prevHash;
            header.timestamp = this.timestamp;
            header.index = this.index;
            header.consensusData = this.consensusData;
            header.nextConsensus = this.nextConsensus;
            header.witness = this.witness;
        }
        return header;
    }

    /**
     * 区块大小
     */
    @Override
    public int size() {
        return super.size() + BitConverter.getVarSize(transactions);
    }

    /**
     * 计算一批交易的网络手续费, network_fee = input.GAS - output.GAS - input.systemfee
     *
     * @param transactions 待计算的交易列表
     * @return 交易的网络手续费
     */
    public static Fixed8 calculateNetFee(Collection<Transaction> transactions) {
        //        Transaction[] ts = transactions.Where(p = > p.Type != TransactionType.MinerTransaction && p.Type != TransactionType.ClaimTransaction).
        //        ToArray();
        //        Fixed8 amount_in = ts.SelectMany(p = > p.References.Values.Where(o = > o.AssetId == Blockchain.UtilityToken.Hash)).
        //        Sum(p = > p.Value);
        //        Fixed8 amount_out = ts.SelectMany(p = > p.Outputs.Where(o = > o.AssetId == Blockchain.UtilityToken.Hash)).
        //        Sum(p = > p.Value);
        //        Fixed8 amount_sysfee = ts.Sum(p = > p.SystemFee);
        Fixed8 amountIn = Fixed8.ZERO;
        Fixed8 amountOut = Fixed8.ZERO;
        Fixed8 amountSysfee = Fixed8.ZERO;
        for (Transaction tx : transactions) {
            if (tx.type != TransactionType.MinerTransaction && tx.type != TransactionType.ClaimTransaction) {
                for (TransactionOutput input : tx.getReferences().values()) {
                    if (input.assetId.equals(Blockchain.UtilityToken.hash())) {
                        amountIn = Fixed8.add(amountIn, input.value);
                    }
                }
                for (TransactionOutput output : tx.outputs) {
                    if (output.assetId.equals(Blockchain.UtilityToken.hash())) {
                        amountOut = Fixed8.add(amountOut, output.value);
                    }
                }
                amountSysfee = Fixed8.add(amountSysfee, tx.getSystemFee());
            }
        }
        return Fixed8.subtract(Fixed8.subtract(amountIn, amountOut), amountSysfee);
    }


    /**
     * 反序列化
     *
     * @param reader 二进制读入器
     * @throws FormatException 如果出现以下情况之一，会抛出异常：
     *                         <ul>
     *                         <li>1）交易数量为0时</li>
     *                         <li>2）第一笔交易不是挖矿交易；</li>
     *                         <li>3）除第一笔交易外，其他交易是挖矿交易；</li>
     *                         <li>4）添加交易Hash已存在；</li>
     *                         <li>5）梅克尔根和计算出来的值不相等。</li>
     *                         </ul>
     */
    @Override
    public void deserialize(BinaryReader reader) {
        super.deserialize(reader);
        Ulong size = reader.readVarInt(new Ulong(0x10000));
        if (Ulong.ZERO.equals(size)) {
            throw new FormatException();
        }
        transactions = new Transaction[size.intValue()];
        HashSet<UInt256> hashes = new HashSet<UInt256>();
        UInt256[] hashArray = new UInt256[transactions.length];

        for (int i = 0; i < transactions.length; i++) {
            transactions[i] = Transaction.deserializeFrom(reader);
            if (i == 0) {
                if (transactions[0].type != TransactionType.MinerTransaction) {
                    throw new FormatException();
                }
            } else {
                if (transactions[i].type == TransactionType.MinerTransaction)
                    throw new FormatException();
            }
            if (!hashes.add(transactions[i].hash())) {// 不包含
                throw new FormatException();
            }
            hashArray[i] = transactions[i].hash();
        }

        if (MerkleTree.computeRoot(hashArray) != merkleRoot) {
            throw new FormatException();
        }
    }

    /**
     * 重新构建梅克尔树
     */
    public void rebuildMerkleRoot() {
        UInt256[] hashArray = new UInt256[transactions.length];
        for (int i = 0; i < transactions.length; i++) {
            hashArray[i] = transactions[i].hash();
        }
        merkleRoot = MerkleTree.computeRoot(hashArray);
    }

    /**
     * 序列化
     *
     * <p>序列化字段</p>
     * <ul>
     * <li>Version: 状态版本号</li>
     * <li>PrevHash: 上一个区块hash</li>
     * <li>MerkleRoot: 梅克尔根</li>
     * <li>Timestamp: 时间戳</li>
     * <li>Index: 区块高度</li>
     * <li>ConsensusData: 共识数据，默认为block nonce。议长出块时生成的一个伪随机数。</li>
     * <li>NextConsensus: 下一个区块共识地址</li>
     * <li>Transactions: 交易集合</li>
     * </ul>
     *
     * @param writer 二进制输出流
     */
    @Override
    public void serialize(BinaryWriter writer) {
        super.serialize(writer);
        writer.writeArray(transactions);
    }

    /**
     * 转成json对象
     */
    @Override
    public JsonObject toJson() {
        JsonObject jsonObject = super.toJson();
        JsonArray array = new JsonArray();
        Arrays.stream(transactions).forEach(p -> array.add(p.toJson()));
        jsonObject.add("tr", array);
        return super.toJson();
    }

    /**
     * 判断两个区块是否相等
     *
     * @param obj 待比较区块
     * @return 若待比较区块为null，直接返回false。否则进行引用和hash值比较
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !(obj instanceof Block)) {
            return false;
        }
        Block other = (Block) obj;
        return hash().equals(other.hash());
    }

    /**
     * 获取区块hash code
     *
     * @return 区块hash code
     */
    @Override
    public int hashCode() {
        return hash().hashCode();
    }

    /**
     * 转成简化版的block。抛弃交易，仅保留交易的哈希值。
     *
     * @return 简化版的block
     */
    public TrimmedBlock trim() {
        TrimmedBlock trimmedBlock = new TrimmedBlock();
        trimmedBlock.version = version;
        trimmedBlock.prevHash = prevHash;
        trimmedBlock.merkleRoot = merkleRoot;
        trimmedBlock.timestamp = timestamp;
        trimmedBlock.index = index;
        trimmedBlock.consensusData = consensusData;
        trimmedBlock.nextConsensus = nextConsensus;
        trimmedBlock.witness = witness;
        trimmedBlock.hashes = new UInt256[transactions.length];
        for (int i = 0; i < transactions.length; i++) {
            trimmedBlock.hashes[i] = transactions[i].hash();
        }
        return trimmedBlock;
    }
}
