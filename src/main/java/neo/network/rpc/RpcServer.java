package neo.network.rpc;

import static akka.pattern.Patterns.ask;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import akka.actor.ActorRef;
import akka.util.Timeout;
import neo.Fixed8;
import neo.Helper;
import neo.NeoSystem;
import neo.UInt160;
import neo.UInt256;
import neo.UIntBase;
import neo.csharp.common.IDisposable;
import neo.wallets.AssetDescriptor;
import neo.wallets.NEP6.NEP6Wallet;
import neo.wallets.TransferOutput;
import neo.cryptography.ecc.ECPoint;
import neo.csharp.BitConverter;
import neo.csharp.Out;
import neo.csharp.Uint;
import neo.csharp.Ushort;
import neo.exception.InvalidOperationException;
import neo.exception.RpcException;
import neo.io.SerializeHelper;
import neo.ledger.AccountState;
import neo.ledger.AssetState;
import neo.ledger.Blockchain;
import neo.ledger.CoinState;
import neo.ledger.ContractState;
import neo.ledger.RelayResultReason;
import neo.ledger.StorageItem;
import neo.ledger.StorageKey;
import neo.ledger.TransactionState;
import neo.log.tr.TR;
import neo.network.p2p.LocalNode;
import neo.network.p2p.payloads.Block;
import neo.network.p2p.payloads.Header;
import neo.network.p2p.payloads.InvocationTransaction;
import neo.network.p2p.payloads.Transaction;
import neo.network.p2p.payloads.TransactionOutput;
import neo.persistence.Snapshot;
import neo.plugins.IRpcPlugin;
import neo.plugins.Plugin;
import neo.smartcontract.ContractParameter;
import neo.smartcontract.ContractParametersContext;
import neo.wallets.Coin;
import neo.wallets.Wallet;
import neo.wallets.WalletAccount;
import neo.smartcontract.ApplicationEngine;
import neo.vm.ScriptBuilder;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

public class RpcServer implements IDisposable {
    public Wallet wallet;
    private Server host;
    private Thread rpcThread = null;
    private Fixed8 maxGasInvoke;
    private final NeoSystem system;

    public RpcServer(NeoSystem system, Wallet wallet, Fixed8 maxGasInvoke) {
        TR.enter();
        this.system = system;
        this.wallet = wallet;
        this.maxGasInvoke = maxGasInvoke;
        TR.exit();
    }

    protected static JsonObject createErrorResponse(int id, int code, String message, Object data) {
        TR.enter();
        JsonObject response = createResponse(id);
        response.add("error", new JsonObject());
        response.get("error").getAsJsonObject().addProperty("code", code);
        response.get("error").getAsJsonObject().addProperty("message", message);
        if (data != null) {
            if (data instanceof JsonObject) {
                response.get("error").getAsJsonObject().add("data", (JsonObject) data);
            } else if (data instanceof String) {
                response.get("error").getAsJsonObject().addProperty("data", (String) data);
            } else {
                response.get("error").getAsJsonObject().add("data", null);
            }
        }
        return TR.exit(response);
    }

    private static JsonObject createResponse(int id) {
        TR.enter();
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.addProperty("id", id);
        return TR.exit(response);
    }

