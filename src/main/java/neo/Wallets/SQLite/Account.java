package neo.Wallets.SQLite;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: Account
 * @Package neo.Wallets.SQLite
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 17:14 2019/3/14
 */
public class Account {
    public byte[] privateKeyEncrypted;
    public byte[] publicKeyHash;

    public Account() {
    }

    public Account(byte[] privateKeyEncrypted, byte[] publicKeyHash) {
        this.privateKeyEncrypted = privateKeyEncrypted;
        this.publicKeyHash = publicKeyHash;
    }

    public byte[] getPrivateKeyEncrypted() {
        return privateKeyEncrypted;
    }

    public void setPrivateKeyEncrypted(byte[] privateKeyEncrypted) {
        this.privateKeyEncrypted = privateKeyEncrypted;
    }

    public byte[] getPublicKeyHash() {
        return publicKeyHash;
    }

    public void setPublicKeyHash(byte[] publicKeyHash) {
        this.publicKeyHash = publicKeyHash;
    }
}