package neo.wallets.SQLite;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: Contract
 * @Package neo.wallets.SQLite
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 17:17 2019/3/14
 */
public class Contract {
    public byte[] rawData;
    public byte[] scriptHash;
    public byte[] publicKeyHash;
    public Account account;
    public Address address;

    public Contract() {
    }

    public Contract(byte[] rawData, byte[] scriptHash, byte[] publicKeyHash, Account account, Address address) {
        this.rawData = rawData;
        this.scriptHash = scriptHash;
        this.publicKeyHash = publicKeyHash;
        this.account = account;
        this.address = address;
    }

    public byte[] getRawData() {
        return rawData;
    }

    public void setRawData(byte[] rawData) {
        this.rawData = rawData;
    }

    public byte[] getScriptHash() {
        return scriptHash;
    }

    public void setScriptHash(byte[] scriptHash) {
        this.scriptHash = scriptHash;
    }

    public byte[] getPublicKeyHash() {
        return publicKeyHash;
    }

    public void setPublicKeyHash(byte[] publicKeyHash) {
        this.publicKeyHash = publicKeyHash;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }
}