    private JsonObject getInvokeResult(byte[] script) {
        TR.enter();
        ApplicationEngine engine = ApplicationEngine.run(script, null, null, false, maxGasInvoke);
        JsonObject json = new JsonObject();
        json.addProperty("script", BitConverter.toHexString(script));
        json.addProperty("state", engine.state.toString());
        json.addProperty("gas_consumed", engine.getGasConsumed().toString());
        try {
            //json["stack"] = new JArray(engine.ResultStack.Select(p => p.ToParameter().ToJson()));
            JsonArray array = new JsonArray();
            int resultStackSize = engine.resultStack.getCount();
            for (int i = 0; i < resultStackSize; i++) {
                array.add(neo.vm.Helper.toParameter(engine.resultStack.peek(i)).toJson());
            }
            json.add("stack", array);
        } catch (InvalidOperationException e) {
            TR.error(e);
            json.addProperty("stack", "error: recursive reference");
        }
        if (wallet != null) {
            InvocationTransaction tx = new InvocationTransaction() {
                {
                    version = 1;
                    script = BitConverter.hexToBytes(json.get("script").getAsString());
                    gas = Fixed8.parse(json.get("gas_consumed").getAsString());
                }
            };
            tx.gas = Fixed8.subtract(tx.gas, Fixed8.fromDecimal(new BigDecimal("10")));
            if (tx.gas.compareTo(Fixed8.ZERO) < 0) {
                tx.gas = Fixed8.ZERO;
            }
            tx.gas = tx.gas.ceiling();
            tx = wallet.makeTransaction(tx);
            if (tx != null) {
                ContractParametersContext context = new ContractParametersContext(tx);
                wallet.sign(context);
                if (context.completed()) {
                    tx.witnesses = context.getWitnesses();
                } else {
                    tx = null;
                }
            }
            json.addProperty("tx", tx != null ? BitConverter.toHexString(SerializeHelper.toBytes(tx)) : null);
        }
        return TR.exit(json);
    }

    private static boolean getRelayResult(RelayResultReason reason) {
        TR.enter();
        switch (reason) {
            case Succeed:
                return true;
            case AlreadyExists:
                TR.exit();
                throw new RpcException(-501, "Block or transaction already exists and cannot be sent repeatedly.");
            case OutOfMemory:
                TR.exit();
                throw new RpcException(-502, "The memory pool is full and no more transactions can be sent.");
            case UnableToVerify:
                TR.exit();
                throw new RpcException(-503, "The block cannot be validated.");
            case Invalid:
                TR.exit();
                throw new RpcException(-504, "Block or transaction validation failed.");
            case PolicyFail:
                TR.exit();
                throw new RpcException(-505, "One of the Policy filters failed.");
            default:
                TR.exit();
                throw new RpcException(-500, "Unknown error.");
        }
    }

    public void openWallet(Wallet wallet) {
        TR.enter();
        this.wallet = wallet;
        TR.exit();
    }

