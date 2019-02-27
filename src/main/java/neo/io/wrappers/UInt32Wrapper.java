package neo.io.wrappers;

import java.io.IOException;

import neo.UInt32;
import neo.io.BinaryReader;
import neo.io.BinaryWriter;

public final class UInt32Wrapper extends SerializableWrapper<UInt32> {

    public UInt32Wrapper() {
        this.value = UInt32.Zero;
    }

    public UInt32Wrapper(UInt32 value) {
        this.value = value;
    }

    //   C#中的 隐式转换方法
    //    public static implicit operator UInt32Wrapper(uint value)
    //    {
    //        return new UInt32Wrapper(value);
    //    }
    public static UInt32Wrapper parseFrom(UInt32 value) {
        return new UInt32Wrapper(value);
    }

    @Override
    public int size() {
        return UInt32.Zero.size();
    }

    @Override
    public void serialize(BinaryWriter writer) throws IOException {
        // NOTE 这里必须与C# uint存储大小保持一致
        this.value.serialize(writer);
    }

    @Override
    public void deserialize(BinaryReader reader) throws IOException {
        // NOTE 这里必须与C# uint存储大小保持一致
        this.value.deserialize(reader);
    }

    //   C#中的 隐式转换方法
    //    public static implicit operator uint(UInt32Wrapper wrapper)
    //    {
    //        return wrapper.value;
    //    }
    public UInt32 toUint32() {
        return this.value;
    }

}
