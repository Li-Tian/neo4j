package neo.network.p2p.payloads;

import com.google.gson.JsonObject;


import neo.csharp.BitConverter;
import neo.csharp.Ushort;
import neo.io.BinaryReader;
import neo.io.BinaryWriter;
import neo.io.ISerializable;

public class TransactionAttribute implements ISerializable {

    public byte usage;
    public byte[] data;

    @Override
    public int size() {
        // C#
//        if (Usage == TransactionAttributeUsage.ContractHash || Usage == TransactionAttributeUsage.ECDH02 || Usage == TransactionAttributeUsage.ECDH03 || Usage == TransactionAttributeUsage.Vote || (Usage >= TransactionAttributeUsage.Hash1 && Usage <= TransactionAttributeUsage.Hash15))
//            return sizeof(TransactionAttributeUsage) + 32;
//        else if (Usage == TransactionAttributeUsage.Script)
//            return sizeof(TransactionAttributeUsage) + 20;
//        else if (Usage == TransactionAttributeUsage.DescriptionUrl)
//            return sizeof(TransactionAttributeUsage) + sizeof(byte) + Data.Length;
//                else
//        return sizeof(TransactionAttributeUsage) + Data.GetVarSize();
        // TODO getVarSize()....
        return 0;
    }


    @Override
    public void deserialize(BinaryReader reader) {
        usage = (byte) reader.readByte();
        if (usage == TransactionAttributeUsage.ContractHash
                || usage == TransactionAttributeUsage.Vote
                || (usage >= TransactionAttributeUsage.Hash1
                && usage <= TransactionAttributeUsage.Hash15)) {
            data = reader.readFully(32);
        } else if (usage == TransactionAttributeUsage.ECDH02 || usage == TransactionAttributeUsage.ECDH03) {
            data = BitConverter.merge(usage, reader.readFully(32));
        } else if (usage == TransactionAttributeUsage.Script) {
            data = reader.readFully(20);
        } else if (usage == TransactionAttributeUsage.DescriptionUrl) {
            data = reader.readFully(reader.readByte());
        } else if (usage == TransactionAttributeUsage.Description || usage >= TransactionAttributeUsage.Remark) {
            data = reader.readVarBytes(Ushort.MAX_VALUE_2.intValue());
        } else {
            throw new IllegalArgumentException();
        }
    }


    @Override
    public void serialize(BinaryWriter writer) {
        writer.writeByte(usage);

        if (usage == TransactionAttributeUsage.DescriptionUrl)
            writer.writeByte((byte) data.length);
        else if (usage == TransactionAttributeUsage.Description || usage >= TransactionAttributeUsage.Remark)
            writer.writeVarInt(data.length);
        if (usage == TransactionAttributeUsage.ECDH02 || usage == TransactionAttributeUsage.ECDH03)
            writer.write(data, 1, 32);
        else
            writer.write(data);

    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("usage", usage);
        json.addProperty("data", BitConverter.toHexString(data));
        return json;
    }
}
