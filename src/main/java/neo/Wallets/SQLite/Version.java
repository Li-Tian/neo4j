package neo.Wallets.SQLite;

import java.math.BigInteger;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: Version
 * @Package neo.Wallets.SQLite
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 17:23 2019/3/15
 */
public class Version {
    private int major = 0;
    private int minor = 0;
    private int build = 0;
    private int revision = 0;
    private byte[] value=new byte[16];

    public Version() {
    }

    public Version(int major, int minor, int build, int revision) {
        this.major = major;
        this.minor = minor;
        this.build = build;
        this.revision = revision;
    }

    public Version(byte[] buffer) {
        byte[] temarray=new byte[4];
        System.arraycopy(buffer, 0, temarray, 0, 4);
        this.major = new BigInteger(temarray).intValue();
        System.arraycopy(buffer, 4, temarray, 0, 4);
        this.minor = new BigInteger(buffer).intValue();
        System.arraycopy(buffer, 8, temarray, 0, 4);
        this.build = new BigInteger(temarray).intValue();
        System.arraycopy(buffer, 12, temarray, 0, 4);
        this.revision = new BigInteger(temarray).intValue();
        this.value=buffer;
    }

    public String toString(){
        return major+"."+minor+"."+build+"."+revision;
    }

    public byte[] getValue() {
        return value;
    }
}