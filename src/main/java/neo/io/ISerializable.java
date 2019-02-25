package neo.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ISerializable {
    int size();

    void serialize(OutputStream writer) throws IOException;

    void deserialize(InputStream reader) throws IOException;
}
