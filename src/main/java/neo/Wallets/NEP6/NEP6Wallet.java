package neo.Wallets.NEP6;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
import neo.Wallets.SQLite.VerificationContract;
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
 * @Description: NEP6钱包类
 * @date Created in 14:09 2019/3/14
 */
public class NEP6Wallet extends Wallet {

    //钱包委托事件寄存器
    private EventHandler<WalletTransactionEventArgs> walletTransaction = new EventHandler<>();

    /**
     * @Author:doubi.liu
     * @description:获取委托寄存器
     * @date:2019/4/2
     */
    @Override
    public EventHandler<WalletTransactionEventArgs> getWalletTransaction() {
        return walletTransaction;
    }

    //钱包索引
    private WalletIndexer indexer;
    //路径
    private String path;
    //密码
    private String password;
    //名称
    private String name;
    //版本
    public Version version;
    //scrypt算法加密参数
    public ScryptParameters scrypt;
    //账户列表
    private Map<UInt160, NEP6Account> accounts;
    //扩展
    private JsonObject extra;
    //未确认交易列表
    private Map<UInt256, Transaction> unconfirmed = new HashMap<>();

    /**
     * @Author:doubi.liu
     * @description:获取名字
     * @date:2019/4/2
     */
    @Override
    public String getName() {
        TR.enter();
        return TR.exit(name);
    }

    /**
     * @Author:doubi.liu
     * @description:获取版本
     * @date:2019/4/2
     */
    @Override
    public Version getVersion() {
        TR.enter();
        return TR.exit(version);
    }

    /**
     * @Author:doubi.liu
     * @description:获取钱包高度
     * @date:2019/4/2
     */
    @Override
    public Uint getWalletHeight() {
        TR.enter();
        return TR.exit(indexer.getIndexHeight());
    }


    /**
     * @param indexer 钱包索引 path 路径 name 名字
     * @Author:doubi.liu
     * @description:构造函数
     * @date:2019/4/2
     */
    public NEP6Wallet(WalletIndexer indexer, String path, String name) {
        TR.enter();
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
                JsonObject wallet = new JsonParser().parse(tempString).getAsJsonObject();
                this.name = wallet.get("name").isJsonNull() ? null : wallet.get("name").getAsString();

                this.version = wallet.get("version").isJsonNull() ? null : Version.parse(wallet.get
                        ("version").getAsString().replace("\"", ""));
                this.scrypt = wallet.get("scrypt").isJsonNull() ? null : ScryptParameters.FromJson(wallet.get("scrypt").getAsJsonObject());
                this.accounts = wallet.get("accounts").isJsonNull() ? null : StreamSupport.stream(wallet.get("accounts").getAsJsonArray()
                        .spliterator(), false).map(p -> NEP6Account.fromJson(p.getAsJsonObject(), this))
                        .collect(Collectors.toMap(k -> k.scriptHash, v -> v));

                this.extra = wallet.get("extra").isJsonNull() ? null : wallet.get("extra").getAsJsonObject();
                indexer.registerAccounts(accounts.keySet());
            } catch (FileNotFoundException e) {
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
        indexer.walletTransaction.addListener(this);
        TR.exit();
    }

    /**
     * @Author:doubi.liu
     * @description:默认构造函数
     * @date:2019/4/2
     */
    public NEP6Wallet() {
        TR.enter();
        TR.exit();
    }

    /**
     * @param indexer 钱包索引 path 路劲
     * @Author:doubi.liu
     * @description:构造函数
     * @date:2019/4/2
     */
    public NEP6Wallet(WalletIndexer indexer, String path) {
        TR.enter();
        String name = null;
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
                JsonObject wallet = new Gson().fromJson(tempString, JsonObject.class);
                this.name = wallet.get("name") == null ? null : wallet.get("name").getAsString();
                this.version = Version.parse(wallet.get("version").getAsString());
                this.scrypt = ScryptParameters.FromJson(wallet.get("scrypt").getAsJsonObject());
                this.accounts = StreamSupport.stream(wallet.get("accounts").getAsJsonArray()
                        .spliterator(), false).map(p -> NEP6Account.fromJson(p.getAsJsonObject(), this))
                        .collect(Collectors.toMap(q -> q.scriptHash, q -> q));

                this.extra = wallet.get("extra").getAsJsonObject();
                indexer.registerAccounts(accounts.keySet());
            } catch (FileNotFoundException e) {
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
        indexer.walletTransaction.addListener(this);
        TR.exit();
    }

