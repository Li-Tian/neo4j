package neo.ledger;

import java.util.HashMap;
import java.util.Map;

import akka.io.UdpSO;
import neo.UInt256;
import neo.csharp.Uint;
import neo.csharp.Ushort;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.io.ICloneable;

public class SpentCoinState extends StateBase implements ICloneable<SpentCoinState> {

    public UInt256 transactionHash;
    public Uint transactionHeight;
    public HashMap<Ushort, Uint> items;


    @Override
    public int size() {
//        Size => base.Size + TransactionHash.Size + sizeof(uint)
//                + IO.Helper.GetVarSize(Items.Count) + Items.Count * (sizeof(ushort) + sizeof(uint));
        // TODO getVarSize
        return super.size();
    }

    @Override
    public SpentCoinState copy() {
        SpentCoinState coin = new SpentCoinState();
        coin.transactionHash = transactionHash;
        coin.transactionHeight = transactionHeight;
        coin.items = items;
        return coin;
    }

    @Override
    public void fromReplica(SpentCoinState replica) {
        this.transactionHash = replica.transactionHash;
        this.transactionHeight = replica.transactionHeight;
        this.items = replica.items;
    }

    @Override
    public void deserialize(BinaryReader reader) {
        super.deserialize(reader);
        transactionHash = reader.readSerializable(UInt256::new);
        transactionHeight = reader.readUint();
        int count = reader.readVarInt().intValue();
        items = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            Ushort index = reader.readUshort();
            Uint height = reader.readUint();
            items.put(index, height);
        }
    }

    @Override
    public void serialize(BinaryWriter writer) {
        super.serialize(writer);
        writer.writeSerializable(transactionHash);
        writer.writeUint(transactionHeight);
        writer.writeVarInt(items.size());
        for (Map.Entry<Ushort, Uint> entry : items.entrySet()) {
            writer.writeUshort(entry.getKey());
            writer.writeUint(entry.getValue());
        }
    }
}
