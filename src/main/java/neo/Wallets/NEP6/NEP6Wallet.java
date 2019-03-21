package neo.Wallets.NEP6;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
import neo.csharp.Ushort;
import neo.csharp.common.IDisposable;
import neo.exception.FormatException;
import neo.ledger.Blockchain;
import neo.ledger.CoinState;
import neo.log.notr.TR;
import neo.network.p2p.payloads.ClaimTransaction;
import neo.network.p2p.payloads.CoinReference;
import neo.network.p2p.payloads.Transaction;
import neo.smartcontract.Contract;
import neo.smartcontract.ContractParameterType;
import neo.smartcontract.EventHandler;
import neo.smartcontract.Helper;

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


    public NEP6Wallet(WalletIndexer indexer, String path, String name) {
        this.indexer = indexer;
        this.path = path;
        File file = new File(path);
        if (file.exists()) {
            try {
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(file)));
                StringBuffer stringBuffer = new StringBuffer();
                String temp = "";
                while ((temp = bufferedReader.readLine()) != null) {
                    stringBuffer.append(temp);
                }
                String tempString = stringBuffer.toString();
                // 解析,创建Gson,需要导入gson的jar包
                JsonObject wallet=new Gson().fromJson(tempString,JsonObject.class);
                this.name = wallet.get("name")==null?null:wallet.get("name").getAsString();
                this.version = Version.parse(wallet.get("version").getAsString());
                this.scrypt = ScryptParameters.FromJson(wallet.get("scrypt").getAsJsonObject());
                this.accounts = StreamSupport.stream(wallet.get("accounts").getAsJsonArray()
                        .spliterator(),false).map(p->NEP6Account.fromJson(p.getAsJsonObject(),this))
                        .collect(Collectors.toMap(q->q.scriptHash,q->q));

                this.extra = wallet.get("extra").getAsJsonObject();
                indexer.registerAccounts(accounts.keySet());
            }catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            this.name = name;
            this.version = Version.parse("1.0");
            this.scrypt = ScryptParameters.getDefault();
            this.accounts = new HashMap<UInt160, NEP6Account>();
            this.extra = null;
        }
        indexer.WalletTransaction.addListener(this);
    }


    public NEP6Wallet(WalletIndexer indexer, String path) {
        String name =null;
        this.indexer = indexer;
        this.path = path;
        File file = new File(path);
        if (file.exists()) {
            try {
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(file)));
                StringBuffer stringBuffer = new StringBuffer();
                String temp = "";
                while ((temp = bufferedReader.readLine()) != null) {
                    stringBuffer.append(temp);
                }
                String tempString = stringBuffer.toString();
                // 解析,创建Gson,需要导入gson的jar包
                JsonObject wallet=new Gson().fromJson(tempString,JsonObject.class);
                this.name = wallet.get("name")==null?null:wallet.get("name").getAsString();
                this.version = Version.parse(wallet.get("version").getAsString());
                this.scrypt = ScryptParameters.FromJson(wallet.get("scrypt").getAsJsonObject());
                this.accounts = StreamSupport.stream(wallet.get("accounts").getAsJsonArray()
                        .spliterator(),false).map(p->NEP6Account.fromJson(p.getAsJsonObject(),this))
                        .collect(Collectors.toMap(q->q.scriptHash,q->q));

                this.extra = wallet.get("extra").getAsJsonObject();
                indexer.registerAccounts(accounts.keySet());
            }catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            this.name = name;
            this.version = Version.parse("1.0");
            this.scrypt = ScryptParameters.getDefault();
            this.accounts = new HashMap<UInt160, NEP6Account>();
            this.extra = null;
        }
        indexer.WalletTransaction.addListener(this);
    }

    private void addAccount(NEP6Account account, boolean is_import) {
        synchronized (accounts) {
            NEP6Account account_old=accounts.getOrDefault(account.scriptHash,null);
            if (account_old!=null) {
                account.label = account_old.label;
                account.isDefault = account_old.isDefault;
                account.lock = account_old.lock;
                if (account.contract == null) {
                    account.contract = account_old.contract;
                } else {
                    NEP6Contract contract_old = (NEP6Contract) account_old.contract;
                    if (contract_old != null) {
                        NEP6Contract contract = (NEP6Contract) account.contract;
                        contract.parameterNames = contract_old.parameterNames;
                        contract.deployed = contract_old.deployed;
                    }
                }
                account.extra = account_old.extra;
            } else {
                List templist=new ArrayList<>();
                templist.add(account.scriptHash);
                indexer.registerAccounts(templist,is_import ? Uint.ZERO : Blockchain.singleton().height());
            }
            accounts.put(account.scriptHash, account);
        }
    }

    @Override
    public void applyTransaction(Transaction tx) {
        synchronized (unconfirmed) {
            unconfirmed.put(tx.hash(), tx);
        }
        //LINQ START
        //RelatedAccounts = tx.Witnesses.Select(p => p.ScriptHash).Union(tx.Outputs.Select(p => p.ScriptHash)).Where(p => Contains(p)).ToArray(),

        Set<UInt160> tempSet1=Arrays.asList(tx.witnesses).stream().map(p-> p.scriptHash()).collect
                (Collectors.toSet());
        Set<UInt160> tempSet2=Arrays.asList(tx.outputs).stream().map(q-> q.scriptHash).collect
                (Collectors.toSet());
        tempSet1.addAll(tempSet2);

        walletTransaction.invoke(this,new WalletTransactionEventArgs(tx,tempSet1.stream().filter(p->contains(p))
                .toArray(UInt160[]::new),null,Uint.parseUint(String.valueOf(Calendar.getInstance()
                .getTimeInMillis()/1000))));
        //LINQ END
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
        NEP6Contract contract = new NEP6Contract();
        contract.script=Contract.createSignatureRedeemScript(key.publicKey);
        contract.parameterList=new ContractParameterType[]{ContractParameterType.Signature};
        contract.parameterNames=new String[]{"signature"};
        contract.deployed=false;
        NEP6Account account = new NEP6Account(this, contract.scriptHash(), key, password);
        account.contract=contract;
        addAccount(account, false);
        return account;
    }

    @Override
    public WalletAccount createAccount(Contract contract, KeyPair key) {
        NEP6Contract nep6contract = (NEP6Contract) contract;
        if (nep6contract == null) {
            nep6contract = new NEP6Contract();
            nep6contract.script = contract.script;
            nep6contract.parameterList = contract.parameterList;
            //LINQ START
            //ParameterNames = contract.ParameterList.Select((p, i) => $"parameter{i}").ToArray(),
            String[] tempNames = new String[contract.parameterList.length];
            for (int i = 0; i < contract.parameterList.length; i++) {
                tempNames[i] = new StringBuilder().append("parameter").append(String.valueOf(i))
                        .toString();
            }
            nep6contract.parameterNames = tempNames;
            //LINQ END
            nep6contract.deployed = false;
        }
        NEP6Account account;
        if (key == null)
            account = new NEP6Account(this, nep6contract.scriptHash());
        else
            account = new NEP6Account(this, nep6contract.scriptHash(), key, password);
        account.contract = nep6contract;
        addAccount(account, false);
        return account;
    }

    @Override
    public WalletAccount createAccount(Contract contract) {
        KeyPair key = null;
        NEP6Contract nep6contract = (NEP6Contract) contract;
        if (nep6contract == null) {
            nep6contract = new NEP6Contract();
            nep6contract.script = contract.script;
            nep6contract.parameterList = contract.parameterList;
            //LINQ START
            //ParameterNames = contract.ParameterList.Select((p, i) => $"parameter{i}").ToArray(),
            String[] tempNames = new String[contract.parameterList.length];
            for (int i = 0; i < contract.parameterList.length; i++) {
                tempNames[i] = new StringBuilder().append("parameter").append(String.valueOf(i))
                        .toString();
            }
            nep6contract.parameterNames = tempNames;
            //LINQ END
            nep6contract.deployed = false;
        }
        NEP6Account account;
        if (key == null)
            account = new NEP6Account(this, nep6contract.scriptHash());
        else
            account = new NEP6Account(this, nep6contract.scriptHash(), key, password);
        account.contract = nep6contract;
        addAccount(account, false);
        return account;
    }

    @Override
    public WalletAccount createAccount(UInt160 scriptHash) {
        NEP6Account account = new NEP6Account(this, scriptHash);
        addAccount(account, true);
        return account;
    }

    public KeyPair decryptKey(String nep2key) {
        return new KeyPair(getPrivateKeyFromNEP2(nep2key, password, scrypt.N, scrypt.R, scrypt.P));
    }

    @Override
    public boolean deleteAccount(UInt160 scriptHash) {
        NEP6Account removed;
        synchronized (accounts) {
            removed = accounts.remove(scriptHash);
        }
        if (removed != null) {
            indexer.unregisterAccounts(Arrays.asList(new UInt160[]{scriptHash}));
        }
        return removed != null ? true : false;
    }

    @Override
    public void dispose() {
        indexer.WalletTransaction.removeListener(this);
    }

    @Override
    public Coin[] findUnspentCoins(UInt256 asset_id, neo.Fixed8 amount, UInt160[] from) {
        //LINQ START
        //return FindUnspentCoins(FindUnspentCoins(from).ToArray().Where(p => GetAccount(p.Output.ScriptHash).Contract.Script.IsSignatureContract()), asset_id, amount) ?? base.FindUnspentCoins(asset_id, amount, from);
        List<Coin> templist = StreamSupport.stream(findUnspentCoins(from).spliterator(), false)
                .filter(p -> Helper.isSignatureContract(getAccount(p.output.scriptHash)
                        .contract.script)).collect(Collectors.toList());
        Coin[] tempArray = findUnspentCoins(templist, asset_id, amount);
        if (tempArray == null) {
            return super.findUnspentCoins(asset_id, amount, from);
        } else {
            return tempArray;
        }
        //LINQ END
    }

    @Override
    public WalletAccount getAccount(UInt160 scriptHash) {
        synchronized (accounts) {
            return accounts.getOrDefault(scriptHash, null);
        }
    }

    @Override
    public Iterable<NEP6Account> getAccounts() {
        synchronized (accounts) {
            return accounts.values();
        }
    }

    @Override
    public Iterable<Coin> getCoins(Iterable<UInt160> accounts) {
        if (unconfirmed.size() == 0)
            return indexer.getCoins(accounts);
        else
            return getCoinsInternal(accounts);
    }

    Iterable<Coin> getCoinsInternal(Iterable<UInt160> accounts) {
        HashSet<Coin> resultSet=new HashSet<>();
        HashSet<CoinReference> inputs;
        HashSet<CoinReference> claims;
        Coin[] coins_unconfirmed;
        synchronized (unconfirmed) {
            //LINQ START
/*            inputs = new HashSet<CoinReference>(unconfirmed.Values.SelectMany(p => p.Inputs));
            claims = new HashSet<CoinReference>(unconfirmed.Values.OfType<ClaimTransaction>().SelectMany(p => p.Claims));
            coins_unconfirmed = unconfirmed.Values.Select(tx => tx.Outputs.Select((o, i) => new Coin
            {
                Reference = new CoinReference
                {
                    PrevHash = tx.Hash,
                            PrevIndex = (ushort)i
                },
                Output = o,
                        State = CoinState.Unconfirmed
            })).SelectMany(p => p).ToArray();*/

            inputs = new HashSet<CoinReference>(unconfirmed.values().stream().flatMap(p -> Arrays.asList(p
                    .inputs).stream()).collect(Collectors.toSet()));



            claims = new HashSet<CoinReference>(unconfirmed.values().stream().filter(p->{
                if (p instanceof ClaimTransaction){
                    return true;
                }else {
                    return false;
                }
            }).flatMap(q -> Arrays.asList(((ClaimTransaction)q).claims).stream()).collect
                    (Collectors.toSet()));


            coins_unconfirmed = unconfirmed.values().stream().map(
                 tx->{
                     Coin[] result=new Coin[tx.outputs.length];
                     for (int i=0;i<tx.outputs.length;i++){
                         Coin temp=new Coin();
                         temp.reference=new CoinReference();
                         temp.reference.prevHash=tx.hash();
                         temp.reference.prevIndex= new Ushort(i);
                         temp.output=tx.outputs[i];
                         temp.state=CoinState.Unconfirmed;
                     }
                     return result;
                 }
            ).flatMap(p->Arrays.asList(p).stream()).toArray(Coin[]::new);
            //LINQ END
        }
        for (Coin coin : indexer.getCoins(accounts)) {
            if (inputs.contains(coin.reference)) {
                if (coin.output.assetId.equals(Blockchain.GoverningToken.hash())){
                    Coin tempCoin=new Coin();
                    tempCoin.reference=coin.reference;
                    tempCoin.output=coin.output;
                    tempCoin.state=new CoinState((byte) (coin.state.value()|CoinState.Spent.value()));
                    resultSet.add(tempCoin);
                }
                continue;
            } else if (claims.contains(coin.reference)) {
                continue;
            }
            resultSet.add(coin);
        }
        HashSet<UInt160> accounts_set = new HashSet<UInt160>();
        accounts.forEach(p->accounts_set.add(p));
        for (Coin coin : coins_unconfirmed) {
            if (accounts_set.contains(coin.output.scriptHash))
                resultSet.add(coin);
        }
        return resultSet;
    }

    @Override
    public Iterable<UInt256> getTransactions() {
        Set<UInt256> result = new HashSet<>();
        for (UInt256 hash : indexer.getTransactions(accounts.keySet()))
            result.add(hash);
        synchronized (unconfirmed) {
            for (UInt256 hash : unconfirmed.keySet())
                result.add(hash);
            return result;
        }
    }

