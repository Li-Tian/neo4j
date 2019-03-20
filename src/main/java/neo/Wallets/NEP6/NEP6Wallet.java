package neo.Wallets.NEP6;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import neo.Fixed8;
import neo.UInt160;
import neo.UInt256;
import neo.Wallets.Coin;
import neo.Wallets.KeyPair;
import neo.Wallets.SQLite.UserWallet;
import neo.Wallets.SQLite.Version;
import neo.Wallets.Wallet;
import neo.Wallets.WalletAccount;
import neo.Wallets.WalletIndexer;
import neo.Wallets.WalletTransactionEventArgs;
import neo.csharp.Uint;
import neo.csharp.common.IDisposable;
import neo.exception.FormatException;
import neo.ledger.Blockchain;
import neo.network.p2p.payloads.CoinReference;
import neo.network.p2p.payloads.Transaction;
import neo.smartcontract.Contract;
import neo.smartcontract.ContractParameterType;
import neo.smartcontract.EventHandler;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: NEP6Wallet
 * @Package neo.Wallets.NEP6
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 14:09 2019/3/14
 */
public class NEP6Wallet extends Wallet {

    private EventHandler<WalletTransactionEventArgs> walletTransaction = new EventHandler<>();

    @Override
    public EventHandler<WalletTransactionEventArgs> getWalletTransaction() {
        return walletTransaction;
    }

    private WalletIndexer indexer;
    private String path;
    private String password;
    private String name;
    public Version version;
    public ScryptParameters scrypt;
    private Map<UInt160, NEP6Account> accounts;
    private JsonObject extra;
    private Map<UInt256, Transaction> unconfirmed = new HashMap<>();

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public Uint getWalletHeight() {
        return indexer.getIndexHeight();
    }


    public NEP6Wallet(WalletIndexer indexer, String path, String name =null) {
        this.indexer = indexer;
        this.path = path;
        if (File.Exists(path)) {
            JsonObject wallet;
            StreamReader reader = new StreamReader(path);
            wallet = JsonObject.parse(reader);
            this.name = wallet.get("name")["name"] ?.AsString();
            this.version = Version.Parse(wallet["version"].AsString());
            this.scrypt = ScryptParameters.FromJson(wallet["scrypt"]);
            this.accounts = ((JArray) wallet["accounts"]).Select(p-> NEP6Account.fromJson(p, this)).
            ToDictionary(p-> p.scriptHash);
            this.extra = wallet["extra"];
            indexer.registerAccounts(accounts.Keys);
        } else {
            this.name = name;
            this.version = Version.parse("1.0");
            this.scrypt = ScryptParameters.getDefault();
            this.accounts = new HashMap<UInt160, NEP6Account>();
            this.extra = JsonObject.Null;
        }
        indexer.WalletTransaction.addListener(this);
    }

    private void addAccount(NEP6Account account, boolean is_import) {
        synchronized (accounts) {
            if (accounts.TryGetValue(account.ScriptHash, out NEP6Account account_old)) {
                account.label = account_old.Label;
                account.isDefault = account_old.IsDefault;
                account.lock = account_old.Lock;
                if (account.contract == null) {
                    account.contract = account_old.Contract;
                } else {
                    NEP6Contract contract_old = (NEP6Contract) account_old.Contract;
                    if (contract_old != null) {
                        NEP6Contract contract = (NEP6Contract) account.contract;
                        contract.parameterNames = contract_old.parameterNames;
                        contract.deployed = contract_old.deployed;
                    }
                }
                account.extra = account_old.Extra;
            } else {
                indexer.registerAccounts(new[]{
                    account.scriptHash},is_import ? 0 : Blockchain.singleton().Height);
            }
            accounts.put(account.scriptHash,account);
        }
    }

    @Override
    public void applyTransaction(Transaction tx) {
        synchronized (unconfirmed) {
            unconfirmed.put(tx.hash(),tx);
        }
        walletTransaction.invoke(this, new WalletTransactionEventArgs
        {
            Transaction = tx,
                    RelatedAccounts = tx.witnesses.Select(p = > p.ScriptHash).
            Union(tx.Outputs.Select(p = > p.ScriptHash)).Where(p = > Contains(p)).ToArray(),
                Height = null,
                Time = DateTime.UtcNow.ToTimestamp()
        });
    }

    @Override
    public boolean contains(neo.UInt160 scriptHash) {
        synchronized (accounts) {
            return accounts.containsKey(scriptHash);
        }
    }

    @Override
    public WalletAccount createAccount(byte[] privateKey) {
        KeyPair key = new KeyPair(privateKey);
        NEP6Contract contract = new NEP6Contract
        {
            script = Contract.createSignatureRedeemScript(key.publicKey),
                    ParameterList = new[]{
            ContractParameterType.Signature
        },
            ParameterNames = new[]{
            "signature"
        },
            Deployed = false
        } ;
        NEP6Account account = new NEP6Account(this, contract.ScriptHash, key, password) {
            Contract =contract
        };
        addAccount(account, false);
        return account;
    }