    /**
     * @param account 账户 is_import 是否是主账户
     * @Author:doubi.liu
     * @description:添加账户
     * @date:2019/4/2
     */
    private void addAccount(NEP6Account account, boolean is_import) {
        TR.enter();
        synchronized (accounts) {
            NEP6Account account_old = accounts.getOrDefault(account.scriptHash, null);
            if (account_old != null) {
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
                List templist = new ArrayList<>();
                templist.add(account.scriptHash);
                indexer.registerAccounts(templist, is_import ? Uint.ZERO : Blockchain.singleton().height());
            }
            accounts.put(account.scriptHash, account);
        }
        TR.exit();
    }

    /**
     * @param tx 交易
     * @Author:doubi.liu
     * @description:发送交易
     * @date:2019/4/2
     */
    @Override
    public void applyTransaction(Transaction tx) {
        TR.enter();
        synchronized (unconfirmed) {
            unconfirmed.put(tx.hash(), tx);
        }
        //LINQ START
        //RelatedAccounts = tx.Witnesses.Select(p => p.ScriptHash).Union(tx.Outputs.Select(p => p.ScriptHash)).Where(p => Contains(p)).ToArray(),

        Set<UInt160> tempSet1 = Arrays.asList(tx.witnesses).stream().map(p -> p.scriptHash()).collect
                (Collectors.toSet());
        Set<UInt160> tempSet2 = Arrays.asList(tx.outputs).stream().map(q -> q.scriptHash).collect
                (Collectors.toSet());
        tempSet1.addAll(tempSet2);

        walletTransaction.invoke(this, new WalletTransactionEventArgs(tx, tempSet1.stream().filter(p -> contains(p))
                .toArray(UInt160[]::new), null, Uint.parseUint(String.valueOf(Calendar.getInstance()
                .getTimeInMillis() / 1000))));
        TR.exit();
        //LINQ END
    }

    /**
     * @param scriptHash 哈希
     * @Author:doubi.liu
     * @description:是否包含脚本
     * @date:2019/4/2
     */
    @Override
    public boolean contains(neo.UInt160 scriptHash) {
        TR.enter();
        synchronized (accounts) {
            return TR.exit(accounts.containsKey(scriptHash));
        }
    }

    /**
     * @param privateKey 私钥
     * @Author:doubi.liu
     * @description:创建账户
     * @date:2019/4/2
     */
    @Override
    public WalletAccount createAccount(byte[] privateKey) {
        TR.enter();
        KeyPair key = new KeyPair(privateKey);
        NEP6Contract contract = new NEP6Contract();
        contract.script = Contract.createSignatureRedeemScript(key.publicKey);
        contract.parameterList = new ContractParameterType[]{ContractParameterType.Signature};
        contract.parameterNames = new String[]{"signature"};
        contract.deployed = false;
        NEP6Account account = new NEP6Account(this, contract.scriptHash(), key, password);
        account.contract = contract;
        addAccount(account, false);
        return TR.exit(account);
    }

    /**
     * @param contract 合约 key 密钥对
     * @Author:doubi.liu
     * @description:创建账户
     * @date:2019/4/2
     */
    @Override
    public WalletAccount createAccount(Contract contract, KeyPair key) {
        TR.enter();
        NEP6Contract nep6contract = null;
        if (contract instanceof NEP6Contract) {
            nep6contract = (NEP6Contract) contract;
        }
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
        return TR.exit(account);
    }

    /**
     * @param contract 合同
     * @Author:doubi.liu
     * @description:创建账户
     * @date:2019/4/2
     */
    @Override
    public WalletAccount createAccount(Contract contract) {
        TR.enter();
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
        return TR.exit(account);
    }

    /**
     * @param scriptHash 脚本哈希
     * @Author:doubi.liu
     * @description:创建账户
     * @date:2019/4/2
     */
    @Override
    public WalletAccount createAccount(UInt160 scriptHash) {
        TR.enter();
        NEP6Account account = new NEP6Account(this, scriptHash);
        addAccount(account, true);
        return TR.exit(account);
    }

    /**
     * @Author:doubi.liu
     * @description:解密nep2key
     * @date:2019/4/2
     */
    public KeyPair decryptKey(String nep2key) {
        TR.enter();
        return TR.exit(new KeyPair(getPrivateKeyFromNEP2(nep2key, password, scrypt.N, scrypt.R,
                scrypt.P)));
    }

