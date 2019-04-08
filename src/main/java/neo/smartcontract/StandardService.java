package neo.smartcontract;

import org.bouncycastle.math.ec.ECCurve;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Function;

import neo.UInt160;
import neo.UInt256;
import neo.cryptography.ecc.ECC;
import neo.cryptography.ecc.ECPoint;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.common.IDisposable;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.csharp.io.MemoryStream;
import neo.exception.FormatException;
import neo.ledger.Blockchain;
import neo.ledger.ContractState;
import neo.ledger.StorageFlags;
import neo.ledger.StorageItem;
import neo.ledger.StorageKey;
import neo.ledger.TransactionState;
import neo.log.notr.TR;
import neo.network.p2p.payloads.Block;
import neo.network.p2p.payloads.BlockBase;
import neo.network.p2p.payloads.Header;
import neo.network.p2p.payloads.IVerifiable;
import neo.network.p2p.payloads.Transaction;
import neo.persistence.Snapshot;
import neo.vm.ExecutionEngine;
import neo.vm.IInteropService;
import neo.vm.StackItem;
import neo.vm.Types.Array;
import neo.vm.Types.Boolean;
import neo.vm.Types.ByteArray;
import neo.vm.Types.Integer;
import neo.vm.Types.InteropInterface;
import neo.vm.Types.Struct;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: StandardService
 * @Package neo.smartcontract
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 14:10 2019/3/12
 */
public class StandardService implements IInteropService, IDisposable {
    public static EventHandler<NotifyEventArgs> notify = new EventHandler<>();
    public static EventHandler<LogEventArgs> log = new EventHandler<>();

    protected TriggerType trigger;
    protected Snapshot snapshot;
    protected final List<IDisposable> disposables = new ArrayList<IDisposable>();
    protected Map<UInt160, UInt160> contractsCreated = new HashMap<UInt160, UInt160>();
    private final List<NotifyEventArgs> notifications = new ArrayList<NotifyEventArgs>();
    private final Map<Uint, Function<ExecutionEngine, java.lang.Boolean>> methods = new HashMap();
    private final Map<Uint, Long> prices = new HashMap<Uint, Long>();

    public List<NotifyEventArgs> getNotifications() {
        return notifications;
    }

    public StandardService(TriggerType trigger, Snapshot snapshot) {
        this.trigger = trigger;
        this.snapshot = snapshot;
        Register("System.ExecutionEngine.GetScriptContainer", this::executionEngineGetScriptContainer, 1);
        Register("System.ExecutionEngine.GetExecutingScriptHash", this::executionEngineGetExecutingScriptHash, 1);
        Register("System.ExecutionEngine.GetCallingScriptHash", this::executionEngineGetCallingScriptHash, 1);
        Register("System.ExecutionEngine.GetEntryScriptHash", this::executionEngineGetEntryScriptHash, 1);
        Register("System.Runtime.Platform", this::runtimePlatform, 1);
        Register("System.Runtime.GetTrigger", this::runtimeGetTrigger, 1);
        Register("System.Runtime.CheckWitness", this::runtimeCheckWitness, 200);
        Register("System.Runtime.Notify", this::runtimeNotify, 1);
        Register("System.Runtime.Log", this::runtimeLog, 1);
        Register("System.Runtime.GetTime", this::runtimeGetTime, 1);
        Register("System.Runtime.Serialize", this::runtimeSerialize, 1);
        Register("System.Runtime.Deserialize", this::runtimeDeserialize, 1);
        Register("System.Blockchain.GetHeight", this::blockchainGetHeight, 1);
        Register("System.Blockchain.GetHeader", this::blockchainGetHeader, 100);
        Register("System.Blockchain.GetBlock", this::blockchainGetBlock, 200);
        Register("System.Blockchain.GetTransaction", this::blockchainGetTransaction, 200);
        Register("System.Blockchain.GetTransactionHeight", this::blockchainGetTransactionHeight,
                100);
        Register("System.Blockchain.GetContract", this::blockchainGetContract, 100);
        Register("System.Header.GetIndex", this::headerGetIndex, 1);
        Register("System.Header.GetHash", this::headerGetHash, 1);
        Register("System.Header.GetPrevHash", this::headerGetPrevHash, 1);
        Register("System.Header.GetTimestamp", this::headerGetTimestamp, 1);
        Register("System.Block.GetTransactionCount", this::blockGetTransactionCount, 1);
        Register("System.Block.GetTransactions", this::blockGetTransactions, 1);
        Register("System.Block.GetTransaction", this::blockGetTransaction, 1);
        Register("System.Transaction.GetHash", this::transactionGetHash, 1);
        Register("System.Contract.Destroy", this::contractDestroy, 1);
        Register("System.Contract.GetStorageContext", this::contractGetStorageContext, 1);
        Register("System.Storage.GetContext", this::storageGetContext, 1);
        Register("System.Storage.GetReadOnlyContext", this::storageGetReadOnlyContext, 1);
        Register("System.Storage.Get", this::storageGet, 100);
        Register("System.Storage.Put", this::storagePut);
        Register("System.Storage.PutEx", this::storagePutEx);
        Register("System.Storage.Delete", this::storageDelete, 100);
        Register("System.StorageContext.AsReadOnly", this::storageContextAsReadOnly, 1);
    }

