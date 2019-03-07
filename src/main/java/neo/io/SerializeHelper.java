package neo.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;

public class SerializeHelper {

    public static byte[] toBytes(ISerializable serializable) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(serializable.size());
        BinaryWriter writer = new BinaryWriter(stream);
        serializable.serialize(writer);
        return stream.toByteArray();
    }

    public static <TValue extends ISerializable> TValue parse(Supplier<TValue> generator, byte[] bytes) {
        ByteArrayInputStream input = new ByteArrayInputStream(bytes);
        BinaryReader reader = new BinaryReader(input);
        return reader.readSerializable(generator);
    }

    /**
     * 从byte数组中，反序列化出数组对象
     *
     * @param bytes    待解析的byte数组
     * @param arrayGen 数组构造器
     * @param objGen   对象构造器
     * @param <TValue> 解析泛型对象
     * @return TValue[]
     */
    public static <TValue extends ISerializable> TValue[] asAsSerializableArray(byte[] bytes, IntFunction<TValue[]> arrayGen, Supplier<TValue> objGen) {
        ByteArrayInputStream input = new ByteArrayInputStream(bytes);
        BinaryReader reader = new BinaryReader(input);
        return reader.readArray(arrayGen, objGen);
    }

}
