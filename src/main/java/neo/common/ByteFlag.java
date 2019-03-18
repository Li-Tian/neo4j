package neo.common;

import java.util.Objects;

import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;
import neo.log.notr.TR;

/**
 * Byte flag
 */
public class ByteFlag implements ISerializable {

    protected byte value;

    public ByteFlag(byte value) {
        this.value = value;
    }


    /**
     * byte value
     */
    public byte value() {
        TR.enter();
        return TR.exit(value);
    }

    /**
     * check whether has the specific flag
     *
     * @param flag specific flag
     */
    public boolean hasFlag(ByteFlag flag) {
        TR.enter();
        return TR.exit((this.value & flag.value) != (byte) 0x00);
    }

    /**
     * check whether the two flag are equal.
     *
     * @param o the other flag
     * @return true - the same value, otherwise is false.
     */
    @Override
    public boolean equals(Object o) {
        TR.enter();
        if (this == o) {
            return TR.exit(true);
        }
        if (o == null || getClass() != o.getClass()) {
            return TR.exit(false);
        }

        ByteFlag that = (ByteFlag) o;
        return TR.exit(value == that.value);
    }

    /**
     * get hash code
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        TR.enter();
        return TR.exit(Objects.hash(value));
    }

    /**
     * size for storage
     */
    @Override
    public int size() {
        TR.enter();
        return TR.exit(Byte.BYTES);
    }


    /**
     * serialize the flag
     *
     * @param writer BinaryWriter
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        writer.writeByte(value);
        TR.exit();
    }

    /**
     * deserialize from the reader
     *
     * @param reader BinaryReader
     */
    @Override
    public void deserialize(BinaryReader reader) {
        TR.enter();
        this.value = (byte) reader.readByte();
        TR.exit();
    }
}
