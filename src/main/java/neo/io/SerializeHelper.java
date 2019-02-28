package neo.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Supplier;

import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;

public class SerializeHelper {

    public static byte[] toBytes(ISerializable serializable) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(serializable.size());
        BinaryWriter writer = new BinaryWriter(stream);
        serializable.serialize(writer);
        return stream.toByteArray();
    }

    public static <TValue extends ISerializable> TValue parse(Supplier<TValue> generator, byte[] bytes) throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(bytes);
        BinaryReader reader = new BinaryReader(input);
        return reader.readSerializable(generator);
    }

}
