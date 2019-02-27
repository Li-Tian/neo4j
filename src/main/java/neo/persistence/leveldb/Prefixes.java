package neo.persistence.leveldb;

public class Prefixes {

    public static final byte DATA_Block = (byte) 0x01;
    public static final byte DATA_Transaction = (byte) 0x02;

    public static final byte ST_Account = (byte) 0x40;
    public static final byte ST_Coin = (byte) 0x44;
    public static final byte ST_SpentCoin = (byte) 0x45;
    public static final byte ST_Validator = (byte) 0x48;
    public static final byte ST_Asset = (byte) 0x4c;
    public static final byte ST_Contract = (byte) 0x50;
    public static final byte ST_Storage = (byte) 0x70;

    public static final byte IX_HeaderHashList = (byte) 0x80;
    public static final byte IX_ValidatorsCount = (byte) 0x90;
    public static final byte IX_CurrentBlock = (byte) 0xc0;
    public static final byte IX_CurrentHeader = (byte) 0xc1;

    public static final byte SYS_Version = (byte) 0xf0;
}