    @Override
    public WalletAccount createAccount(Contract contract, KeyPair key) {
        NEP6Contract nep6contract = contract as NEP6Contract;
        if (nep6contract == null) {
            nep6contract = new NEP6Contract
            {
                Script = contract.Script,
                        ParameterList = contract.ParameterList,
                        ParameterNames = contract.ParameterList.Select((p, i) =>$ "parameter{i}").
                ToArray(),
                        Deployed = false
            } ;
        }
        NEP6Account account;
        if (key == null)
            account = new NEP6Account(this, nep6contract.ScriptHash);
        else
            account = new NEP6Account(this, nep6contract.ScriptHash, key, password);
        account.Contract = nep6contract;
        AddAccount(account, false);
        return account;
    }


    @Override
    public WalletAccount createAccount(Contract contract) {
        KeyPair key =null;
        NEP6Contract nep6contract = contract as NEP6Contract;
        if (nep6contract == null) {
            nep6contract = new NEP6Contract
            {
                Script = contract.Script,
                        ParameterList = contract.ParameterList,
                        ParameterNames = contract.ParameterList.Select((p, i) =>$ "parameter{i}").
                ToArray(),
                        Deployed = false
            } ;
        }
        NEP6Account account;
        if (key == null)
            account = new NEP6Account(this, nep6contract.ScriptHash);
        else
            account = new NEP6Account(this, nep6contract.ScriptHash, key, password);
        account.Contract = nep6contract;
        AddAccount(account, false);
        return account;
    }

    @Override
    public WalletAccount createAccount(UInt160 scriptHash) {
        NEP6Account account = new NEP6Account(this, scriptHash);
        addAccount(account, true);
        return account;
    }

    public KeyPair decryptKey(String nep2key) {
        return new KeyPair(getPrivateKeyFromNEP2(nep2key, password, Scrypt.N, Scrypt.R, Scrypt.P));
    }

    @Override
    public boolean deleteAccount(UInt160 scriptHash) {
        boolean removed;
        synchronized (accounts) {
            removed = accounts.remove(scriptHash);
        }
        if (removed) {
            indexer.unregisterAccounts(new[]{
                scriptHash
            });
        }
        return removed;
    }

    @Override
    public void dispose() {
        indexer.WalletTransaction.removeListener(this);
    }

    @Override
    public Coin[] findUnspentCoins(UInt256 asset_id, neo.Fixed8 amount, UInt160[] from) {
        return findUnspentCoins(findUnspentCoins(from).toArray().Where(p = > GetAccount(p.Output
                .ScriptHash).Contract.Script.IsSignatureContract()),
        asset_id, amount) ??base.FindUnspentCoins(asset_id, amount, from);
    }

    @Override
    public WalletAccount getAccount(UInt160 scriptHash) {
        synchronized (accounts) {
            accounts.TryGetValue(scriptHash, out NEP6Account account);
            return account;
        }
    }

    @Override
    public Iterable<WalletAccount> getAccounts() {
        synchronized (accounts) {
            return accounts.values();
            yield return account;
        }
    }

    @Override
    public Iterable<Coin> getCoins(Iterable<UInt160> accounts) {
        if (unconfirmed.size() == 0)
            return indexer.getCoins(accounts);
        else
            return getCoinsInternal();
    }

    Iterable<Coin> getCoinsInternal ()
    {
        HashSet<CoinReference> inputs, claims;
        Coin[] coins_unconfirmed;
        synchronized (unconfirmed)
        {
            inputs = new HashSet<CoinReference>(unconfirmed.Values.SelectMany(p = > p.Inputs));
            claims = new HashSet<CoinReference>(unconfirmed.Values.OfType < ClaimTransaction > ().SelectMany(p = > p.Claims))
            ;
            coins_unconfirmed = unconfirmed.Values.Select(tx = > tx.Outputs.Select((o, i) =>
            new Coin
            {
                Reference = new CoinReference
                {
                    PrevHash = tx.Hash,
                            PrevIndex = (ushort) i
                },
                Output = o,
                        State = CoinState.Unconfirmed
            })).SelectMany(p = > p).ToArray();
        }
        for(Coin coin:indexer.getCoins(accounts))
        {
            if (inputs.contains(coin.reference)) {
                if (coin.output.assetId.equals(Blockchain.GoverningToken.hash()))
                    yield return new Coin
                {
                    Reference = coin.Reference,
                            Output = coin.Output,
                            State = coin.State | CoinState.Spent
                } ;
                continue;
            } else if (claims.Contains(coin.Reference)) {
                continue;
            }
            yield return coin;
        }
        HashSet<UInt160> accounts_set = new HashSet<UInt160>(accounts);
        for(Coin coin:coins_unconfirmed)
        {
            if (accounts_set.contains(coin.output.scriptHash))
                yield return coin;
        }
    }

