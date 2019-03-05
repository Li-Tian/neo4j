package neo.network.p2p.payloads;

import com.google.gson.JsonObject;

import neo.Fixed8;
import neo.UInt160;
import neo.UInt256;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;
import neo.exception.FormatException;

/**
 * 交易输出
 */
public class TransactionOutput implements ISerializable {

    /**
     * 资产Id
     */
    public UInt256 assetId;

    /**
     * 转账金额
     */
    public Fixed8 value;

    /**
     * 收款人地址脚本hash
     */
    public UInt160 scriptHash;

    /**
     * 存储大小
     */
    @Override
    public int size() {
        return assetId.size() + value.size() + scriptHash.size();
    }

    /**
     * 序列化
     *
     * @param writer 二进制输入
     */
    @Override
    public void serialize(BinaryWriter writer) {
        writer.writeSerializable(assetId);
        writer.writeSerializable(value);
        writer.writeSerializable(scriptHash);
    }

    /**
     * 反序列化
     *
     * @param reader 二进制输出
                * @throws FormatException 若转账小于0时，抛出该异常
                */
        @Override
        public void deserialize(BinaryReader reader) {
            this.assetId = reader.readSerializable(UInt256::new);
            this.value = reader.readSerializable(Fixed8::new);
        if (value.compareTo(Fixed8.ZERO) <= 0) throw new FormatException();
        this.scriptHash = reader.readSerializable(UInt160::new);
    }

    /**
     * 转成json数据
     *
     * @param index 此UTXO在交易的output列表中的index。从0开始。
     * @return json对象
     */
    public JsonObject toJson(int index) {
        JsonObject json = new JsonObject();
        json.addProperty("n", index);
        json.addProperty("asset", assetId.toString());
        json.addProperty("value", value.toString());
        json.addProperty("address", scriptHash.toAddress());
        return json;
    }
}
