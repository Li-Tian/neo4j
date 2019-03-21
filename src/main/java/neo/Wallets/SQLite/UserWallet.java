package neo.Wallets.SQLite;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import neo.Fixed8;
import neo.UInt160;
import neo.UInt256;
import neo.Wallets.Coin;
import neo.Wallets.KeyPair;
import neo.Wallets.Wallet;
import neo.Wallets.WalletAccount;
import neo.Wallets.WalletIndexer;
import neo.Wallets.WalletTransactionEventArgs;
import neo.cryptography.Helper;
import neo.csharp.Uint;
import neo.ledger.Blockchain;
import neo.network.p2p.payloads.CoinReference;
import neo.network.p2p.payloads.Transaction;
import neo.smartcontract.ContractParameterType;
import neo.smartcontract.EventHandler;

import static neo.network.p2p.payloads.TransactionType.ClaimTransaction;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: UserWallet
 * @Package neo.Wallets.SQLite
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 17:25 2019/3/14
 */
public class UserWallet extends Wallet {

    private EventHandler<WalletTransactionEventArgs> walletTransaction = new EventHandler<>();

    @Override
    public EventHandler<WalletTransactionEventArgs> getWalletTransaction() {
        return walletTransaction;
    }

    private Object db_lock = new Object();
    private WalletIndexer indexer;
    private String path;
    private byte[] iv;
    private byte[] masterKey;
    private Map<neo.UInt160, UserWalletAccount> accounts;
    private Map<UInt256, Transaction> unconfirmed = new HashMap<UInt256, Transaction>();


    @Override
    public String getName() {
        return Path.getFileNameWithoutExtension(path);
    }

    @Override
    public Uint getWalletHeight() {
        return indexer.getIndexHeight();
    }

    @Override
    public Version getVersion() {
        byte[] buffer = loadStoredData("Version");
        if (buffer == null || buffer.length < 16) return new Version(new byte[16]);
        return new Version(buffer);
    }


    private UserWallet(WalletIndexer indexer, String path, byte[] passwordKey, boolean create) {
        this.indexer = indexer;
        this.path = path;
        if (create) {
            this.iv = new byte[16];
            this.masterKey = new byte[32];
            this.accounts = new HashMap<>();
            Random rng = new Random();
            rng.nextBytes(iv);
            rng.nextBytes(masterKey);

            Version version = Assembly.GetExecutingAssembly().GetName().Version;
            buildDatabase();
            saveStoredData("PasswordHash", passwordKey.Sha256());
            saveStoredData("IV", iv);
            saveStoredData("MasterKey", masterKey.AesEncrypt(passwordKey, iv));
            saveStoredData("Version", new[]{
                version.Major, version.Minor, version.Build, version.Revision
            }.Select(p = > BitConverter.GetBytes(p)).SelectMany(p = > p).ToArray());
        } else {
            byte[] passwordHash = loadStoredData("PasswordHash");
            if (passwordHash != null && !passwordHash.SequenceEqual(passwordKey.Sha256()))
                throw new CryptographicException();
            this.iv = loadStoredData("IV");
            this.masterKey = loadStoredData("MasterKey").AesDecrypt(passwordKey, iv);
            this.accounts = loadAccounts();
            indexer.registerAccounts(accounts.keySet());
        }
        indexer.WalletTransaction.addListener(this);
    }

