package scenario;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import neo.UInt160;
import neo.UInt256;
import neo.csharp.Uint;
import neo.network.p2p.payloads.Transaction;
import neo.smartcontract.Contract;
import neo.smartcontract.ContractParameterType;
import neo.smartcontract.EventHandler;
import neo.wallets.Coin;
import neo.wallets.KeyPair;
import neo.wallets.SQLite.UserWalletAccount;
import neo.wallets.SQLite.VerificationContract;
import neo.wallets.SQLite.Version;
import neo.wallets.Wallet;
import neo.wallets.WalletAccount;
import neo.wallets.WalletTransactionEventArgs;

public class MyWallet3 extends Wallet {

    private HashMap<UInt160, UserWalletAccount> accounts = new HashMap<>();
    private HashSet<Coin> coins = null;

    public MyWallet3(int accountCount) {
        byte[] privateKey = new byte[32];

        for (int i = 0; i < accountCount; i++){
            Random rng = new Random();
            rng.nextBytes(privateKey);
            WalletAccount account = createAccount(privateKey);
            account.isDefault = i == 0; // the first account is default account
        }
    }

    public void initTransaction (HashSet<Coin> input) {
        coins = input;
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
        return null;
    }

    @Override
    public void applyTransaction(Transaction tx) {

    }

    public UserWalletAccount getDefaultAccount(){
       for (Map.Entry<UInt160, UserWalletAccount> entry: accounts.entrySet()){
           return entry.getValue();
       }
       return null;
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
        if (coins == null) return new HashSet<Coin>();
        return coins;
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
