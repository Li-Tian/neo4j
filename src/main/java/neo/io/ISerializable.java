package neo.io;


import java.io.IOException;

public interface ISerializable {
    int size();

    void serialize(BinaryWriter writer);

    void deserialize(BinaryReader reader);
}
