package neo.ledger;


import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.io.ICloneable;

/**
 * UTXO状态
 */
public class UnspentCoinState extends StateBase implements ICloneable<UnspentCoinState> {

    /**
     * output项状态列表
     */
    public CoinState[] items;

    /**
     * 存储大小
     */
    @Override
    public int size() {
        return super.size() + BitConverter.getVarSize(items);
    }

    /**
     * 克隆
     *
     * @return 克隆对象
     */
    @Override
    public UnspentCoinState copy() {
        UnspentCoinState state = new UnspentCoinState();
        state.items = new CoinState[items.length];
        for (int i = 0; i < items.length; i++) {
            state.items[i] = items[i];
        }
        return state;
    }

    /**
     * 从副本复制
     *
     * @param replica 副本
     */
    @Override
    public void fromReplica(UnspentCoinState replica) {
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
        byte[] bytes = reader.readVarBytes();
        items = new CoinState[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            items[i] = new CoinState(bytes[i]);
        }
    }

    /**
     * 序列化
     * <p>序列化字段</p>
     * <ul>
     * <li>stateVersion: 状态版本号</li>
     * <li>items: output项状态列表</li>
     * </ul>
     *
     * @param writer 二进制输出流
     */
    @Override
    public void serialize(BinaryWriter writer) {
        super.serialize(writer);
        byte[] bytes = new byte[items.length];
        for (int i = 0; i < items.length; i++) {
            bytes[i] = items[i].value();
        }
        writer.writeVarBytes(bytes);
    }

}
