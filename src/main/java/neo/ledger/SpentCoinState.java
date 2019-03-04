package neo.ledger;

import java.util.HashMap;
import java.util.Map;

import neo.UInt256;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.Ushort;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.io.ICloneable;

/**
 * 已花费交易的状态（主要用来记录NEO的，方便计算claim GAS)
 */
public class SpentCoinState extends StateBase implements ICloneable<SpentCoinState> {

    /**
     * 交易hash
     */
    public UInt256 transactionHash;

    /**
     * 交易所在区块高度
     */
    public Uint transactionHeight;

    /**
     * 已花费的outputs高度信息, output.index -> 花费该output的block.Index
     */
    public HashMap<Ushort, Uint> items;


    /**
     * 存储大小
     */
    @Override
    public int size() {
        // c# code: Size => base.Size + TransactionHash.Size + sizeof(uint)
        // + IO.Helper.GetVarSize(Items.Count) + Items.Count * (sizeof(ushort) + sizeof(uint));
        return super.size() + transactionHash.size() + Uint.BYTES + BitConverter.getVarSize(items.size())
                + items.size() * (Ushort.BYTES + Uint.BYTES);
    }

    /**
     * 克隆
     *
     * @return 克隆对象
     */
    @Override
    public SpentCoinState copy() {
        SpentCoinState coin = new SpentCoinState();
        coin.transactionHash = transactionHash;
        coin.transactionHeight = transactionHeight;
        coin.items = items;
        return coin;
    }

    /**
     * 从副本复制
     *
     * @param replica 副本数据
     */
    @Override
    public void fromReplica(SpentCoinState replica) {
        this.transactionHash = replica.transactionHash;
        this.transactionHeight = replica.transactionHeight;
        this.items = replica.items;
    }

    /**
     * 反序列化
     *
     * @param reader 二进制输入流
     */
    @Override
    public void deserialize(BinaryReader reader) {
        super.deserialize(reader);
        transactionHash = reader.readSerializable(UInt256::new);
        transactionHeight = reader.readUint();
        int count = reader.readVarInt().intValue();
        items = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            Ushort index = reader.readUshort();
            Uint height = reader.readUint();
            items.put(index, height);
        }
    }

    /**
     * 序列化
     * <p>序列化字段</p>
     * <ul>
     * <li>StateVersion: 状态版本号</li>
     * <li>TransactionHash: 交易hash</li>
     * <li>TransactionHeight: 交易所在区块高度</li>
     * <li>Items: 已花费的outputs高度信息</li>
     * </ul>
     *
     * @param writer 二进制输出流
     */
    @Override
    public void serialize(BinaryWriter writer) {
        super.serialize(writer);
        writer.writeSerializable(transactionHash);
        writer.writeUint(transactionHeight);
        writer.writeVarInt(items.size());
        for (Map.Entry<Ushort, Uint> entry : items.entrySet()) {
            writer.writeUshort(entry.getKey());
            writer.writeUint(entry.getValue());
        }
    }
}
