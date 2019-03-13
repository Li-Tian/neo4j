package neo.network.p2p.payloads;


import neo.csharp.common.ByteEnum;

/**
 * The usage of transaction attribute. for more about the new attribute usage, you can see NEP-9 in
 * details.
 *
 * @note be careful with the limit length of attribute's value.
 * <ul>
 * <li> ContractHash: the length must be 32 bytes </li>
 * <li> Vote: the length must be 32 bytes </li>
 * <li> Hash1 ~ Hash15: the length must be 32 bytes </li>
 * <li> ECDH02: the length must be 32 bytes </li>
 * <li> ECDH03: the length must be 32 bytes </li>
 * <li> Script: the length must be 20 bytes </li>
 * <li> DescriptionUrl: the max length is 255 bytes  </li>
 * <li> Description: the max length is 65535 bytes  </li>
 * <li> Remark ~ Remark15: the max length is 65535 bytes </li>
 * </ul>
 */
public enum TransactionAttributeUsage implements ByteEnum {

    /**
     * The hash of the contract, value is 0x00
     */
    ContractHash((byte) 0x00),
    /**
     * The public key which is used for ECDH. The first byte of that public key is 0x02
     */
    ECDH02((byte) 0x02),

    /**
     * The public key which is used for ECDH. The first byte of that public key is 0x03
     */
    ECDH03((byte) 0x03),

    /**
     * The additional verification for the transaction, such as share transfer, value is 0x20
     */
    Script((byte) 0x20),

    /**
     * Votes, value is 0x30
     */
    Vote((byte) 0x30),

    /**
     * The description url of external description, value is 0x81
     */
    DescriptionUrl((byte) 0x81),

    /**
     * The simple introduction, value is 0x90
     */
    Description((byte) 0x90),

    /**
     * Save customize hash value, value is 0xa1
     */
    Hash1((byte) 0xa1),

    /**
     * Save customize hash value, value is 0xa2
     */
    Hash2((byte) 0xa2),

    /**
     * Save customize hash value, value is 0xa3
     */
    Hash3((byte) 0xa3),

    /**
     * Save customize hash value, value is 0xa4
     */
    Hash4((byte) 0xa4),

    /**
     * Save customize hash value, value is 0xa5
     */
    Hash5((byte) 0xa5),

    /**
     * Save customize hash value, value is 0xa6
     */
    Hash6((byte) 0xa6),

    /**
     * Save customize hash value, value is 0xa7
     */
    Hash7((byte) 0xa7),

    /**
     * Save customize hash value, value is 0xa8
     */
    Hash8((byte) 0xa8),

    /**
     * Save customize hash value, value is 0xa9
     */
    Hash9((byte) 0xa9),

    /**
     * Save customize hash value, value is 0xaa
     */
    Hash10((byte) 0xaa),

    /**
     * Save customize hash value, value is 0xab
     */
    Hash11((byte) 0xab),

    /**
     * Save customize hash value, value is 0xac
     */
    Hash12((byte) 0xac),

    /**
     * Save customize hash value, value is 0xad
     */
    Hash13((byte) 0xad),

    /**
     * Save customize hash value, value is 0xae
     */
    Hash14((byte) 0xae),

    /**
     * Save customize hash value, value is 0xaf
     */
    Hash15((byte) 0xaf),

    /**
     * Save customize notes, value is 0xf0
     */
    Remark((byte) 0xf0),

    /**
     * Save customize notes, value is 0xf1
     */
    Remark1((byte) 0xf1),

    /**
     * Save customize notes, value is 0xf2
     */
    Remark2((byte) 0xf2),

    /**
     * Save customize notes, value is 0xf3
     */
    Remark3((byte) 0xf3),

    /**
     * Save customize notes, value is 0xf4
     */
    Remark4((byte) 0xf4),

    /**
     * Save customize notes, value is 0xf5
     */
    Remark5((byte) 0xf5),

    /**
     * Save customize notes, value is 0xf6
     */
    Remark6((byte) 0xf6),

    /**
     * Save customize notes, value is 0xf7
     */
    Remark7((byte) 0xf7),

    /**
     * Save customize notes, value is 0xf8
     */
    Remark8((byte) 0xf8),

    /**
     * Save customize notes, value is 0xf9
     */
    Remark9((byte) 0xf9),

    /**
     * Save customize notes, value is 0xfa
     */
    Remark10((byte) 0xfa),

    /**
     * Save customize notes, value is 0xfb
     */
    Remark11((byte) 0xfb),

    /**
     * Save customize notes, value is 0xfc
     */
    Remark12((byte) 0xfc),

    /**
     * Save customize notes, value is 0xfd
     */
    Remark13((byte) 0xfd),

    /**
     * Save customize notes, value is 0xfe
     */
    Remark14((byte) 0xfe),

    /**
     * Save customize notes, value is 0xff
     */
    Remark15((byte) 0xff);

    private byte value;

    TransactionAttributeUsage(byte val) {
        this.value = val;
    }

    /**
     * get the type's value
     */
    @Override
    public byte value() {
        return this.value;
    }

    /**
     * get unsigned int value, as byte is signed in java
     */
    public int getUint() {
        return this.value & 0xff;
    }


    /**
     * Parse Usage type from the byte value
     *
     * @param type TransactionAttributeUsage
     * @return TransactionAttributeUsage
     * @throws IllegalArgumentException throws the exception when the type is not exist.
     */
    public static TransactionAttributeUsage parse(byte type) {
        return ByteEnum.parse(TransactionAttributeUsage.values(), type);
    }

}
