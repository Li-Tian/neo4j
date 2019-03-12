package neo.network.p2p.payloads;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import neo.UInt256;
import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;
import neo.log.notr.TR;


/**
 * The inventory payload class which record the hash of inventory data
 */
public class InvPayload implements ISerializable {

    /**
     * The maximum number of hash
     */
    public static final int MaxHashesCount = 500;

    /**
     * The type of inventory data
     */
    public InventoryType type;

    /**
     * The hashes for the stored hashes
     */
    public UInt256[] hashes;

    /**
     * The size of object
     */
    @Override
    public int size() {
        TR.enter();
        // C# code Size => sizeof(InventoryType) + Hashes.GetVarSize();
        return TR.exit(InventoryType.BYTES + BitConverter.getVarSize(hashes));
    }

    /**
     * Serialization
     *
     * @param writer The binary out writer
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        writer.writeByte(type.value());
        writer.writeArray(hashes);
        TR.exit();
    }

    /**
     * deserialization
     *
     * @param reader The binary input reader
     */
    @Override
    public void deserialize(BinaryReader reader) {
        TR.enter();
        type = InventoryType.parse((byte) reader.readByte());
        hashes = reader.readArray(UInt256[]::new, UInt256::new, MaxHashesCount);
        TR.exit();
    }

    /**
     * Construct a inventory payload object
     *
     * @param type   The type of inventory data
     * @param hashes The hash data
     * @return The inventory payload
     */
    public static InvPayload create(InventoryType type, UInt256[] hashes) {
        TR.enter();
        InvPayload payload = new InvPayload();
        payload.type = type;
        payload.hashes = hashes;
        return TR.exit(payload);
    }

    /**
     * Construct an array of inventory payload object
     *
     * @param type   The type of inventory data
     * @param hashes The hash data
     * @return IThe enumerable inventory payload through the yield, and everytime the mamximum
     * number of hashes is 500
     */
    public static Collection<InvPayload> createGroup(InventoryType type, UInt256[] hashes) {
        TR.enter();
        ArrayList<InvPayload> payloadArrayList = new ArrayList<>();
        for (int i = 0; i < hashes.length; i += MaxHashesCount) {
            UInt256[] arrs = Arrays.stream(hashes).skip(i).limit(MaxHashesCount).toArray((size) -> new UInt256[size]);
            InvPayload invPayload = create(type, arrs);
            payloadArrayList.add(invPayload);
        }
        return TR.exit(payloadArrayList);
    }

}