    /**
     * @param scriptHash 脚本哈希
     * @Author:doubi.liu
     * @description:删除账户
     * @date:2019/4/2
     */
    @Override
    public boolean deleteAccount(UInt160 scriptHash) {
        TR.enter();
        NEP6Account removed;
        synchronized (accounts) {
            removed = accounts.remove(scriptHash);
        }
        if (removed != null) {
            indexer.unregisterAccounts(Arrays.asList(new UInt160[]{scriptHash}));
        }
        return TR.exit(removed != null ? true : false);
    }

    /**
     * @Author:doubi.liu
     * @description:资源回收方法
     * @date:2019/4/2
     */
    @Override
    public void dispose() {
        TR.enter();
        indexer.walletTransaction.removeListener(this);
        TR.exit();
    }

    /**
     * @param asset_id 资产id amount 数量 from 地址
     * @Author:doubi.liu
     * @description:查找未花费coin
     * @date:2019/4/2
     */
    @Override
    public Coin[] findUnspentCoins(UInt256 asset_id, neo.Fixed8 amount, UInt160[] from) {
        TR.enter();
        //LINQ START
        //return FindUnspentCoins(FindUnspentCoins(from).ToArray().Where(p => GetAccount(p.Output.ScriptHash).Contract.Script.IsSignatureContract()), asset_id, amount) ?? base.FindUnspentCoins(asset_id, amount, from);
        List<Coin> templist = StreamSupport.stream(findUnspentCoins(from).spliterator(), false)
                .filter(p -> Helper.isSignatureContract(getAccount(p.output.scriptHash)
                        .contract.script)).collect(Collectors.toList());
        Coin[] tempArray = findUnspentCoins(templist, asset_id, amount);
        if (tempArray == null) {
            return TR.exit(super.findUnspentCoins(asset_id, amount, from));
        } else {
            return TR.exit(tempArray);
        }
        //LINQ END
    }

    /**
     * @param scriptHash 脚本哈希
     * @Author:doubi.liu
     * @description:获取账户
     * @date:2019/4/2
     */
    @Override
    public WalletAccount getAccount(UInt160 scriptHash) {
        TR.enter();
        synchronized (accounts) {
            return TR.exit(accounts.getOrDefault(scriptHash, null));
        }
    }

    /**
     * @Author:doubi.liu
     * @description:获取账户集合
     * @date:2019/4/2
     */
    @Override
    public Iterable<NEP6Account> getAccounts() {
        TR.enter();
        synchronized (accounts) {
            return TR.exit(accounts.values());
        }
    }

    /**
     * @param accounts 指定账户
     * @Author:doubi.liu
     * @description:获取指定账户的coin集合
     * @date:2019/4/2
     */
    @Override
    public Iterable<Coin> getCoins(Iterable<UInt160> accounts) {
        TR.enter();
        if (unconfirmed.size() == 0) {
            return TR.exit(indexer.getCoins(accounts));
        } else {
            return TR.exit(getCoinsInternal(accounts));
        }
    }

