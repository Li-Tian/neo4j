package neo.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;

/**
 * Serialize helper, provides parse methods
 */
public class SerializeHelper {


    /**
     * Serialize the serializable array to byte array
     *
     * @param serializables the serializables array
     * @return byte array
     */
    public static byte[] toBytes(ISerializable[] serializables) {
        int size = 0;
        for (ISerializable serializable : serializables) {
            size += serializable.size();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream(size);
        BinaryWriter writer = new BinaryWriter(output);
        writer.writeArray(serializables);
        return output.toByteArray();
    }


    /**
     * Serialize to byte array
     *
     * @param serializable the object to be serialized
     * @return byte array
     */
    public static byte[] toBytes(ISerializable serializable) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(serializable.size());
        BinaryWriter writer = new BinaryWriter(stream);
        serializable.serialize(writer);
        return stream.toByteArray();
    }

    /**
     * Parse from byte array
     *
     * @param generator value generator
     * @param bytes     byte array
     * @param offset    byte array offset
     * @param <TValue>  the object to be serialize
     * @return TValue
     */
    public static <TValue extends ISerializable> TValue parse(Supplier<TValue> generator, byte[] bytes, int offset) {
        bytes = BitConverter.subBytes(bytes, offset, bytes.length);
        ByteArrayInputStream input = new ByteArrayInputStream(bytes);
        BinaryReader reader = new BinaryReader(input);
        return reader.readSerializable(generator);
    }


    /**
     * Parse from byte array
     *
     * @param generator value generator
     * @param bytes     byte array
     * @param <TValue>  the object to be serialize
     * @return TValue
     */
    public static <TValue extends ISerializable> TValue parse(Supplier<TValue> generator, byte[] bytes) {
        ByteArrayInputStream input = new ByteArrayInputStream(bytes);
        BinaryReader reader = new BinaryReader(input);
        return reader.readSerializable(generator);
    }

    /**
     * Parse value array from byte array
     *
     * @param bytes    byte array
     * @param arrayGen array generator
     * @param objGen   value generator
     * @param <TValue> the object to be serialized
     * @return TValue[]
     */
    public static <TValue extends ISerializable> TValue[] asAsSerializableArray(byte[] bytes, IntFunction<TValue[]> arrayGen, Supplier<TValue> objGen) {
        ByteArrayInputStream input = new ByteArrayInputStream(bytes);
        BinaryReader reader = new BinaryReader(input);
        return reader.readArray(arrayGen, objGen);
    }

}
