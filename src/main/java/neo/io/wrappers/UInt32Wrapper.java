package neo.io.wrappers;


import neo.UInt32;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.log.notr.TR;

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
        TR.enter();
        return TR.exit(UInt32.Zero.size());
    }

    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        // NOTE 这里必须与C# uint存储大小保持一致
        this.value.serialize(writer);
        TR.exit();
    }

    @Override
    public void deserialize(BinaryReader reader) {
        TR.enter();
        // NOTE 这里必须与C# uint存储大小保持一致
        this.value.deserialize(reader);
        TR.exit();
    }

    //   C#中的 隐式转换方法
    //    public static implicit operator uint(UInt32Wrapper wrapper)
    //    {
    //        return wrapper.value;
    //    }
    public UInt32 toUint32() {
        TR.enter();
        return TR.exit(this.value);
    }

}
