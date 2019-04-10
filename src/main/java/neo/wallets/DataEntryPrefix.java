package neo.wallets;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: DataEntryPrefix
 * @Package neo.wallets
 * @Description: 数据库前缀索引
 * @date Created in 13:44 2019/3/14
 */
public class DataEntryPrefix {

    public final static byte ST_Coin = 0x44;

    public final static byte ST_Transaction = 0x48;

    public final static byte IX_Group = (byte) 0x80;

    public final static byte IX_Accounts = (byte) 0x81;

    public final static byte SYS_Version = (byte) 0xf0;
}