    private void addAccount(UserWalletAccount account, boolean is_import) {
        synchronized (accounts) {
            if (accounts.TryGetValue(account.scriptHash, out UserWalletAccount account_old)) {
                if (account.contract == null) {
                    account.contract = account_old.Contract;
                }
            } else {
                indexer.registerAccounts(new[]{
                    account.scriptHash
                },is_import ? 0 : Blockchain.singleton().height());
            }
            accounts.put(account.scriptHash, account);
        }
        synchronized (db_lock) {
            WalletDataContext ctx = new WalletDataContext(path);
            if(account.hasKey())

                {
                    byte[] decryptedPrivateKey = new byte[96];
                    System.arraycopy(account.key.publicKey.encodePoint(false), 1,
                        decryptedPrivateKey,
                            0, 64);
                    System.arraycopy(account.key.privateKey, 0, decryptedPrivateKey, 64, 32);
                    byte[] encryptedPrivateKey = Helper.encryptPrivateKey(decryptedPrivateKey);
                    Arrays.fill(decryptedPrivateKey, 0, decryptedPrivateKey.length, (byte) 0x00);
                    Account db_account = ctx.Accounts.FirstOrDefault(p = > p.PublicKeyHash.SequenceEqual(account.Key.PublicKeyHash.ToArray()))
                    ;
                    if (db_account == null) {
                        db_account = ctx.Accounts.add(new Account
                        {
                            PrivateKeyEncrypted = encryptedPrivateKey,
                                    PublicKeyHash = account.Key.PublicKeyHash.ToArray()
                        }).Entity;
                    } else {
                        db_account.privateKeyEncrypted = encryptedPrivateKey;
                    }
                }
            if(account.contract !=null)

                {
                    Contract db_contract = ctx.Contracts.FirstOrDefault(p = > p.ScriptHash.SequenceEqual(account.Contract.ScriptHash.ToArray()))
                    ;
                    if (db_contract != null) {
                        db_contract.publicKeyHash = account.key.publicKeyHash.ToArray();
                    } else {
                        ctx.Contracts.Add(new Contract
                        {
                            RawData = ((VerificationContract) account.Contract).ToArray(),
                                    ScriptHash = account.Contract.ScriptHash.ToArray(),
                                    PublicKeyHash = account.Key.PublicKeyHash.ToArray()
                        });
                    }
                }

                //add address
                {
                    Address db_address = ctx.Addresses.FirstOrDefault(p = > p.ScriptHash.SequenceEqual(account.Contract.ScriptHash.ToArray()))
                    ;
                    if (db_address == null) {
                        ctx.Addresses.add(new Address
                        {
                            ScriptHash = account.Contract.ScriptHash.ToArray()
                        });
                    }
                }
            ctx.saveChanges();

        }
    }

    @Override
    public void applyTransaction(Transaction tx) {
        synchronized (unconfirmed) {
            unconfirmed.put(tx.hash(), tx);
        }
        walletTransaction.invoke(this, new WalletTransactionEventArgs
        {
            Transaction = tx,
                    RelatedAccounts = tx.Witnesses.Select(p = > p.ScriptHash).
            Union(tx.Outputs.Select(p = > p.ScriptHash)).Where(p = > Contains(p)).ToArray(),
                Height = null,
                Time = DateTime.UtcNow.ToTimestamp()
        });
    }

    private void buildDatabase() {
        WalletDataContext ctx = new WalletDataContext(path) {
            ctx.Database.EnsureDeleted();
            ctx.Database.EnsureCreated();
        }
    }

    public boolean changePassword(String password_old, String password_new) {
        if (!verifyPassword(password_old)) return false;
        byte[] passwordKey = Helper.toAesKey(password_new);
        try {
            saveStoredData("PasswordHash", Helper.sha256(passwordKey));
            saveStoredData("MasterKey", Helper.aesEncrypt(masterKey, passwordKey, iv));
            return true;
        } finally {
            Arrays.fill(passwordKey, 0, passwordKey.length, (byte) 0x00);
        }
    }

    @Override
    public boolean contains(UInt160 scriptHash) {
        synchronized (accounts) {
            return accounts.containsKey(scriptHash);
        }
    }

    public static UserWallet create(WalletIndexer indexer, String path, String password) {
        return new UserWallet(indexer, path, Helper.toAesKey(password), true);
    }

    //// TODO: 2019/3/21
    //SecureString 未调查清楚
/*    public static UserWallet create(WalletIndexer indexer, String path, SecureString password) {
        return new UserWallet(indexer, path, password.ToAesKey(), true);
    }*/

    @Override
    public WalletAccount createAccount(byte[] privateKey) {
        KeyPair key = new KeyPair(privateKey);
        VerificationContract contract = new VerificationContract();
        contract.script = neo.smartcontract.Contract.createSignatureRedeemScript(key.publicKey);
        contract.parameterList = new ContractParameterType[]{ContractParameterType.Signature};
        UserWalletAccount account = new UserWalletAccount(contract.scriptHash());
        account.key = key;
        account.contract = contract;
        addAccount(account, false);
        return account;
    }

