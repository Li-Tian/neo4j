package neo.plugins;

import java.util.HashMap;

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


    private static final HashMap<Byte, LogLevel> map = new HashMap<>();

    static {
        for (LogLevel type : LogLevel.values()) {
            map.put(type.value, type);
        }
    }

    public static LogLevel parse(byte type) {
        if (map.containsKey(type)) {
            return map.get(type);
        }
        throw new IllegalArgumentException();
    }


    public static byte[] toBytes(LogLevel[] types) {
        return ByteEnum.toBytes(types);
    }
}