package neo.wallets;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
import neo.csharp.BitConverter;
import neo.wallets.SQLite.Version;
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
 * @Package neo.wallets
 * @Description: 钱包基类
 * @date Created in 11:17 2019/3/14
 */
public abstract class Wallet implements IDisposable, EventHandler.Listener<WalletTransactionEventArgs> {

    public abstract EventHandler<WalletTransactionEventArgs> getWalletTransaction();

    //随机数生成器
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

    /**
     * @Author:doubi.liu
     * @description:创建账户
     * @date:2019/4/9
     */
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
                .output.assetId.equals(asset_id)).toArray(Coin[]::new);
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
        while (unspents_ordered[i].output.value.compareTo(amount) <= 0) {
            amount = neo.Fixed8.subtract(amount, unspents_ordered[i++].output.value);
        }

        Coin[] array1 = Arrays.asList(unspents_ordered).stream().limit(i).toArray(Coin[]::new);
        if (amount.compareTo(neo.Fixed8.ZERO) == 0) {
            return array1;
        } else {
            Coin[] resultArray = new Coin[i + 1];
            System.arraycopy(array1, 0, resultArray, 0, array1.length);
            resultArray[i] = Arrays.asList(unspents_asset).stream().skip(i).sorted(Comparator.comparing(u -> u.output.value)).findFirst().get();
            return resultArray;
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
                neo.vm.Helper.emitAppCall(sb, (neo.UInt160) asset_id, "balanceOf", account);
                sb.emit(OpCode.ADD);
            }
            neo.vm.Helper.emitAppCall(sb, (neo.UInt160) asset_id, "decimals");
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
                .findFirst().orElse(null);
        if (account == null)
            account = Arrays.asList(accounts).stream().filter(p -> p.contract == null ? false :
                    neo.smartcontract.Helper.isSignatureContract(p.contract.script) == true)
                    .findFirst().orElse(null);
        if (account == null)
            account = Arrays.asList(accounts).stream().filter(p -> !p.watchOnly()).findFirst()
                    .orElse(null);
        if (account == null)
            account = Arrays.asList(accounts).stream().findFirst().orElse(null);
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
        if (data.length != 39 || (data[0] & 0xff) != 0x01 || (data[1] & 0xff) != 0x42 || (data[2] & 0xff)
                != 0xe0)
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
        ECPoint pubkey = new ECPoint(ECC.Secp256r1.getG().multiply(new BigInteger(1, prikey)).normalize());
        UInt160 script_hash = neo.smartcontract.Helper.toScriptHash(Contract
                .createSignatureRedeemScript(pubkey));
        String address = script_hash.toAddress();
        try {
            //LINQ START
/*            if (!Encoding.ASCII.GetBytes(address).Sha256().Sha256().Take(4).SequenceEqual
                    (addresshash))*/
            byte[] tempArray = new byte[4];
            System.arraycopy(Helper.sha256(Helper.sha256(address.getBytes("ascii"))),
                    0, tempArray, 0, 4);
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
        return getPrivateKeyFromNEP2(nep2, passphrase, 16384, 8, 8);
    }


    public static byte[] getPrivateKeyFromWIF(String wif) {
        if (wif == null) {
            throw new IllegalArgumentException();
        }
        byte[] data = neo.cryptography.Helper.base58CheckDecode(wif);
        if (data.length != 34 || (data[0] & 0xff) != 0x80 || (data[33] & 0xff) != 0x01)
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
        return makeTransaction(tx, null, null, new Fixed8());
    }


    public <T extends Transaction> T makeTransaction(T tx, UInt160 from, UInt160
            changeAddress, Fixed8 fee) {
        if (tx.outputs == null) {
            tx.outputs = new TransactionOutput[0];
        }
        if (tx.attributes == null) {
            tx.attributes = new TransactionAttribute[0];
        }
        final Fixed8 totalFee = neo.Fixed8.add(fee, tx.getSystemFee());
        final boolean hasFee = totalFee.compareTo(Fixed8.ZERO) > 0;
        //LINQ START
/*        var pay_total = (typeof(T) == typeof(IssueTransaction) ? new TransactionOutput[0] : tx.Outputs).GroupBy(p => p.AssetId, (k, g) => new
        {
            AssetId = k,
            Value = g.Sum(p => p.Value)
        }).ToDictionary(p => p.AssetId);*/

        // TODO updated by luchuan 2019/4/18 -------------------------------------------------------------
        final Map<UInt256, Fixed8> payTotal = new HashMap<>();
        if (!(tx instanceof IssueTransaction)) {
            Arrays.stream(tx.outputs)
                    .collect(Collectors.groupingBy(p -> p.assetId))
                    .forEach((assetId, list) -> {
                        Fixed8 sum = list.stream().map(p -> p.value).reduce((x, y) -> Fixed8.add(x, y)).get();
                        if (assetId.equals(Blockchain.UtilityToken.hash()) && hasFee) {
                            sum = Fixed8.add(sum, totalFee);
                        }
                        payTotal.put(assetId, sum);
                    });
        }
        if (!payTotal.containsKey(Blockchain.UtilityToken.hash()) && hasFee) {
            payTotal.put(Blockchain.UtilityToken.hash(), totalFee);
        }

        if (changeAddress == null) changeAddress = getChangeAddress();

        // build the output and input
        List<TransactionOutput> outputsNew = new ArrayList<>(Arrays.asList(tx.outputs));
        ArrayList<CoinReference> inputs = new ArrayList<>();

        for (Map.Entry<UInt256, Fixed8> entry : payTotal.entrySet()) {
            UInt256 assetId = entry.getKey();
            Fixed8 sum = entry.getValue();

            Coin[] coins;
            if (from == null) {
                coins = findUnspentCoins(assetId, sum);
            } else {
                coins = findUnspentCoins(assetId, sum, from);
            }
            if (coins == null) {
                return null;                // no utxo
            }

            // check the charge
            Fixed8 total = Fixed8.ZERO;
            for (Coin coin : coins) {
                total = Fixed8.add(total, coin.output.value);

                inputs.add(coin.reference);     // add input
            }
            if (total.compareTo(payTotal.get(assetId)) > 0) { // add charge output
                TransactionOutput changeOutput = new TransactionOutput();
                changeOutput.assetId = assetId;
                changeOutput.value = Fixed8.subtract(total, payTotal.get(assetId));
                changeOutput.scriptHash = changeAddress;
                outputsNew.add(changeOutput);
            }
        }
        tx.inputs = inputs.toArray(new CoinReference[0]);
        tx.outputs = outputsNew.toArray(new TransactionOutput[0]);
        return tx;


//        Map<UInt256, List<AbstractMap.SimpleEntry<UInt256, Fixed8>>> temp_pay_total = Arrays.asList(
//                (tx instanceof IssueTransaction) ? new TransactionOutput[0] : tx.outputs)
//                .stream()
//                .map(p -> new AbstractMap.SimpleEntry<>(p.assetId, p.value))
//                .collect(Collectors.groupingBy(AbstractMap.SimpleEntry<UInt256, Fixed8>::getKey));
//
//        Map<UInt256, AbstractMap.SimpleEntry<UInt256, Fixed8>> pay_total = new HashMap<>();
//
//        for (Map.Entry<UInt256, List<AbstractMap.SimpleEntry<UInt256, Fixed8>>> e : temp_pay_total
//                .entrySet()) {
//            Fixed8 temp = Fixed8.ZERO;
//            for (AbstractMap.SimpleEntry<UInt256, Fixed8> f : e.getValue()) {
//                temp = Fixed8.add(temp, f.getValue());
//            }
//            pay_total.put(e.getKey(), new AbstractMap.SimpleEntry<UInt256, Fixed8>(e.getKey(), temp));
//        }
//
//        //LINQ END
//        if (fee.compareTo(neo.Fixed8.ZERO) > 0) {
//            if (pay_total.containsKey(Blockchain.UtilityToken.hash())) {
//                pay_total.put(Blockchain.UtilityToken.hash(), new AbstractMap.SimpleEntry<UInt256, Fixed8>(
//                        Blockchain.UtilityToken.hash(),
//                        neo.Fixed8.add(pay_total.get(Blockchain.UtilityToken.hash()).getValue(), fee)));
//            } else {
//                pay_total.put(Blockchain.UtilityToken.hash(), new AbstractMap
//                        .SimpleEntry<UInt256, Fixed8>(
//                        Blockchain.UtilityToken.hash(), fee
//                ));
//            }
//        }
//        Map<UInt256, AbstractMap.SimpleEntry<UInt256, Coin[]>> pay_coins =
//                pay_total.entrySet().stream().map(p ->
//                        new AbstractMap.SimpleEntry<UInt256, Coin[]>(p
//                                .getKey(), from == null ? findUnspentCoins(p.getKey(), p.getValue().getValue()) :
//                                findUnspentCoins(p.getKey(), p.getValue().getValue(), from)))
//                        .collect(Collectors.toMap((e) -> {
//                            return e.getKey();
//                        }, (e) -> {
//                            return e;
//                        }));
//        //LINQ START
//        //if (pay_coins.Any(p => p.Value.Unspents == null)) return null;
//        if (pay_coins.values().stream().anyMatch(p -> p.getValue() == null)) {
//            return null;
//        }
        //LINQ END
        //LINQ START
/*        var input_sum = pay_coins.Values.ToDictionary(p => p.AssetId, p => new
        {
            p.AssetId,
                    Value = p.Unspents.Sum(q => q.Output.Value)
        });*/


//        Map<UInt256, AbstractMap.SimpleEntry<UInt256, Fixed8>> input_sum = pay_coins.values()
//                .stream().map(p -> new AbstractMap.SimpleEntry<UInt256, Fixed8>(p
//                        .getKey(), Arrays.asList(p.getValue()).stream().map(q -> q.output.value).reduce
//                        ((x, y) -> Fixed8.add(x, y)).get()))
//                .collect(Collectors.toMap((e) -> {
//                    return e.getKey();
//                }, (e) -> {
//                    return e;
//                }));


        //LINQ END
//        if (change_address == null) change_address = getChangeAddress();
//        List<TransactionOutput> outputs_new = new ArrayList<>(Arrays.asList(tx.outputs));

//        for (UInt256 asset_id : input_sum.keySet()) {
//            if (input_sum.get(asset_id).getValue().compareTo(payTotal.get(asset_id)) > 0) {
//                TransactionOutput temp = new TransactionOutput();
//                temp.assetId = asset_id;
//                temp.value = Fixed8.subtract(input_sum.get(asset_id).getValue(), payTotal.get(asset_id));
//                temp.scriptHash = change_address;
//                outputs_new.add(temp);
//            }
//LINQ START
/*
            tx.Inputs = pay_coins.Values.SelectMany(p => p.Unspents).Select(p => p.Reference).ToArray();
*/
//            tx.inputs = pay_coins.values().stream().map(p -> p.reference).toArray(CoinReference[]::new);
//
////LINQ END
//            tx.outputs = outputs_new.toArray(new TransactionOutput[0]);
//            return tx;
//        }
//        return tx;

        // @end update -----------------------------------------------------------------------------
    }


    public Transaction makeTransaction(List<TransactionAttribute> attributes,
                                       Iterable<TransferOutput> outputs) {
        return makeTransaction(attributes, outputs, null, null, new Fixed8());
    }

    public Transaction makeTransaction(List<TransactionAttribute> attributes,
                                       Iterable<TransferOutput> outputs, UInt160 from, UInt160
                                               change_address, Fixed8 fee) {
        //LINQ START
        // TODO updated by luchuan 2019/4/19 ------------------------------------------------------------
        final ArrayList<TransferOutput> cOutputs = new ArrayList<>();
        StreamSupport.stream(outputs.spliterator(), false)
                .filter(p -> !p.isGlobalAsset())         // group by assetId + scriptHash
                .collect(Collectors.groupingBy(o -> BitConverter.merge(o.assetId.toArray(), o.scriptHash.toArray())))
                .forEach((key, list) -> {                // merge multi-transfer into one
                    BigDecimal value = list.stream().map(p -> p.getValue()).reduce((x, y) -> x.add(y)).get();
                    TransferOutput first = list.get(0);
                    cOutputs.add(new TransferOutput(first.assetId, value, first.scriptHash));
                });
        // @end update -----------------------------------------------------------------------------

        /*  var cOutputs = outputs.Where(p => !p.IsGlobalAsset).GroupBy(p => new
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
            attributes = new ArrayList<>();
        }
        if (cOutputs.size() == 0) {
            tx = new ContractTransaction();
        } else {
            //LINQ START
            //UInt160[] accounts = from == null ? GetAccounts().Where(p => !p.Lock && !p.WatchOnly).Select(p => p.ScriptHash).ToArray() : new[] { from };
            UInt160[] accounts = (from != null) ? new UInt160[]{from}
                    : StreamSupport.stream(getAccounts().spliterator(), false)
                    .filter(p -> !p.lock && !p.watchOnly())
                    .map(p -> p.scriptHash)
                    .toArray(UInt160[]::new);

            //LINQ END
            HashSet<UInt160> sAttributes = new HashSet<UInt160>();
            ScriptBuilder sb = new ScriptBuilder();

            for (TransferOutput output : cOutputs) {
                List<Map.Entry<UInt160, BigInteger>> balances = new ArrayList<>();//UInt160
                // Account, BigInteger value
                for (UInt160 account : accounts) {
                    ScriptBuilder sb2 = new ScriptBuilder();
                    neo.vm.Helper.emitAppCall(sb2, (UInt160) output.assetId, "balanceOf", account);
                    byte[] script = sb2.toArray();

                    ApplicationEngine engine = ApplicationEngine.run(script);
                    if (engine.state.hasFlag(VMState.FAULT)) {
                        return null;
                    }
                    balances.add(new AbstractMap.SimpleEntry(account, engine.resultStack.pop().getBigInteger()));
                }
                //LINQ START
                //BigInteger sum = balances.Aggregate(BigInteger.Zero, (x, y) => x + y.Value);
                BigInteger sum = balances.stream().map(p -> p.getValue()).reduce(BigInteger.ZERO, (x, y) -> x.add(y));
                if (sum.compareTo(output.value.toBigInteger()) < 0) {
                    return null;
                }
                //LINQ END

                if (sum.compareTo(output.value.toBigInteger()) != 0) {
                    //LINQ START
                    //balances = balances.OrderByDescending(p => p.Value).ToList();
                    balances = balances.stream().sorted((x, y) -> -x.getValue().compareTo(y.getValue()))
                            .collect(Collectors.toList());

                    //LINQ END
                    BigInteger amount = output.value.toBigInteger();
                    int i = 0;
                    while (balances.get(i).getValue().compareTo(amount) <= 0) {
                        amount = amount.subtract(balances.get(i++).getValue());
                    }

                    if (amount == BigInteger.ZERO) {
                        //LINQ START
                        //balances = balances.Take(i).ToList();
                        balances = balances.stream().limit(i).collect(Collectors.toList());
                        //LINQ END
                    } else {
                        BigInteger finalAmount = amount;

                        // TODO updated by luchuan  2019/4/19 -------------------------------------------
                        Map.Entry<UInt160, BigInteger> lastAccountMeetTheAsset
                                = balances.stream()
                                .filter(p -> p.getValue().compareTo(finalAmount) >= 0)
                                .sorted(Comparator.comparing(Map.Entry::getValue))
                                .findFirst()
                                .get();

                        balances = balances.stream().limit(i).collect(Collectors.toList());
                        balances.add(lastAccountMeetTheAsset);


                        /*
                        balances = balances.stream().limit(i).Concat(new[]{
                        balances.Last(p -> p.value >= amount)
                        }).toList();*/

                        // @end --------------------------------------------------------------------
                    }
                    //LINQ START
                    /*sum = balances.Aggregate(BigInteger.ZERO, (x, y) =>x + y.Value);*/
                    sum = balances.stream()
                            .map(p -> p.getValue())
                            .reduce(BigInteger.ZERO, (x, y) -> x.add(y));
                    //LINQ END

                }
                //LINQ START
                //sAttributes.UnionWith(balances.Select(p => p.Account));
                sAttributes = balances.stream().map(p -> p.getKey()).collect(Collectors.toCollection(HashSet::new));

                //LINQ END
                for (int i = 0; i < balances.size(); i++) {
                    BigInteger value = balances.get(i).getValue();
                    UInt160 account = balances.get(i).getKey();
                    UInt160 assetId = (UInt160) output.assetId;

                    if (i == 0) {
                        BigInteger change = sum.subtract(output.value.toBigInteger());
                        if (change.intValue() > 0) value = value.subtract(change);
                    }
                    neo.vm.Helper.emitAppCall(sb, assetId, "transfer", account, output.scriptHash, value);
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

            for (UInt160 hash : sAttributes) {
                TransactionAttribute t = new TransactionAttribute();
                t.usage = TransactionAttributeUsage.Script;
                t.data = hash.toArray();
                attributes.add(t);
            }
        }
        tx.attributes = attributes.toArray(new TransactionAttribute[0]);
        tx.inputs = new CoinReference[0];
        tx.outputs = StreamSupport.stream(outputs.spliterator(), false)
                .filter(p -> p.isGlobalAsset())
                .map(p -> p.toTxOutput())
                .toArray(TransactionOutput[]::new);
        tx.witnesses = new Witness[0];

        if (tx instanceof InvocationTransaction) {
            InvocationTransaction itx = (InvocationTransaction) tx;
//      TODO updated by luchuan 2019/4/19 ---------------------------------------------------------
            ApplicationEngine engine = ApplicationEngine.run(itx.script, itx, null, false, Fixed8.ZERO);
//            ApplicationEngine engine = ApplicationEngine.run(itx.script, itx, null, false, null);
            if (engine.state.hasFlag(VMState.FAULT)) return null;

            itx.gas = InvocationTransaction.getGas(engine.getGasConsumed());
//
//            tx = new InvocationTransaction();
//            tx.version = itx.version;
//            ((InvocationTransaction) tx).script = itx.script;
//            ((InvocationTransaction) tx).gas = itx.gas;
//            tx.attributes = itx.attributes;
//            tx.inputs = itx.inputs;
//            tx.outputs = itx.outputs;
//            @end ---------------------------------------------------------------------------------
        }
        tx = makeTransaction(tx, from, change_address, fee);
        return tx;
    }

    public boolean sign(ContractParametersContext context) {
        boolean fSuccess = false;
        for (UInt160 scriptHash : context.scriptHashes()) {
            WalletAccount account = getAccount(scriptHash);
            if (account == null) {
                continue;
            } else {
                if (account.hasKey() != true) {
                    continue;
                }
            }
            KeyPair key = account.getKey();
            byte[] signature = neo.wallets.Helper.sign(context.verifiable, key);
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
            temp[i] = (byte) ((x[i] & 0xff) ^ (y[i] & 0xff));
        }
        //return x.Zip(y, (a, b) => (byte)(a ^ b)).ToArray();
        return temp;
        //LINQ END
    }
}