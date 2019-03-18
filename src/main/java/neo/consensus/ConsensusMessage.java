package neo.consensus;

import java.util.HashMap;
import java.util.function.Supplier;

import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;
import neo.exception.FormatException;
import neo.io.SerializeHelper;

/**
 * Abstract consensus message
 */
public abstract class ConsensusMessage implements ISerializable {


    /**
     * consensus message type
     */
    public final ConsensusMessageType type;

    /**
     * view number
     */
    public byte viewNumber;

    private static final HashMap<ConsensusMessageType, Supplier<? extends ConsensusMessage>>
            generatorMap = new HashMap<>(4);

    protected ConsensusMessage(ConsensusMessageType type, Supplier<? extends ConsensusMessage> generator) {
        this.type = type;

        if (!generatorMap.containsKey(type)) {
            generatorMap.put(type, generator);
        }
    }

    /**
     * parse ConsensusMessage from byte array
     *
     * @param data byte array
     * @return ConsensusMessage
     */
    public static ConsensusMessage deserializeFrom(byte[] data) {
        if (data == null || data.length < 1) {
            throw new FormatException();
        }
        ConsensusMessageType type = ConsensusMessageType.parse(data[0]);
        return SerializeHelper.parse(generatorMap.get(type), data);
    }

    @Override
    public int size() {
        return ConsensusMessageType.BYTES + Byte.BYTES;
    }

    @Override
    public void serialize(BinaryWriter writer) {
        writer.writeByte(type.value());
        writer.writeByte(viewNumber);
    }


    @Override
    public void deserialize(BinaryReader reader) {
        if ((int) (type.value()) != reader.readByte())
            throw new FormatException();
        viewNumber = (byte) reader.readByte();
    }

}