    @Override
    public WalletAccount createAccount(neo.smartcontract.Contract contract, KeyPair key) {
        VerificationContract verification_contract = (VerificationContract) contract;
        if (verification_contract == null) {
            verification_contract = new VerificationContract();
            verification_contract.script = contract.script;
            verification_contract.parameterList = contract.parameterList;
        }
        UserWalletAccount account = new UserWalletAccount(verification_contract.scriptHash());
        account.contract = verification_contract;
        addAccount(account, false);
        return account;
    }

    @Override
    public WalletAccount createAccount(neo.smartcontract.Contract contract) {
        KeyPair key = null;
        VerificationContract verification_contract = (VerificationContract) contract;
        if (verification_contract == null) {
            verification_contract = new VerificationContract();
            verification_contract.script = contract.script;
            verification_contract.parameterList = contract.parameterList;
        }
        UserWalletAccount account = new UserWalletAccount(verification_contract.scriptHash());
        account.contract = verification_contract;
        addAccount(account, false);
        return account;
    }

    @Override
    public WalletAccount createAccount(UInt160 scriptHash) {
        UserWalletAccount account = new UserWalletAccount(scriptHash);
        addAccount(account, true);
        return account;
    }

    private byte[] decryptPrivateKey(byte[] encryptedPrivateKey) {
        if (encryptedPrivateKey == null)
            throw new NullPointerException("encryptedPrivateKey");
        if (encryptedPrivateKey.length != 96) throw new IllegalArgumentException();
        return Helper.aesDecrypt(encryptedPrivateKey, masterKey, iv);
    }

    @Override
    public boolean deleteAccount(UInt160 scriptHash) {
        UserWalletAccount account = null;
        synchronized (accounts) {
            account = accounts.getOrDefault(scriptHash, null);
            if (account != null)
                accounts.remove(scriptHash);
        }
        if (account != null) {

            indexer.unregisterAccounts(Arrays.asList(new UInt160[]{scriptHash}));
            synchronized (db_lock) {
                WalletDataContext ctx = new WalletDataContext(path)
                if (account.hasKey()) {
                    Account db_account = ctx.Accounts.First(p = > p.PublicKeyHash.SequenceEqual(account.Key.PublicKeyHash.ToArray()))
                    ctx.accounts.remove(db_account);
                }
                if (account.contract != null)

                {
                    Contract db_contract = ctx.Contracts.First(p = > p.ScriptHash.SequenceEqual(scriptHash.ToArray()))
                    ;
                    ctx.Contracts.remove(db_contract);
                }

                //delete address
                {
                    Address db_address = ctx.Addresses.First(p = > p.ScriptHash.SequenceEqual(scriptHash.ToArray()))
                    ;
                    ctx.Addresses.Remove(db_address);
                }
                ctx.saveChanges();
            }
            return true;
        }
        return false;
    }

    @Override
    public void dispose() {
        indexer.WalletTransaction.removeListener(this);
    }

    private byte[] encryptPrivateKey(byte[] decryptedPrivateKey) {
        return Helper.aesEncrypt(decryptedPrivateKey, masterKey, iv);
    }

    @Override
    public Coin[] findUnspentCoins(UInt256 asset_id, Fixed8 amount, UInt160[] from) {
        return findUnspentCoins(findUnspentCoins(from).ToArray().Where(p-> getAccount(p.output
                .ScriptHash).Contract.Script.IsSignatureContract()),
        asset_id, amount) ??base.FindUnspentCoins(asset_id, amount, from);
    }

    @Override
    public WalletAccount getAccount(UInt160 scriptHash) {
        synchronized (accounts) {
            UserWalletAccount account = accounts.getOrDefault(scriptHash, null);
            return account;
        }
    }

    @Override
    public Iterable<WalletAccount> getAccounts() {
        synchronized (accounts) {
            List<WalletAccount> list = new ArrayList<>(accounts.values());
            return list;
        }
    }