    @Override
    public Iterable<UInt256> getTransactions() {
        for(UInt256 hash:indexer.getTransactions(accounts.keySet()))
        yield return hash;
        synchronized (unconfirmed)
        {
            foreach(UInt256 hash in unconfirmed.Keys)
            yield return hash;
        }
    }

    @Override
    public WalletAccount imports(X509Certificate2 cert) {
        KeyPair key;
        ECDsa ecdsa = cert.GetECDsaPrivateKey()
        {
            key = new KeyPair(ecdsa.ExportParameters(true).D);
        }
        NEP6Contract contract = new NEP6Contract();
        contract.script=Contract.createSignatureRedeemScript(key.publicKey);
        contract.parameterList=new ContractParameterType[]{ContractParameterType.Signature};
        contract.parameterNames=new String[]{"signature"};
        contract.deployed=false;
        NEP6Account account = new NEP6Account(this, contract.scriptHash(), key, password);
        account.contract=contract;
        addAccount(account, true);
        return account;
    }

    @Override
    public WalletAccount imports(String wif) {
        KeyPair key = new KeyPair(getPrivateKeyFromWIF(wif));
        NEP6Contract contract = new NEP6Contract();
        contract.script=Contract.createSignatureRedeemScript(key.publicKey);
        contract.parameterList=new ContractParameterType[]{ContractParameterType.Signature};
        contract.parameterNames=new String[]{"signature"};
        contract.deployed=false;
        NEP6Account account = new NEP6Account(this, contract.scriptHash(), key, password);
        account.contract=contract;
        addAccount(account, true);
        return account;
    }

    @Override
    public WalletAccount imports(String nep2, String passphrase) {
        KeyPair key = new KeyPair(getPrivateKeyFromNEP2(nep2, passphrase));
        NEP6Contract contract = new NEP6Contract();
        contract.script=Contract.createSignatureRedeemScript(key.publicKey);
        contract.parameterList=new ContractParameterType[]{ContractParameterType.Signature};
        contract.parameterNames=new String[]{"signature"};
        contract.deployed=false;
        NEP6Account account=null;
        if (scrypt.N == 16384 && scrypt.R == 8 && scrypt.P == 8)
            account = new NEP6Account(this, contract.scriptHash(), nep2);
        else
            account = new NEP6Account(this, contract.scriptHash(), key, passphrase);
        account.contract = contract;
        addAccount(account, true);
        return account;
    }

    void Lock() {
        password = null;
    }

    public static NEP6Wallet migrate(WalletIndexer indexer, String path, String db3path, String
            password) {
        UserWallet wallet_old = UserWallet.open(indexer, db3path, password);

        NEP6Wallet wallet_new = new NEP6Wallet(indexer, path, wallet_old.getName());
        wallet_new.unlock(password);
            for (WalletAccount account : wallet_old.getAccounts()) {
                wallet_new.createAccount(account.contract, account.getKey());
            }
        return wallet_new;

    }

    public void save() {
        JsonObject wallet = new JsonObject();
        wallet.addProperty("name", name);
        wallet.addProperty("version", version.toString());
        wallet.add("scrypt", scrypt.toJson());
        wallet.add("accounts",new JsonArray(accounts.Values.Select(p = > p.ToJson()));
        wallet.add("extra", extra);
        File.writeAllText(path, wallet.ToString());
    }

    public IDisposable unlock(String password) {
        if (!verifyPassword(password))
            throw new CryptographicException();
        this.password = password;
        return new WalletLocker(this);
    }

    @Override
    public boolean verifyPassword(String password) {
        synchronized (accounts) {
            NEP6Account account = accounts.values().stream().findFirst(p-> !p.decrypted());
            if (account == null) {
                account = accounts.values().stream().FirstOrDefault(p-> p.hasKey());
            }
            if (account == null) return true;
            if (account.decrypted()) {
                return account.verifyPassword(password);
            } else {
                try {
                    account.getKey(password);
                    return true;
                } catch (FormatException e) {
                    return false;
                }
            }
        }
    }

    @Override
    public void doWork(Object sender, WalletTransactionEventArgs eventArgs) {
        synchronized (unconfirmed) {
            unconfirmed.remove(eventArgs.transaction.hash());
        }
        UInt160[] relatedAccounts;
        synchronized (accounts) {
            //LINQ START
            //relatedAccounts = e.RelatedAccounts.Where(p => accounts.ContainsKey(p)).ToArray();
            relatedAccounts = Arrays.asList(eventArgs.relatedAccounts).stream().filter(p->
                    accounts.containsKey(p)).toArray(UInt160[]::new);
            //LINQ END
        }
        if (relatedAccounts.length > 0) {
            walletTransaction.invoke(this, new WalletTransactionEventArgs(
                    eventArgs.transaction,
                    relatedAccounts,
                    eventArgs.height,
                    eventArgs.time
            ));
        }
    }
}