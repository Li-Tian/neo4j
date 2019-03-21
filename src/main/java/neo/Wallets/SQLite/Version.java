package neo.Wallets.SQLite;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private byte[] value = new byte[16];

    public Version() {
    }

    public Version(int major, int minor, int build, int revision) {
        this.major = major;
        this.minor = minor;
        this.build = build;
        this.revision = revision;
    }

    public Version(byte[] buffer) {
        byte[] temarray = new byte[4];
        System.arraycopy(buffer, 0, temarray, 0, 4);
        this.major = new BigInteger(temarray).intValue();
        System.arraycopy(buffer, 4, temarray, 0, 4);
        this.minor = new BigInteger(buffer).intValue();
        System.arraycopy(buffer, 8, temarray, 0, 4);
        this.build = new BigInteger(temarray).intValue();
        System.arraycopy(buffer, 12, temarray, 0, 4);
        this.revision = new BigInteger(temarray).intValue();
        this.value = buffer;
    }

    public String toString() {
        return major + "." + minor + "." + build + "." + revision;
    }

    public byte[] getValue() {
        return value;
    }

    public static Version parse(String value) {
        if (value == null) {
            throw new NullPointerException("参数不能为空");
        }
        if (value.split(".").length < 2) {
            throw new IllegalArgumentException("非法参数格式");
        }
        if (!Arrays.asList(value.split(".")).stream().allMatch(p -> {
            Pattern pattern = Pattern.compile("[0-9]*");
            Matcher isNum = pattern.matcher(p);
            if (!isNum.matches()) {
                return false;
            }
            return true;
        })){
            throw new IllegalArgumentException("非法参数格式");
        }
        String[] temp=value.split(".");
        int major = 0;
        int minor = 0;
        int build = 0;
        int revision = 0;
        for (int i=0;i<temp.length;i++){
            switch (i){
                case 0:
                    major = Integer.parseInt(temp[i]);
                    break;
                case 1:
                    minor = Integer.parseInt(temp[i]);
                    break;
                case 2:
                    build = Integer.parseInt(temp[i]);
                    break;
                case 3:
                    revision = Integer.parseInt(temp[i]);
                    break;
            }
        }
        return new Version(major,minor,build,revision);
    }
}