    boolean checkStorageContext(StorageContext context) {
        ContractState contract = snapshot.getContracts().tryGet(context.scriptHash);
        if (contract == null) return false;
        if (!contract.hasStorage()) return false;
        return true;
    }

    public void commit() {
        snapshot.commit();
    }

    @Override
    public void dispose() {
        for (IDisposable disposable : disposables)
            disposable.dispose();
        disposables.clear();
    }

    public long getPrice(Uint hash) {
        return prices.getOrDefault(hash, 0L);
    }

    @Override
    public boolean invoke(byte[] method, ExecutionEngine engine) {
        Uint hash = null;
        try {
            hash = (method.length == 4)
                    ? BitConverter.toUint(method)
                    : Helper.toInteropMethodHash(new String(method, "ascii"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Function<ExecutionEngine, java.lang.Boolean> func = methods.getOrDefault(hash, null);
        if (func == null) {
            return false;
        }
        return func.apply(engine);
    }

    protected void Register(String method, Function<ExecutionEngine, java.lang.Boolean> handler) {
        methods.put(Helper.toInteropMethodHash(method), handler);
    }

    protected void Register(String method, Function<ExecutionEngine, java.lang.Boolean> handler, long price) {
        Register(method, handler);
        prices.put(Helper.toInteropMethodHash(method), price);
    }

    protected boolean executionEngineGetScriptContainer(ExecutionEngine engine) {
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(engine
                .scriptContainer));
        return true;
    }

    protected boolean executionEngineGetExecutingScriptHash(ExecutionEngine engine) {
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(engine.getCurrentContext().getScriptHash
                ()));
        return true;
    }

    protected boolean executionEngineGetCallingScriptHash(ExecutionEngine engine) {
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(engine.getCallingContext().getScriptHash
                ()));
        return true;
    }

