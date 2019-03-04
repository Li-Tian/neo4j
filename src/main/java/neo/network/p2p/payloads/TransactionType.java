package neo.network.p2p.payloads;


import neo.common.ByteEnum;
import neo.exception.TypeNotExistException;

/**
 * 交易枚举类型
 */
public enum TransactionType implements ByteEnum {

    /**
     * 挖矿交易，值 0x00
     */
    MinerTransaction((byte) 0x00),

    /**
     * 发行资产交易 值 0x01
     */
    IssueTransaction((byte) 0x01),

    /**
     * Claim GAS交易 值 0x02
     */
    ClaimTransaction((byte) 0x02),

    /**
     * 注册验证人交易（已经弃用。参考StateTransaction） 值 0x20
     */
    EnrollmentTransaction((byte) 0x20),

    /**
     * 注册资产交易 值 0x40
     */
    RegisterTransaction((byte) 0x40),

    /**
     * 普通交易 值 0x80
     */
    ContractTransaction((byte) 0x80),

    /**
     * 投票或申请验证人交易 值 0x90
     */
    StateTransaction((byte) 0x90),

    /**
     * 部署智能合约到区块链 值 0xd0。已经弃用。参考 InvocationTransaction
     */
    PublishTransaction((byte) 0xd0),

    /**
     * 执行交易 值 0xd1，调用智能合约或执行脚本。或者部署智能合约。
     */
    InvocationTransaction((byte) 0xd1);

    private byte value;

    TransactionType(byte val) {
        this.value = val;
    }

    /**
     * 交易类型值
     *
     * @return byte
     */
    @Override
    public byte value() {
        return this.value;
    }

    /**
     * 解析交易类型
     *
     * @param type 交易类型值
     * @return TransactionType
     * @throws IllegalArgumentException 若交易类型不存在
     */
    public static TransactionType parse(byte type) {
        return ByteEnum.parse(TransactionType.values(), type);
    }

    /**
     * 占用字节数大小
     */
    public static final int BYTES = Byte.BYTES;
}
