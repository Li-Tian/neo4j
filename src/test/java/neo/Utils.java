package neo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.function.Supplier;

import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;

public class Utils {

    public static <T extends ISerializable> T copyFromSerialize(T serializable, Supplier<T> generator) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        BinaryWriter writer = new BinaryWriter(output);
        serializable.serialize(writer);

        ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
        BinaryReader reader = new BinaryReader(input);
        T t = generator.get();
        t.deserialize(reader);
        return t;
    }

}
