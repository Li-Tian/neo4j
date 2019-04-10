package neo.Wallets.NEP6;

import com.google.gson.JsonObject;

import neo.UInt160;
import neo.Wallets.Helper;
import neo.Wallets.KeyPair;
import neo.Wallets.Wallet;
import neo.Wallets.WalletAccount;
import neo.exception.FormatException;
import neo.log.notr.TR;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: NEP6Account
 * @Package neo.Wallets.NEP6
 * @Description: NEP6Account账户类
 * @date Created in 14:04 2019/3/14
 */
public class NEP6Account extends WalletAccount {
    //NEP6类型钱包
    private NEP6Wallet wallet;
    //NEP-2格式密钥
    public String nep2key;
    //密钥对
    private KeyPair key;
    //扩展
    public JsonObject extra;

    /**
     * @Author:doubi.liu
     * @description:是否解密
     * @date:2019/4/1
     */
    public boolean decrypted() {
        TR.enter();
        return TR.exit(nep2key == null || key != null);
    }

    /**
     * @Author:doubi.liu
     * @description:是否包含密钥
     * @date:2019/4/1
     */
    @Override
    public boolean hasKey() {
        TR.enter();
        return TR.exit(nep2key != null);
    }

    /**
     * @param wallet 钱包 scriptHash 脚本哈希 nep2key nep-2格式私钥
     * @Author:doubi.liu
     * @description:构造函数
     * @date:2019/4/1
     */
    public NEP6Account(NEP6Wallet wallet, UInt160 scriptHash, String nep2key) {
        super(scriptHash);
        TR.enter();
        this.wallet = wallet;
        this.nep2key = nep2key;
        TR.exit();
    }

    /**
     * @param wallet 钱包 scriptHash 脚本哈希
     * @Author:doubi.liu
     * @description:构造函数
     * @date:2019/4/1
     */
    public NEP6Account(NEP6Wallet wallet, UInt160 scriptHash) {
        super(scriptHash);
        TR.enter();
        this.wallet = wallet;
        this.nep2key = null;
        TR.exit();
    }


    /**
     * @param wallet NEP6钱包对象,scriptHash 脚本哈希,key密钥对,password 密码
     * @Author:doubi.liu
     * @description:构造函数
     * @date:2019/4/1
     */
    public NEP6Account(NEP6Wallet wallet, UInt160 scriptHash, KeyPair key, String password) {
        this(wallet, scriptHash, key.export(password, wallet.scrypt.N, wallet.scrypt.R, wallet
                .scrypt.P));
        TR.enter();
        this.key = key;
        TR.exit();
    }

    /**
     * @param json json对象, wallet nep6钱包对象
     * @Author:doubi.liu
     * @description:json对象与NEP6Account对象互转
     * @date:2019/4/1
     */
    public static NEP6Account fromJson(JsonObject json, NEP6Wallet wallet) {
        TR.enter();
        NEP6Account temp = new NEP6Account(wallet, !json.get("address").isJsonNull() ? Helper
                .toScriptHash(json.get("address").getAsString()) : null,
                !json.get("key").isJsonNull() ? json.get("key").getAsString() : null);
        temp.label = !json.get("label").isJsonNull() ? json.get("label").getAsString() : null;
        temp.isDefault = !json.get("isDefault").isJsonNull() ? json.get("isDefault").getAsBoolean()
                : false;
        temp.contract = !json.get("contract").isJsonNull() ? NEP6Contract.fromJson(json.get("contract")
                .getAsJsonObject()) : null;
        temp.extra = !json.get("extra").isJsonNull() ? json.get("extra").getAsJsonObject() : null;
        return TR.exit(temp);
    }

    /**
     * @Author:doubi.liu
     * @description:获取密钥对
     * @date:2019/4/1
     */
    @Override
    public KeyPair getKey() {
        TR.enter();
        if (nep2key == null) {
            return TR.exit(null);
        }
        if (key == null) {
            key = wallet.decryptKey(nep2key);
        }
        return TR.exit(key);
    }


    /**
     * @param password 密码
     * @Author:doubi.liu
     * @description:获取密钥对
     * @date:2019/4/1
     */
    public KeyPair getKey(String password) {
        TR.enter();
        if (nep2key == null) {
            return TR.exit(null);
        }
        if (key == null) {
            key = new KeyPair(Wallet.getPrivateKeyFromNEP2(nep2key, password, wallet.scrypt.N,
                    wallet.scrypt.R, wallet.scrypt.P));
        }
        return TR.exit(key);
    }

    /**
     * @Author:doubi.liu
     * @description:json字符串转换
     * @date:2019/4/1
     */
    public JsonObject toJson() {
        TR.enter();
        JsonObject account = new JsonObject();
        account.addProperty("address", scriptHash.toAddress());
        account.addProperty("label", label);
        account.addProperty("isDefault", isDefault);
        account.addProperty("lock", lock);
        account.addProperty("key", nep2key);
        account.add("contract", ((NEP6Contract) contract) == null ? null : ((NEP6Contract) contract)
                .toJson());
        account.add("extra", extra);
        return TR.exit(account);
    }

    /**
     * @param password 密码
     * @Author:doubi.liu
     * @description:验证密码
     * @date:2019/4/1
     */
    public boolean verifyPassword(String password) {
        TR.enter();
        try {
            Wallet.getPrivateKeyFromNEP2(nep2key, password, wallet.scrypt.N, wallet.scrypt.R,
                    wallet.scrypt.P);
            return TR.exit(true);
        } catch (FormatException e) {
            return TR.exit(false);
        }
    }
}