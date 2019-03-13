package neo.network.p2p.payloads;

import com.google.gson.JsonObject;


import neo.csharp.BitConverter;
import neo.csharp.Ushort;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;
import neo.log.tr.TR;


/**
 * The attribute of transaction
 *
 * @note be careful with the limit length of attribute's value.
 * <ul>
 * <li> ContractHash: the length must be 32 bytes </li>
 * <li> Vote: the length must be 32 bytes </li>
 * <li> Hash1 ~ Hash15: the length must be 32 bytes </li>
 * <li> ECDH02: the length must be 32 bytes </li>
 * <li> ECDH03: the length must be 32 bytes </li>
 * <li> Script: the length must be 20 bytes </li>
 * <li> DescriptionUrl: the max length is 255 bytes  </li>
 * <li> Description: the max length is 65535 bytes  </li>
 * <li> Remark ~ Remark15: the max length is 65535 bytes </li>
 * </ul>
 */
public class TransactionAttribute implements ISerializable {

    /**
     * The usage of attribute
     */
    public TransactionAttributeUsage usage;

    /**
     * The attribute data
     */
    public byte[] data;

    /**
     * The storage of size
     */
    @Override
    public int size() {
        TR.enter();
        // C# code:
        //        if (Usage == TransactionAttributeUsage.ContractHash
        // || Usage == TransactionAttributeUsage.ECDH02
        // || Usage == TransactionAttributeUsage.ECDH03
        // || Usage == TransactionAttributeUsage.Vote
        // || (Usage >= TransactionAttributeUsage.Hash1 && Usage <= TransactionAttributeUsage.Hash15))
        //            return sizeof(TransactionAttributeUsage) + 32;
        //        else if (Usage == TransactionAttributeUsage.Script)
        //            return sizeof(TransactionAttributeUsage) + 20;
        //        else if (Usage == TransactionAttributeUsage.DescriptionUrl)
        //            return sizeof(TransactionAttributeUsage) + sizeof(byte) + Data.Length;
        //                else
        //        return sizeof(TransactionAttributeUsage) + Data.GetVarSize();
        if (usage == TransactionAttributeUsage.ContractHash || usage == TransactionAttributeUsage.ECDH02
                || usage == TransactionAttributeUsage.ECDH03 || usage == TransactionAttributeUsage.Vote
                || (usage.getUint() >= TransactionAttributeUsage.Hash1.getUint() && usage.getUint() <= TransactionAttributeUsage.Hash15.getUint())) {
            return TR.exit(TransactionAttributeUsage.BYTES + 32);
        } else if (usage == TransactionAttributeUsage.Script) {
            return TR.exit(TransactionAttributeUsage.BYTES + 20);
        } else if (usage == TransactionAttributeUsage.DescriptionUrl) {
            return TR.exit(TransactionAttributeUsage.BYTES + Byte.BYTES + data.length);
        } else {
            return TR.exit(TransactionAttributeUsage.BYTES + BitConverter.getVarSize(data));
        }
    }


    /**
     * deserialization
     *
     * @param reader >The binary input reader
     */
    @Override
    public void deserialize(BinaryReader reader) {
        TR.enter();
        usage = TransactionAttributeUsage.parse((byte) reader.readByte());
        if (usage == TransactionAttributeUsage.ContractHash
                || usage == TransactionAttributeUsage.Vote
                || (usage.getUint() >= TransactionAttributeUsage.Hash1.getUint()
                && usage.getUint() <= TransactionAttributeUsage.Hash15.getUint())) {
            data = reader.readFully(32);
        } else if (usage == TransactionAttributeUsage.ECDH02 || usage == TransactionAttributeUsage.ECDH03) {
            data = BitConverter.merge(usage.value(), reader.readFully(32));
        } else if (usage == TransactionAttributeUsage.Script) {
            data = reader.readFully(20);
        } else if (usage == TransactionAttributeUsage.DescriptionUrl) {
            data = reader.readFully(reader.readByte());
        } else if (usage == TransactionAttributeUsage.Description || usage.getUint() >= TransactionAttributeUsage.Remark.getUint()) {
            data = reader.readVarBytes(Ushort.MAX_VALUE_2.intValue());
        } else {
            throw new IllegalArgumentException();
        }
        TR.exit();
    }


    /**
     * The serialization
     *
     * @param writer The binary output writer
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        writer.writeByte(usage.value());

        if (usage == TransactionAttributeUsage.DescriptionUrl) {
            writer.writeByte((byte) data.length);
        } else if (usage == TransactionAttributeUsage.Description
                || usage.getUint() >= TransactionAttributeUsage.Remark.getUint()) {
            writer.writeVarInt(data.length);
        }

        if (usage == TransactionAttributeUsage.ECDH02 || usage == TransactionAttributeUsage.ECDH03) {
            writer.write(data, 1, 32);
        } else {
            writer.write(data);
        }
        TR.exit();
    }

    /**
     * Convert to json object
     *
     * @return json object
     */
    public JsonObject toJson() {
        TR.enter();
        JsonObject json = new JsonObject();
        json.addProperty("usage", usage.value());
        json.addProperty("data", BitConverter.toHexString(data));
        return TR.exit(json);
    }
}
