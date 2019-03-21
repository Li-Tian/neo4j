package neo.Wallets;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import neo.Fixed8;
import neo.UInt160;
import neo.UInt256;
import neo.UIntBase;
import neo.Wallets.SQLite.Version;
import neo.cryptography.Helper;
import neo.cryptography.SCrypt;
import neo.cryptography.ecc.ECC;
import neo.cryptography.ecc.ECPoint;
import neo.csharp.Uint;
import neo.csharp.common.IDisposable;
import neo.exception.FormatException;
import neo.ledger.Blockchain;
import neo.ledger.CoinState;
import neo.log.notr.TR;
import neo.network.p2p.payloads.CoinReference;
import neo.network.p2p.payloads.ContractTransaction;
import neo.network.p2p.payloads.InvocationTransaction;
import neo.network.p2p.payloads.IssueTransaction;
import neo.network.p2p.payloads.Transaction;
import neo.network.p2p.payloads.TransactionAttribute;
import neo.network.p2p.payloads.TransactionAttributeUsage;
import neo.network.p2p.payloads.TransactionOutput;
import neo.network.p2p.payloads.Witness;
import neo.smartcontract.ApplicationEngine;
import neo.smartcontract.Contract;
import neo.smartcontract.ContractParametersContext;
import neo.smartcontract.EventHandler;
import neo.vm.OpCode;
import neo.vm.ScriptBuilder;
import neo.vm.VMState;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: Wallet
 * @Package neo.Wallets
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 11:17 2019/3/14
 */
public abstract class Wallet implements IDisposable, EventHandler.Listener<WalletTransactionEventArgs> {
    public abstract EventHandler<WalletTransactionEventArgs> getWalletTransaction();

    private static Random rand = new Random();

    public abstract String getName();

    public abstract Version getVersion();

    public abstract Uint getWalletHeight();

    public abstract void applyTransaction(Transaction tx);

    public abstract boolean contains(UInt160 scriptHash);

    public abstract WalletAccount createAccount(byte[] privateKey);

    public abstract WalletAccount createAccount(Contract contract, KeyPair key);

    public abstract WalletAccount createAccount(Contract contract);//KeyPair key=null

    public abstract WalletAccount createAccount(UInt160 scriptHash);

    public abstract boolean deleteAccount(UInt160 scriptHash);

    public abstract WalletAccount getAccount(UInt160 scriptHash);

    public abstract Iterable<? extends WalletAccount> getAccounts();

    public abstract Iterable<Coin> getCoins(Iterable<UInt160> accounts);

    public abstract Iterable<UInt256> getTransactions();

    public WalletAccount createAccount() {
        byte[] privateKey = new byte[32];
        Random rng = new Random();
        rng.nextBytes(privateKey);
        WalletAccount account = createAccount(privateKey);
        Arrays.fill(privateKey, 0, privateKey.length, (byte) 0x00);
        return account;
    }

    public WalletAccount createAccount(Contract contract, byte[] privateKey) {
        if (privateKey == null) return createAccount(contract);
        return createAccount(contract, new KeyPair(privateKey));
    }

    public void dispose() {
    }

    public Iterable<Coin> findUnspentCoins(UInt160... from) {

        /*
        LINQ START
        Iterable<UInt160> accounts = from.length > 0 ? from : getAccounts().Where(p = > !p.Lock
                && !p.WatchOnly).Select(p-> p.scriptHash);
        return getCoins(accounts).Where(p-> p.state.hasFlag(CoinState.Confirmed) && !p.state
                .hasFlag(CoinState.Spent) && !p.state.hasFlag(CoinState.Frozen));*/
        Iterable<UInt160> accounts = from.length > 0 ? Arrays.asList(from) : StreamSupport.stream
                (getAccounts().spliterator(), false).filter(p -> !p.lock && !p.watchOnly()).map(p -> p.scriptHash)
                .collect(Collectors.toList());
        return StreamSupport.stream(getCoins(accounts).spliterator(), false).filter(p -> p.state
                .hasFlag(CoinState.Confirmed) && !p.state.hasFlag(CoinState.Spent) && !p.state
                .hasFlag(CoinState.Frozen)).collect(Collectors.toList());
        /*LINQ END*/
    }

    public Coin[] findUnspentCoins(UInt256 asset_id, Fixed8 amount, UInt160... from) {
        return findUnspentCoins(findUnspentCoins(from), asset_id, amount);
    }

    protected static Coin[] findUnspentCoins(Iterable<Coin> unspents, UInt256 asset_id, Fixed8
            amount) {
        //LINQ START
        //Coin[] unspents_asset = unspents.Where(p => p.Output.AssetId == asset_id).ToArray();
        //Fixed8 sum = unspents_asset.Sum(p => p.Output.Value);
        Coin[] unspents_asset = StreamSupport.stream(unspents.spliterator(), false).filter(p -> p
                .output.assetId == asset_id).toArray(Coin[]::new);
        Fixed8 sum = Arrays.asList(unspents_asset).stream().map(p -> p.output.value).reduce(neo
                .Fixed8.ZERO, neo.Fixed8::add);
        //LINQ END
        if (sum.compareTo(amount) < 0) {
            return null;
        }
        if (sum == amount) {
            return unspents_asset;
        }
        Coin[] unspents_ordered = Arrays.asList(unspents_asset).stream().sorted((u1, u2) ->
                -u1.output.value.compareTo(u2.output.value)).toArray(Coin[]::new);
        int i = 0;
        //LINQ START
/*        while (unspents_ordered[i].Output.Value <= amount)
            amount -= unspents_ordered[i++].Output.Value;
        if (amount == Fixed8.Zero)
            return unspents_ordered.Take(i).ToArray();
        else
            return unspents_ordered.Take(i).Concat(new[] { unspents_ordered.Last(p => p.Output.Value >= amount) }).ToArray();*/
        while (unspents_ordered[i].output.value.compareTo(amount) <= 0)
            amount = neo.Fixed8.subtract(amount, unspents_ordered[i++].output.value);
        if (amount.compareTo(neo.Fixed8.ZERO) == 0)
            return Arrays.asList(unspents_ordered).stream().limit(i).toArray(Coin[]::new);
        else {
            Coin[] tempArray1 = Arrays.asList(unspents_ordered).stream().limit(i).toArray(Coin[]::new);
            neo.Fixed8 finalAmount = amount;
            Coin[] tempArray2 = Arrays.asList(unspents_ordered).stream().filter(p -> p.output.value
                    .compareTo(finalAmount) >= 0).toArray(Coin[]::new);
            if (tempArray2.length == 0) {
                return tempArray1;
            } else {
                Coin[] resultArray = new Coin[tempArray1.length + tempArray2.length];
                System.arraycopy(tempArray1, 0, resultArray, 0, tempArray1.length);
                System.arraycopy(tempArray2, 0, resultArray, tempArray1.length, tempArray2.length);
                return resultArray;
            }

        }
        //LINQ END
    }

