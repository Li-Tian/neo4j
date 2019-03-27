package neo.smartcontract;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import neo.UInt160;
import neo.UInt256;
import neo.csharp.Uint;
import neo.csharp.common.IDisposable;
import neo.csharp.io.BinaryReader;
import neo.ledger.Blockchain;
import neo.ledger.ContractState;
import neo.ledger.StorageFlags;
import neo.ledger.StorageItem;
import neo.ledger.StorageKey;
import neo.network.p2p.payloads.Block;
import neo.network.p2p.payloads.BlockBase;
import neo.network.p2p.payloads.Header;
import neo.network.p2p.payloads.Transaction;
import neo.persistence.Snapshot;
import neo.vm.ExecutionEngine;
import neo.vm.IInteropService;
import neo.vm.StackItem;
import neo.vm.Types.Boolean;
import neo.vm.Types.ByteArray;
import neo.vm.Types.Integer;
import neo.vm.Types.InteropInterface;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: StandardService
 * @Package neo.smartcontract
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 14:10 2019/3/12
 */
public class StandardService implements IInteropService, IDisposable {
    @Override
    public boolean invoke(byte[] bytes, ExecutionEngine executionEngine) {
        return false;
    }

    public static EventHandler<NotifyEventArgs> notify = new EventHandler<>();
    public static EventHandler<LogEventArgs> log = new EventHandler<>();

    protected TriggerType trigger;
    protected Snapshot snapshot;
    protected final List<IDisposable> disposables = new ArrayList<IDisposable>();
    protected Map<UInt160, UInt160> contractsCreated = new HashMap<UInt160, UInt160>();
    private final List<NotifyEventArgs> notifications = new ArrayList<NotifyEventArgs>();
    private final Map<Uint, Func<ExecutionEngine, Boolean>> methods = new HashMap<Uint,
            Func<ExecutionEngine, Boolean>>();
    private final Map<Uint, Long> prices = new HashMap<Uint, Long>();

    public List<NotifyEventArgs> getNotifications() {
        return notifications;
    }

    public StandardService(TriggerType trigger, Snapshot snapshot) {
        this.trigger = trigger;
        this.snapshot = snapshot;
        Register("System.ExecutionEngine.GetScriptContainer", ExecutionEngine_GetScriptContainer, 1);
        Register("System.ExecutionEngine.GetExecutingScriptHash", ExecutionEngine_GetExecutingScriptHash, 1);
        Register("System.ExecutionEngine.GetCallingScriptHash", ExecutionEngine_GetCallingScriptHash, 1);
        Register("System.ExecutionEngine.GetEntryScriptHash", ExecutionEngine_GetEntryScriptHash, 1);
        Register("System.Runtime.Platform", Runtime_Platform, 1);
        Register("System.Runtime.GetTrigger", Runtime_GetTrigger, 1);
        Register("System.Runtime.CheckWitness", Runtime_CheckWitness, 200);
        Register("System.Runtime.Notify", Runtime_Notify, 1);
        Register("System.Runtime.Log", Runtime_Log, 1);
        Register("System.Runtime.GetTime", Runtime_GetTime, 1);
        Register("System.Runtime.Serialize", Runtime_Serialize, 1);
        Register("System.Runtime.Deserialize", Runtime_Deserialize, 1);
        Register("System.Blockchain.GetHeight", Blockchain_GetHeight, 1);
        Register("System.Blockchain.GetHeader", Blockchain_GetHeader, 100);
        Register("System.Blockchain.GetBlock", Blockchain_GetBlock, 200);
        Register("System.Blockchain.GetTransaction", Blockchain_GetTransaction, 200);
        Register("System.Blockchain.GetTransactionHeight", Blockchain_GetTransactionHeight, 100);
        Register("System.Blockchain.GetContract", Blockchain_GetContract, 100);
        Register("System.Header.GetIndex", Header_GetIndex, 1);
        Register("System.Header.GetHash", Header_GetHash, 1);
        Register("System.Header.GetPrevHash", Header_GetPrevHash, 1);
        Register("System.Header.GetTimestamp", Header_GetTimestamp, 1);
        Register("System.Block.GetTransactionCount", Block_GetTransactionCount, 1);
        Register("System.Block.GetTransactions", Block_GetTransactions, 1);
        Register("System.Block.GetTransaction", Block_GetTransaction, 1);
        Register("System.Transaction.GetHash", Transaction_GetHash, 1);
        Register("System.Contract.Destroy", Contract_Destroy, 1);
        Register("System.Contract.GetStorageContext", Contract_GetStorageContext, 1);
        Register("System.Storage.GetContext", Storage_GetContext, 1);
        Register("System.Storage.GetReadOnlyContext", Storage_GetReadOnlyContext, 1);
        Register("System.Storage.Get", Storage_Get, 100);
        Register("System.Storage.Put", Storage_Put);
        Register("System.Storage.PutEx", Storage_PutEx);
        Register("System.Storage.Delete", Storage_Delete, 100);
        Register("System.StorageContext.AsReadOnly", StorageContext_AsReadOnly, 1);
    }

