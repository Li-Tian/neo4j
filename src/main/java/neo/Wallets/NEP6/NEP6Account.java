package neo.Wallets.NEP6;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import neo.UInt160;
import neo.Wallets.Helper;
import neo.Wallets.KeyPair;
import neo.Wallets.Wallet;
import neo.Wallets.WalletAccount;
import neo.exception.FormatException;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: NEP6Account
 * @Package neo.Wallets.NEP6
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 14:04 2019/3/14
 */
public class NEP6Account extends WalletAccount {
    private NEP6Wallet wallet;
    private String nep2key;
    private KeyPair key;
    public JsonObject extra;

    public boolean decrypted() {
        return nep2key == null || key != null;
    }

    @Override
    public boolean hasKey() {
        return nep2key != null;
    }

    public NEP6Account(NEP6Wallet wallet, UInt160 scriptHash, String nep2key) {
        super(scriptHash);
        this.wallet = wallet;
        this.nep2key = nep2key;
    }

    public NEP6Account(NEP6Wallet wallet, UInt160 scriptHash) {
        super(scriptHash);
        this.wallet = wallet;
        this.nep2key = null;
    }


    public NEP6Account(NEP6Wallet wallet, UInt160 scriptHash, KeyPair key, String password) {
        this(wallet, scriptHash, key.export(password, wallet.scrypt.N, wallet.scrypt.R, wallet
                .scrypt.P));
        this.key = key;
    }

    public static NEP6Account fromJson(JsonObject json, NEP6Wallet wallet) {
        NEP6Account temp= new NEP6Account(wallet, Helper.toScriptHash(json.get("address")
                .getAsString()),json.get("key").getAsString());
        temp.label=json.get("label").getAsString();
        temp.isDefault=json.get("isDefault").getAsBoolean();
        temp.contract=NEP6Contract.fromJson(json.get("contract").getAsJsonObject());
        temp.extra=json.get("extra").getAsJsonObject();
        return temp;
    }

    @Override
    public KeyPair getKey() {
        if (nep2key == null) return null;
        if (key == null) {
            key = wallet.decryptKey(nep2key);
        }
        return key;
    }


    public KeyPair getKey(String password) {
        if (nep2key == null) return null;
        if (key == null) {
            key = new KeyPair(Wallet.getPrivateKeyFromNEP2(nep2key, password, wallet.scrypt.N,
                    wallet.scrypt.R, wallet.scrypt.P));
        }
        return key;
    }

    public JsonObject toJson() {
        JsonObject account = new JsonObject();
        account.addProperty("address", scriptHash.toAddress());
        account.addProperty("label", label);
        account.addProperty("isDefault", isDefault);
        account.addProperty("lock", lock);
        account.addProperty("key", nep2key);
        account.add("contract", ((NEP6Contract) contract) == null ? null : ((NEP6Contract) contract)
                .toJson());
        account.add("extra", extra);
        return account;
    }

    public boolean verifyPassword(String password) {
        try {
            Wallet.getPrivateKeyFromNEP2(nep2key, password, wallet.scrypt.N, wallet.scrypt.R,
                    wallet.scrypt.P);
            return true;
        } catch (FormatException e) {
            return false;
        }
    }
}