    protected boolean executionEngineGetEntryScriptHash(ExecutionEngine engine) {
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(engine
                .getEntryContext().getScriptHash()));
        return true;
    }

    protected boolean runtimePlatform(ExecutionEngine engine) {
        try {
            engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem("NEO".getBytes
                    ("ascii")));
        } catch (UnsupportedEncodingException e) {
            TR.fixMe("字符串类型转换异常");
            throw new RuntimeException(e);
        }
        return true;
    }

    protected boolean runtimeGetTrigger(ExecutionEngine engine) {
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(StackItem.getStackItem(((int) trigger
                .getTriggerType()))));
        return true;
    }

    protected boolean checkWitness(ExecutionEngine engine, UInt160 hash) {
        IVerifiable container = (IVerifiable) engine.scriptContainer;
        UInt160[] _hashes_for_verifying = container.getScriptHashesForVerifying(snapshot);
        return Arrays.asList(_hashes_for_verifying).stream().anyMatch(p -> p.equals(hash));
    }

    protected boolean checkWitness(ExecutionEngine engine, ECPoint pubkey) {
        return checkWitness(engine, Helper.toScriptHash(Contract.createSignatureRedeemScript
                (pubkey)));
    }

    protected boolean runtimeCheckWitness(ExecutionEngine engine) {
        byte[] hashOrPubkey = engine.getCurrentContext().evaluationStack.pop().getByteArray();
        boolean result;
        if (hashOrPubkey.length == 20)
            result = checkWitness(engine, new UInt160(hashOrPubkey));
        else if (hashOrPubkey.length == 33)
            result = checkWitness(engine, new ECPoint(ECC.Secp256r1.getCurve().decodePoint
                    (hashOrPubkey)));
        else
            return false;
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(result));
        return true;
    }

    protected boolean runtimeNotify(ExecutionEngine engine) {
        StackItem state = engine.getCurrentContext().evaluationStack.pop();
        NotifyEventArgs notification = new NotifyEventArgs(engine.scriptContainer, new UInt160
                (engine.getCurrentContext().getScriptHash()), state);
        notify.invoke(this, notification);
        notifications.add(notification);
        return true;
    }

    protected boolean runtimeLog(ExecutionEngine engine) {
        String message = null;
        try {
            message = new String(engine.getCurrentContext().evaluationStack
                    .pop().getByteArray(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            TR.fixMe("字符串转换异常");
            throw new RuntimeException(e);
        }
        log.invoke(this, new LogEventArgs(engine.scriptContainer, new UInt160(engine
                .getCurrentContext().getScriptHash()), message));
        return true;
    }

    protected boolean runtimeGetTime(ExecutionEngine engine) {
        if (snapshot.getPersistingBlock() == null) {
            Header header = snapshot.getHeader(snapshot.getCurrentBlockHash());
            engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(StackItem.getStackItem(header
                    .timestamp.add(new Uint(Blockchain.SecondsPerBlock)))));
        } else {
            engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(StackItem.getStackItem(snapshot
                    .getPersistingBlock()
                    .timestamp)));
        }
        return true;
    }

    private void SerializeStackItem(StackItem item, BinaryWriter writer) {
        List<StackItem> serialized = new ArrayList<StackItem>();
        Stack<StackItem> unserialized = new Stack<StackItem>();
        unserialized.push(item);
        while (unserialized.size() > 0) {
            item = unserialized.pop();
            if (item instanceof ByteArray) {
                writer.writeByte(StackItemType.ByteArray.getStackItemType());
                writer.writeVarBytes(item.getByteArray());
            } else if (item instanceof Boolean) {
                writer.writeByte(StackItemType.Boolean.getStackItemType());
                writer.writeBoolean(item.getBoolean());
            } else if (item instanceof Integer) {
                writer.writeByte(StackItemType.Integer.getStackItemType());
                writer.writeVarBytes(item.getByteArray());
            } else if (item instanceof InteropInterface) {
                throw new UnsupportedOperationException();
            } else if (item instanceof Array) {
                //LINQ START
/*                if (serialized.Any(p = > ReferenceEquals(p, array))){
                    throw new UnsupportedOperationException();
                }*/
                StackItem finalItem = item;
                if (serialized.stream().anyMatch(p -> p == finalItem)) {
                    throw new UnsupportedOperationException();
                }
                //LINQ END
                serialized.add(item);
                if (item instanceof Struct) {
                    writer.writeByte(StackItemType.Struct.getStackItemType());
                } else {
                    writer.writeByte(StackItemType.Array.getStackItemType());
                }
                writer.writeVarInt(((Array) item).getCount());
                for (int i = ((Array) item).getCount() - 1; i >= 0; i--) {
                    unserialized.push(((Array) item).getArrayItem(i));
                }
            } else if (item instanceof neo.vm.Types.Map) {
                //LINQ START
/*                if (serialized.Any(p = > ReferenceEquals(p, map))){
                    throw new UnsupportedOperationException();
                }*/
                StackItem finalItem = item;
                if (serialized.stream().anyMatch(p -> p == finalItem)) {
                    throw new UnsupportedOperationException();
                }
                //LINQ END
                serialized.add(item);
                writer.writeByte(StackItemType.Map.getStackItemType());
                writer.writeVarInt(((neo.vm.Types.Map) item).getCount());
                //// TODO: 2019/3/27
                //不清楚为何map要reserve
                for (Map.Entry<StackItem, StackItem> pair : ((neo.vm.Types.Map) item)) {
                    unserialized.push(pair.getValue());
                    unserialized.push(pair.getKey());
                }
/*                for (Map.Entry<StackItem,StackItem> pair:((neo.vm.Types.Map) item).reverse())
                {
                    unserialized.push(pair.getValue());
                    unserialized.push(pair.getKey());
                }*/
            }
        }
    }

    protected boolean runtimeSerialize(ExecutionEngine engine) {
        ByteArrayOutputStream ms = new ByteArrayOutputStream();
        BinaryWriter writer = new BinaryWriter(ms);
        {
            try {
                SerializeStackItem(engine.getCurrentContext().evaluationStack.pop(), writer);
            } catch (UnsupportedOperationException e) {
                return false;
            }
            writer.flush();

            if (ms.size() > ApplicationEngine.MaxItemSize.intValue())
                return false;
            engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(ms.toByteArray()));
        }
        return true;
    }

    private StackItem deserializeStackItem(BinaryReader reader) {
        Stack<StackItem> deserialized = new Stack<StackItem>();
        int undeserialized = 1;
        while (undeserialized-- > 0) {
            StackItemType type = StackItemType.fromByte((byte) reader.readByte());
            switch (type) {
                case ByteArray:
                    deserialized.push(new ByteArray(reader.readVarBytes()));
                    break;
                case Boolean:
                    deserialized.push(new Boolean(reader.readBoolean()));
                    break;
                case Integer:
                    deserialized.push(new Integer(new BigInteger(reader.readVarBytes())));
                    break;
                case Array:
                case Struct: {
                    int count = reader.readVarInt(ApplicationEngine.MaxArraySize.ulongValue()).intValue();
                    ContainerPlaceholder tempContainerPlaceholder = new ContainerPlaceholder();
                    tempContainerPlaceholder.Type = type;
                    tempContainerPlaceholder.ElementCount = count;
                    deserialized.push(tempContainerPlaceholder);
                    undeserialized += count;
                }
                break;
                case Map: {
                    int count = reader.readVarInt(ApplicationEngine.MaxArraySize.ulongValue
                            ()).intValue();
                    ContainerPlaceholder tempContainerPlaceholder = new ContainerPlaceholder();
                    tempContainerPlaceholder.Type = type;
                    tempContainerPlaceholder.ElementCount = count;
                    deserialized.push(tempContainerPlaceholder);
                    undeserialized += count * 2;
                }
                break;
                default:
                    throw new FormatException();
            }
        }
        Stack<StackItem> stack_temp = new Stack<StackItem>();
        while (deserialized.size() > 0) {
            StackItem item = deserialized.pop();
            if (item instanceof ContainerPlaceholder) {
                switch (((ContainerPlaceholder) item).Type) {
                    case Array:
                        Array array = new Array();
                        for (int i = 0; i < ((ContainerPlaceholder) item).ElementCount; i++)
                            array.add(stack_temp.pop());
                        item = array;
                        break;
                    case Struct:
                        Struct struct = new Struct();
                        for (int i = 0; i < ((ContainerPlaceholder) item).ElementCount; i++)
                            struct.add(stack_temp.pop());
                        item = struct;
                        break;
                    case Map:
                        neo.vm.Types.Map map = new neo.vm.Types.Map();
                        for (int i = 0; i < ((ContainerPlaceholder) item).ElementCount; i++) {
                            StackItem key = stack_temp.pop();
                            StackItem value = stack_temp.pop();
                            map.add(key, value);
                        }
                        item = map;
                        break;
                }
            }
            stack_temp.push(item);
        }
        return stack_temp.peek();
    }

    protected boolean runtimeDeserialize(ExecutionEngine engine) {
        byte[] data = engine.getCurrentContext().evaluationStack.pop().getByteArray();
        MemoryStream ms = new MemoryStream(data);
        BinaryReader reader = new BinaryReader(ms);
        StackItem item;
        try {
            item = deserializeStackItem(reader);
        } catch (FormatException e) {
            return false;
        }
        engine.getCurrentContext().evaluationStack.push(item);

        return true;
    }

    protected boolean blockchainGetHeight(ExecutionEngine engine) {
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(StackItem.getStackItem(snapshot.getHeight
                ())));
        return true;
    }

    protected boolean blockchainGetHeader(ExecutionEngine engine) {
        byte[] data = engine.getCurrentContext().evaluationStack.pop().getByteArray();
        UInt256 hash;
        if (data.length <= 5)
            hash = Blockchain.singleton().getBlockHash(BitConverter.toUint(data));
        else if (data.length == 32)
            hash = new UInt256(data);
        else
            return false;
        if (hash == null) {
            engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(new byte[0]));
        } else {
            Header header = snapshot.getHeader(hash);
            engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(header));
        }
        return true;
    }

    protected boolean blockchainGetBlock(ExecutionEngine engine) {
        byte[] data = engine.getCurrentContext().evaluationStack.pop().getByteArray();
        UInt256 hash;
        if (data.length <= 5)
            hash = Blockchain.singleton().getBlockHash(new Uint(String.valueOf(new BigInteger
                    (data))));
        else if (data.length == 32)
            hash = new UInt256(data);
        else
            return false;
        if (hash == null) {
            engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(new byte[0]));
        } else {
            Block block = snapshot.getBlock(hash);
            engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(block));
        }
        return true;
    }

    protected boolean blockchainGetTransaction(ExecutionEngine engine) {
        byte[] hash = engine.getCurrentContext().evaluationStack.pop().getByteArray();
        Transaction tx = snapshot.getTransaction(new UInt256(hash));
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(tx));
        return true;
    }

    protected boolean blockchainGetTransactionHeight(ExecutionEngine engine) {
        byte[] hash = engine.getCurrentContext().evaluationStack.pop().getByteArray();
        TransactionState temp = snapshot.getTransactions().tryGet(new UInt256(hash));
        Uint height = temp.blockIndex;

        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(
                (height != null ? StackItem.getStackItem(height) : new BigInteger("-1"))));
        return true;
    }

    protected boolean blockchainGetContract(ExecutionEngine engine) {
        UInt160 hash = new UInt160(engine.getCurrentContext().evaluationStack.pop().getByteArray());
        ContractState contract = snapshot.getContracts().tryGet(hash);
        if (contract == null)
            engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(new byte[0]));
        else
            engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(contract));
        return true;
    }

    protected boolean headerGetIndex(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().evaluationStack.pop();
        if (_interface instanceof InteropInterface) {
            BlockBase header = ((InteropInterface<BlockBase>) _interface).getInterface();
            if (header == null) return false;
            engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(StackItem
                    .getStackItem(header.index)));
            return true;
        }
        return false;
    }

    protected boolean headerGetHash(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().evaluationStack.pop();
        if (_interface instanceof InteropInterface) {
            BlockBase header = ((InteropInterface<BlockBase>) _interface).getInterface();
            if (header == null) return false;
            engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(header.hash()
                    .toArray()));
            return true;
        }
        return false;
    }

    protected boolean headerGetPrevHash(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().evaluationStack.pop();
        if (_interface instanceof InteropInterface) {
            BlockBase header = ((InteropInterface<BlockBase>) _interface).getInterface();
            if (header == null) return false;
            engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(header
                    .prevHash.toArray()));
            return true;
        }
        return false;
    }

    protected boolean headerGetTimestamp(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().evaluationStack.pop();
        if (_interface instanceof InteropInterface) {
            BlockBase header = ((InteropInterface<BlockBase>) _interface).getInterface();
            if (header == null) return false;
            engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(StackItem.getStackItem(header
                    .timestamp)));
            return true;
        }
        return false;
    }

    protected boolean blockGetTransactionCount(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().evaluationStack.pop();
        if (_interface instanceof InteropInterface) {
            Block block = ((InteropInterface<Block>) _interface).getInterface();
            if (block == null) return false;
            engine.getCurrentContext().evaluationStack
                    .push(StackItem.getStackItem(StackItem.getStackItem(block
                            .transactions.length)));
            return true;
        }
        return false;
    }

    protected boolean blockGetTransactions(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().evaluationStack.pop();
        if (_interface instanceof InteropInterface) {
            Block block = ((InteropInterface<Block>) _interface).getInterface();
            if (block == null) return false;
            if (block.transactions.length > ApplicationEngine.MaxArraySize.intValue())
                return false;
            //LINQ START
            /*engine.getCurrentContext().evaluationStack.push(block.transactions.Select(p = >
                    StackItem.fromInterface(p)).ToArray());*/
            StackItem[] array = Arrays.asList(block.transactions).stream().map(p -> StackItem
                    .fromInterface(p)).toArray(StackItem[]::new);
            engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(array));
            //LINQ END
            return true;
        }
        return false;
    }

    protected boolean blockGetTransaction(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().evaluationStack.pop();
        if (_interface instanceof InteropInterface) {
            Block block = ((InteropInterface<Block>) _interface).getInterface();
            int index = engine.getCurrentContext().evaluationStack.pop().getBigInteger().intValue();
            if (block == null) return false;
            if (index < 0 || index >= block.transactions.length) return false;
            Transaction tx = block.transactions[index];
            engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(tx));
            return true;
        }
        return false;
    }

    protected boolean transactionGetHash(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().evaluationStack.pop();
        if (_interface instanceof InteropInterface) {
            Transaction tx = ((InteropInterface<Transaction>) _interface).getInterface();
            if (tx == null) return false;
            engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(tx.hash()
                    .toArray()));
            return true;
        }
        return false;
    }

    protected boolean storageGetContext(ExecutionEngine engine) {
        StorageContext temp = new StorageContext();
        temp.scriptHash = new UInt160(engine.getCurrentContext().getScriptHash());
        temp.isReadOnly = false;
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(temp));
        return true;
    }

    protected boolean storageGetReadOnlyContext(ExecutionEngine engine) {
        StorageContext temp = new StorageContext();
        temp.scriptHash = new UInt160(engine.getCurrentContext().getScriptHash());
        temp.isReadOnly = true;
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(temp));
        return true;
    }

    protected boolean storageGet(ExecutionEngine engine) {
        StackItem tempItem = engine.getCurrentContext().evaluationStack.pop();
        if (tempItem instanceof InteropInterface) {
            StorageContext context = ((InteropInterface<StorageContext>) tempItem).getInterface();
            if (!checkStorageContext(context)) return false;
            byte[] key = engine.getCurrentContext().evaluationStack.pop().getByteArray();
            StorageKey temp = new StorageKey();
            temp.scriptHash = context.scriptHash;
            temp.key = key;
            StorageItem item = snapshot.getStorages().tryGet(temp);
            engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem
                    (item != null ? item.value : new byte[0]));
            return true;
        }
        return false;
    }

    protected boolean storageContextAsReadOnly(ExecutionEngine engine) {
        StackItem tempItem = engine.getCurrentContext().evaluationStack.pop();
        if (tempItem instanceof InteropInterface) {
            StorageContext context = ((InteropInterface<StorageContext>) tempItem).getInterface();
            if (!context.isReadOnly) {
                StorageContext temp = new StorageContext();
                temp.scriptHash = context.scriptHash;
                temp.isReadOnly = true;
                context = temp;
            }
            engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(context));
            return true;
        }
        return false;
    }

    protected boolean contractGetStorageContext(ExecutionEngine engine) {
        StackItem tempItem = engine.getCurrentContext().evaluationStack.pop();
        if (tempItem instanceof InteropInterface) {
            ContractState contract = ((InteropInterface<ContractState>) tempItem).getInterface();
            UInt160 created = contractsCreated.getOrDefault(contract.getScriptHash(), null);
            if (created == null)
                return false;
            if (!created.equals(new UInt160(engine.getCurrentContext().getScriptHash())))
                return false;

            StorageContext temp = new StorageContext();
            temp.scriptHash = contract.getScriptHash();
            temp.isReadOnly = false;
            engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(temp));
            return true;
        }
        return false;
    }

    protected boolean contractDestroy(ExecutionEngine engine) {
        if (trigger != TriggerType.Application) return false;
        UInt160 hash = new UInt160(engine.getCurrentContext().getScriptHash());
        ContractState contract = snapshot.getContracts().tryGet(hash);
        if (contract == null) return true;
        snapshot.getContracts().delete(hash);
        if (contract.hasStorage()) {
            for (Map.Entry<StorageKey, StorageItem> pair : snapshot.getStorages().find(hash.toArray())) {
                snapshot.getStorages().delete(pair.getKey());
            }
        }
        return true;
    }

    private boolean putEx(StorageContext context, byte[] key, byte[] value, StorageFlags flags) {
        if (trigger != TriggerType.Application && trigger != TriggerType.ApplicationR)
            return false;
        if (key.length > 1024) return false;
        if (context.isReadOnly) return false;
        if (!checkStorageContext(context)) return false;
        StorageKey skey = new StorageKey();
        skey.scriptHash = context.scriptHash;
        skey.key = key;
        StorageItem item = snapshot.getStorages().getAndChange(skey, StorageItem::new);
        if (item.isConstant) return false;
        item.value = value;
        item.isConstant = flags.hasFlag(StorageFlags.Constant);
        return true;
    }

    protected boolean storagePut(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().evaluationStack.pop();
        if (!(_interface instanceof InteropInterface))
            return false;
        StorageContext context = ((InteropInterface<StorageContext>) _interface).getInterface();
        byte[] key = engine.getCurrentContext().evaluationStack.pop().getByteArray();
        byte[] value = engine.getCurrentContext().evaluationStack.pop().getByteArray();
        return putEx(context, key, value, StorageFlags.None);
    }

    protected boolean storagePutEx(ExecutionEngine engine) {
        StackItem _interface = engine.getCurrentContext().evaluationStack.pop();
        if (!(_interface instanceof InteropInterface))
            return false;
        StorageContext context = ((InteropInterface<StorageContext>) _interface).getInterface();
        byte[] key = engine.getCurrentContext().evaluationStack.pop().getByteArray();
        byte[] value = engine.getCurrentContext().evaluationStack.pop().getByteArray();
        StorageFlags flags = StorageFlags.parse(engine.getCurrentContext().evaluationStack.pop
                ().getBigInteger().byteValue());
        return putEx(context, key, value, flags);
    }

    protected boolean storageDelete(ExecutionEngine engine) {
        if (trigger != TriggerType.Application && trigger != TriggerType.ApplicationR) {
            return false;
        }
        StackItem _interface = engine.getCurrentContext().evaluationStack.pop();
        if (_interface instanceof InteropInterface) {
            StorageContext context = ((InteropInterface<StorageContext>) _interface).getInterface();
            if (context.isReadOnly) {
                return false;
            }
            if (!checkStorageContext(context)) {
                return false;
            }
            StorageKey key = new StorageKey();
            key.scriptHash = context.scriptHash;
            key.key = engine.getCurrentContext().evaluationStack.pop().getByteArray();
            StorageItem tempItem = snapshot.getStorages().tryGet(key);
            if (tempItem != null ? tempItem.isConstant == true : false) {
                return false;
            }
            snapshot.getStorages().delete(key);
            return true;
        }
        return false;
    }


}