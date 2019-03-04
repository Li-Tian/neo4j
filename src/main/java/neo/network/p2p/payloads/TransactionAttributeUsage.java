package neo.network.p2p.payloads;

import neo.common.ByteEnum;

/**
 * 交易属性用途。新增的交易属性参考 NEP-9
 */
public enum TransactionAttributeUsage implements ByteEnum {

    /**
     * 外部合同的散列值
     */
    ContractHash((byte) 0x00),
    /**
     * 用于ECDH密钥交换的公钥，该公钥的第一个字节为0x02
     */
    ECDH02((byte) 0x02),

    /**
     * 用于ECDH密钥交换的公钥，该公钥的第一个字节为0x03
     */
    ECDH03((byte) 0x03),

    /**
     * 用于对交易进行额外的验证, 如股权类转账，存放收款人的脚本hash
     */
    Script((byte) 0x20),

    /**
     * 投票
     */
    Vote((byte) 0x30),

    /**
     * 外部介绍信息地址
     */
    DescriptionUrl((byte) 0x81),

    /**
     * 简短的介绍信息
     */
    Description((byte) 0x90),

    /**
     * 用于存放自定义的散列值
     */
    Hash1((byte) 0xa1),

    /**
     * 用于存放自定义的散列值
     */
    Hash2((byte) 0xa2),

    /**
     * 用于存放自定义的散列值
     */
    Hash3((byte) 0xa2),

    /**
     * 用于存放自定义的散列值
     */
    Hash4((byte) 0xa4),

    /**
     * 用于存放自定义的散列值
     */
    Hash5((byte) 0xa5),

    /**
     * 用于存放自定义的散列值
     */
    Hash6((byte) 0xa6),

    /**
     * 用于存放自定义的散列值
     */
    Hash7((byte) 0xa7),

    /**
     * 用于存放自定义的散列值
     */
    Hash8((byte) 0xa8),

    /**
     * 用于存放自定义的散列值
     */
    Hash9((byte) 0xa9),

    /**
     * 用于存放自定义的散列值
     */
    Hash10((byte) 0xaa),

    /**
     * 用于存放自定义的散列值
     */
    Hash11((byte) 0xab),

    /**
     * 用于存放自定义的散列值
     */
    Hash12((byte) 0xac),

    /**
     * 用于存放自定义的散列值
     */
    Hash13((byte) 0xad),

    /**
     * 用于存放自定义的散列值
     */
    Hash14((byte) 0xae),

    /**
     * 用于存放自定义的散列值
     */
    Hash15((byte) 0xaf),

    /**
     * 用于存放自定义的备注
     */
    Remark((byte) 0xf0),

    /**
     * 用于存放自定义的备注
     */
    Remark1((byte) 0xf1),

    /**
     * 用于存放自定义的备注
     */
    Remark2((byte) 0xf2),

    /**
     * 用于存放自定义的备注
     */
    Remark3((byte) 0xf3),

    /**
     * 用于存放自定义的备注
     */
    Remark4((byte) 0xf4),

    /**
     * 用于存放自定义的备注
     */
    Remark5((byte) 0xf5),

    /**
     * 用于存放自定义的备注
     */
    Remark6((byte) 0xf6),

    /**
     * 用于存放自定义的备注
     */
    Remark7((byte) 0xf7),

    /**
     * 用于存放自定义的备注
     */
    Remark8((byte) 0xf8),

    /**
     * 用于存放自定义的备注
     */
    Remark9((byte) 0xf9),

    /**
     * 用于存放自定义的备注
     */
    Remark10((byte) 0xfa),

    /**
     * 用于存放自定义的备注
     */
    Remark11((byte) 0xfb),

    /**
     * 用于存放自定义的备注
     */
    Remark12((byte) 0xfc),

    /**
     * 用于存放自定义的备注
     */
    Remark13((byte) 0xfd),

    /**
     * 用于存放自定义的备注
     */
    Remark14((byte) 0xfe),

    /**
     * 用于存放自定义的备注
     */
    Remark15((byte) 0xff);

    /**
     * 占用字节数大小
     */
    public static final int BYTES = Byte.BYTES;

    private byte value;

    TransactionAttributeUsage(byte val) {
        this.value = val;
    }

    /**
     * 查询TransactionAttributeUsage类型的具体byte值
     */
    @Override
    public byte value() {
        return this.value;
    }


    /**
     * 从byte中解析TransactionAttributeUsage类型
     *
     * @param type TransactionAttributeUsage类型
     * @return TransactionAttributeUsage
     * @throws IllegalArgumentException 当类型不存在时，抛出该异常
     */
    public static TransactionAttributeUsage parse(byte type) {
        return ByteEnum.parse(TransactionAttributeUsage.values(), type);
    }

}
