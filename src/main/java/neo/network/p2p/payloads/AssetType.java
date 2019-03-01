package neo.network.p2p.payloads;


public class AssetType {

    public static final byte CreditFlag = (byte) 0x40;
    public static final byte DutyFlag = (byte) 0x80;

    public static final byte GoverningToken = (byte) 0x00;
    public static final byte UtilityToken = (byte) 0x01;
    public static final byte Currency = (byte) 0x08;
    public static final byte Share = DutyFlag | (byte) 0x10;
    public static final byte Invoice = DutyFlag | (byte) 0x18;
    public static final byte Token = DutyFlag | (byte) 0x20;


    /**
     * 占用字节数大小
     */
    public static final int BYTES = 1;
}
