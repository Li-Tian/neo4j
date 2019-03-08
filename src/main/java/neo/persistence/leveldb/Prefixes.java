package neo.persistence.leveldb;

/**
 * Leveldb table prefix
 */
public class Prefixes {

    /**
     * block prefix
     */
    public static final byte DATA_Block = (byte) 0x01;

    /**
     * transaction prefix
     */
    public static final byte DATA_Transaction = (byte) 0x02;


    /**
     * account prefix
     */
    public static final byte ST_Account = (byte) 0x40;

    /**
     * utxo prefix
     */
    public static final byte ST_Coin = (byte) 0x44;

    /**
     * spent transaction prefix
     */
    public static final byte ST_SpentCoin = (byte) 0x45;

    /**
     * validator prefix
     */
    public static final byte ST_Validator = (byte) 0x48;

    /**
     * asset prefix
     */
    public static final byte ST_Asset = (byte) 0x4c;

    /**
     * contract prefix
     */
    public static final byte ST_Contract = (byte) 0x50;

    /**
     * contract's storage prefix
     */
    public static final byte ST_Storage = (byte) 0x70;


    /**
     * Prefix of header hash list
     */
    public static final byte IX_HeaderHashList = (byte) 0x80;

    /**
     * Prefix of validators' count table
     */
    public static final byte IX_ValidatorsCount = (byte) 0x90;

    /**
     * current block prefix
     */
    public static final byte IX_CurrentBlock = (byte) 0xc0;

    /**
     * current header prefix
     */
    public static final byte IX_CurrentHeader = (byte) 0xc1;


    /**
     * version prefix
     */
    public static final byte SYS_Version = (byte) 0xf0;
}
