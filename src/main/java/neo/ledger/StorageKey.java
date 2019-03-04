package neo.ledger;

import java.util.Arrays;

import neo.UInt160;
import neo.cryptography.Murmur3;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;
import neo.log.tr.TR;

/**
 * 智能合约存储的键
 */
public class StorageKey implements ISerializable {

    /**
     * 合约脚本hash
     */
    public UInt160 scriptHash;

    /**
     * 具体的键
     */
    public byte[] key;

    /**
     * 序列化的字节大小
     */
    @Override
    public int size() {
        TR.enter();
        // C# code
        // ScriptHash.Size + (Key.Length / 16 + 1) * 17; why?????
        return TR.exit(scriptHash.size() + BitConverter.getGroupVarSize(key));
    }


    /**
     * 序列化
     *
     * @param writer 二进制输出器
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        writer.writeSerializable(scriptHash);
        writer.writeBytesWithGrouping(key);
        TR.exit();
    }

    /**
     * 反序列化
     *
     * @param reader 二进制读入器
     */
    @Override
    public void deserialize(BinaryReader reader) {
        TR.enter();
        scriptHash = reader.readSerializable(UInt160::new);
        key = reader.readBytesWithGrouping();
        TR.exit();
    }

    /**
     * 获取hash code
     *
     * @return >等于脚本的hash code 加上key的murmur32值
     */
    @Override
    public int hashCode() {
        //C# code
        // return ScriptHash.GetHashCode() + (int)Key.Murmur32(0);
        TR.enter();
        byte[] hashbytes = new Murmur3(Uint.ZERO).computeHash(key);
        return TR.exit(scriptHash.hashCode() + Long.valueOf(BitConverter.toHexString(hashbytes), 16).hashCode());
    }

    /**
     * 比较两个合约存储的键是否相等。
     *
     * @param obj 待对比的智能合约存储键
     * @return 是否相等
     * @doc 若obj是null或不是StorageKey 返回false，否则进行对比合约脚本相等且键值相等。
     */
    @Override
    public boolean equals(Object obj) {
        TR.enter();
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof StorageKey)) {
            return false;
        }

        StorageKey other = (StorageKey) obj;
        return TR.exit(scriptHash.equals(other.scriptHash) && Arrays.equals(key, other.key));
    }
}
