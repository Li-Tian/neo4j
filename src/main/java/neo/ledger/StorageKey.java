package neo.ledger;

import java.util.Arrays;

import neo.UInt160;
import neo.cryptography.Murmur3;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;

public class StorageKey implements ISerializable {

    public UInt160 scriptHash;
    public byte[] key;

    @Override
    public int size() {
        // C# code
        // ScriptHash.Size + (Key.Length / 16 + 1) * 17; why?????
        return scriptHash.size() + (key.length / 16 + 1) * 17;
    }

    @Override
    public void serialize(BinaryWriter writer) {
        writer.writeSerializable(scriptHash);
        writer.writeBytesWithGrouping(key);
    }

    @Override
    public void deserialize(BinaryReader reader) {
        scriptHash = reader.readSerializable(UInt160::new);
        key = reader.readBytesWithGrouping();
    }

    @Override
    public int hashCode() {
        //C# code
        // return ScriptHash.GetHashCode() + (int)Key.Murmur32(0);
        byte[] hashbytes = new Murmur3(Uint.ZERO).computeHash(key);
        return scriptHash.hashCode() + Integer.valueOf(BitConverter.toHexString(hashbytes), 16);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof StorageKey)) return false;
        StorageKey other = (StorageKey) obj;
        return scriptHash.equals(other.scriptHash) && Arrays.equals(key, other.key);
    }
}
