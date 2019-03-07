package neo.persistence.leveldb;

/**
 * Leveldb表前缀
 */
public class Prefixes {

    /**
     * 区块前缀
     */
    public static final byte DATA_Block = (byte) 0x01;

    /**
     * 交易前缀
     */
    public static final byte DATA_Transaction = (byte) 0x02;


    /**
     * 账户前缀
     */
    public static final byte ST_Account = (byte) 0x40;

    /**
     * UTXO前缀
     */
    public static final byte ST_Coin = (byte) 0x44;

    /**
     * 已花费交易前缀
     */
    public static final byte ST_SpentCoin = (byte) 0x45;

    /**
     * 验证人前缀
     */
    public static final byte ST_Validator = (byte) 0x48;

    /**
     * 资产前缀
     */
    public static final byte ST_Asset = (byte) 0x4c;

    /**
     * 合约前缀
     */
    public static final byte ST_Contract = (byte) 0x50;

    /**
     * 合约存储前缀
     */
    public static final byte ST_Storage = (byte) 0x70;


    /**
     * 区块头hash列表索引前缀
     */
    public static final byte IX_HeaderHashList = (byte) 0x80;

    /**
     * 验证人个数的投票前缀
     */
    public static final byte IX_ValidatorsCount = (byte) 0x90;

    /**
     * 当前区块前缀
     */
    public static final byte IX_CurrentBlock = (byte) 0xc0;

    /**
     * 当前区块头前缀
     */
    public static final byte IX_CurrentHeader = (byte) 0xc1;


    /**
     * 系统版本号前缀
     */
    public static final byte SYS_Version = (byte) 0xf0;
}
