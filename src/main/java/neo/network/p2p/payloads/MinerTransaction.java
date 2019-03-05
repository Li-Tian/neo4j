package neo.network.p2p.payloads;

import com.google.gson.JsonObject;

import neo.Fixed8;
import neo.csharp.Uint;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.FormatException;
import neo.ledger.Blockchain;

/**
 * 挖矿交易，奖励给出块的共识节点，同时，作为每个区块的第一笔交易
 */
public class MinerTransaction extends Transaction {

    /**
     * TODO 由于 Nonce 的值是 2的32次方，因此在300万个区块中有很高的概率会使得生成的nonce相同，
     * 从而使得两个交易的哈希值完全相同。最好的解决方法是在MinerTransaction中添加区块高度。
     */

    /**
     * 交易nonce 随机值
     */
    public Uint nonce = Uint.ZERO;

    /**
     * 构造函数
     */
    public MinerTransaction() {
        super(TransactionType.MinerTransaction);
    }

    /**
     * 存储大小
     */
    @Override
    public int size() {
        return super.size() + Uint.BYTES;
    }

    /**
     * 交易网络手续费，默认为0
     */
    @Override
    public Fixed8 getNetworkFee() {
        return Fixed8.ZERO;
    }

    /**
     * 反序列化，读取nonce值
     *
     * @param reader 二进制输入流
     */
    @Override
    protected void deserializeExclusiveData(BinaryReader reader) {
        if (version != 0) throw new FormatException();
        this.nonce = reader.readUint();
    }

    /**
     * 交易反序列化后处理
     *
     * @throws FormatException 若挖矿交易的输入不为空，或者资产不为GAS时，则抛出该异常
     */
    @Override
    protected void onDeserialized() {
        super.onDeserialized();
        if (inputs.length != 0)
            throw new FormatException();
        for (TransactionOutput output : outputs) {
            if (output.assetId != Blockchain.UtilityToken.hash()) {
                throw new FormatException();
            }
        }
    }

    /**
     * 序列化出data外的字段
     *
     * @param writer 二进制输出流
     */
    @Override
    protected void serializeExclusiveData(BinaryWriter writer) {
        writer.writeUint(nonce);
    }

    /**
     * 转成json对象
     *
     * @return json对象
     */
    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("nonce", nonce);
        return json;
    }
}
