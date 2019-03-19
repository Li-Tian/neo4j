package neo.smartcontract;

import java.util.HashMap;
import java.util.Map;

import neo.log.notr.TR;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: StackItemType
 * @Package neo.smartcontract
 * @Description: StackItemType枚举器
 * @date Created in 14:03 2019/3/12
 */
public enum StackItemType {
    ByteArray(0x00),
    Boolean(0x01),
    Integer(0x02),
    InteropInterface(0x40),
    Array(0x80),
    Struct(0x81),
    Map(0x82);


    //字节数据和StackItemType的映射关系
    private static final Map<Byte, StackItemType> byteToTypeMap = new HashMap<Byte, StackItemType>();

    static {
        TR.info("StackItemType枚举器初始化");
        for (StackItemType type : StackItemType.values()) {
            byteToTypeMap.put(type.getStackItemType(), type);
        }
    }
    private byte stackItemType;

    /**
     * @Author:doubi.liu
     * @description:构造函数
     * @param value 字节数据
     * @date:2019/3/11
     */
    StackItemType(int value) {
        TR.enter();
        stackItemType = (byte) value;
        TR.exit();
    }

    /**
     * @Author:doubi.liu
     * @description:构造函数
     * @param value opcode
     * @date:2019/3/11
     */
    StackItemType(StackItemType value) {
        TR.enter();
        this.stackItemType = value.getStackItemType();
        TR.exit();
    }

    /**
     * @Author:doubi.liu
     * @description:获取StackItemType的字节数据
     * @param
     * @date:2019/3/11
     */
    public byte getStackItemType() {
        TR.enter();
        return TR.exit(stackItemType);
    }

    /**
     * @Author:doubi.liu
     * @description:通过字节数据获取StackItemType
     * @param i 字节数据
     * @date:2019/3/11
     */
    public static StackItemType fromByte(byte i) {
        TR.enter();
        StackItemType type = byteToTypeMap.get(i);
        if (type == null){
            throw TR.exit(new UnsupportedOperationException());
        }
        return TR.exit(type);
    }
}