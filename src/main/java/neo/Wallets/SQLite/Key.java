package neo.Wallets.SQLite;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: Key
 * @Package neo.Wallets.SQLite
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 17:22 2019/3/14
 */
public class Key {
    public String name;
    public byte[] value;

    public Key() {
    }

    public Key(String name, byte[] value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }
}