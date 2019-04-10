package neo.wallets.SQLite;

import neo.UInt160;
import neo.wallets.KeyPair;
import neo.wallets.WalletAccount;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: UserWalletAccount
 * @Package neo.wallets.SQLite
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 17:35 2019/3/14
 */
public class UserWalletAccount extends WalletAccount {
    public KeyPair key;


    @Override
    public boolean hasKey() {
        return key != null;
    }


    public UserWalletAccount(UInt160 scriptHash) {
        super(scriptHash);
    }


    @Override
    public KeyPair getKey() {
        return key;
    }

}