    public WalletAccount getAccount(ECPoint pubkey) {
        return getAccount(neo.smartcontract.Helper.toScriptHash(Contract
                .createSignatureRedeemScript(pubkey)));
    }

    public Fixed8 getAvailable(UInt256 asset_id) {
        return StreamSupport.stream(findUnspentCoins().spliterator(), false).filter(p -> p.output
                .assetId.equals(asset_id)).map(p -> p.output.value).reduce(neo.Fixed8.ZERO, neo.Fixed8::add);
    }

    public BigDecimal getAvailable(UIntBase asset_id) {
        if (asset_id instanceof UInt160) {
            byte[] script;
            UInt160[] accounts = StreamSupport.stream(getAccounts().spliterator(), false).filter(p ->
                    !p.watchOnly()).map(p -> p.scriptHash).toArray(UInt160[]::new);
            ScriptBuilder sb = new ScriptBuilder();

            sb.emitPush(new BigInteger("0"));
            for (UInt160 account : accounts) {
                neo.VM.Helper.emitAppCall(sb, (neo.UInt160) asset_id, "balanceOf", account);
                sb.emit(OpCode.ADD);
            }
            neo.VM.Helper.emitAppCall(sb, (neo.UInt160) asset_id, "decimals");
            script = sb.toArray();

            ApplicationEngine engine = ApplicationEngine.run(script, null, null, false, Fixed8.multiply(neo
                    .Fixed8.fromDecimal(new BigDecimal("0.2")), accounts.length));
            if (engine.state.hasFlag(VMState.FAULT))
                return new BigDecimal(BigInteger.ZERO, 0);
            byte decimals = engine.resultStack.pop().getBigInteger().byteValue();
            BigInteger amount = engine.resultStack.pop().getBigInteger();
            return new BigDecimal(amount, decimals);
        } else {
            return new BigDecimal(new BigInteger(String.valueOf(getAvailable((UInt256) asset_id).getData
                    ())), 8);
        }
    }

    public Fixed8 getBalance(UInt256 asset_id) {
        //LINQ START
        //return getCoins(getAccounts().Select(p = > p.ScriptHash)).Where(p = > !p.State.HasFlag
        //       (CoinState.Spent) && p.Output.AssetId.Equals(asset_id)).Sum(p = > p.Output.Value);
        return StreamSupport.stream(getCoins(StreamSupport.stream(getAccounts().spliterator(), false).map
                (p -> p.scriptHash).collect(Collectors.toList())).spliterator(), false).filter(p -> !p
                .state.hasFlag(CoinState.Spent) && p.output.assetId.equals(asset_id)).map(p -> p
                .output.value).reduce(neo.Fixed8.ZERO, neo.Fixed8::add);

        //LINQ END
    }

    public UInt160 getChangeAddress() {
        WalletAccount[] accounts = StreamSupport.stream(getAccounts().spliterator(), false)
                .toArray(WalletAccount[]::new);
        //LINQ START
/*        WalletAccount account = accounts.FirstOrDefault(p => p.IsDefault);
        if (account == null)
            account = accounts.FirstOrDefault(p => p.Contract?.Script.IsSignatureContract() == true);
        if (account == null)
            account = accounts.FirstOrDefault(p => !p.WatchOnly);
        if (account == null)
            account = accounts.FirstOrDefault();*/
        WalletAccount account = Arrays.asList(accounts).stream().filter(p -> p.isDefault)
                .findFirst().get();
        if (account == null)
            account = Arrays.asList(accounts).stream().filter(p -> p.contract == null ? false :
                    neo.smartcontract.Helper.isSignatureContract(p.contract.script) == true)
                    .findFirst().get();
        if (account == null)
            account = Arrays.asList(accounts).stream().filter(p -> !p.watchOnly()).findFirst().get();
        if (account == null)
            account = Arrays.asList(accounts).stream().findFirst().get();
        return account == null ? null : account.scriptHash;
        //LINQ END
    }

    public Iterable<Coin> getCoins() {
        //LINQ START
        // return GetCoins(GetAccounts().Select(p => p.ScriptHash));
        return getCoins(StreamSupport.stream(getAccounts().spliterator(), false).map(p -> p
                .scriptHash).collect(Collectors.toList()));
        //LINQ END
    }