    @Override
    public Iterable<Coin> getCoins(Iterable<UInt160> accounts) {
        if (unconfirmed.size() == 0)
            return indexer.getCoins(accounts);
        else
            return getCoinsInternal();
    }

    Iterable<Coin> getCoinsInternal() {
        HashSet<CoinReference> inputs, claims;
        Coin[] coins_unconfirmed;
        synchronized (unconfirmed) {
            inputs = new HashSet<CoinReference>(unconfirmed.values().SelectMany(p = > p.Inputs));
            claims = new HashSet<CoinReference>(unconfirmed.values().OfType < ClaimTransaction > ()
                    .SelectMany(p-> p.Claims))
            ;
            coins_unconfirmed = unconfirmed.values().Select(tx = > tx.Outputs.Select((o, i) =>
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
                if (coin.output.AssetId.Equals(Blockchain.GoverningToken.hash()))
                    yield return new Coin
                {
                    Reference = coin.Reference,
                            Output = coin.Output,
                            State = coin.State | CoinState.Spent
                } ;
                continue;
            } else if (claims.contains(coin.reference)) {
                continue;
            }
            yield return coin;
        }
        HashSet<UInt160> accounts_set = new HashSet<UInt160>(accounts);
        for (Coin coin : coins_unconfirmed) {
            if (accounts_set.contains(coin.output.scriptHash))
                yield return coin;
        }
    }

    @Override
    public Iterable<UInt256> getTransactions() {
        indexer.getTransactions(accounts.keySet());

        synchronized (unconfirmed) {
            Set<UInt256> iterable = new HashSet<>();
            iterable.addAll(indexer.getTransactions(accounts.keySet()));
            unconfirmed.
                    foreach(UInt256 hash in unconfirmed.Keys)
            yield return hash;
        }
    }

    private Map<UInt160, UserWalletAccount> loadAccounts() {
        WalletDataContext ctx = new WalletDataContext(path) {
            Map<UInt160, UserWalletAccount> accounts = ctx.Addresses.Select(p = > p.ScriptHash).

            AsEnumerable().

            Select(p =>new UserWalletAccount(new UInt160(p))).

            ToDictionary(p =>p.ScriptHash);

            foreach(Contract db_contract in ctx.Contracts.Include(p=>p.Account))

            {
                VerificationContract contract = db_contract.RawData.AsSerializable < VerificationContract > ();
                UserWalletAccount account = accounts[contract.ScriptHash];
                account.contract = contract;
                account.key = new KeyPair(DecryptPrivateKey(db_contract.Account
                        .PrivateKeyEncrypted));
            }
            return accounts;
        }
    }

    private byte[] loadStoredData(String name) {
        WalletDataContext ctx = new WalletDataContext(path) {
            return ctx.Keys.FirstOrDefault(p =>p.Name ==name)?.Value;
        }
    }

    public static UserWallet open(WalletIndexer indexer, String path, String password) {
        return new UserWallet(indexer, path, Helper.toAesKey(password), false);
    }

    //// TODO: 2019/3/21
    //SecureString java 實現未調查清楚
/*    public static UserWallet open(WalletIndexer indexer, String path, SecureString password) {
        return new UserWallet(indexer, path, password.ToAesKey(), false);
    }*/

    private void saveStoredData(String name, byte[] value) {
        synchronized (db_lock) {
            WalletDataContext ctx = new WalletDataContext(path);
            saveStoredData(ctx, name, value);
            ctx.saveChanges();
        }
    }

    private static void saveStoredData(WalletDataContext ctx, String name, byte[] value) {
        Key key = ctx.Keys.FirstOrDefault(p -> p.Name == name);
        if (key == null) {
            ctx.Keys.add(new Key
            {
                Name = name,
                        Value = value
            });
        } else {
            key.Value = value;
        }
    }

    @Override
    public boolean verifyPassword(String password) {
        return Arrays.equals(Helper.sha256(Helper.toAesKey(password)), loadStoredData
                ("PasswordHash"));
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
            walletTransaction.invoke(this, new WalletTransactionEventArgs(eventArgs.transaction, relatedAccounts, eventArgs.height, eventArgs.time));
        }
    }
}