    boolean checkStorageContext(StorageContext context) {
        ContractState contract = snapshot.getContracts().TryGet(context.ScriptHash);
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
        prices.TryGetValue(hash, out long price);
        return price;
    }

    bool IInteropService.

    Invoke(byte[] method, ExecutionEngine engine) {
        uint hash = method.Length == 4
                ? BitConverter.ToUInt32(method, 0)
                : Encoding.ASCII.GetString(method).ToInteropMethodHash();
        if (!methods.TryGetValue(hash, out Func < ExecutionEngine, bool > func)) return false;
        return func(engine);
    }

    protected void Register(String method, Func<ExecutionEngine, bool> handler) {
        methods.Add(method.ToInteropMethodHash(), handler);
    }

    protected void Register(String method, Func<ExecutionEngine, bool> handler, long price) {
        Register(method, handler);
        prices.Add(method.ToInteropMethodHash(), price);
    }

    protected boolean ExecutionEngine_GetScriptContainer(ExecutionEngine engine) {
        engine.getCurrentContext().evaluationStack.push(StackItem.FromInterface(engine
                .ScriptContainer));
        return true;
    }

    protected boolean ExecutionEngine_GetExecutingScriptHash(ExecutionEngine engine) {
        engine.getCurrentContext().evaluationStack.push(engine.getCurrentContext().ScriptHash);
        return true;
    }

    protected boolean ExecutionEngine_GetCallingScriptHash(ExecutionEngine engine) {
        engine.getCurrentContext().evaluationStack.push(engine.CallingContext.ScriptHash);
        return true;
    }

    protected boolean ExecutionEngine_GetEntryScriptHash(ExecutionEngine engine) {
        engine.getCurrentContext().evaluationStack.push(engine.EntryContext.ScriptHash);
        return true;
    }

    protected boolean Runtime_Platform(ExecutionEngine engine) {
        engine.getCurrentContext().evaluationStack.push(Encoding.ASCII.GetBytes("NEO"));
        return true;
    }

    protected boolean Runtime_GetTrigger(ExecutionEngine engine) {
        engine.getCurrentContext().evaluationStack.push((int) Trigger);
        return true;
    }

    protected boolean CheckWitness(ExecutionEngine engine, UInt160 hash) {
        IVerifiable container = (IVerifiable) engine.ScriptContainer;
        UInt160[] _hashes_for_verifying = container.GetScriptHashesForVerifying(Snapshot);
        return _hashes_for_verifying.Contains(hash);
    }

    protected boolean CheckWitness(ExecutionEngine engine, ECPoint pubkey) {
        return CheckWitness(engine, Contract.CreateSignatureRedeemScript(pubkey).ToScriptHash());
    }

    protected boolean Runtime_CheckWitness(ExecutionEngine engine) {
        byte[] hashOrPubkey = engine.getCurrentContext().evaluationStack.pop().GetByteArray();
        bool result;
        if (hashOrPubkey.length == 20)
            result = checkWitness(engine, new UInt160(hashOrPubkey));
        else if (hashOrPubkey.length == 33)
            result = CheckWitness(engine, ECPoint.DecodePoint(hashOrPubkey, ECCurve.Secp256r1));
        else
            return false;
        engine.getCurrentContext().evaluationStack.Push(result);
        return true;
    }

