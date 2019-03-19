package neo.Wallets;

import neo.UInt160;
import neo.smartcontract.Contract;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: WalletAccount
 * @Package neo.Wallets
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 11:35 2019/3/14
 */
public abstract class WalletAccount {
    public UInt160 scriptHash;
    public String label;
    public boolean isDefault;
    public boolean lock;
    public Contract contract;

    public String getAddress() {
        return scriptHash.toAddress();
    }

    public abstract boolean hasKey();

    public boolean watchOnly() {
        return contract == null;
    }

    public abstract KeyPair getKey();

    protected WalletAccount(UInt160 scriptHash) {
        this.scriptHash = scriptHash;
    }
}