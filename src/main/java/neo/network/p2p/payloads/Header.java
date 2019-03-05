package neo.network.p2p.payloads;


import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.FormatException;

/**
 * 区块头
 */
public class Header extends BlockBase {

    /**
     * 存储大小
     */
    @Override
    public int size() {
        return super.size() + 1;
    }

    /**
     * 反序列化
     *
     * @param reader 二进制输入流
     * @throws FormatException 二进制数据格式与Header序列化后格式不符时抛出
     */
    @Override
    public void deserialize(BinaryReader reader) {
        super.deserialize(reader);
        if (reader.readByte() != 0) {
            throw new FormatException();
        }
    }


    /**
     * 序列化，尾部写入固定值0
     * <p>序列化字段</p>
     * <ul>
     * <li>Version: 状态版本号</li>
     * <li>PrevHash: 上一个区块hash</li>
     * <li>MerkleRoot: 梅克尔树</li>
     * <li>Timestamp: 时间戳</li>
     * <li>Index: 区块高度</li>
     * <li>ConsensusData: 共识数据，默认为block nonce</li>
     * <li>NextConsensus: 下一个区块共识地址</li>
     * <li>0: 固定值0</li>
     * </ul>
     *
     * @param writer 二进制输出流
     */
    @Override
    public void serialize(BinaryWriter writer) {
        super.serialize(writer);
        writer.writeByte((byte) 0);
    }

    /**
     * 获取hash code
     *
     * @return 区块哈希的hashcode
     */
    @Override
    public int hashCode() {
        return hash().hashCode();
    }

    /**
     * 比较区块头
     *
     * @param other 待比较对象
     * @return 若待比较区块头为null，返回false。否则按哈希值比较
     */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof Header)) {
            return false;
        }
        return hash().equals(((Header) other).hash());
    }
}
