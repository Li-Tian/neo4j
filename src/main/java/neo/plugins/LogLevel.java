package neo.plugins;

import neo.csharp.common.ByteEnum;

public enum LogLevel implements ByteEnum {
    Fatal((byte) 0x00),
    Error((byte) 0x01),
    Warning((byte) 0x02),
    Info((byte) 0x03),
    Debug((byte) 0x04);

    private byte value;

    LogLevel(byte value) {
        this.value = value;
    }

    /**
     * 获取类型值
     */
    @Override
    public byte value() {
        return value;
    }

    public static LogLevel parse(byte type) {
        return ByteEnum.parse(LogLevel.values(), type);
    }

    public static byte[] toBytes(LogLevel[] types) {
        return ByteEnum.toBytes(types);
    }
}