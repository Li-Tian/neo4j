package neo.network.p2p.payloads;

import com.google.gson.JsonObject;


import neo.csharp.BitConverter;
import neo.csharp.Ushort;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;


/**
 * 交易属性
 */
public class TransactionAttribute implements ISerializable {

    /**
     * 属性用途
     */
    public TransactionAttributeUsage usage;

    /**
     * 属性值
     */
    public byte[] data;

    /**
     * 存储大小
     */
    @Override
    public int size() {
        // C# code:
        //        if (Usage == TransactionAttributeUsage.ContractHash || Usage == TransactionAttributeUsage.ECDH02 || Usage == TransactionAttributeUsage.ECDH03 || Usage == TransactionAttributeUsage.Vote || (Usage >= TransactionAttributeUsage.Hash1 && Usage <= TransactionAttributeUsage.Hash15))
        //            return sizeof(TransactionAttributeUsage) + 32;
        //        else if (Usage == TransactionAttributeUsage.Script)
        //            return sizeof(TransactionAttributeUsage) + 20;
        //        else if (Usage == TransactionAttributeUsage.DescriptionUrl)
        //            return sizeof(TransactionAttributeUsage) + sizeof(byte) + Data.Length;
        //                else
        //        return sizeof(TransactionAttributeUsage) + Data.GetVarSize();
        if (usage == TransactionAttributeUsage.ContractHash || usage == TransactionAttributeUsage.ECDH02
                || usage == TransactionAttributeUsage.ECDH03 || usage == TransactionAttributeUsage.Vote
                || (usage.value() >= TransactionAttributeUsage.Hash1.value() && usage.value() <= TransactionAttributeUsage.Hash15.value())) {
            return TransactionAttributeUsage.BYTES + 32;
        } else if (usage == TransactionAttributeUsage.Script) {
            return TransactionAttributeUsage.BYTES + 20;
        } else if (usage == TransactionAttributeUsage.DescriptionUrl) {
            return TransactionAttributeUsage.BYTES + Byte.BYTES + data.length;
        } else {
            return TransactionAttributeUsage.BYTES + BitConverter.getVarSize(data);
        }
    }


    /**
     * 反序列化
     *
     * @param reader 二进制输入
     */
    @Override
    public void deserialize(BinaryReader reader) {
        usage = TransactionAttributeUsage.parse((byte) reader.readByte());
        if (usage == TransactionAttributeUsage.ContractHash
                || usage == TransactionAttributeUsage.Vote
                || (usage.value() >= TransactionAttributeUsage.Hash1.value()
                && usage.value() <= TransactionAttributeUsage.Hash15.value())) {
            data = reader.readFully(32);
        } else if (usage == TransactionAttributeUsage.ECDH02 || usage == TransactionAttributeUsage.ECDH03) {
            data = BitConverter.merge(usage.value(), reader.readFully(32));
        } else if (usage == TransactionAttributeUsage.Script) {
            data = reader.readFully(20);
        } else if (usage == TransactionAttributeUsage.DescriptionUrl) {
            data = reader.readFully(reader.readByte());
        } else if (usage == TransactionAttributeUsage.Description || usage.value() >= TransactionAttributeUsage.Remark.value()) {
            data = reader.readVarBytes(Ushort.MAX_VALUE_2.intValue());
        } else {
            throw new IllegalArgumentException();
        }
    }


    /**
     * 序列化
     *
     * @param writer 二进制输出
     */
    @Override
    public void serialize(BinaryWriter writer) {
        writer.writeByte(usage.value());

        if (usage == TransactionAttributeUsage.DescriptionUrl)
            writer.writeByte((byte) data.length);
        else if (usage == TransactionAttributeUsage.Description || usage.value() >= TransactionAttributeUsage.Remark.value())
            writer.writeVarInt(data.length);
        if (usage == TransactionAttributeUsage.ECDH02 || usage == TransactionAttributeUsage.ECDH03)
            writer.write(data, 1, 32);
        else
            writer.write(data);

    }

    /**
     * 转成json对象
     *
     * @return 转换的Json对象
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("usage", usage.value());
        json.addProperty("data", BitConverter.toHexString(data));
        return json;
    }
}