    public static byte[] getPrivateKeyFromNEP2(String nep2, String passphrase, int N, int
            r, int p) {
        if (nep2 == null) throw new NullPointerException("nep2");
        if (passphrase == null) throw new NullPointerException("passphrase");//nameof
        byte[] data = Helper.base58CheckDecode(nep2);
        if (data.length != 39 || data[0] != 0x01 || data[1] != 0x42 || data[2] != 0xe0)
            throw new FormatException();
        byte[] addresshash = new byte[4];
        System.arraycopy(data, 3, addresshash, 0, 4);
        byte[] derivedkey = new byte[0];
        try {
            derivedkey = SCrypt.deriveKey(passphrase.getBytes("UTF-8"), addresshash, N,
                    r, p, 64);
        } catch (UnsupportedEncodingException e) {
            TR.fixMe("String转byte[]异常");
            throw new RuntimeException(e);
        }
        byte[] derivedhalf1 = new byte[32];
        byte[] derivedhalf2 = new byte[32];
        System.arraycopy(derivedkey, 0, derivedhalf1, 0, 32);
        System.arraycopy(derivedkey, 32, derivedhalf2, 0, 32);

        byte[] encryptedkey = new byte[32];
        System.arraycopy(data, 7, encryptedkey, 0, 32);
        byte[] prikey = Wallet.XOR(Helper.aes256Decrypt(encryptedkey, derivedhalf2), derivedhalf1);
        ECPoint pubkey = (ECPoint) ECC.Secp256r1.getG().multiply(new BigInteger(prikey));
        UInt160 script_hash = neo.smartcontract.Helper.toScriptHash(Contract
                .createSignatureRedeemScript(pubkey));
        String address = script_hash.toAddress();
        try {
            //LINQ START
/*            if (!Encoding.ASCII.GetBytes(address).Sha256().Sha256().Take(4).SequenceEqual
                    (addresshash))*/
            byte[] tempArray = new byte[4];
            System.arraycopy(tempArray, 0, Helper.sha256(Helper.sha256(address.getBytes("ascii"))),
                    0, 4);
            if (!Arrays.equals(tempArray, addresshash))
                throw new FormatException();
        } catch (UnsupportedEncodingException e) {
            TR.fixMe("String转byte[]异常");
            throw new RuntimeException(e);
        }
        //LINQ END
        return prikey;
    }

    public static byte[] getPrivateKeyFromNEP2(String nep2, String passphrase) {
        int N = 16384;
        int r = 8;
        int p = 8;
        if (nep2 == null) throw new NullPointerException("nep2");
        if (passphrase == null) throw new NullPointerException("passphrase");//nameof
        byte[] data = Helper.base58CheckDecode(nep2);
        if (data.length != 39 || data[0] != 0x01 || data[1] != 0x42 || data[2] != 0xe0)
            throw new FormatException();
        byte[] addresshash = new byte[4];
        System.arraycopy(data, 3, addresshash, 0, 4);
        byte[] derivedkey = new byte[0];
        try {
            derivedkey = SCrypt.deriveKey(passphrase.getBytes("UTF-8"), addresshash, N,
                    r, p, 64);
        } catch (UnsupportedEncodingException e) {
            TR.fixMe("String转byte[]异常");
            throw new RuntimeException(e);
        }
        byte[] derivedhalf1 = new byte[32];
        byte[] derivedhalf2 = new byte[32];
        System.arraycopy(derivedkey, 0, derivedhalf1, 0, 32);
        System.arraycopy(derivedkey, 32, derivedhalf2, 0, 32);

        byte[] encryptedkey = new byte[32];
        System.arraycopy(data, 7, encryptedkey, 0, 32);
        byte[] prikey = Wallet.XOR(Helper.aes256Decrypt(encryptedkey, derivedhalf2), derivedhalf1);
        ECPoint pubkey = (ECPoint) ECC.Secp256r1.getG().multiply(new BigInteger(prikey));
        UInt160 script_hash = neo.smartcontract.Helper.toScriptHash(Contract
                .createSignatureRedeemScript(pubkey));
        String address = script_hash.toAddress();
        try {
            //LINQ START
/*            if (!Encoding.ASCII.GetBytes(address).Sha256().Sha256().Take(4).SequenceEqual
                    (addresshash))*/
            byte[] tempArray = new byte[4];
            System.arraycopy(tempArray, 0, Helper.sha256(Helper.sha256(address.getBytes("ascii"))),
                    0, 4);
            if (!Arrays.equals(tempArray, addresshash))
                throw new FormatException();
        } catch (UnsupportedEncodingException e) {
            TR.fixMe("String转byte[]异常");
            throw new RuntimeException(e);
        }
        //LINQ END
        return prikey;
    }


    public static byte[] getPrivateKeyFromWIF(String wif) {
        if (wif == null) {
            throw new IllegalArgumentException();
        }
        byte[] data = neo.cryptography.Helper.base58CheckDecode(wif);
        if (data.length != 34 || data[0] != 0x80 || data[33] != 0x01)
            throw new FormatException();
        byte[] privateKey = new byte[32];
        System.arraycopy(data, 1, privateKey, 0, privateKey.length);
        Arrays.fill(data, 0, data.length, (byte) 0x00);
        return privateKey;
    }


    public Iterable<Coin> getUnclaimedCoins() {
        //LINQ START
/*        IEnumerable<UInt160> accounts = GetAccounts().Where(p => !p.Lock && !p.WatchOnly).Select(p => p.ScriptHash);
        IEnumerable<Coin> coins = GetCoins(accounts);
        coins = coins.Where(p => p.Output.AssetId.Equals(Blockchain.GoverningToken.Hash));
        coins = coins.Where(p => p.State.HasFlag(CoinState.Confirmed) && p.State.HasFlag(CoinState.Spent));
        coins = coins.Where(p => !p.State.HasFlag(CoinState.Claimed) && !p.State.HasFlag(CoinState.Frozen));
        return coins;*/
        Iterable<UInt160> accounts = StreamSupport.stream(getAccounts().spliterator(), false)
                .filter(p -> !p.lock && !p.watchOnly()).map(p -> p.scriptHash).collect(Collectors.toList());
        Iterable<Coin> coins = getCoins(accounts);
        coins = StreamSupport.stream(coins.spliterator(), false).filter(p -> p.output.assetId.equals
                (Blockchain.GoverningToken.hash())).collect(Collectors.toList());
        coins = StreamSupport.stream(coins.spliterator(), false).filter(p -> p.state.hasFlag(CoinState
                .Confirmed) && p.state.hasFlag(CoinState.Spent)).collect(Collectors.toList());
        coins = StreamSupport.stream(coins.spliterator(), false).filter(p -> !p.state.hasFlag(CoinState
                .Claimed) && !p.state.hasFlag(CoinState.Frozen)).collect(Collectors.toList());
        return coins;
        //LINQ END
    }


