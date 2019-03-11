package neo.network.p2p.payloads;


import neo.common.ByteEnum;

/**
 * The enum class for the transaction type
 */
public enum TransactionType implements ByteEnum {

    /**
     * The miner transaction, value is 0x00
     */
    MinerTransaction((byte) 0x00),

    /**
     * The transaction of issuing asset, value is 0x01
     */
    IssueTransaction((byte) 0x01),

    /**
     * The transaction of issuing asset, value is 0x02
     */
    ClaimTransaction((byte) 0x02),

    /**
     * Validator entrollment transaction(Deprecated. Reference  StateTransaction), value is 0x20
     */
    EnrollmentTransaction((byte) 0x20),

    /**
     * The asset registeration transaction, value is 0x40
     */
    RegisterTransaction((byte) 0x40),

    /**
     * The normal transaction, value is 0x80
     */
    ContractTransaction((byte) 0x80),

    /**
     * The transaction of voting or applying for validators, value is 0x90
     */
    StateTransaction((byte) 0x90),

    /**
     * Deploy smart contract to blockchain, which is already deprecated.Reference
     * InvocationTransaction, value is 0xd0
     */
    PublishTransaction((byte) 0xd0),

    /**
     * Transaction execution, invoke the smart contract or the script, or deploy smart contract,
     * value is 0xd1
     */
    InvocationTransaction((byte) 0xd1);

    private byte value;

    TransactionType(byte val) {
        this.value = val;
    }

    /**
     * get the value of type
     *
     * @return byte
     */
    @Override
    public byte value() {
        return this.value;
    }

    /**
     * Parse TransactionType from byte value
     *
     * @param type type value
     * @return TransactionType
     * @throws IllegalArgumentException if the type is not exist, throw this exception
     */
    public static TransactionType parse(byte type) {
        return ByteEnum.parse(TransactionType.values(), type);
    }
}
