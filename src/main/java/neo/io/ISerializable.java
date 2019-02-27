package neo.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ISerializable {
    int size();

    void serialize(BinaryWriter writer) throws IOException;

    void deserialize(BinaryReader reader) throws IOException;
}