 /*   public WalletAccount imports(X509Certificate2 cert) {
        byte[] privateKey;
        ECDsa ecdsa = cert.GetECDsaPrivateKey())
        {
            privateKey = ecdsa.ExportParameters(true).D;
        }
        WalletAccount account = createAccount(privateKey);
        Arrays.fill(privateKey, 0, privateKey.length, (byte) 0x00);
        return account;
    }*/

    public WalletAccount imports(String wif) {
        byte[] privateKey = getPrivateKeyFromWIF(wif);
        WalletAccount account = createAccount(privateKey);
        Arrays.fill(privateKey, 0, privateKey.length, (byte) 0x00);
        return account;
    }

    public WalletAccount imports(String nep2, String passphrase) {
        byte[] privateKey = getPrivateKeyFromNEP2(nep2, passphrase);
        WalletAccount account = createAccount(privateKey);
        Arrays.fill(privateKey, 0, privateKey.length, (byte) 0x00);
        return account;
    }

    public <T extends Transaction> T makeTransaction(T tx) //where T : Transaction
    {
        UInt160 from = null;
        UInt160 change_address = null;
        Fixed8 fee = null;
        if (tx.outputs == null) {
            tx.outputs = new TransactionOutput[0];
        }
        if (tx.attributes == null) {
            tx.attributes = new TransactionAttribute[0];
        }
        fee = neo.Fixed8.add(fee, tx.getSystemFee());

        //LINQ START
/*        var pay_total = (typeof(T) == typeof(IssueTransaction) ? new TransactionOutput[0] : tx.Outputs).GroupBy(p => p.AssetId, (k, g) => new
        {
            AssetId = k,
                    Value = g.Sum(p => p.Value)
        }).ToDictionary(p => p.AssetId);*/

        Map<UInt256, List<AbstractMap.SimpleEntry<UInt256, Fixed8>>> temp_pay_total = Arrays.asList(
                (tx instanceof IssueTransaction) ? new TransactionOutput[0] : tx.outputs)
                .stream()
                .map(p -> new AbstractMap.SimpleEntry<UInt256, Fixed8>(p.assetId, p.value))
                .collect(Collectors.groupingBy(AbstractMap.SimpleEntry<UInt256, Fixed8>::getKey));
        Map<UInt256, AbstractMap.SimpleEntry<UInt256, Fixed8>> pay_total = new HashMap<>();
        for (Map.Entry<UInt256, List<AbstractMap.SimpleEntry<UInt256, Fixed8>>> e : temp_pay_total
                .entrySet()) {
            Fixed8 temp = Fixed8.ZERO;
            for (AbstractMap.SimpleEntry<UInt256, Fixed8> f : e.getValue()) {
                Fixed8.add(temp, f.getValue());
            }
        }

        //LINQ END
        if (fee.compareTo(neo.Fixed8.ZERO) > 0) {
            if (pay_total.containsKey(Blockchain.UtilityToken.hash())) {
                pay_total.put(Blockchain.UtilityToken.hash(), new AbstractMap.SimpleEntry<UInt256, Fixed8>(
                        Blockchain.UtilityToken.hash(),
                        neo.Fixed8.add(pay_total.get(Blockchain.UtilityToken.hash()).getValue(), fee)));
            } else {
                pay_total.put(Blockchain.UtilityToken.hash(), new AbstractMap
                        .SimpleEntry<UInt256, Fixed8>(
                        Blockchain.UtilityToken.hash(), fee
                ));
            }
        }
        Map<UInt256, AbstractMap.SimpleEntry<UInt256, Coin[]>> pay_coins =
                pay_total.entrySet().stream().map(p ->
                        new AbstractMap.SimpleEntry<UInt256, Coin[]>(p
                                .getKey(), from == null ? findUnspentCoins(p.getKey(), p.getValue().getValue()) :
                                findUnspentCoins(p.getKey(), p.getValue().getValue(), from)))
                        .collect(Collectors.toMap((e) -> {
                            return e.getKey();
                        }, (e) -> {
                            return e;
                        }));
        //LINQ START
        //if (pay_coins.Any(p => p.Value.Unspents == null)) return null;
        if (pay_coins.entrySet().stream().anyMatch(p -> p.getValue() == null)) {
            return null;
        }
        //LINQ END
        //LINQ START
/*        var input_sum = pay_coins.Values.ToDictionary(p => p.AssetId, p => new
        {
            p.AssetId,
                    Value = p.Unspents.Sum(q => q.Output.Value)
        });*/

        Map<UInt256, AbstractMap.SimpleEntry<UInt256, Fixed8>> input_sum=pay_coins.values()
                .stream().map(p -> new AbstractMap.SimpleEntry<UInt256, Fixed8>(p
                .getKey(), Arrays.asList(p.getValue()).stream().map(q -> q.output.value).reduce
                ((x, y) -> Fixed8.add(x, y)).get()))
                .collect(Collectors.toMap((e) -> {return e.getKey();}, (e) -> {return e;}));
        /*
        Map<UInt256, List<AbstractMap.SimpleEntry<UInt256, Fixed8>>> input_sum = pay_coins.values()
                .stream().flatMap(p -> new AbstractMap.SimpleEntry<UInt256, Fixed8>(
                        p.getKey(), Arrays.asList(p.getValue()).stream().map(q -> q.output.value))
                ).collect(Collectors.toMap((e) -> {
                    return e.getKey();
                }, (e) -> {
                    return e;
                }));


        Map<UInt256, AbstractMap.SimpleEntry<UInt256, Fixed8>> input_sum = pay_coins.values()
                .stream().flatMap(p -> new AbstractMap.SimpleEntry<UInt256, Fixed8>(p.getKey(),
                        Arrays.asList(p.getValue()).stream()
                                .map(q -> q.output.value).reduce(Fixed8.ZERO, (x, y) -> Fixed8.add(x,
                                y))))
                .collect(Collectors.toMap((e) -> {
                    return e.getKey();
                }, (e) -> {
                    return e;
                }))
                .ToDictionary(p -> p.assetId, p ->
                        new
        {
            p.AssetId,
                    Value = p.Unspents.Sum(q -> q.Output.Value)
        });


        */
        //LINQ END
        if (change_address == null) change_address = getChangeAddress();
        List<TransactionOutput> outputs_new = new ArrayList<TransactionOutput>();
        outputs_new.addAll(Arrays.asList(tx.outputs));

        for (UInt256 asset_id : input_sum.keySet()) {
            if (input_sum.get(asset_id).getValue().compareTo(pay_total.get(asset_id).getValue()) > 0) {
                TransactionOutput temp = new TransactionOutput();
                temp.assetId = asset_id;
                temp.value = Fixed8.subtract(input_sum.get(asset_id).getValue(), pay_total.get
                        (asset_id).getValue());
                temp.scriptHash = change_address;
                outputs_new.add(temp);
            }
//LINQ START
/*
            tx.Inputs = pay_coins.Values.SelectMany(p => p.Unspents).Select(p => p.Reference).ToArray();
*/
            tx.inputs = pay_coins.values().stream().flatMap(p -> Stream.of(p.getValue())).map(p -> p
                    .reference).toArray(CoinReference[]::new);

//LINQ END
            tx.outputs = outputs_new.toArray(new TransactionOutput[0]);
            return tx;
        }
        return tx;
    }


