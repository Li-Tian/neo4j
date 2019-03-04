package neo.ledger;

import com.google.gson.JsonObject;

import neo.csharp.Uint;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.io.ICloneable;
import neo.log.tr.TR;
import neo.network.p2p.payloads.Transaction;

/**
 * 交易状态
 */
public class TransactionState extends StateBase implements ICloneable<TransactionState> {

    /**
     * 交易所在区块高度
     */
    public Uint blockIndex;

    /**
     * 具体的交易
     */
    public Transaction transaction;

    /**
     * 存储大小
     */
    @Override
    public int size() {
        TR.enter();
        return TR.exit(super.size() + Uint.BYTES + transaction.size());
    }

    /**
     * 克隆
     *
     * @return 克隆对象
     */
    @Override
    public TransactionState copy() {
        TR.enter();
        TransactionState copy = new TransactionState();
        copy.blockIndex = blockIndex;
        copy.transaction = transaction;
        return TR.exit(copy);
    }

    /**
     * 从副本复制
     *
     * @param replica 副本
     */
    @Override
    public void fromReplica(TransactionState replica) {
        TR.enter();
        this.blockIndex = replica.blockIndex;
        this.transaction = replica.transaction;
        TR.exit();
    }

    /**
     * 反序列化
     *
     * @param reader 二进制输入流
     */
    @Override
    public void deserialize(BinaryReader reader) {
        TR.enter();
        super.deserialize(reader);
        blockIndex = reader.readUint();
        transaction = Transaction.deserializeFrom(reader);
        TR.exit();
    }

    /**
     * 序列化
     * <p>序列化字段</p>
     * <ul>
     * <li>StateVersion: 状态版本号</li>
     * <li>BlockIndex: 交易所在区块高度</li>
     * <li>Transaction: 具体的交易</li>
     * </ul>
     *
     * @param writer 二进制输出流
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        super.serialize(writer);
        writer.writeUint(blockIndex);
        writer.writeSerializable(transaction);
        TR.exit();
    }

    /**
     * 转成json对象
     *
     * @return json对象
     */
    @Override
    public JsonObject toJson() {
        TR.enter();
        JsonObject json = super.toJson();
        json.addProperty("height", blockIndex);
        json.add("tx", transaction.toJson());
        return TR.exit(json);
    }
}