    protected boolean Runtime_Notify(ExecutionEngine engine) {
        StackItem state = engine.getCurrentContext().evaluationStack.pop();
        NotifyEventArgs notification = new NotifyEventArgs(engine.scriptContainer, new UInt160
                (engine.getCurrentContext().getScriptHash()), state);
        Notify ?.Invoke(this, notification);
        notifications.add(notification);
        return true;
    }

    protected boolean Runtime_Log(ExecutionEngine engine) {
        String message = Encoding.UTF8.GetString(engine.getCurrentContext().evaluationStack.pop()
                .GetByteArray());
        Log ?.
        Invoke(this, new LogEventArgs(engine.ScriptContainer, new UInt160(engine.getCurrentContext().ScriptHash), message));
        return true;
    }

    protected boolean Runtime_GetTime(ExecutionEngine engine) {
        if (snapshot.getPersistingBlock() == null) {
            Header header = snapshot.getHeader(snapshot.getCurrentBlockHash());
            engine.getCurrentContext().evaluationStack.push(header.Timestamp + Blockchain
                    .SecondsPerBlock);
        } else {
            engine.getCurrentContext().evaluationStack.push(snapshot.PersistingBlock.Timestamp);
        }
        return true;
    }

    private void SerializeStackItem(StackItem item, BinaryWriter writer) {
        List<StackItem> serialized = new List<StackItem>();
        Stack<StackItem> unserialized = new Stack<StackItem>();
        unserialized.Push(item);
        while (unserialized.Count > 0) {
            item = unserialized.pop();
            switch (item) {
                case ByteArray _:
                writer.Write((byte) StackItemType.ByteArray);
                    writer.WriteVarBytes(item.GetByteArray());
                    break;
                case VMBoolean _:
                writer.Write((byte) StackItemType.Boolean);
                    writer.Write(item.GetBoolean());
                    break;
                case Integer _:
                writer.Write((byte) StackItemType.Integer);
                    writer.WriteVarBytes(item.GetByteArray());
                    break;
                case InteropInterface _:
                throw new NotSupportedException();
                case VMArray array:
                if (serialized.Any(p = > ReferenceEquals(p, array)))
                    throw new NotSupportedException();
                    serialized.Add(array);
                    if (array is Struct)
                    writer.Write((byte) StackItemType.Struct);
                        else
                    writer.Write((byte) StackItemType.Array);
                    writer.WriteVarInt(array.Count);
                    for (int i = array.Count - 1; i >= 0; i--)
                        unserialized.Push(array[i]);
                    break;
                case Map map:
                if (serialized.Any(p = > ReferenceEquals(p, map)))
                    throw new NotSupportedException();
                    serialized.Add(map);
                    writer.Write((byte) StackItemType.Map);
                    writer.WriteVarInt(map.Count);
                    foreach(var pair in map.Reverse())
                {
                    unserialized.Push(pair.Value);
                    unserialized.Push(pair.Key);
                }
                break;
            }
        }
    }

