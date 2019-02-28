package neo.network.p2p.payloads;


import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;

public class Header extends BlockBase {

    @Override
    public int size() {
        return super.size() + 1;
    }

    @Override
    public void deserialize(BinaryReader reader) {
        super.deserialize(reader);
        if (reader.readByte() != 0) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void serialize(BinaryWriter writer) {
        super.serialize(writer);
        writer.writeByte((byte) 0);
    }

    @Override
    public int hashCode() {
        return hash().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof Header)) {
            return false;
        }
        return hash().equals(((Header) other).hash());
    }
}
