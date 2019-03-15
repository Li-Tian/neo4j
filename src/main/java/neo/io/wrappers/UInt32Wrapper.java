package neo.io.wrappers;


import neo.UInt32;
import neo.csharp.Uint;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.log.notr.TR;

/**
 * A subclass of SerializableWrapper, used to encapsulate data objects of  uint type
 */
public final class UInt32Wrapper extends SerializableWrapper<Uint> {


    /**
     * Constructor without arguments
     */
    public UInt32Wrapper() {
        this.value = Uint.ZERO;
    }

    /**
     * Constructor
     *
     * @param value uint type data that needs to be encapsulated
     */
    public UInt32Wrapper(Uint value) {
        this.value = value;
    }

    /**
     * Convert a uint type data to a UInt32Wrapper object
     *
     * @param value uint value
     * @return UInt32Wrapper
     */
    public static UInt32Wrapper parseFrom(Uint value) {
        return new UInt32Wrapper(value);
    }

    /**
     * Size, the default is the size of the internally encapsulated uint type data object
     */
    @Override
    public int size() {
        TR.enter();
        return TR.exit(UInt32.Zero.size());
    }

    /**
     * Serialize method
     *
     * @param writer BinaryWriter
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        writer.writeUint(this.value);
        TR.exit();
    }

    /**
     * Deserialize method
     *
     * @param reader BinaryReader
     */
    @Override
    public void deserialize(BinaryReader reader) {
        TR.enter();
        this.value = reader.readUint();
        TR.exit();
    }

    /**
     * Convert a UInt32Wrapper object to a uint type data
     *
     * @return uint
     */
    public Uint toUint32() {
        TR.enter();
        return TR.exit(this.value);
    }

}