    protected boolean runtimeSerialize(ExecutionEngine engine) {
        using(MemoryStream ms = new MemoryStream())
        using(BinaryWriter writer = new BinaryWriter(ms))
        {
            try {
                SerializeStackItem(engine.getCurrentContext().evaluationStack.pop(), writer);
            } catch (NotSupportedException) {
                return false;
            }
            writer.Flush();
            if (ms.Length > ApplicationEngine.MaxItemSize)
                return false;
            engine.getCurrentContext().evaluationStack.Push(ms.ToArray());
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
                    deserialized.push(new ContainerPlaceholder
                    {
                        Type = type,
                                ElementCount = count
                    });
                    undeserialized += count;
                }
                break;
                case Map: {
                    int count = reader.readVarInt(ApplicationEngine.MaxArraySize.ulongValue
                            ()).intValue();
                    deserialized.push(new ContainerPlaceholder
                    {
                        Type = type,
                                ElementCount = count
                    });
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
            if (item is ContainerPlaceholder placeholder)
            {
                switch (placeholder.Type) {
                    case StackItemType.Array:
                        VMArray array = new VMArray();
                        for (int i = 0; i < placeholder.ElementCount; i++)
                            array.Add(stack_temp.pop());
                        item = array;
                        break;
                    case Struct:
                        Struct @struct =new Struct();
                        for (int i = 0; i < placeholder.ElementCount; i++)
                            @struct.Add(stack_temp.pop()) ;
                        item = @struct ;
                        break;
                    case Map:
                        Map map = new Map();
                        for (int i = 0; i < placeholder.ElementCount; i++) {
                            StackItem key = stack_temp.pop();
                            StackItem value = stack_temp.pop();
                            map.Add(key, value);
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
        using(MemoryStream ms = new MemoryStream(data, false))
        using(BinaryReader reader = new BinaryReader(ms))
        {
            StackItem item;
            try {
                item = deserializeStackItem(reader);
            } catch (FormatException) {
                return false;
            } catch (IOException) {
                return false;
            }
            engine.getCurrentContext().evaluationStack.push(item);
        }
        return true;
    }

    protected boolean blockchainGetHeight(ExecutionEngine engine) {
        engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(snapshot.getHeight
                ()));
        return true;
    }

    protected boolean blockchainGetHeader(ExecutionEngine engine) {
        byte[] data = engine.getCurrentContext().evaluationStack.pop().getByteArray();
        UInt256 hash;
        if (data.length <= 5)
            hash = Blockchain.singleton().getBlockHash((uint) new BigInteger(data));
        else if (data.length == 32)
            hash = new UInt256(data);
        else
            return false;
        if (hash == null) {
            engine.getCurrentContext().evaluationStack.push(new byte[0]);
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
            engine.getCurrentContext().evaluationStack.push(new byte[0]);
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
            int?height = (int?)Snapshot.Transactions.TryGet(new UInt256(hash)) ?.BlockIndex;
        engine.getCurrentContext().evaluationStack.Push(height ? ? -1);
        return true;
    }

    protected boolean blockchainGetContract(ExecutionEngine engine) {
        UInt160 hash = new UInt160(engine.getCurrentContext().evaluationStack.pop().getByteArray());
        ContractState contract = snapshot.getContracts().TryGet(hash);
        if (contract == null)
            engine.getCurrentContext().evaluationStack.push(new byte[0]);
        else
            engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(contract));
        return true;
    }

    protected boolean headerGetIndex(ExecutionEngine engine) {
        StackItem _interface=engine.getCurrentContext().evaluationStack.pop();
        if (_interface instanceof InteropInterface){
            BlockBase header = ((InteropInterface<BlockBase>)_interface).getInterface();
            if (header == null) return false;
            engine.getCurrentContext().evaluationStack.push(header.index);
            return true;
        }
        return false;
    }

    protected boolean headerGetHash(ExecutionEngine engine) {
        StackItem _interface=engine.getCurrentContext().evaluationStack.pop();
        if (_interface instanceof InteropInterface){
            BlockBase header = ((InteropInterface<BlockBase>)_interface).getInterface();
            if (header == null) return false;
            engine.getCurrentContext().evaluationStack.push(StackItem.getStackItem(header.hash()
                    .toArray()));
            return true;
        }
        return false;
    }

    protected boolean headerGetPrevHash(ExecutionEngine engine) {
        StackItem _interface=engine.getCurrentContext().evaluationStack.pop();
        if (_interface instanceof InteropInterface){
            BlockBase header = ((InteropInterface<BlockBase>)_interface).getInterface();
            if (header == null) return false;
            engine.getCurrentContext().evaluationStack.push(header.prevHash.toArray());
            return true;
        }
        return false;
    }

    protected boolean headerGetTimestamp(ExecutionEngine engine) {
        StackItem _interface=engine.getCurrentContext().evaluationStack.pop();
        if (_interface instanceof InteropInterface){
            BlockBase header = ((InteropInterface<BlockBase>)_interface).getInterface();
            if (header == null) return false;
            engine.getCurrentContext().evaluationStack.push(header.timestamp);
            return true;
        }
        return false;
    }

    protected boolean blockGetTransactionCount(ExecutionEngine engine) {
        StackItem _interface=engine.getCurrentContext().evaluationStack.pop();
        if (_interface instanceof InteropInterface){
            Block block = ((InteropInterface<Block>)_interface).getInterface();
            if (block == null) return false;
            engine.getCurrentContext().evaluationStack.push(block.transactions.length);
            return true;
        }
        return false;
    }

    protected boolean blockGetTransactions(ExecutionEngine engine) {
        StackItem _interface=engine.getCurrentContext().evaluationStack.pop();
        if (_interface instanceof InteropInterface){
            Block block = ((InteropInterface<Block>)_interface).getInterface();
            if (block == null) return false;
            if (block.transactions.length > ApplicationEngine.MaxArraySize.intValue())
                return false;
            engine.getCurrentContext().evaluationStack.push(block.transactions.Select(p = >
                    StackItem.fromInterface(p)).ToArray())
            ;
            return true;
        }
        return false;
    }

    protected boolean blockGetTransaction(ExecutionEngine engine) {
        StackItem _interface=engine.getCurrentContext().evaluationStack.pop();
        if (_interface instanceof InteropInterface){
            Block block = ((InteropInterface<Block>)_interface).getInterface();
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
            engine.getCurrentContext().evaluationStack.push(tx.Hash.ToArray());
            return true;
        }
        return false;
    }

    protected boolean storageGetContext(ExecutionEngine engine) {
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(new StorageContext
        {
            ScriptHash = new UInt160(engine.CurrentContext.ScriptHash),
                    IsReadOnly = false
        }));
        return true;
    }

    protected boolean storageGetReadOnlyContext(ExecutionEngine engine) {
        engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(new StorageContext
        {
            ScriptHash = new UInt160(engine.CurrentContext.ScriptHash),
                    IsReadOnly = true
        }));
        return true;
    }

    protected boolean storageGet(ExecutionEngine engine) {
        if (engine.getCurrentContext().evaluationStack.pop() is InteropInterface _interface)
        {
            StorageContext context = _interface.GetInterface < StorageContext > ();
            if (!checkStorageContext(context)) return false;
            byte[] key = engine.getCurrentContext().evaluationStack.pop().getByteArray();
            StorageItem item = snapshot.getStorages().TryGet(new StorageKey
            {
                ScriptHash = context.ScriptHash,
                        Key = key
            });
            engine.getCurrentContext().evaluationStack.push(item ?.Value ??new byte[0]);
            return true;
        }
        return false;
    }

    protected boolean storageContextAsReadOnly(ExecutionEngine engine) {
        if (engine.getCurrentContext().evaluationStack.pop() is InteropInterface _interface)
        {
            StorageContext context = _interface.GetInterface < StorageContext > ();
            if (!context.isReadOnly)
                context = new StorageContext
            {
                ScriptHash = context.ScriptHash,
                        IsReadOnly = true
            } ;
            engine.getCurrentContext().evaluationStack.push(StackItem.fromInterface(context));
            return true;
        }
        return false;
    }

    protected boolean contractGetStorageContext(ExecutionEngine engine) {
        if (engine.getCurrentContext().evaluationStack.pop() is InteropInterface _interface)
        {
            ContractState contract = _interface.GetInterface < ContractState > ();
            if (!contractsCreated.TryGetValue(contract.ScriptHash, out UInt160 created))
                return false;
            if (!created.Equals(new UInt160(engine.getCurrentContext().getScriptHash())))
                return false;
            engine.getCurrentContext().evaluationStack.push(StackItem.FromInterface(new
                    StorageContext
            {
                ScriptHash = contract.ScriptHash,
                        IsReadOnly = false
            }));
            return true;
        }
        return false;
    }

    protected boolean contractDestroy(ExecutionEngine engine) {
        if (trigger != TriggerType.Application) return false;
        UInt160 hash = new UInt160(engine.getCurrentContext().scriptHash);
        ContractState contract = snapshot.getContracts().TryGet(hash);
        if (contract == null) return true;
        snapshot.getContracts().delete(hash);
        if (contract.hasStorage())
            for (var pair in snapshot.getStorages().Find(hash.ToArray()))
        snapshot.getStorages().delete(pair.Key);
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
        StorageItem item = snapshot.getStorages().getAndChange(skey, () = > new StorageItem());
        if (item.isConstant) return false;
        item.value = value;
        item.isConstant = flags.hashCode(StorageFlags.Constant);
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
            if (snapshot.getStorages().TryGet(key) ?.isConstant == true){
                return false;
            }
            snapshot.getStorages().delete(key);
            return true;
        }
        return false;
    }


}