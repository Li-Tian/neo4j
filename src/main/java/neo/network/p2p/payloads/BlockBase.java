package neo.network.p2p.payloads;


import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;

import neo.UInt160;
import neo.UInt256;
import neo.cryptography.Crypto;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.InvalidOperationException;
import neo.log.tr.TR;
import neo.persistence.Snapshot;

/**
 * 区块基类
 */
public abstract class BlockBase implements IVerifiable {

    /**
     * 区块版本号
     */
    public Uint version;

    /**
     * 上一个区块hash
     */
    public UInt256 prevHash;

    /**
     * 交易的梅克尔根
     */
    public UInt256 merkleRoot;

    /**
     * 区块时间戳
     */
    public Uint timestamp;

    /**
     * 区块高度
     */
    public Uint index;

    /**
     * 共识附加数据，默认为block nonce。议长出块时生成的一个伪随机数
     */
    public Ulong consensusData;

    /**
     * 下一个区块共识地址，为共识节点三分之二多方签名合约地址
     */
    public UInt160 nextConsensus;

    /**
     * 见证人
     */
    public Witness witness;

    private UInt256 hash = null;

    /**
     * 区块hash
     *
     * @return UInt256
     */
    public UInt256 hash() {
        if (hash == null) {
            hash = new UInt256(Crypto.Default.hash256(this.GetMessage()));
        }
        return hash;
    }


    /**
     * 获取见证人
     *
     * @return Witness[]
     */
    @Override
    public Witness[] getWitnesses() {
        return new Witness[]{witness};
    }

    /**
     * 设置见证人
     *
     * @param witnesses 见证人列表
     */
    @Override
    public void setWitnesses(Witness[] witnesses) {
        if (witnesses.length != 1) throw new IllegalArgumentException();

    }


    /**
     * 存储大小
     */
    @Override
    public int size() {
        // C# code  Size => sizeof(uint) + PrevHash.Size + MerkleRoot.Size + sizeof(uint) + sizeof(uint) + sizeof(ulong) + NextConsensus.Size + 1 + Witness.Size;
        // 4 + 32 + 32 + 4 + 4 + 8 + 20 + 1 + （1+2+1+2） =105 + 6 => 111
        return Uint.BYTES + prevHash.size() + merkleRoot.size() + Uint.BYTES
                + Uint.BYTES + Ulong.BYTES + nextConsensus.size() + 1 + witness.size();
    }

    /**
     * 反序列化
     *
     * @param reader 二进制输入流
     */
    @Override
    public void deserialize(BinaryReader reader) {
        this.deserializeUnsigned(reader);
        if (reader.readByte() != 1) throw new IllegalArgumentException();
        witness = reader.readSerializable(() -> new Witness());
    }


    /**
     * 反序列化（区块头）
     *
     * @param reader 二进制输入
     */
    @Override
    public void deserializeUnsigned(BinaryReader reader) {
        version = reader.readUint();
        prevHash = reader.readSerializable(() -> new UInt256());
        merkleRoot = reader.readSerializable(() -> new UInt256());
        timestamp = reader.readUint();
        index = reader.readUint();
        consensusData = reader.readUlong();
        nextConsensus = reader.readSerializable(() -> new UInt160());
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
     * </ul>
     *
     * @param writer 二进制输出流
     */
    @Override
    public void serialize(BinaryWriter writer) {
        this.serializeUnsigned(writer);
        writer.writeByte((byte) 1);
        writer.writeSerializable(witness);
    }

    /**
     * 序列化（区块头）
     *
     * @param writer 二进制输出流
     */
    @Override
    public void serializeUnsigned(BinaryWriter writer) {
        writer.writeUint(version);
        writer.writeSerializable(prevHash);
        writer.writeSerializable(merkleRoot);
        writer.writeUint(timestamp);
        writer.writeUint(index);
        writer.writeUlong(consensusData);
        writer.writeSerializable(nextConsensus);
    }

    /**
     * 获取用于验证的哈希脚本。实际为当前区块共识节点三分之二多方签名合约地址。
     *
     * @param snapshot 数据库快照
     * @return 脚本哈希的数组
     */
    @Override
    public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
        if (prevHash == UInt256.Zero) {
            return new UInt160[]{witness.scriptHash()};
        }

        Header prevHeader = snapshot.getHeader(prevHash);
        if (prevHeader == null) {
            throw new InvalidOperationException();
        }
        return new UInt160[]{prevHeader.nextConsensus};
        // C# code
        //        Header prev_header = snapshot.GetHeader(PrevHash);
        //        if (prev_header == null) throw new InvalidOperationException();
        //        return new UInt160[]{prev_header.NextConsensus};
    }

    /**
     * 获取原始哈希数据
     *
     * @return 原始哈希数据
     */
    @Override
    public byte[] GetMessage() {
        return this.getHashData();
    }

    /**
     * 获取指定对象序列化后的数据
     *
     * @return 序列化后的原始数据
     */
    @Override
    public byte[] getHashData() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BinaryWriter writer = new BinaryWriter(outputStream);
        serializeUnsigned(writer);
        writer.flush();
        return outputStream.toByteArray();
    }

    /**
     * 转成json对象
     *
     * @return json对象
     */
    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("hash", hash().toString());
        jsonObject.addProperty("size", size());
        jsonObject.addProperty("version", version);
        jsonObject.addProperty("previousblockhash", prevHash.toString());
        jsonObject.addProperty("merkleroot", merkleRoot.toString());
        jsonObject.addProperty("time", timestamp);
        jsonObject.addProperty("index", index);
        jsonObject.addProperty("nonce", consensusData.toString());
        jsonObject.addProperty("nextconsensus", nextConsensus.toString());
        jsonObject.add("script", witness.toJson());
        return jsonObject;
    }

    /**
     * 根据当前区块快照，校验该区块
     *
     * @param snapshot 区块快照
     * @return 若满足以下4个条件之一，则验证节点为false。
     * <ul>
     * <li>1）若上一个区块不存在</li>
     * <li>2）若上一个区块高度加一不等于当前区块高度</li>
     * <li>3）若上一个区块时间戳大于等于当前区块时间戳</li>
     * <li>4）若见证人校验失败</li>
     * </ul>
     */
    public boolean verify(Snapshot snapshot) {
        Header prevHeader = snapshot.getHeader(prevHash);
        if (prevHeader == null) return false;
        if (!prevHeader.index.add(new Uint(1)).equals(index)) return false;
        if (prevHeader.timestamp.compareTo(timestamp) >= 0) return false;
        TR.fixMe("waiting for smartcontact");
//        TODO waiting for smartcontract
//        if (!this.verifyWitnesses(snapshot)) return false;
        return true;
    }

}