/*    @Override
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
    }*/

    @Override
    public WalletAccount imports(String wif) {
        KeyPair key = new KeyPair(getPrivateKeyFromWIF(wif));
        NEP6Contract contract = new NEP6Contract();
        contract.script = Contract.createSignatureRedeemScript(key.publicKey);
        contract.parameterList = new ContractParameterType[]{ContractParameterType.Signature};
        contract.parameterNames = new String[]{"signature"};
        contract.deployed = false;
        NEP6Account account = new NEP6Account(this, contract.scriptHash(), key, password);
        account.contract = contract;
        addAccount(account, true);
        return account;
    }

    @Override
    public WalletAccount imports(String nep2, String passphrase) {
        KeyPair key = new KeyPair(getPrivateKeyFromNEP2(nep2, passphrase));
        NEP6Contract contract = new NEP6Contract();
        contract.script = Contract.createSignatureRedeemScript(key.publicKey);
        contract.parameterList = new ContractParameterType[]{ContractParameterType.Signature};
        contract.parameterNames = new String[]{"signature"};
        contract.deployed = false;
        NEP6Account account = null;
        if (scrypt.N == 16384 && scrypt.R == 8 && scrypt.P == 8)
            account = new NEP6Account(this, contract.scriptHash(), nep2);
        else
            account = new NEP6Account(this, contract.scriptHash(), key, passphrase);
        account.contract = contract;
        addAccount(account, true);
        return account;
    }

    void lock() {
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
        //LINQ START
        //wallet["accounts"] = new JArray(accounts.Values.Select(p => p.ToJson()));
        JsonArray temparray = new JsonArray();
        accounts.values().stream().map(p -> p.toJson()).forEach(p -> temparray.add(p));
        wallet.add("accounts", temparray);
        //LINQ END
        wallet.add("extra", extra);
        try {
            File writeName = new File(path);
            writeName.createNewFile();
            try (FileWriter writer = new FileWriter(writeName);
                 BufferedWriter out = new BufferedWriter(writer)
            ) {
                out.write(wallet.toString());
                out.flush();
            }
        } catch (IOException e) {
            TR.fixMe("文件写入异常");
            throw new RuntimeException(e);
        }
    }

    public IDisposable unlock(String password) {
        if (!verifyPassword(password))
            throw new RuntimeException("密码验证错误");
        this.password = password;
        return new WalletLocker(this);
    }

    @Override
    public boolean verifyPassword(String password) {
        synchronized (accounts) {
            //LINQ START
/*            NEP6Account account = accounts.values().stream().findFirst(p-> !p.decrypted());
            if (account == null) {
                account = accounts.values().stream().FirstOrDefault(p-> p.hasKey());
            }*/
            NEP6Account account = null;
            List<NEP6Account> templist = accounts.values().stream().filter(p -> !p.decrypted()).collect
                    (Collectors.toList());
            if (templist.size() != 0) {
                account = templist.get(0);
            }
            if (account == null) {
                templist = accounts.values().stream().filter(p -> p.hasKey()).collect(Collectors.toList());
                if (templist.size() != 0) {
                    account = templist.get(0);
                }
            }
            //LINQ END

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
            relatedAccounts = Arrays.asList(eventArgs.relatedAccounts).stream().filter(p ->
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