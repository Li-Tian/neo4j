package neo.wallets.SQLite;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: Address
 * @Package neo.wallets.SQLite
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 17:16 2019/3/14
 */
public class Address {
    public byte[] ScriptHash;

    public Address() {
    }

    public Address(byte[] scriptHash) {
        ScriptHash = scriptHash;
    }

    public byte[] getScriptHash() {
        return ScriptHash;
    }

    public void setScriptHash(byte[] scriptHash) {
        ScriptHash = scriptHash;
    }
}