    public <T extends Transaction> T makeTransaction(T tx, UInt160 from, UInt160
            change_address, Fixed8 fee) {
        if (tx.outputs == null) {
            tx.outputs = new TransactionOutput[0];
        }
        if (tx.attributes == null) {
            tx.attributes = new TransactionAttribute[0];
        }
        fee = neo.Fixed8.add(fee, tx.getSystemFee());

        //LINQ START
/*        var pay_total = (typeof(T) == typeof(IssueTransaction) ? new TransactionOutput[0] : tx.Outputs).GroupBy(p => p.AssetId, (k, g) => new
        {
            AssetId = k,
                    Value = g.Sum(p => p.Value)
        }).ToDictionary(p => p.AssetId);*/

        Map<UInt256, List<AbstractMap.SimpleEntry<UInt256, Fixed8>>> temp_pay_total = Arrays.asList(
                (tx instanceof IssueTransaction) ? new TransactionOutput[0] : tx.outputs)
                .stream()
                .map(p -> new AbstractMap.SimpleEntry<UInt256, Fixed8>(p.assetId, p.value))
                .collect(Collectors.groupingBy(AbstractMap.SimpleEntry<UInt256, Fixed8>::getKey));
        Map<UInt256, AbstractMap.SimpleEntry<UInt256, Fixed8>> pay_total = new HashMap<>();
        for (Map.Entry<UInt256, List<AbstractMap.SimpleEntry<UInt256, Fixed8>>> e : temp_pay_total
                .entrySet()) {
            Fixed8 temp = Fixed8.ZERO;
            for (AbstractMap.SimpleEntry<UInt256, Fixed8> f : e.getValue()) {
                Fixed8.add(temp, f.getValue());
            }
        }

        //LINQ END
        if (fee.compareTo(neo.Fixed8.ZERO) > 0) {
            if (pay_total.containsKey(Blockchain.UtilityToken.hash())) {
                pay_total.put(Blockchain.UtilityToken.hash(), new AbstractMap.SimpleEntry<UInt256, Fixed8>(
                        Blockchain.UtilityToken.hash(),
                        neo.Fixed8.add(pay_total.get(Blockchain.UtilityToken.hash()).getValue(), fee)));
            } else {
                pay_total.put(Blockchain.UtilityToken.hash(), new AbstractMap
                        .SimpleEntry<UInt256, Fixed8>(
                        Blockchain.UtilityToken.hash(), fee
                ));
            }
        }
        Map<UInt256, AbstractMap.SimpleEntry<UInt256, Coin[]>> pay_coins =
                pay_total.entrySet().stream().map(p ->
                        new AbstractMap.SimpleEntry<UInt256, Coin[]>(p
                                .getKey(), from == null ? findUnspentCoins(p.getKey(), p.getValue().getValue()) :
                                findUnspentCoins(p.getKey(), p.getValue().getValue(), from)))
                        .collect(Collectors.toMap((e) -> {
                            return e.getKey();
                        }, (e) -> {
                            return e;
                        }));
        //LINQ START
        //if (pay_coins.Any(p => p.Value.Unspents == null)) return null;
        if (pay_coins.entrySet().stream().anyMatch(p -> p.getValue() == null)) {
            return null;
        }
        //LINQ END
        //LINQ START
/*        var input_sum = pay_coins.Values.ToDictionary(p => p.AssetId, p => new
        {
            p.AssetId,
                    Value = p.Unspents.Sum(q => q.Output.Value)
        });*/

        Map<UInt256, AbstractMap.SimpleEntry<UInt256, Fixed8>> input_sum=pay_coins.values()
                .stream().map(p -> new AbstractMap.SimpleEntry<UInt256, Fixed8>(p
                        .getKey(), Arrays.asList(p.getValue()).stream().map(q -> q.output.value).reduce
                        ((x, y) -> Fixed8.add(x, y)).get()))
                .collect(Collectors.toMap((e) -> {return e.getKey();}, (e) -> {return e;}));


        //LINQ END
        if (change_address == null) change_address = getChangeAddress();
        List<TransactionOutput> outputs_new = new ArrayList<TransactionOutput>();
        outputs_new.addAll(Arrays.asList(tx.outputs));

        for (UInt256 asset_id : input_sum.keySet()) {
            if (input_sum.get(asset_id).getValue().compareTo(pay_total.get(asset_id).getValue()) > 0) {
                TransactionOutput temp = new TransactionOutput();
                temp.assetId = asset_id;
                temp.value = Fixed8.subtract(input_sum.get(asset_id).getValue(), pay_total.get
                        (asset_id).getValue());
                temp.scriptHash = change_address;
                outputs_new.add(temp);
            }
//LINQ START
/*
            tx.Inputs = pay_coins.Values.SelectMany(p => p.Unspents).Select(p => p.Reference).ToArray();
*/
            tx.inputs = pay_coins.values().stream().flatMap(p -> Stream.of(p.getValue())).map(p -> p
                    .reference).toArray(CoinReference[]::new);

//LINQ END
            tx.outputs = outputs_new.toArray(new TransactionOutput[0]);
            return tx;
        }
        return tx;
    }