    //Returns JsonObject, JsonArray, String or Integer type, or NULL
    private Object process(String method, JsonArray _params) {
        TR.enter();
        switch (method) {
            case "dumpprivkey":
                if (wallet == null) {
                    TR.exit();
                    throw new RpcException(-400, "Access denied");
                } else {
                    UInt160 scriptHash = neo.wallets.Helper.toScriptHash(_params.get(0).getAsString());
                    WalletAccount account = wallet.getAccount(scriptHash);
                    return TR.exit(account.getKey().export());
                }
            case "getaccountstate": {
                UInt160 script_hash = neo.wallets.Helper.toScriptHash(_params.get(0).getAsString());
                AccountState account = Blockchain.singleton().getStore().getAccounts().tryGet(script_hash);
                if (account == null) {
                    account = new AccountState(script_hash);
                }
                return TR.exit(account.toJson());
            }
            case "getassetstate": {
                UInt256 asset_id = UInt256.parse(_params.get(0).getAsString());
                AssetState asset = Blockchain.singleton().getStore().getAssets().tryGet(asset_id);
                JsonObject state = asset != null ? asset.toJson() : null;
                if (state != null) {
                    return TR.exit(state);
                } else {
                    TR.exit();
                    throw new RpcException(-100, "Unknown asset");
                }
            }
            case "getbalance":
                if (wallet == null) {
                    TR.exit();
                    throw new RpcException(-400, "Access denied.");
                } else {
                    JsonObject json = new JsonObject();
                    UIntBase asset_id = UIntBase.parse(_params.get(0).getAsString());
                    if (asset_id instanceof UInt160) {
                        json.addProperty("balance", wallet.getAvailable((UInt160) asset_id).toString());
                    } else if (asset_id instanceof UInt256) {
                        //Global Assets balance
                        //IEnumerable<Coin> coins = Wallet.GetCoins().Where(p => !p.State.HasFlag(CoinState.Spent) && p.Output.AssetId.Equals(asset_id_256));
                        List<Coin> coins = StreamSupport.stream(wallet.getCoins().spliterator(), false)
                                .filter(p -> !p.state.hasFlag(CoinState.Spent) && p.output.assetId.equals((UInt256) asset_id))
                                .collect(Collectors.toList());
                        //json["balance"] = coins.Sum(p => p.Output.Value).ToString();
                        //json["confirmed"] = coins.Where(p => p.State.HasFlag(CoinState.Confirmed)).Sum(p => p.Output.Value).ToString();
                        json.addProperty("balance", Helper.sum(coins, p -> p.output.value).toString());
                        json.addProperty("confirmed", Helper.sum(coins.stream().
                                filter(p -> p.state.hasFlag(CoinState.Confirmed)).
                                collect(Collectors.toList()), p -> p.output.value).toString());
                    }
                    return TR.exit(json);
                }
            case "getbestblockhash":
                return TR.exit(Blockchain.singleton().currentBlockHash().toString());
            case "getblock": {
                Block block;
                String parameter = _params.get(0).getAsString();
                try {
                    block = Blockchain.singleton().getStore().getBlock(new Uint(Integer.parseInt(parameter)));
                } catch (NumberFormatException e) {
                    block = Blockchain.singleton().getStore().getBlock(UInt256.parse(parameter));
                }
                if (block == null) {
                    TR.exit();
                    throw new RpcException(-100, "Unknown block");
                }
                boolean verbose = _params.size() >= 2 && _params.get(1).getAsInt() == 1;
                if (verbose) {
                    JsonObject json = block.toJson();
                    json.addProperty("confirmations", Blockchain.singleton().height().subtract(block.index).add(Uint.ONE).toString());
                    UInt256 hash = Blockchain.singleton().getStore().getNextBlockHash(block.hash());
                    if (hash != null)
                        json.addProperty("nextblockhash", hash.toString());
                    return TR.exit(json);
                }
                return TR.exit(BitConverter.toHexString(SerializeHelper.toBytes(block)));
            }
            case "getblockcount":
                return TR.exit(Blockchain.singleton().height().add(Uint.ONE).intValue());
            case "getblockhash": {
                try {
                    Uint height = new Uint(_params.get(0).getAsInt());
                    if (height.compareTo(Blockchain.singleton().height()) <= 0) {
                        return TR.exit(Blockchain.singleton().getBlockHash(height).toString());
                    } else {
                        TR.exit();
                        throw new RpcException(-100, "Invalid Height");
                    }
                } catch (NumberFormatException e) {
                    TR.error(e);
                    throw new RpcException(-100, "Invalid Height");
                }
            }
            case "getblockheader": {
                Header header;
                String parameter = _params.get(0).getAsString();
                try {
                    header = Blockchain.singleton().getStore().getHeader(new Uint(Integer.parseInt(parameter)));
                } catch (NumberFormatException e) {
                    header = Blockchain.singleton().getStore().getHeader(UInt256.parse(parameter));
                }
                if (header == null) {
                    TR.exit();
                    throw new RpcException(-100, "Unknown block");
                }

                boolean verbose = _params.size() >= 2 && _params.get(1).getAsInt() == 1;
                if (verbose) {
                    JsonObject json = header.toJson();
                    json.addProperty("confirmations", Blockchain.singleton().height().subtract(header.index).add(Uint.ONE).intValue());
                    UInt256 hash = Blockchain.singleton().getStore().getNextBlockHash(header.hash());
                    if (hash != null) {
                        json.addProperty("nextblockhash", hash.toString());
                    }
                    return TR.exit(json);
                }

                return TR.exit(BitConverter.toHexString(SerializeHelper.toBytes(header)));
            }
            case "getblocksysfee": {
                Uint height = new Uint(_params.get(0).getAsInt());
                if (height.compareTo(Blockchain.singleton().height()) <= 0) {
                    return TR.exit(String.valueOf(Blockchain.singleton().getStore().getSysFeeAmount(height)));
                } else {
                    TR.exit();
                    throw new RpcException(-100, "Invalid Height");
                }
            }
            case "getconnectioncount":
                return TR.exit(LocalNode.singleton().getConnectedCount());
            case "getcontractstate": {
                UInt160 script_hash = UInt160.parse(_params.get(0).getAsString());
                ContractState contract = Blockchain.singleton().getStore().getContracts().tryGet(script_hash);
                JsonObject result = contract != null ? contract.toJson() : null;
                if (result != null) {
                    return TR.exit(result);
                } else {
                    TR.exit();
                    throw new RpcException(-100, "Unknown contract");
                }
            }
            case "getnewaddress":
                if (wallet == null) {
                    TR.exit();
                    throw new RpcException(-400, "Access denied");
                } else {
                    WalletAccount account = wallet.createAccount();
                    if (wallet instanceof NEP6Wallet) {
                        ((NEP6Wallet) wallet).save();
                    }
                    return TR.exit(account.getAddress());
                }
            case "getpeers": {
                JsonObject json = new JsonObject();
                //json["unconnected"] = new JArray(LocalNode.Singleton.GetUnconnectedPeers().Select(p =>
                //                        {
                //                            JObject peerJson = new JObject();
                //                            peerJson["address"] = p.Address.ToString();
                //                            peerJson["port"] = p.Port;
                //                            return peerJson;
                //                        }));
                JsonArray array = new JsonArray();
                LocalNode.singleton().getUnconnectedPeers().stream().map(p -> {
                    JsonObject peerJson = new JsonObject();
                    peerJson.addProperty("address", p.getAddress().toString());
                    peerJson.addProperty("port", p.getPort());
                    return peerJson;
                }).forEach(p -> array.add(p));
                json.add("unconnected", array);
                json.add("bad", new JsonArray()); //badpeers has been removed
                //json["connected"] = new JArray(LocalNode.Singleton.GetRemoteNodes().Select(p =>
                //                        {
                //                            JObject peerJson = new JObject();
                //                            peerJson["address"] = p.Remote.Address.ToString();
                //                            peerJson["port"] = p.ListenerPort;
                //                            return peerJson;
                //                        }));
                JsonArray array2 = new JsonArray();
                LocalNode.singleton().getRemoteNodes().stream().map(p -> {
                    JsonObject peerJson = new JsonObject();
                    peerJson.addProperty("address", p.remote.getAddress().toString());
                    peerJson.addProperty("port", p.getListenerPort());
                    return peerJson;
                }).forEach(p -> array2.add(p));
                json.add("connected", array2);
                return TR.exit(json);
            }
            case "getrawmempool": {
                boolean shouldGetUnverified = _params.size() >= 1 && _params.get(0).getAsInt() == 1;
                if (!shouldGetUnverified) {
                    //return new JArray(Blockchain.Singleton.MemPool.GetVerifiedTransactions().Select(p => (JObject)p.Hash.ToString()));
                    JsonArray txArray = new JsonArray();
                    Blockchain.singleton().getMemPool().getVerifiedTransactions().forEach(p -> txArray.add(p.hash().toString()));
                    return TR.exit(txArray);
                }

                JsonObject json = new JsonObject();
                json.addProperty("height", Blockchain.singleton().height().intValue());
                Out<Collection<Transaction>> verifiedTransactions = new Out<>();
                Out<Collection<Transaction>> unverifiedTransactions = new Out<>();
                Blockchain.singleton().getMemPool().getVerifiedAndUnverifiedTransactions(
                        verifiedTransactions,
                        unverifiedTransactions);
                //json["verified"] = new JArray(verifiedTransactions.Select(p => (JObject) p.Hash.ToString()));
                JsonArray array = new JsonArray();
                verifiedTransactions.get().forEach(p -> array.add(p.hash().toString()));
                json.add("verified", array);
                //json["unverified"] = new JArray(unverifiedTransactions.Select(p = > (JObject) p.Hash.ToString()));
                JsonArray array2 = new JsonArray();
                unverifiedTransactions.get().forEach(p -> array2.add(p.hash().toString()));
                json.add("unverified", array2);
                return TR.exit(json);
            }
            case "getrawtransaction": {
                UInt256 hash = UInt256.parse(_params.get(0).getAsString());
                boolean verbose = _params.size() >= 2 && _params.get(1).getAsInt() == 1;
                Transaction tx = Blockchain.singleton().getTransaction(hash);
                if (tx == null) {
                    TR.exit();
                    throw new RpcException(-100, "Unknown transaction");
                }
                if (verbose) {
                    JsonObject json = tx.toJson();
                    TransactionState state = Blockchain.singleton().getStore().getTransactions().tryGet(hash);
                    Uint height = state != null ? state.blockIndex : null;
                    if (height != null) {
                        Header header = Blockchain.singleton().getStore().getHeader(height);
                        json.addProperty("blockhash", header.hash().toString());
                        json.addProperty("confirmations", Blockchain.singleton().height().subtract(header.index).add(Uint.ONE));
                        json.addProperty("blocktime", header.timestamp);
                    }
                    return TR.exit(json);
                }
                return TR.exit(BitConverter.toHexString(SerializeHelper.toBytes(tx)));
            }
            case "getstorage": {
                UInt160 script_hash = UInt160.parse(_params.get(0).getAsString());
                byte[] inputKey = BitConverter.hexToBytes(_params.get(1).getAsString());
                StorageItem item = Blockchain.singleton().getStore().getStorages().tryGet(new StorageKey() {
                    {
                        scriptHash = script_hash;
                        key = inputKey;
                    }
                });
                if (item == null) {
                    item = new StorageItem();
                }
                return TR.exit(item.value != null ? BitConverter.toHexString(item.value) : null);
            }
            case "gettransactionheight": {
                UInt256 hash = UInt256.parse(_params.get(0).getAsString());
                TransactionState state = Blockchain.singleton().getStore().getTransactions().tryGet(hash);
                Uint height = state != null ? state.blockIndex : null;
                if (height != null) {
                    return TR.exit(height.intValue());
                } else {
                    TR.exit();
                    throw new RpcException(-100, "Unknown transaction");
                }
            }
            case "gettxout": {
                UInt256 hash = UInt256.parse(_params.get(0).getAsString());
                Ushort index = new Ushort(_params.get(1).getAsInt());
                TransactionOutput output = Blockchain.singleton().getStore().getUnspent(hash, index);
                return TR.exit(output != null ? output.toJson(index.intValue()) : null);
            }
            case "getvalidators":
                Snapshot snapshot = Blockchain.singleton().getSnapshot();
                ECPoint[] validators = snapshot.getValidatorPubkeys();
                Stream stream = Arrays.stream(validators);
                JsonArray array = new JsonArray();
                snapshot.getEnrollments().stream().map(p ->
                {
                    JsonObject validator = new JsonObject();
                    validator.addProperty("publickey", p.publicKey.toString());
                    validator.addProperty("votes", p.votes.toString());
                    validator.addProperty("active", stream.anyMatch(q -> q.equals(p.publicKey)));
                    return validator;
                }).forEach(p -> array.add(p));
                return TR.exit(array);
            case "getversion": {
                JsonObject json = new JsonObject();
                json.addProperty("port", LocalNode.singleton().getListenerPort());
                json.addProperty("nonce", LocalNode.NONCE);
                json.addProperty("useragent", LocalNode.USER_AGENT);
                return TR.exit(json);
            }
            case "getwalletheight":
                if (wallet == null) {
                    TR.exit();
                    throw new RpcException(-400, "Access denied.");
                } else {
                    return TR.exit(wallet.getWalletHeight().compareTo(Uint.ZERO) > 0 ? wallet.getWalletHeight().subtract(Uint.ONE).intValue() : 0);
                }
            case "invoke": {
                UInt160 script_hash = UInt160.parse(_params.get(0).getAsString());
                ArrayList<ContractParameter> parameters = new ArrayList<ContractParameter>();
                _params.get(1).getAsJsonArray().forEach(p -> parameters.add(ContractParameter.fromJson(p.getAsJsonObject())));
                byte[] script;
                ScriptBuilder sb = new ScriptBuilder();
                script = neo.vm.Helper.emitAppCall(sb, script_hash, parameters.toArray(new ContractParameter[parameters.size()])).toArray();
                return TR.exit(getInvokeResult(script));
            }
            case "invokefunction": {
                UInt160 script_hash = UInt160.parse(_params.get(0).getAsString());
                String operation = _params.get(1).getAsString();
                ArrayList<ContractParameter> args = new ArrayList<ContractParameter>();
                if (_params.size() >= 3) {
                    _params.get(2).getAsJsonArray().forEach(p -> args.add(ContractParameter.fromJson(p.getAsJsonObject())));
                }
                byte[] script;
                ScriptBuilder sb = new ScriptBuilder();
                script = neo.vm.Helper.emitAppCall(sb, script_hash, operation, args.toArray(new ContractParameter[args.size()])).toArray();
                return TR.exit(getInvokeResult(script));
            }
            case "invokescript": {
                byte[] script = BitConverter.hexToBytes(_params.get(0).getAsString());
                return TR.exit(getInvokeResult(script));
            }
            case "listaddress":
                if (wallet == null) {
                    TR.exit();
                    throw new RpcException(-400, "Access denied.");
                } else {
                    JsonArray addressList = new JsonArray();
                    StreamSupport.stream(wallet.getAccounts().spliterator(), false).map(p -> {
                        JsonObject account = new JsonObject();
                        account.addProperty("address", p.getAddress());
                        account.addProperty("haskey", p.hasKey());
                        account.addProperty("label", p.label);
                        account.addProperty("watchonly", p.watchOnly());
                        return account;
                    }).forEach(p -> addressList.add(p));
                    return TR.exit(addressList);
                }
            case "sendfrom":
                if (wallet == null) {
                    TR.exit();
                    throw new RpcException(-400, "Access denied");
                } else {
                    UIntBase assetId = UIntBase.parse(_params.get(0).getAsString());
                    AssetDescriptor descriptor = new AssetDescriptor(assetId);
                    UInt160 from = neo.wallets.Helper.toScriptHash(_params.get(1).getAsString());
                    UInt160 to = neo.wallets.Helper.toScriptHash(_params.get(2).getAsString());
                    BigDecimal value = BigDecimal.valueOf(_params.get(3).getAsDouble());
                    value.setScale(descriptor.decimals);
                    if (value.signum() <= 0) {
                        TR.exit();
                        throw new RpcException(-32602, "Invalid params");
                    }
                    Fixed8 fee = _params.size() >= 5 ? Fixed8.parse(String.valueOf(_params.get(4).getAsDouble())) : Fixed8.ZERO;
                    if (fee.compareTo(Fixed8.ZERO) < 0) {
                        TR.exit();
                        throw new RpcException(-32602, "Invalid params");
                    }
                    UInt160 change_address = _params.size() >= 6 ? neo.wallets.Helper.toScriptHash(_params.get(5).getAsString()) : null;
                    Transaction tx = wallet.makeTransaction(null, Arrays.asList(new TransferOutput[]
                            {
                                    new TransferOutput(assetId, value, to)
                            }), from, change_address, fee);
                    if (tx == null) {
                        TR.exit();
                        throw new RpcException(-300, "Insufficient funds");
                    }
                    ContractParametersContext context = new ContractParametersContext(tx);
                    wallet.sign(context);
                    if (context.completed()) {
                        tx.witnesses = context.getWitnesses();
                        wallet.applyTransaction(tx);
                        system.localNode.tell(new LocalNode.Relay() {
                            {
                                inventory = tx;
                            }
                        }, ActorRef.noSender());
                        return TR.exit(tx.toJson());
                    } else {
                        return TR.exit(context.toJson());
                    }
                }
            case "sendmany":
                if (wallet == null) {
                    TR.exit();
                    throw new RpcException(-400, "Access denied");
                } else {
                    JsonArray to = _params.get(0).getAsJsonArray();
                    if (to.size() == 0) {
                        TR.exit();
                        throw new RpcException(-32602, "Invalid params");
                    }
                    TransferOutput[] outputs = new TransferOutput[to.size()];
                    for (int i = 0; i < to.size(); i++) {
                        JsonObject object = to.get(i).getAsJsonObject();
                        UIntBase asset_id = UIntBase.parse(object.get("asset").getAsString());
                        AssetDescriptor descriptor = new AssetDescriptor(asset_id);
                        outputs[i] = new TransferOutput(asset_id,
                                BigDecimal.valueOf(object.get("value").getAsInt(), descriptor.decimals),
                                neo.wallets.Helper.toScriptHash(object.get("address").getAsString()));
                        if (outputs[i].value.signum() <= 0) {
                            TR.exit();
                            throw new RpcException(-32602, "Invalid params");
                        }
                    }
                    Fixed8 fee = _params.size() >= 2 ? Fixed8.parse(String.valueOf(_params.get(1).getAsString())) : Fixed8.ZERO;
                    if (fee.compareTo(Fixed8.ZERO) < 0) {
                        TR.exit();
                        throw new RpcException(-32602, "Invalid params");
                    }
                    UInt160 change_address = _params.size() >= 3 ? neo.wallets.Helper.toScriptHash(_params.get(2).getAsString()) : null;
                    Transaction tx = wallet.makeTransaction(null, Arrays.asList(outputs), null, change_address, fee);
                    if (tx == null) {
                        TR.exit();
                        throw new RpcException(-300, "Insufficient funds");
                    }
                    ContractParametersContext context = new ContractParametersContext(tx);
                    wallet.sign(context);
                    if (context.completed()) {
                        tx.witnesses = context.getWitnesses();
                        wallet.applyTransaction(tx);
                        system.localNode.tell(new LocalNode.Relay() {
                            {
                                inventory = tx;
                            }
                        }, ActorRef.noSender());
                        return TR.exit(tx.toJson());
                    } else {
                        return TR.exit(context.toJson());
                    }
                }
            case "sendrawtransaction": {
                try {
                    Transaction tx = Transaction.deserializeFrom(BitConverter.hexToBytes(_params.get(0).getAsString()));
                    Future<Object> future = ask(system.blockchain, tx, new Timeout(FiniteDuration.apply(60, TimeUnit.SECONDS)));
                    return TR.exit(getRelayResult((RelayResultReason) Await.result(future, Duration.create(60, TimeUnit.SECONDS))));
                } catch (Exception e) {
                    TR.error(e);
                    throw new RuntimeException(e);
                }
            }
            case "sendtoaddress":
                if (wallet == null) {
                    TR.exit();
                    throw new RpcException(-400, "Access denied");
                } else {
                    UIntBase assetId = UIntBase.parse(_params.get(0).getAsString());
                    AssetDescriptor descriptor = new AssetDescriptor(assetId);
                    UInt160 scriptHash = neo.wallets.Helper.toScriptHash(_params.get(1).getAsString());
                    BigDecimal value = BigDecimal.valueOf(_params.get(2).getAsDouble());
                    value.setScale(descriptor.decimals);
                    if (value.signum() <= 0) {
                        TR.exit();
                        throw new RpcException(-32602, "Invalid params");
                    }
                    Fixed8 fee = _params.size() >= 4 ? Fixed8.parse(String.valueOf(_params.get(3).getAsDouble())) : Fixed8.ZERO;
                    if (fee.compareTo(Fixed8.ZERO) < 0) {
                        TR.exit();
                        throw new RpcException(-32602, "Invalid params");
                    }
                    UInt160 change_address = _params.size() >= 5 ? neo.wallets.Helper.toScriptHash(_params.get(4).getAsString()) : null;
                    Transaction tx = wallet.makeTransaction(null, Arrays.asList(new TransferOutput[]
                            {new TransferOutput(assetId, value, scriptHash)}), null, change_address, fee);
                    if (tx == null) {
                        TR.exit();
                        throw new RpcException(-300, "Insufficient funds");
                    }
                    ContractParametersContext context = new ContractParametersContext(tx);
                    wallet.sign(context);
                    if (context.completed()) {
                        tx.witnesses = context.getWitnesses();
                        wallet.applyTransaction(tx);
                        system.localNode.tell(new LocalNode.Relay() {
                            {
                                inventory = tx;
                            }
                        }, ActorRef.noSender());
                        return TR.exit(tx.toJson());
                    } else {
                        return TR.exit(context.toJson());
                    }
                }
            case "submitblock": {
                try {
                    Block block = SerializeHelper.parse(Block::new, BitConverter.hexToBytes(_params.get(0).getAsString()));
                    Future<Object> future = ask(system.blockchain, block, new Timeout(null));
                    return TR.exit(getRelayResult((RelayResultReason) Await.result(future, Duration.create(10, TimeUnit.SECONDS))));
                } catch (Exception e) {
                    TR.error(e);
                    throw new RuntimeException(e);
                }
            }
            case "validateaddress": {
                JsonObject json = new JsonObject();
                UInt160 scriptHash;
                try {
                    scriptHash = neo.wallets.Helper.toScriptHash(_params.get(0).getAsString());
                } catch (Exception e) {
                    scriptHash = null;
                }
                json.addProperty("address", _params.get(0).getAsString());
                json.addProperty("isvalid", scriptHash != null);
                return TR.exit(json);
            }
            default:
                TR.exit();
                throw new RpcException(-32601, "Method not found");
        }
    }

