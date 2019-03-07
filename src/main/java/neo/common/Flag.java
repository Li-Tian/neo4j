package neo.common;

import java.util.Objects;

import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.ISerializable;

/**
 * 标志类
 */
public class Flag implements ISerializable {

    protected byte value;

    public Flag(byte value) {
        this.value = value;
    }


    /**
     * 属性值
     */
    public byte value() {
        return value;
    }

    /**
     * 是否包含某属性
     *
     * @param flag 属性
     */
    public boolean hasFlag(Flag flag) {
        return (this.value & flag.value) != (byte) 0x00;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Flag that = (Flag) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public int size() {
        return Byte.BYTES;
    }

    @Override
    public void serialize(BinaryWriter writer) {
        writer.writeByte(value);
    }

    @Override
    public void deserialize(BinaryReader reader) {
        this.value = (byte) reader.readByte();
    }
}