    public Transaction makeTransaction(List<TransactionAttribute> attributes,
                                       Iterable<TransferOutput> outputs) {
        UInt160 from = null;
        UInt160 change_address = null;
        neo.Fixed8 fee = null;
        //LINQ START
        Map<UIntBase, Map<UInt160, List<TransferOutput>>> transferOutputByAssetIdAndAccount
                = StreamSupport.stream(outputs.spliterator(), false)
                .filter(p -> !p.isGlobalAsset())
                .collect(Collectors.groupingBy(TransferOutput::getAssetId,
                        Collectors.groupingBy(TransferOutput::getScriptHash)));
        List<TransferOutput> templist = new ArrayList<>();
        for (Map.Entry<UIntBase, Map<UInt160, List<TransferOutput>>>
                e : transferOutputByAssetIdAndAccount.entrySet()) {
            for (Map.Entry<neo.UInt160, List<TransferOutput>> f : e.getValue().entrySet()) {
                BigDecimal value = f.getValue().stream().map(p -> p.getValue()).reduce((x, y) -> x.add
                        (y)).get();
                templist.add(new TransferOutput(e.getKey(), value, f.getKey()));
            }

        }
        TransferOutput[] cOutputs = templist.toArray(new TransferOutput[0]);
/*        var cOutputs = outputs.Where(p => !p.IsGlobalAsset).GroupBy(p => new
        {
            AssetId = (UInt160)p.AssetId,
                    Account = p.ScriptHash
        }, (k, g) => new
        {
            k.AssetId,
                    Value = g.Aggregate(BigInteger.Zero, (x, y) => x + y.Value.Value),
            k.Account
        }).ToArray();*/
        //LINQ END
        Transaction tx;
        if (attributes == null) {
            attributes = new ArrayList<TransactionAttribute>();
        }
        if (cOutputs.length == 0) {
            tx = new ContractTransaction();
        } else {
            //LINQ START
            //UInt160[] accounts = from == null ? GetAccounts().Where(p => !p.Lock && !p.WatchOnly).Select(p => p.ScriptHash).ToArray() : new[] { from };

            UInt160[] accounts = (from == null) ? StreamSupport.stream(getAccounts().spliterator
                    (), false).filter(p -> !p.lock && !p.watchOnly()).map(p -> p.scriptHash).toArray(UInt160[]::new)
                    : new UInt160[]{from};
            //LINQ END
            HashSet<UInt160> sAttributes = new HashSet<UInt160>();
            ScriptBuilder sb = new ScriptBuilder();

            for (TransferOutput output : cOutputs) {
                List<Map.Entry<UInt160, BigInteger>> balances = new ArrayList<>();//UInt160
                // Account, BigInteger value
                for (UInt160 account : accounts) {
                    byte[] script;
                    ScriptBuilder sb2 = new ScriptBuilder();
                    neo.VM.Helper.emitAppCall(sb2, (UInt160) output.assetId, "balanceOf", account);
                    script = sb2.toArray();

                    ApplicationEngine engine = ApplicationEngine.run(script);
                    if (engine.state.hasFlag(VMState.FAULT)) {
                        return null;
                    }
                    Map<neo.UInt160, BigInteger> tempMap = new HashMap<>();
                    tempMap.put(account, engine.resultStack.pop().getBigInteger());
                    for (Map.Entry e : tempMap.entrySet()) {
                        balances.add(e);
                    }
                }
                //LINQ START
                //BigInteger sum = balances.Aggregate(BigInteger.Zero, (x, y) => x + y.Value);
                BigInteger sum = balances.stream().map(p -> p.getValue()).reduce(BigInteger.ZERO,
                        (x, y) -> x.add(y));
                if (sum.compareTo(output.value.toBigInteger()) < 0) {
                    return null;
                }
                //LINQ END
                if (sum.compareTo(output.value.toBigInteger()) != 0) {
                    //LINQ START
                    //balances = balances.OrderByDescending(p => p.Value).ToList();
                    balances = balances.stream().sorted((x, y) -> -x.getValue().compareTo(y.getValue
                            ())).collect(Collectors.toList());
                    //LINQ END
                    BigInteger amount = output.value.toBigInteger();
                    int i = 0;
                    while (balances.get(i).getValue().compareTo(amount) <= 0)
                        amount = amount.subtract(balances.get(i++).getValue());
                    if (amount == BigInteger.ZERO)
                        //LINQ START
                        //balances = balances.Take(i).ToList();
                        balances = balances.stream().limit(i).collect(Collectors.toList());
                        //LINQ END
                    else {
                        BigInteger finalAmount = amount;
                        List<Map.Entry<UInt160, BigInteger>> tempbalances = balances.stream()
                                .filter(p -> p.getValue().compareTo(finalAmount) >= 0)
                                .collect(Collectors.toList());
                        balances = balances.stream().limit(i).collect(Collectors.toList());
                        balances.add(tempbalances.get(tempbalances.size() - 1));
                        /*
                        balances = balances.stream().limit(i).Concat(new[]{
                        balances.Last(p -> p.value >= amount)
                    }).toList();*/
                    }
                    //LINQ START
                    /*sum = balances.Aggregate(BigInteger.ZERO, (x, y) =>x + y.Value);*/
                    sum = balances.stream().map(p -> p.getValue()).reduce(BigInteger.ZERO, (x, y) -> x
                            .add(y));
                    //LINQ END

                }
                //LINQ START
                //sAttributes.UnionWith(balances.Select(p => p.Account));
                sAttributes = balances.stream().map(p -> p.getKey()).collect(Collectors
                        .toCollection(HashSet::new));
                //LINQ END
                for (int i = 0; i < balances.size(); i++) {
                    BigInteger value = balances.get(i).getValue();
                    if (i == 0) {
                        BigInteger change = sum.subtract(output.value.toBigInteger());
                        if (change.intValue() > 0) value = value.subtract(change);
                    }
                    neo.VM.Helper.emitAppCall(sb, (UInt160) output.assetId, "transfer", balances
                            .get(i)
                            .getKey
                            //// TODO: 2019/3/20 验证account是否是scriptHash
                                    (), output.scriptHash, value);
                    sb.emit(OpCode.THROWIFNOT);
                }
                //LINQ END
            }
            byte[] nonce = new byte[8];
            rand.nextBytes(nonce);
            sb.emit(OpCode.RET, nonce);
            tx = new InvocationTransaction();
            tx.version = 1;
            ((InvocationTransaction) tx).script = sb.toArray();
            attributes.addAll(sAttributes.stream().map(p -> {
                TransactionAttribute t = new TransactionAttribute();
                t.usage = TransactionAttributeUsage.Script;
                t.data = p.toArray();
                return t;
            }).collect(Collectors.toList()));
        }
        tx.attributes = attributes.toArray(new TransactionAttribute[0]);
        tx.inputs = new CoinReference[0];
        tx.outputs = StreamSupport.stream(outputs.spliterator(), false).filter(p -> p
                .isGlobalAsset()).map(p -> p.toTxOutput()).toArray(TransactionOutput[]::new);
        tx.witnesses = new Witness[0];
        if (tx instanceof InvocationTransaction) {
            Transaction itx = tx;
            ApplicationEngine engine = ApplicationEngine.run(((InvocationTransaction) itx)
                    .script, itx, null, false, null);
            if (engine.state.hasFlag(VMState.FAULT)) return null;

            tx = new InvocationTransaction();
            tx.version = itx.version;
            ((InvocationTransaction) tx).script = ((InvocationTransaction) itx).script;
            ((InvocationTransaction) tx).gas = ((InvocationTransaction) itx).gas;
            tx.attributes = itx.attributes;
            tx.inputs = itx.inputs;
            tx.outputs = itx.outputs;
        }
        tx = makeTransaction(tx, from, change_address, fee);
        return tx;
    }