    /**
     * @param accounts 指定账户
     * @Author:doubi.liu
     * @description:获取指定账户的coin集合
     * @date:2019/4/2
     */
    Iterable<Coin> getCoinsInternal(Iterable<UInt160> accounts) {
        TR.enter();
        HashSet<Coin> resultSet = new HashSet<>();
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


            claims = new HashSet<CoinReference>(unconfirmed.values().stream().filter(p -> {
                if (p instanceof ClaimTransaction) {
                    return true;
                } else {
                    return false;
                }
            }).flatMap(q -> Arrays.asList(((ClaimTransaction) q).claims).stream()).collect
                    (Collectors.toSet()));


            coins_unconfirmed = unconfirmed.values().stream().map(
                    tx -> {
                        Coin[] result = new Coin[tx.outputs.length];
                        for (int i = 0; i < tx.outputs.length; i++) {
                            Coin temp = new Coin();
                            temp.reference = new CoinReference();
                            temp.reference.prevHash = tx.hash();
                            temp.reference.prevIndex = new Ushort(i);
                            temp.output = tx.outputs[i];
                            temp.state = CoinState.Unconfirmed;
                        }
                        return result;
                    }
            ).flatMap(p -> Arrays.asList(p).stream()).toArray(Coin[]::new);
            //LINQ END
        }
        for (Coin coin : indexer.getCoins(accounts)) {
            if (inputs.contains(coin.reference)) {
                if (coin.output.assetId.equals(Blockchain.GoverningToken.hash())) {
                    Coin tempCoin = new Coin();
                    tempCoin.reference = coin.reference;
                    tempCoin.output = coin.output;
                    tempCoin.state = new CoinState((byte) (coin.state.value() | CoinState.Spent.value()));
                    resultSet.add(tempCoin);
                }
                continue;
            } else if (claims.contains(coin.reference)) {
                continue;
            }
            resultSet.add(coin);
        }
        HashSet<UInt160> accounts_set = new HashSet<UInt160>();
        accounts.forEach(p -> accounts_set.add(p));
        for (Coin coin : coins_unconfirmed) {
            if (accounts_set.contains(coin.output.scriptHash))
                resultSet.add(coin);
        }
        return TR.exit(resultSet);
    }

    /**
     * @Author:doubi.liu
     * @description:获取交易
     * @date:2019/4/2
     */
    @Override
    public Iterable<UInt256> getTransactions() {
        TR.enter();
        Set<UInt256> result = new HashSet<>();
        for (UInt256 hash : indexer.getTransactions(accounts.keySet())) {
            result.add(hash);
        }
        synchronized (unconfirmed) {
            for (UInt256 hash : unconfirmed.keySet()) {
                result.add(hash);
            }
            return TR.exit(result);
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

    /**
     * @param wif wif格式字符串
     * @Author:doubi.liu
     * @description:导入账户
     * @date:2019/4/2
     */
    @Override
    public WalletAccount imports(String wif) {
        TR.enter();
        KeyPair key = new KeyPair(getPrivateKeyFromWIF(wif));
        NEP6Contract contract = new NEP6Contract();
        contract.script = Contract.createSignatureRedeemScript(key.publicKey);
        contract.parameterList = new ContractParameterType[]{ContractParameterType.Signature};
        contract.parameterNames = new String[]{"signature"};
        contract.deployed = false;
        NEP6Account account = new NEP6Account(this, contract.scriptHash(), key, password);
        account.contract = contract;
        addAccount(account, true);
        return TR.exit(account);
    }

    /**
     * @param nep2 nep2字符串 passphrase密码
     * @Author:doubi.liu
     * @description:导入账户
     * @date:2019/4/2
     */
    @Override
    public WalletAccount imports(String nep2, String passphrase) {
        TR.enter();
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
        return TR.exit(account);
    }

    /**
     * @Author:doubi.liu
     * @description:锁钱包
     * @date:2019/4/2
     */
    void lock() {
        TR.enter();
        password = null;
        TR.exit();
    }

    /**
     * @param indexer 钱包索引 path 路径 db3path db3钱包路径 password 密码
     * @Author:doubi.liu
     * @description:升级钱包
     * @date:2019/4/2
     */
    public static NEP6Wallet migrate(WalletIndexer indexer, String path, String db3path, String
            password) {
        TR.enter();
        UserWallet wallet_old = UserWallet.open(indexer, db3path, password);

        NEP6Wallet wallet_new = new NEP6Wallet(indexer, path, wallet_old.getName());
        wallet_new.unlock(password);
        for (WalletAccount account : wallet_old.getAccounts()) {
            wallet_new.createAccount(account.contract, account.getKey());
        }
        return TR.exit(wallet_new);

    }

    /**
     * @Author:doubi.liu
     * @description:保存密码
     * @date:2019/4/2
     */
    public void save() {
        TR.enter();
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
            throw TR.exit(new RuntimeException(e));
        }
        TR.exit();
    }

    /**
     * @param password 密码
     * @Author:doubi.liu
     * @description:解锁钱包
     * @date:2019/4/2
     */
    public IDisposable unlock(String password) {
        TR.enter();
        if (!verifyPassword(password))
            throw TR.exit(new RuntimeException("密码验证错误"));
        this.password = password;
        return TR.exit(new WalletLocker(this));
    }

    /**
     * @param password 密码
     * @Author:doubi.liu
     * @description:确认密码
     * @date:2019/4/2
     */
    @Override
    public boolean verifyPassword(String password) {
        TR.enter();
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

            if (account == null) {
                return TR.exit(true);
            }
            if (account.decrypted()) {
                return TR.exit(account.verifyPassword(password));
            } else {
                try {
                    account.getKey(password);
                    return TR.exit(true);
                } catch (FormatException e) {
                    return TR.exit(false);
                }
            }
        }
    }

    /**
     * @param sender 消息发送者，eventArgs 消息
     * @Author:doubi.liu
     * @description:委托事件处理方法
     * @date:2019/4/2
     */
    @Override
    public void doWork(Object sender, WalletTransactionEventArgs eventArgs) {
        TR.enter();
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
        TR.exit();
    }
}