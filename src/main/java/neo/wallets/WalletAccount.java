package neo.wallets;

import neo.UInt160;
import neo.smartcontract.Contract;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: WalletAccount
 * @Package neo.wallets
 * @Description: 钱包账户基类
 * @date Created in 11:35 2019/3/14
 */
public abstract class WalletAccount {
    //脚本哈希
    public UInt160 scriptHash;
    //标签
    public String label;
    //默认账户
    public boolean isDefault;
    //是否锁定
    public boolean lock;
    //合约脚本
    public Contract contract;

    /**
      * @Author:doubi.liu
      * @description:获取地址
      * @date:2019/4/3
    */
    public String getAddress() {
        return scriptHash.toAddress();
    }

    /**
      * @Author:doubi.liu
      * @description:是否存在key
      * @date:2019/4/3
    */
    public abstract boolean hasKey();

    /**
      * @Author:doubi.liu
      * @description:是否是监视账户
      * @date:2019/4/3
    */
    public boolean watchOnly() {
        return contract == null;
    }

    /**
      * @Author:doubi.liu
      * @description:获取密钥对
      * @date:2019/4/3
    */
    public abstract KeyPair getKey();

    /**
      * @Author:doubi.liu
      * @description:构造函数
      * @param scriptHash 脚本哈希
      * @date:2019/4/3
    */
    protected WalletAccount(UInt160 scriptHash) {
        this.scriptHash = scriptHash;
    }
}