    public Transaction makeTransaction(List<TransactionAttribute> attributes,
                                       Iterable<TransferOutput> outputs, UInt160 from, UInt160
                                               change_address, Fixed8 fee) {
        //LINQ START
        Map<UIntBase, Map<UInt160, List<TransferOutput>>> transferOutputByAssetIdAndAccount
                = StreamSupport.stream(outputs.spliterator(), false)
                .filter(p -> !p.isGlobalAsset())
                .collect(Collectors.groupingBy(TransferOutput::getAssetId,
                        Collectors.groupingBy(TransferOutput::getScriptHash)));
        List<TransferOutput> templist = new ArrayList<>();
        for (Map.Entry<UIntBase, Map<UInt160, List<TransferOutput>>>
                e : transferOutputByAssetIdAndAccount.entrySet()) {
            for (Map.Entry<neo.UInt160, List<TransferOutput>> f : e.getValue().entrySet()) {
                BigDecimal value = f.getValue().stream().map(p -> p.getValue()).reduce((x, y) -> x.add
                        (y)).get();
                templist.add(new TransferOutput(e.getKey(), value, f.getKey()));
            }

        }
        TransferOutput[] cOutputs = templist.toArray(new TransferOutput[0]);
    /*        var cOutputs = outputs.Where(p => !p.IsGlobalAsset).GroupBy(p => new
            {
                AssetId = (UInt160)p.AssetId,
                        Account = p.ScriptHash
            }, (k, g) => new
            {
                k.AssetId,
                        Value = g.Aggregate(BigInteger.Zero, (x, y) => x + y.Value.Value),
                k.Account
            }).ToArray();*/
        //LINQ END
        Transaction tx;
        if (attributes == null) {
            attributes = new ArrayList<TransactionAttribute>();
        }
        if (cOutputs.length == 0) {
            tx = new ContractTransaction();
        } else {
            //LINQ START
            //UInt160[] accounts = from == null ? GetAccounts().Where(p => !p.Lock && !p.WatchOnly).Select(p => p.ScriptHash).ToArray() : new[] { from };
            UInt160[] accounts = (from == null) ? StreamSupport.stream(getAccounts().spliterator
                    (), false).filter(p -> !p.lock && !p.watchOnly()).map(p -> p.scriptHash).toArray(UInt160[]::new)
                    : new UInt160[]{from};
            //LINQ END
            HashSet<UInt160> sAttributes = new HashSet<UInt160>();
            ScriptBuilder sb = new ScriptBuilder();

            for (TransferOutput output : cOutputs) {
                List<Map.Entry<UInt160, BigInteger>> balances = new ArrayList<>();//UInt160
                // Account, BigInteger value
                for (UInt160 account : accounts) {
                    byte[] script;
                    ScriptBuilder sb2 = new ScriptBuilder();
                    neo.VM.Helper.emitAppCall(sb2, (UInt160) output.assetId, "balanceOf", account);
                    script = sb2.toArray();

                    ApplicationEngine engine = ApplicationEngine.run(script);
                    if (engine.state.hasFlag(VMState.FAULT)) {
                        return null;
                    }
                    Map<neo.UInt160, BigInteger> tempMap = new HashMap<>();
                    tempMap.put(account, engine.resultStack.pop().getBigInteger());
                    for (Map.Entry e : tempMap.entrySet()) {
                        balances.add(e);
                    }
                }
                //LINQ START
                //BigInteger sum = balances.Aggregate(BigInteger.Zero, (x, y) => x + y.Value);
                BigInteger sum = balances.stream().map(p -> p.getValue()).reduce(BigInteger.ZERO,
                        (x, y) -> x.add(y));
                if (sum.compareTo(output.value.toBigInteger()) < 0) {
                    return null;
                }
                //LINQ END
                if (sum.compareTo(output.value.toBigInteger()) != 0) {
                    //LINQ START
                    //balances = balances.OrderByDescending(p => p.Value).ToList();
                    balances = balances.stream().sorted((x, y) -> -x.getValue().compareTo(y.getValue
                            ())).collect(Collectors.toList());
                    //LINQ END
                    BigInteger amount = output.value.toBigInteger();
                    int i = 0;
                    while (balances.get(i).getValue().compareTo(amount) <= 0)
                        amount = amount.subtract(balances.get(i++).getValue());
                    if (amount == BigInteger.ZERO)
                        //LINQ START
                        //balances = balances.Take(i).ToList();
                        balances = balances.stream().limit(i).collect(Collectors.toList());
                        //LINQ END
                    else {
                        BigInteger finalAmount = amount;
                        List<Map.Entry<UInt160, BigInteger>> tempbalances = balances.stream()
                                .filter(p -> p.getValue().compareTo(finalAmount) >= 0)
                                .collect(Collectors.toList());
                        balances = balances.stream().limit(i).collect(Collectors.toList());
                        balances.add(tempbalances.get(tempbalances.size() - 1));
                        /*
                        balances = balances.stream().limit(i).Concat(new[]{
                        balances.Last(p -> p.value >= amount)
                    }).toList();*/
                    }
                    //LINQ START
                    /*sum = balances.Aggregate(BigInteger.ZERO, (x, y) =>x + y.Value);*/
                    sum = balances.stream().map(p -> p.getValue()).reduce(BigInteger.ZERO, (x, y) -> x
                            .add(y));
                    //LINQ END

                }
                //LINQ START
                //sAttributes.UnionWith(balances.Select(p => p.Account));
                HashSet<UInt160> tempSet = balances.stream().map(p -> p.getKey()).collect(Collectors
                        .toCollection(HashSet::new));
                //LINQ END
                for (int i = 0; i < balances.size(); i++) {
                    BigInteger value = balances.get(i).getValue();
                    if (i == 0) {
                        BigInteger change = sum.subtract(output.value.toBigInteger());
                        if (change.intValue() > 0) value = value.subtract(change);
                    }
                    neo.VM.Helper.emitAppCall(sb, (UInt160) output.assetId, "transfer", balances.get(i)
                            .getKey
                            //// TODO: 2019/3/20
                                    (), output.scriptHash, value);
                    sb.emit(OpCode.THROWIFNOT);
                }
                //LINQ END
            }
            byte[] nonce = new byte[8];
            rand.nextBytes(nonce);
            sb.emit(OpCode.RET, nonce);
            tx = new InvocationTransaction();
            tx.version = 1;
            ((InvocationTransaction) tx).script = sb.toArray();
            attributes.addAll(sAttributes.stream().map(p -> {
                TransactionAttribute t = new TransactionAttribute();
                t.usage = TransactionAttributeUsage.Script;
                t.data = p.toArray();
                return t;
            }).collect(Collectors.toList()));
        }
        tx.attributes = attributes.toArray(new TransactionAttribute[0]);
        tx.inputs = new CoinReference[0];
        tx.outputs = StreamSupport.stream(outputs.spliterator(), false).filter(p -> p
                .isGlobalAsset()).map(p -> p.toTxOutput()).toArray(TransactionOutput[]::new);
        tx.witnesses = new Witness[0];
        if (tx instanceof InvocationTransaction) {
            Transaction itx = tx;
            ApplicationEngine engine = ApplicationEngine.run(((InvocationTransaction) itx)
                    .script, itx, null, false, null);
            if (engine.state.hasFlag(VMState.FAULT)) return null;

            tx = new InvocationTransaction();
            tx.version = itx.version;
            ((InvocationTransaction) tx).script = ((InvocationTransaction) itx).script;
            ((InvocationTransaction) tx).gas = ((InvocationTransaction) itx).gas;
            tx.attributes = itx.attributes;
            tx.inputs = itx.inputs;
            tx.outputs = itx.outputs;
        }
        tx = makeTransaction(tx, from, change_address, fee);
        return tx;
    }

    public boolean sign(ContractParametersContext context) {
        boolean fSuccess = false;
        for (UInt160 scriptHash : context.scriptHashes()) {
            WalletAccount account = getAccount(scriptHash);
            if (account==null){
                continue;
            }else{
                if (account.hasKey()!=true){
                    continue;
                }
            }
            KeyPair key = account.getKey();
            byte[] signature = neo.Wallets.Helper.sign(context.verifiable,key);
            fSuccess |= context.addSignature(account.contract, key.publicKey, signature);
        }
        return fSuccess;
    }


    public abstract boolean verifyPassword(String password);

    private static byte[] XOR(byte[] x, byte[] y) {
        if (x.length != y.length) throw new IllegalArgumentException();
        //LINQ START
        byte[] temp = new byte[x.length];
        for (int i = 0; i < x.length; i++) {
            temp[i] = (byte) (x[i] ^ y[i]);
        }
        //return x.Zip(y, (a, b) => (byte)(a ^ b)).ToArray();
        return temp;
        //LINQ END
    }
}