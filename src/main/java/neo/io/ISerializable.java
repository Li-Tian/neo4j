package neo.io;

import java.io.IOException;

public interface ISerializable {
    int size();

    void serialize(BinaryWriter writer) throws IOException;

    void deserialize(BinaryReader reader) throws IOException;
}
