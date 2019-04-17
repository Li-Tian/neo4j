package neo.wallets.SQLite;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import neo.Fixed8;
import neo.UInt160;
import neo.UInt256;
import neo.wallets.Coin;
import neo.wallets.KeyPair;
import neo.wallets.SQLite.sqlitJDBC.DataAccessException;
import neo.wallets.Wallet;
import neo.wallets.WalletAccount;
import neo.wallets.WalletIndexer;
import neo.wallets.WalletTransactionEventArgs;
import neo.cryptography.Helper;
import neo.csharp.Uint;
import neo.csharp.Ushort;
import neo.io.SerializeHelper;
import neo.ledger.Blockchain;
import neo.ledger.CoinState;
import neo.log.notr.TR;
import neo.network.p2p.payloads.ClaimTransaction;
import neo.network.p2p.payloads.CoinReference;
import neo.network.p2p.payloads.Transaction;
import neo.network.p2p.payloads.TransactionOutput;
import neo.smartcontract.ContractParameterType;
import neo.smartcontract.EventHandler;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: UserWallet
 * @Package neo.wallets.SQLite
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
        File tempFile = new File(path.trim());
        String fileName = tempFile.getName();
        return fileName;
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

            Version version = Version.parse("2.9.2");
            buildDatabase();
            saveStoredData("PasswordHash", Helper.sha256(passwordKey));
            saveStoredData("IV", iv);
            saveStoredData("MasterKey", Helper.aesEncrypt(masterKey, passwordKey, iv));
            saveStoredData("Version", version.getValue());
            //LINQ START
            //SaveStoredData("Version", new[] { version.Major, version.Minor, version.Build, version.Revision }.Select(p => BitConverter.GetBytes(p)).SelectMany(p => p).ToArray());
            //LINQ END
        } else {
            byte[] passwordHash = loadStoredData("PasswordHash");
            if (passwordHash != null && !Arrays.equals(passwordHash, Helper.sha256
                    (passwordKey)))
                throw new RuntimeException("passwordHash");
            this.iv = loadStoredData("IV");
            this.masterKey = Helper.aesDecrypt(loadStoredData("MasterKey"), passwordKey, iv);
            this.accounts = loadAccounts();
            indexer.registerAccounts(accounts.keySet());
        }
        indexer.walletTransaction.addListener(this);
    }

    private void addAccount(UserWalletAccount account, boolean is_import) {
        synchronized (accounts) {
            UserWalletAccount account_old = null;
            if ((account_old = accounts.getOrDefault(account.scriptHash, null)) != null) {
                if (account.contract == null) {
                    account.contract = account_old.contract;
                }
            } else {
                indexer.registerAccounts(Arrays.asList(new UInt160[]{account.scriptHash}),
                        is_import ? Uint.ZERO : Blockchain.singleton().height());
            }
            accounts.put(account.scriptHash, account);
        }
        synchronized (db_lock) {
            WalletDataContext ctx = null;
            try {
                ctx = new WalletDataContext(path);
                ctx.beginTranscation();
                if (account.hasKey()) {
                    byte[] decryptedPrivateKey = new byte[96];
                    System.arraycopy(account.key.publicKey.getEncoded(false), 1,
                            decryptedPrivateKey, 0, 64);
                    System.arraycopy(account.key.privateKey, 0, decryptedPrivateKey, 64, 32);
                    byte[] encryptedPrivateKey = encryptPrivateKey(decryptedPrivateKey);
                    Arrays.fill(decryptedPrivateKey, 0, decryptedPrivateKey.length, (byte) 0x00);
                    //LINQ START
/*                  Account db_account = ctx.accounts.FirstOrDefault(p -> p.publicKeyHash
                        .SequenceEqual(account.Key.PublicKeyHash.ToArray()));*/
                    byte[] publicKeyHash = account.getKey().getPublicKeyHash().toArray();
                    Account db_account = ctx.firstOrDefaultAccount(publicKeyHash);

                    //LINQ END
                    if (db_account == null) {
                        Account tempAccount = new Account(encryptedPrivateKey, account.getKey().getPublicKeyHash().toArray());
                        db_account = ctx.insertAccount(tempAccount);
                    } else {
                        db_account.privateKeyEncrypted = encryptedPrivateKey;
                        ctx.updateAccount(db_account);
                    }
                }
                if (account.contract != null) {
                    //LINQ START
/*                Contract db_contract = ctx.contracts.FirstOrDefault(p = > p.ScriptHash.SequenceEqual
                        (account.contract.scriptHash.ToArray()))*/
                    byte[] scriptHash = account.contract.scriptHash().toArray();
                    Contract db_contract = ctx.firstOrDefaultContract(scriptHash);

                    //LINQ END
                    if (db_contract != null) {
                        db_contract.publicKeyHash = account.key.getPublicKeyHash().toArray();
                        ctx.updateContract(db_contract);
                    } else {
                        Contract tempContract = new Contract();
                        tempContract.setRawData(SerializeHelper.toBytes((VerificationContract)
                                account.contract));
                        tempContract.setScriptHash(account.contract.scriptHash().toArray());
                        tempContract.setPublicKeyHash(account.getKey().getPublicKeyHash().toArray());
                        ctx.insertContract(tempContract);
                        // TODO account.key = null  throw null exception
                    }
                }

                //add address

                //LINQ START
                /*Address db_address = ctx.addresses.FirstOrDefault(p-> p.scriptHash
                        .SequenceEqual(account.contract.scriptHash().toArray()));*/
                Address db_address = ctx.firstOrDefaultAddress(account.contract.scriptHash().toArray());
                //LINQ END
                if (db_address == null) {
                    ctx.insertAddress(new Address(account.contract.scriptHash().toArray()));
                }
                ctx.commitTranscation();
            } catch (DataAccessException e) {
                TR.warn(e.getMessage());
                try {
                    ctx.rollBack();
                } catch (DataAccessException e1) {
                    TR.warn(e.getMessage());
                    //throw new RuntimeException(e);
                }
                throw new RuntimeException(e);
            } finally {
                try {
                    ctx.close();
                } catch (DataAccessException e) {
                    TR.warn(e.getMessage());
                }
            }
        }

    }

    @Override
    public void applyTransaction(Transaction tx) {
        synchronized (unconfirmed) {
            unconfirmed.put(tx.hash(), tx);
        }

        //LINQ START
        WalletTransactionEventArgs tempEvent = new WalletTransactionEventArgs();
        tempEvent.transaction = tx;
        Set<UInt160> tempSet = Arrays.asList(tx.witnesses).stream().map(p -> p.scriptHash())
                .collect(Collectors.toSet());
        tempSet.addAll(Arrays.asList(tx.outputs).stream().map(q -> q.scriptHash).collect(Collectors.toSet()));
        tempEvent.relatedAccounts = tempSet.stream().filter(p -> contains(p)).toArray(UInt160[]::new);
        tempEvent.height = null;
        tempEvent.time = Uint.parseUint(String.valueOf(Calendar.getInstance().getTimeInMillis() / 1000));
        walletTransaction.invoke(this, tempEvent);

/*        walletTransaction.invoke(this, new WalletTransactionEventArgs
        {
            Transaction = tx,
                    RelatedAccounts = tx.Witnesses.Select(p = > p.ScriptHash).
            Union(tx.Outputs.Select(p = > p.ScriptHash)).Where(p = > Contains(p)).ToArray(),
                Height = null,
                Time = DateTime.UtcNow.ToTimestamp()
        });*/
        //LINQ END
    }

    private void buildDatabase() {
        WalletDataContext ctx = null;
        try {
            ctx = new WalletDataContext(path);
            ctx.beginTranscation();
            ctx.deleteDB();
            ctx.createDB();
            ctx.commitTranscation();

        } catch (DataAccessException e) {
            TR.warn(e.getMessage());
            throw new RuntimeException(e);
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (DataAccessException e) {
                    TR.warn(e.getMessage());
                    //throw new RuntimeException(e);
                }
            }
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
        account.key = key;
        account.contract = verification_contract;
        addAccount(account, false);
        return account;
    }

    /**
     * TODO This method cannot be used, it will throws KeyPair NullPointerException
     */
    @Deprecated
    @Override
    public WalletAccount createAccount(neo.smartcontract.Contract contract) {
        VerificationContract verification_contract = (VerificationContract) contract;
        if (verification_contract == null) {
            verification_contract = new VerificationContract();
            verification_contract.script = contract.script;
            verification_contract.parameterList = contract.parameterList;
        }
        UserWalletAccount account = new UserWalletAccount(verification_contract.scriptHash());
        account.contract = verification_contract;
        account.key = null;
        addAccount(account, false);
        return account;
    }

    /**
     * TODO This method cannot be used, it will throws KeyPair NullPointerException
     */
    @Deprecated
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
                WalletDataContext ctx = null;
                try {
                    ctx = new WalletDataContext(path);
                    ctx.beginTranscation();
                    if (account.hasKey()) {
                        //LINQ START
/*                    Account db_account = ctx.accounts.First(p = > p.PublicKeyHash.SequenceEqual
                            (account.Key.PublicKeyHash.ToArray()))*/
                        UserWalletAccount finalAccount = account;
                        Account db_account = ctx.firstOrDefaultAccount(account.getKey()
                                .getPublicKeyHash().toArray());
                        //LINQ END
                        if (db_account != null) {
                            ctx.deleteAccount(db_account);
                        }
                    }
                    if (account.contract != null) {
                        //LINQ START
                        Contract db_contract = ctx.firstOrDefaultContract(scriptHash.toArray());
                        if (db_contract != null) {
                            ctx.deleteContract(db_contract);
                        }
                    }

                    //delete address
                    //LINQ START
                    Address db_address = ctx.firstOrDefaultAddress(scriptHash.toArray());
/*                   Address db_address = ctx.addresses.First(p = > p.ScriptHash.SequenceEqual
                            (scriptHash.ToArray()));*/
                    if (db_address != null) {
                        ctx.deleteAddress(db_address);
                    }
                    //LINQ END
                    ctx.commitTranscation();
                } catch (DataAccessException e) {
                    TR.warn(e.getMessage());
                    try {
                        ctx.rollBack();
                    } catch (DataAccessException e1) {
                        TR.warn(e.getMessage());
                        //throw new RuntimeException(e);
                    }
                    throw new RuntimeException(e);
                } finally {
                    try {
                        ctx.close();
                    } catch (DataAccessException e) {
                        TR.warn(e.getMessage());
                    }
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void dispose() {
        indexer.walletTransaction.removeListener(this);
    }

    public byte[] encryptPrivateKey(byte[] decryptedPrivateKey) {
        return Helper.aesEncrypt(decryptedPrivateKey, masterKey, iv);
    }

    @Override
    public Coin[] findUnspentCoins(UInt256 asset_id, Fixed8 amount, UInt160[] from) {

        //LINQ START
        //return FindUnspentCoins(FindUnspentCoins(from).ToArray().Where(p => GetAccount(p.Output.ScriptHash).Contract.Script.IsSignatureContract()), asset_id, amount) ?? base.FindUnspentCoins(asset_id, amount, from);
        Iterable<Coin> iterable = StreamSupport.stream(findUnspentCoins(from).spliterator(), false)
                .filter(p -> neo.smartcontract.Helper.isSignatureContract(getAccount(p.output.scriptHash).contract.script))
                .collect(Collectors.toList());

        Coin[] tempArray = findUnspentCoins(iterable, asset_id, amount);

        return tempArray != null ? tempArray : super.findUnspentCoins(asset_id, amount, from);
        //LINQ END
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
            return getCoinsInternal(accounts);
    }

    Iterable<Coin> getCoinsInternal(Iterable<UInt160> accounts) {
        Set<Coin> resultSet = new HashSet<>();
        HashSet<CoinReference> inputs;
        HashSet<CoinReference> claims;
        Coin[] coins_unconfirmed;
        synchronized (unconfirmed) {
            //LINQ START
            //inputs = new HashSet<CoinReference>(unconfirmed.values().SelectMany(p = > p.Inputs));
            //claims = new HashSet<CoinReference>(unconfirmed.values().OfType < ClaimTransaction > ()
            //        .SelectMany(p -> p.claims));
            /* coins_unconfirmed = unconfirmed.values().Select(tx -> tx.outputs.Select((o, i) = >
                    new Coin{
                Reference = new CoinReference{
                    PrevHash = tx.Hash,
                    PrevIndex = (ushort) i
                },
                Output = o,
                State = CoinState.Unconfirmed
            })).SelectMany(p = > p).ToArray();  */

            inputs = new HashSet<>(unconfirmed.values().stream()
                    .flatMap(p -> Arrays.asList(p.inputs).stream())
                    .collect(Collectors.toSet()));
            claims = new HashSet<>(unconfirmed.values().stream()
                    .filter(p -> (p instanceof neo.network.p2p.payloads.ClaimTransaction))
                    .flatMap(q -> Arrays.asList(((ClaimTransaction) q).claims).stream())
                    .collect(Collectors.toSet()));

            coins_unconfirmed = unconfirmed.values().stream().map(p -> {
                TransactionOutput[] transactionOutputArray = p.outputs;
                List<Coin> coinlist = new ArrayList<Coin>();
                for (int i = 0; i < transactionOutputArray.length; i++) {
                    Coin tempCoin = new Coin();
                    tempCoin.reference = new CoinReference();
                    tempCoin.reference.prevHash = p.hash();
                    tempCoin.reference.prevIndex = new Ushort(i);
                    tempCoin.output = transactionOutputArray[i];
                    tempCoin.state = CoinState.Unconfirmed;
                    coinlist.add(tempCoin);
                }
                return coinlist;
            }).flatMap(q -> q.stream()).toArray(Coin[]::new);
        }
        //LINQ END
        for (Coin coin : indexer.getCoins(accounts)) {
            if (inputs.contains(coin.reference)) {
                if (coin.output.assetId.equals(Blockchain.GoverningToken.hash())) {
                    Coin tempCoin = new Coin();
                    tempCoin.reference = coin.reference;
                    tempCoin.output = coin.output;
                    tempCoin.state = coin.state.or(CoinState.Spent);
                    resultSet.add(tempCoin);
                }
                continue;
            } else if (claims.contains(coin.reference)) {
                continue;
            }
            resultSet.add(coin);
        }
        HashSet<UInt160> accounts_set = new HashSet<>();
        accounts.forEach(p -> accounts_set.add(p));
        for (Coin coin : coins_unconfirmed) {
            if (accounts_set.contains(coin.output.scriptHash))
                resultSet.add(coin);
        }
        return resultSet;
    }

    @Override
    public Iterable<UInt256> getTransactions() {
        Set<UInt256> resultSet = new HashSet<>();
        indexer.getTransactions(accounts.keySet()).forEach(p -> resultSet.add(p));
        synchronized (unconfirmed) {
            resultSet.addAll(unconfirmed.keySet());
            return resultSet;
        }
    }

    private Map<UInt160, UserWalletAccount> loadAccounts() {
        WalletDataContext ctx = null;
        try {
            //LINQ START
/*            WalletDataContext ctx = new WalletDataContext(path);
            Map<UInt160, UserWalletAccount> accounts = ctx.addresses.stream().map(p -> p
                    .getScriptHash()).map(q->new UserWalletAccount(new UInt160(q))).collect(Collectors.toMap(
                    p->p.scriptHash,p->p));
            for (Contract db_contract : ctx.contracts.include(p = > p.Account)){
                VerificationContract contract = db_contract.RawData.AsSerializable < VerificationContract > ();
                UserWalletAccount account = accounts.get(contract.scriptHash());
                account.contract = contract;
                account.key = new KeyPair(decryptPrivateKey(db_contract.account
                        .PrivateKeyEncrypted));
            }*/


            ctx = new WalletDataContext(path);
            Map<UInt160, UserWalletAccount> accounts = ctx.queryAddressAll().stream()
                    .map(p -> p.getScriptHash()).map(q -> new UserWalletAccount(new UInt160(q)))
                    .collect(Collectors.toMap(x -> x.scriptHash, y -> y));

            for (Contract db_contract : ctx.include()) {
                VerificationContract contract = SerializeHelper.parse(VerificationContract::new,
                        db_contract.rawData);
                UserWalletAccount account = accounts.get(contract.scriptHash());
                account.contract = contract;
                account.key = new KeyPair(decryptPrivateKey(db_contract.account
                        .privateKeyEncrypted));
            }
            //LINQ END
            return accounts;
        } catch (DataAccessException e) {
            TR.warn(e.getMessage());
            throw new RuntimeException(e);
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (DataAccessException e) {
                    TR.warn(e.getMessage());
                    //throw new RuntimeException(e);
                }
            }
        }
    }

    private byte[] loadStoredData(String name) {
        WalletDataContext ctx = null;
        try {
            ctx = new WalletDataContext(path);
            //LINQ START
            //return ctx.keys.FirstOrDefault(p->p.Name==name)?.value;
            Key resultKey = ctx.firstOrDefaultKey(name);
            return resultKey == null ? null : resultKey.getValue();
            //LINQ END
        } catch (DataAccessException e) {
            TR.warn(e.getMessage());
            throw new RuntimeException(e);
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (DataAccessException e) {
                    TR.warn(e.getMessage());
                    //throw new RuntimeException(e);
                }
            }
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
            WalletDataContext ctx = null;
            try {
                ctx = new WalletDataContext(path);
                ctx.beginTranscation();
                saveStoredData(ctx, name, value);
                ctx.commitTranscation();
            } catch (DataAccessException e) {
                TR.warn(e.getMessage());
                try {
                    ctx.rollBack();
                } catch (DataAccessException e1) {
                    TR.warn(e.getMessage());
                    //throw new RuntimeException(e);
                }
                throw new RuntimeException(e);
            } finally {
                if (ctx != null) {
                    try {
                        ctx.close();
                    } catch (DataAccessException e) {
                        TR.warn(e.getMessage());
                        //throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private static void saveStoredData(WalletDataContext ctx, String name, byte[] value) throws DataAccessException {
        //LINQ START
        //Key key = ctx.keys.FirstOrDefault(p -> p.name == name);
        Key key = ctx.firstOrDefaultKey(name);
        //LINQ END
        if (key == null) {
            ctx.insertKey(new Key(name, value));
        } else {
            ctx.updateKey(new Key(name, value));
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