    protected JsonObject processRequest(HttpServletRequest req, HttpServletResponse resp, JsonObject request) {
        TR.enter();
        if (!request.has("id")) {
            return TR.exit(null);
        }
        if (!request.has("method") || !request.has("params") || !(request.get("params") instanceof JsonArray)) {
            return TR.exit(createErrorResponse(request.get("id").getAsInt(), -32600, "Invalid Request", null));
        }
        Object result = null;
        try {
            String method = request.get("method").getAsString();
            JsonArray _params = request.getAsJsonArray("params");
            for (IRpcPlugin plugin : Plugin.getRPCPlugins()) {
                result = plugin.onProcess(req, resp, method, _params);
                if (result != null) {
                    break;
                }
            }
            if (result == null) {
                result = process(method, _params);
            }
        } catch (Exception ex) {
            boolean isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean().
                    getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;
            if (isDebug) {
                return TR.exit(createErrorResponse(request.get("id").getAsInt(), 0, ex.getMessage(), ex.getStackTrace().toString()));
            } else {
                return TR.exit(createErrorResponse(request.get("id").getAsInt(), 0, ex.getMessage(), null));
            }
        }
        JsonObject response = createResponse(request.get("id").getAsInt());
        if (result == null) {
            response.add("result", null);
        } else if (result instanceof JsonObject) {
            response.add("result", (JsonObject) result);
        } else if (result instanceof JsonArray) {
            response.add("result", (JsonArray) result);
        } else if (result instanceof String) {
            response.addProperty("result", (String) result);
        } else if (result instanceof Integer) {
            response.addProperty("result", (Integer) result);
        }
        return TR.exit(response);
    }

    public void start(InetSocketAddress bindAddress, String sslCert, String password, String[] trustedAuthorities) {
        //TODO: SSL Certificate verification
        TR.enter();
        host = new Server(bindAddress);
        ServletContextHandler context = new ServletContextHandler(host, "/");
        host.setHandler(context);
        context.addServlet(RpcServerHandler.class, "");
        rpcThread = new Thread() {
            public void run() {
                try {
                    host.start();
                    host.join();
                } catch (Exception e) {
                    TR.error(e);
                    throw new RuntimeException(e);
                }
            }
        };
        rpcThread.start();
        TR.exit();
    }

    public void dispose() {
        TR.enter();
        if (host != null) {
            host = null;
        }
        rpcThread.interrupt();
        rpcThread = null;
        TR.exit();
    }
}