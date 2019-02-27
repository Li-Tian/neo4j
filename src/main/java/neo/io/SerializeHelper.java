package neo.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SerializeHelper {

    public static byte[] toBytes(ISerializable serializable) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(serializable.size());
        BinaryWriter writer = new BinaryWriter(stream);
        serializable.serialize(writer);
        return stream.toByteArray();
    }

}
