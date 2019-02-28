package neo.network.p2p.payloads;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import neo.UInt256;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;

public class InvPayload implements ISerializable {

    public static final int MaxHashesCount = 500;

    public InventoryType type;
    public UInt256[] hashes;

    @Override
    public int size() {
        // TODO Size => sizeof(InventoryType) + Hashes.GetVarSize();
        return 0;
    }

    @Override
    public void serialize(BinaryWriter writer) {
        writer.writeByte(type.value());
        writer.writeArray(hashes);
    }

    @Override
    public void deserialize(BinaryReader reader) {
        type = InventoryType.parse((byte) reader.readByte());
        hashes = reader.readArray(UInt256[]::new, UInt256::new, MaxHashesCount);
    }

    public static InvPayload create(InventoryType type, UInt256[] hashes) {
        InvPayload payload = new InvPayload();
        payload.type = type;
        payload.hashes = hashes;
        return payload;
    }

    public static Collection<InvPayload> createGroup(InventoryType type, UInt256[] hashes) {
        ArrayList<InvPayload> payloadArrayList = new ArrayList<>();
        for (int i = 0; i < hashes.length; i += MaxHashesCount) {
            UInt256[] arrs = (UInt256[]) Arrays.stream(hashes).skip(i).limit(MaxHashesCount).toArray();
            InvPayload invPayload = create(type, arrs);
            payloadArrayList.add(invPayload);
        }
        return payloadArrayList;
    }

}
