package neo.consensus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import neo.UInt160;
import neo.UInt256;
import neo.wallets.Coin;
import neo.wallets.KeyPair;
import neo.wallets.SQLite.UserWalletAccount;
import neo.wallets.SQLite.VerificationContract;
import neo.wallets.SQLite.Version;
import neo.wallets.Wallet;
import neo.wallets.WalletAccount;
import neo.wallets.WalletTransactionEventArgs;
import neo.csharp.Uint;
import neo.network.p2p.payloads.Transaction;
import neo.smartcontract.Contract;
import neo.smartcontract.ContractParameterType;
import neo.smartcontract.EventHandler;

public class MyWallet extends Wallet {

    private HashMap<UInt160, UserWalletAccount> accounts = new HashMap<>();

    public MyWallet() {
        byte[] privateKey = new byte[32];
        Random rng = new Random();

        rng.nextBytes(privateKey);
        WalletAccount account = createAccount(privateKey);
        account.isDefault = true; // the first account is default account

        privateKey = new byte[32];
        rng.nextBytes(privateKey);
        createAccount(privateKey);

        privateKey = new byte[32];
        rng.nextBytes(privateKey);
        createAccount(privateKey);

        privateKey = new byte[32];
        rng.nextBytes(privateKey);
       createAccount(privateKey);

        privateKey = new byte[32];
        rng.nextBytes(privateKey);
       createAccount(privateKey);

        privateKey = new byte[32];
        rng.nextBytes(privateKey);
        createAccount(privateKey);

        privateKey = new byte[32];
        rng.nextBytes(privateKey);
        createAccount(privateKey);
    }

    @Override
    public EventHandler<WalletTransactionEventArgs> getWalletTransaction() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Version getVersion() {
        return null;
    }

    @Override
    public Uint getWalletHeight() {
        return Uint.ZERO;
    }

    @Override
    public void applyTransaction(Transaction tx) {

    }

    @Override
    public boolean contains(UInt160 scriptHash) {
        return false;
    }

    @Override
    public WalletAccount createAccount(byte[] privateKey) {
        KeyPair key = new KeyPair(privateKey);
        VerificationContract contract = new VerificationContract() {
            {
                script = Contract.createSignatureRedeemScript(key.publicKey);
                parameterList = new ContractParameterType[]{
                        ContractParameterType.Signature
                };
            }
        };

        UserWalletAccount account = new UserWalletAccount(contract.scriptHash());
        account.key = key;
        account.contract = contract;
        accounts.put(account.scriptHash, account);
        return account;
    }

    @Override
    public WalletAccount createAccount(Contract contract, KeyPair key) {
        return null;
    }

    @Override
    public WalletAccount createAccount(Contract contract) {
        return null;
    }

    @Override
    public WalletAccount createAccount(UInt160 scriptHash) {
        return null;
    }

    @Override
    public boolean deleteAccount(UInt160 scriptHash) {
        return false;
    }

    @Override
    public WalletAccount getAccount(UInt160 scriptHash) {
        return accounts.get(scriptHash);
    }

    @Override
    public Iterable<? extends WalletAccount> getAccounts() {
        return accounts.values();
    }

    @Override
    public Iterable<Coin> getCoins(Iterable<UInt160> accounts) {
        return new HashSet<Coin>();
    }

    @Override
    public Iterable<UInt256> getTransactions() {
        return null;
    }

    @Override
    public boolean verifyPassword(String password) {
        return false;
    }

    @Override
    public void doWork(Object sender, WalletTransactionEventArgs eventArgs) {

    }
}
