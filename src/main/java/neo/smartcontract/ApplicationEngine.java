package neo.smartcontract;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import neo.Fixed8;
import neo.UInt160;
import neo.UInt256;
import neo.cryptography.Crypto;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.csharp.Ushort;
import neo.ledger.Blockchain;
import neo.ledger.ContractPropertyState;
import neo.log.notr.TR;
import neo.network.p2p.payloads.Block;
import neo.network.p2p.payloads.Transaction;
import neo.network.p2p.payloads.Witness;
import neo.persistence.Snapshot;
import neo.vm.ExecutionContext;
import neo.vm.ExecutionEngine;
import neo.vm.ICollection;
import neo.vm.IScriptContainer;
import neo.vm.OpCode;
import neo.vm.RandomAccessStack;
import neo.vm.StackItem;
import neo.vm.Types.Array;
import neo.vm.Types.Map;
import neo.vm.VMState;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: ApplicationEngine
 * @Package neo.smartcontract
 * @Description: 自定义ExecutionEngine子类，扩展类
 * @date Created in 18:33 2019/3/11
 */
public class ApplicationEngine extends ExecutionEngine {

    /// <summary>
    /// Max value for SHL and SHR
    /// </summary>
    public static final int Max_SHL_SHR = Ushort.MAX_VALUE;
    /// <summary>
    /// Min value for SHL and SHR
    /// </summary>
    public static final int Min_SHL_SHR = -Max_SHL_SHR;
    /// <summary>
    /// Set the max size allowed size for BigInteger
    /// </summary>
    public static final int MaxSizeForBigInteger = 32;
    /// <summary>
    /// Set the max Stack Size
    /// </summary>
    public static final Uint MaxStackSize = new Uint(2 * 1024);
    /// <summary>
    /// Set Max Item Size
    /// </summary>
    public static final Uint MaxItemSize = new Uint(1024 * 1024);
    /// <summary>
    /// Set Max Invocation Stack Size
    /// </summary>
    public static final Uint MaxInvocationStackSize = new Uint(1024);
    /// <summary>
    /// Set Max Array Size
    /// </summary>
    public static final Uint MaxArraySize = new Uint(1024);

    private final long ratio = 100000;
    private final long gas_free = 10 * 100000000;
    private long gas_amount;
    private long gas_consumed = 0;
    private boolean testMode;
    private Snapshot snapshot;

    private int stackitem_count = 0;
    private boolean is_stackitem_count_strict = true;

    public Fixed8 getGasConsumed() {
        return new Fixed8(gas_consumed);
    }

    public NeoService getService() {
        return (NeoService) super.service;
    }

    public ApplicationEngine(TriggerType trigger, IScriptContainer container, Snapshot snapshot,
                             Fixed8 gas) {
        super(container, Crypto.Default, snapshot, new NeoService(trigger, snapshot));
        boolean testMode = false;
        this.gas_amount = gas_free + gas.getData();
        this.testMode = testMode;
        this.snapshot = snapshot;
    }

    public ApplicationEngine(TriggerType trigger, IScriptContainer container, Snapshot snapshot,
                             Fixed8 gas, boolean testMode) {
        super(container, Crypto.Default, snapshot, new NeoService(trigger, snapshot));
        this.gas_amount = gas_free + gas.getData();
        this.testMode = testMode;
        this.snapshot = snapshot;
    }

    private boolean checkArraySize(OpCode nextInstruction) {
        int size;
        switch (nextInstruction) {
            case PACK:
            case NEWARRAY:
            case NEWSTRUCT: {
                if (getCurrentContext().getEvaluationStack().getCount() == 0) {
                    return false;
                }
                size = getCurrentContext().getEvaluationStack().peek().getBigInteger().intValue();
            }
            break;
            case SETITEM: {
                if (getCurrentContext().getEvaluationStack().getCount() < 3) {
                    return false;
                }
                StackItem itemtemp = getCurrentContext().getEvaluationStack().peek(2);
                if (!(itemtemp instanceof Map)) {
                    return true;
                }
                StackItem key = getCurrentContext().getEvaluationStack().peek(1);
                if (key instanceof ICollection) {
                    return false;
                }
                if (((Map) itemtemp).containsKey(key)) return true;
                size = ((Map) itemtemp).getCount() + 1;
            }
            break;
            case APPEND: {
                if (getCurrentContext().getEvaluationStack().getCount() < 2) {
                    return false;
                }
                StackItem itemtemp = getCurrentContext().getEvaluationStack().peek(1);
                if (!(itemtemp instanceof Array)) {
                    return false;
                }
                size = ((Array) itemtemp).getCount() + 1;
            }
            break;
            default:
                return true;
        }
        return new Uint(size).compareTo(MaxArraySize) <= 0;
    }

    private boolean checkInvocationStack(OpCode nextInstruction) {
        switch (nextInstruction) {
            case CALL:
            case APPCALL:
            case CALL_I:
            case CALL_E:
            case CALL_ED:
                if (invocationStack.getCount() >= MaxInvocationStackSize.intValue()) {
                    return false;
                }
                return true;
            default:
                return true;
        }
    }

    private boolean checkItemSize(OpCode nextInstruction) {
        switch (nextInstruction) {
            case PUSHDATA4: {
                if (getCurrentContext().getInstructionPointer() + 4 >= getCurrentContext().script.length)
                    return false;
                byte[] tempbytearry = new byte[4];
                tempbytearry[0] = getCurrentContext().script[getCurrentContext()
                        .getInstructionPointer() + 1];
                tempbytearry[1] = getCurrentContext().script[getCurrentContext()
                        .getInstructionPointer() + 2];
                tempbytearry[2] = getCurrentContext().script[getCurrentContext()
                        .getInstructionPointer() + 3];
                tempbytearry[3] = getCurrentContext().script[getCurrentContext()
                        .getInstructionPointer() + 4];
                int length = BitConverter.toUint(tempbytearry).intValue();
                if (new Uint(length).compareTo(MaxItemSize) > 0) {
                    return false;
                }
                return true;
            }
            case CAT: {
                if (getCurrentContext().getEvaluationStack().getCount() < 2) return false;
                int length = getCurrentContext().getEvaluationStack().peek(0).getByteArray()
                        .length + getCurrentContext().getEvaluationStack().peek(1).getByteArray().length;
                if (new Uint(length).compareTo(MaxItemSize) > 0) return false;
                return true;
            }
            default:
                return true;
        }
    }

    /// <summary>
    /// Check if the BigInteger is allowed for numeric operations
    /// </summary>
    /// <param name="value">Value</param>
    /// <returns>Return True if are allowed, otherwise False</returns>
    private boolean checkBigInteger(BigInteger value) {
        return value.toByteArray().length <= MaxSizeForBigInteger;
    }

    /// <summary>
    /// Check if the BigInteger is allowed for numeric operations
    /// </summary>
    private boolean checkBigIntegers(OpCode nextInstruction) {
        switch (nextInstruction) {
            case SHL: {
                BigInteger ishift = getCurrentContext().getEvaluationStack().peek(0)
                        .getBigInteger();

                if (ishift.compareTo(new BigInteger(String.valueOf(Max_SHL_SHR))) > 0 || ishift
                        .compareTo(new BigInteger(String.valueOf(Min_SHL_SHR))) < 0) {
                    return false;
                }

                BigInteger x = getCurrentContext().getEvaluationStack().peek(1).getBigInteger();

                if (!checkBigInteger(x.shiftLeft(ishift.intValue()))) {
                    return false;
                }

                break;
            }
            case SHR: {
                BigInteger ishift = getCurrentContext().getEvaluationStack().peek(0)
                        .getBigInteger();

                if (ishift.compareTo(new BigInteger(String.valueOf(Max_SHL_SHR))) > 0 || ishift
                        .compareTo(new BigInteger(String.valueOf(Min_SHL_SHR))) < 0) {
                    return false;
                }
                BigInteger x = getCurrentContext().getEvaluationStack().peek(1).getBigInteger();

                if (!checkBigInteger(x.shiftRight(ishift.intValue())))
                    return false;

                break;
            }
            case INC: {
                BigInteger x = getCurrentContext().getEvaluationStack().peek().getBigInteger();

                if (!checkBigInteger(x) || !checkBigInteger(x.add(new BigInteger("1"))))
                    return false;

                break;
            }
            case DEC: {
                BigInteger x = getCurrentContext().getEvaluationStack().peek().getBigInteger();

                if (!checkBigInteger(x) || (x.signum() <= 0 && !checkBigInteger(x.subtract(new
                        BigInteger("1")))))
                    return false;

                break;
            }
            case ADD: {
                BigInteger x2 = getCurrentContext().getEvaluationStack().peek().getBigInteger();
                BigInteger x1 = getCurrentContext().getEvaluationStack().peek(1).getBigInteger();

                if (!checkBigInteger(x2) || !checkBigInteger(x1) || !checkBigInteger(x1.add(x2)))
                    return false;

                break;
            }
            case SUB: {
                BigInteger x2 = getCurrentContext().getEvaluationStack().peek().getBigInteger();
                BigInteger x1 = getCurrentContext().getEvaluationStack().peek(1).getBigInteger();

                if (!checkBigInteger(x2) || !checkBigInteger(x1) || !checkBigInteger(x1.subtract
                        (x2)))
                    return false;

                break;
            }
            case MUL: {
                BigInteger x2 = getCurrentContext().getEvaluationStack().peek().getBigInteger();
                BigInteger x1 = getCurrentContext().getEvaluationStack().peek(1).getBigInteger();

                int lx1 = x1.toByteArray().length;

                if (lx1 > MaxSizeForBigInteger)
                    return false;

                int lx2 = x2.toByteArray().length;

                if ((lx1 + lx2) > MaxSizeForBigInteger)
                    return false;

                break;
            }
            case DIV: {
                BigInteger x2 = getCurrentContext().getEvaluationStack().peek().getBigInteger();
                BigInteger x1 = getCurrentContext().getEvaluationStack().peek(1).getBigInteger();

                if (!checkBigInteger(x2) || !checkBigInteger(x1))
                    return false;

                break;
            }
            case MOD: {
                BigInteger x2 = getCurrentContext().getEvaluationStack().peek().getBigInteger();
                BigInteger x1 = getCurrentContext().getEvaluationStack().peek(1).getBigInteger();

                if (!checkBigInteger(x2) || !checkBigInteger(x1))
                    return false;

                break;
            }
        }

        return true;
    }

    private boolean checkStackSize(OpCode nextInstruction) {
        if (nextInstruction.getCode() <= OpCode.PUSH16.getCode())
            stackitem_count += 1;
        else
            switch (nextInstruction) {
                case JMPIF:
                case JMPIFNOT:
                case DROP:
                case NIP:
                case EQUAL:
                case BOOLAND:
                case BOOLOR:
                case CHECKMULTISIG:
                case REVERSE:
                case HASKEY:
                case THROWIFNOT:
                    stackitem_count -= 1;
                    is_stackitem_count_strict = false;
                    break;
                case XSWAP:
                case ROLL:
                case CAT:
                case LEFT:
                case RIGHT:
                case AND:
                case OR:
                case XOR:
                case ADD:
                case SUB:
                case MUL:
                case DIV:
                case MOD:
                case SHL:
                case SHR:
                case NUMEQUAL:
                case NUMNOTEQUAL:
                case LT:
                case GT:
                case LTE:
                case GTE:
                case MIN:
                case MAX:
                case CHECKSIG:
                case CALL_ED:
                case CALL_EDT:
                    stackitem_count -= 1;
                    break;
                case RET:
                case APPCALL:
                case TAILCALL:
                case NOT:
                case ARRAYSIZE:
                    is_stackitem_count_strict = false;
                    break;
                case SYSCALL:
                case PICKITEM:
                case SETITEM:
                case APPEND:
                case VALUES:
                    stackitem_count = Integer.MAX_VALUE;
                    is_stackitem_count_strict = false;
                    break;
                case DUPFROMALTSTACK:
                case DEPTH:
                case DUP:
                case OVER:
                case TUCK:
                case NEWMAP:
                    stackitem_count += 1;
                    break;
                case XDROP:
                case REMOVE:
                    stackitem_count -= 2;
                    is_stackitem_count_strict = false;
                    break;
                case SUBSTR:
                case WITHIN:
                case VERIFY:
                    stackitem_count -= 2;
                    break;
                case UNPACK:
                    stackitem_count += getCurrentContext().getEvaluationStack().peek()
                            .getBigInteger().intValue();
                    is_stackitem_count_strict = false;
                    break;
                case NEWARRAY:
                case NEWSTRUCT:
                    stackitem_count += ((Array) getCurrentContext().getEvaluationStack().peek())
                            .getCount();
                    break;
                case KEYS:
                    stackitem_count += ((Array) getCurrentContext().getEvaluationStack().peek())
                            .getCount();
                    is_stackitem_count_strict = false;
                    break;
            }
        if (new Uint(stackitem_count).compareTo(MaxStackSize) <= 0) return true;
        if (is_stackitem_count_strict) return false;

        List<ExecutionContext> tempInvocationStack = new ArrayList<>();
        Iterator<ExecutionContext> tempInvocationStackiterator = invocationStack.getEnumerator();

        while (tempInvocationStackiterator.hasNext()) {
            tempInvocationStack.add(tempInvocationStackiterator.next());
        }
        //LINQ START
/*        stackitem_count = getItemCount(tempInvocationStack.stream().map(p -> p
                .getEvaluationStack().concat(p.getAltStack())));*/

        List<StackItem> list=new ArrayList<>();
        for (ExecutionContext ec:tempInvocationStack){
            Iterator<StackItem> itemIterator1=ec.getEvaluationStack().getEnumerator();
            while (itemIterator1.hasNext()){
                list.add(itemIterator1.next());
            }
            Iterator<StackItem> itemIterator2=ec.getAltStack().getEnumerator();
            while (itemIterator2.hasNext()){
                list.add(itemIterator2.next());
            }
        }
/*        tempInvocationStack.stream().map(p->{
            List<StackItem> list2=new ArrayList<>();
            Iterator<StackItem> itemIterator1=p.getEvaluationStack().getEnumerator();
            while (itemIterator1.hasNext()){
                list2.add(itemIterator1.next());
            }
            Iterator<StackItem> itemIterator2=p.getAltStack().getEnumerator();
            while (itemIterator2.hasNext()){
                list2.add(itemIterator2.next());
            }
            return list2;
        }).flatMap(p->p.stream()).collect(Collectors.toList());*/
        stackitem_count = getItemCount(list);
        //LINQ END
        if (new Uint(stackitem_count).compareTo(MaxStackSize) > 0) return false;
        is_stackitem_count_strict = true;
        return true;
    }

    private boolean checkDynamicInvoke(OpCode nextInstruction) {
        switch (nextInstruction) {
            case APPCALL:
            case TAILCALL:
                for (int i = getCurrentContext().getInstructionPointer() + 1; i < getCurrentContext()
                        .getInstructionPointer() + 21; i++) {
                    if (getCurrentContext().script[i] != 0) return true;
                }
                // if we get this far it is a dynamic call
                // now look at the current executing script
                // to determine if it can do dynamic calls
                return snapshot.getContracts().get(new UInt160(getCurrentContext().getScriptHash()))
                        .hasDynamicInvoke();
            case CALL_ED:
            case CALL_EDT:
                return snapshot.getContracts().get(new UInt160(getCurrentContext().getScriptHash()))
                        .hasDynamicInvoke();
            default:
                return true;
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        getService().dispose();
    }

    /**
     * @Author:doubi.liu
     * @description:替换原有ExecuteEngine的execute方法，C#版本特殊语法，可由变量引用指定调用的父类还是子类方法
     * @date:2019/3/12
     */
    public boolean execute2() {
        try {
            while (true) {
                OpCode nextOpcode = getCurrentContext().getInstructionPointer() >=
                        getCurrentContext().script.length ? OpCode.RET : getCurrentContext()
                        .getNextInstruction();
                if (!preStepInto(nextOpcode)) {
                    int temp = state.getState();
                    temp |= VMState.FAULT.getState();
                    state = VMState.fromByte((byte) temp);
                    return false;
                }
                stepInto();
                if (state.hasFlag(VMState.HALT) || state.hasFlag(VMState.FAULT))
                    break;
                if (!postStepInto(nextOpcode)) {
                    int temp = state.getState();
                    temp |= VMState.FAULT.getState();
                    state = VMState.fromByte((byte) temp);
                    return false;
                }
            }
        } catch (Exception e) {
            int temp = state.getState();
            temp |= VMState.FAULT.getState();
            state = VMState.fromByte((byte) temp);
            return false;
        }
        return !state.hasFlag(VMState.FAULT);
    }

    private static int getItemCount(Iterable<StackItem> items) {

        List<StackItem> copy = new ArrayList<StackItem>();
        Iterator<StackItem> iterator = items.iterator();
        while (iterator.hasNext()) {
            copy.add(iterator.next());
        }
        Queue<StackItem> queue = new ConcurrentLinkedQueue<StackItem>(copy);
        List<StackItem> counted = new ArrayList<>();
        int count = 0;
        while (queue.size() > 0) {
            StackItem item = queue.poll();
            count++;
            if (item instanceof Array) {
                if (counted.stream().anyMatch(p -> p == item))
                    continue;
                counted.add(item);
                Iterator arrayIterator = ((Array) item).iterator();
                while (arrayIterator.hasNext()) {
                    queue.add((StackItem) arrayIterator.next());
                }
            } else if (item instanceof Map) {
                if (counted.stream().anyMatch(p -> p == item))
                    continue;
                counted.add(item);
                for (StackItem subitem : ((Map) item).getValues()) {
                    queue.add(subitem);
                }
                break;
            }
        }
        return count;
    }

    protected long getPrice(OpCode nextInstruction) {
        if (nextInstruction.getCode() <= OpCode.NOP.getCode()) {
            return 0;
        }
        switch (nextInstruction) {
            case APPCALL:
            case TAILCALL:
                return 10;
            case SYSCALL:
                return getPriceForSysCall();
            case SHA1:
            case SHA256:
                return 10;
            case HASH160:
            case HASH256:
                return 20;
            case CHECKSIG:
            case VERIFY:
                return 100;
            case CHECKMULTISIG: {
                if (getCurrentContext().getEvaluationStack().getCount() == 0) {
                    return 1;
                }

                StackItem item = getCurrentContext().getEvaluationStack().peek();

                int n;
                if (item instanceof Array) {
                    n = ((Array) item).getCount();
                } else {
                    n = item.getBigInteger().intValue();
                }

                if (n < 1) {
                    return 1;
                }
                return 100 * n;
            }
            default:
                return 1;
        }
    }

    protected long getPriceForSysCall() {
        if (getCurrentContext().getInstructionPointer() >= getCurrentContext().script.length - 3)
            return 1;
        byte length = getCurrentContext().script[getCurrentContext().getInstructionPointer() + 1];
        if (getCurrentContext().getInstructionPointer() > (getCurrentContext().script.length -
                length - 2))
            return 1;
        byte[] tempByteArray = new byte[2];
        tempByteArray[0] = getCurrentContext().script[getCurrentContext().getInstructionPointer() + 2];
        tempByteArray[1] = getCurrentContext().script[getCurrentContext().getInstructionPointer() + 3];
        Uint api_hash = null;
        try {
            api_hash = (length == 4)
                    ? BitConverter.toUint(tempByteArray)
                    : Helper.toInteropMethodHash(new String(getCurrentContext().script,
                    getCurrentContext().getInstructionPointer
                    ()+2,length, "ascii"));
        } catch (UnsupportedEncodingException e) {
            TR.fixMe("转码格式问题，一般不发生");
            throw new RuntimeException(e);
        }
        long price = ((NeoService)service).getPrice(api_hash);
        if (price > 0) return price;
        if (api_hash == Helper.toInteropMethodHash("Neo.Asset.Create") ||
                api_hash == Helper.toInteropMethodHash("AntShares.Asset.Create"))
            return 5000L * 100000000L / ratio;
        if (api_hash == Helper.toInteropMethodHash("Neo.Asset.Renew") ||
                api_hash == Helper.toInteropMethodHash("AntShares.Asset.Renew"))
            return getCurrentContext().getEvaluationStack().peek(1).getBigInteger()
                    .multiply(new BigInteger(String.valueOf(5000L))).multiply(new BigInteger
                            (String.valueOf(100000000L)))
                    .divide(new BigInteger(String.valueOf(ratio))).byteValue();
        if (api_hash == Helper.toInteropMethodHash("Neo.Contract.Create") ||
                api_hash == Helper.toInteropMethodHash("Neo.Contract.Migrate") ||
                api_hash == Helper.toInteropMethodHash("AntShares.Contract.Create") ||
                api_hash == Helper.toInteropMethodHash("AntShares.Contract.Migrate")) {
            long fee = 100L;

            ContractPropertyState contract_properties = new ContractPropertyState(
                    getCurrentContext().getEvaluationStack().peek(3).getBigInteger().byteValue());

            if (contract_properties.hasFlag(ContractPropertyState.HasStorage)) {
                fee += 400L;
            }
            if (contract_properties.hasFlag(ContractPropertyState.HasDynamicInvoke)) {
                fee += 500L;
            }
            return fee * 100000000L / ratio;
        }
        if (api_hash == Helper.toInteropMethodHash("System.Storage.Put") ||
                api_hash == Helper.toInteropMethodHash("System.Storage.PutEx") ||
                api_hash == Helper.toInteropMethodHash("Neo.Storage.Put") ||
                api_hash == Helper.toInteropMethodHash("AntShares.Storage.Put"))
            return ((getCurrentContext().getEvaluationStack().peek(1).getByteArray().length +
                    getCurrentContext().getEvaluationStack().peek(2).getByteArray().length - 1) /
                    1024 + 1) * 1000;
        return 1;
    }

    private boolean postStepInto(OpCode nextOpcode) {
        if (!checkStackSize(nextOpcode)) return false;
        return true;
    }

    private boolean preStepInto(OpCode nextOpcode) {
        if (getCurrentContext().getInstructionPointer() >= getCurrentContext().script.length)
            return true;
        gas_consumed = Math.addExact(gas_consumed, Math.multiplyExact(getPrice(nextOpcode), ratio));
        if (!testMode && gas_consumed > gas_amount) {
            return false;
        }
        if (!checkItemSize(nextOpcode)) {
            return false;
        }
        if (!checkArraySize(nextOpcode)) {
            return false;
        }
        if (!checkInvocationStack(nextOpcode)) {
            return false;
        }
        if (!checkBigIntegers(nextOpcode)) {
            return false;
        }
        if (!checkDynamicInvoke(nextOpcode)) {
            return false;
        }
        return true;
    }

    public static ApplicationEngine run(byte[] script, Snapshot snapshot, IScriptContainer
            container, Block persistingBlock, boolean testMode, Fixed8 extraGAS) {
        extraGAS = null;
        if (persistingBlock == null) {
            if (snapshot.getPersistingBlock() == null) {
                snapshot.setPersistingBlock(new Block());

                Block tempBlock = new Block();
                tempBlock.version = Uint.ZERO;
                tempBlock.prevHash = snapshot.getCurrentBlockHash();
                tempBlock.merkleRoot = new UInt256();
                tempBlock.timestamp = snapshot.getBlocks().get(snapshot.getCurrentBlockHash())
                        .trimmedBlock.timestamp.add(new Uint(Blockchain.SecondsPerBlock));
                tempBlock.index = snapshot.getHeight().add(new Uint(1));
                tempBlock.consensusData = Ulong.ZERO;
                tempBlock.nextConsensus = snapshot.getBlocks().get(snapshot
                        .getCurrentBlockHash()).trimmedBlock.nextConsensus;
                Witness tempwitness = new Witness();
                tempwitness.invocationScript = new byte[0];
                tempwitness.verificationScript = new byte[0];
                tempBlock.witness = tempwitness;
                tempBlock.transactions = new Transaction[0];

            } else {
                snapshot.setPersistingBlock(snapshot.getPersistingBlock());
            }
        } else {
            snapshot.setPersistingBlock(persistingBlock);
        }
        ApplicationEngine engine = new ApplicationEngine(TriggerType.Application, container, snapshot, extraGAS, testMode);
        engine.loadScript(script);
        engine.execute();
        return engine;
    }

    public static ApplicationEngine run(byte[] script, Snapshot snapshot)

    {
        IScriptContainer container = null;
        Block persistingBlock = null;
        boolean testMode = false;
        Fixed8 extraGAS = null;

        if (persistingBlock == null) {
            if (snapshot.getPersistingBlock() == null) {
                snapshot.setPersistingBlock(new Block());

                Block tempBlock = new Block();
                tempBlock.version = Uint.ZERO;
                tempBlock.prevHash = snapshot.getCurrentBlockHash();
                tempBlock.merkleRoot = new UInt256();
                tempBlock.timestamp = snapshot.getBlocks().get(snapshot.getCurrentBlockHash())
                        .trimmedBlock.timestamp.add(new Uint(Blockchain.SecondsPerBlock));
                tempBlock.index = snapshot.getHeight().add(new Uint(1));
                tempBlock.consensusData = Ulong.ZERO;
                tempBlock.nextConsensus = snapshot.getBlocks().get(snapshot
                        .getCurrentBlockHash()).trimmedBlock.nextConsensus;
                Witness tempwitness = new Witness();
                tempwitness.invocationScript = new byte[0];
                tempwitness.verificationScript = new byte[0];
                tempBlock.witness = tempwitness;
                tempBlock.transactions = new Transaction[0];

            } else {
                snapshot.setPersistingBlock(snapshot.getPersistingBlock());
            }
        } else {
            snapshot.setPersistingBlock(persistingBlock);
        }
        ApplicationEngine engine = new ApplicationEngine(TriggerType.Application, container, snapshot, extraGAS, testMode);
        engine.loadScript(script);
        engine.execute();
        return engine;
    }


    public static ApplicationEngine run(byte[] script, IScriptContainer container, Block
            persistingBlock, boolean testMode, Fixed8 extraGAS) {
        extraGAS = null;//default(Fixed8);
        Snapshot snapshot = Blockchain.singleton().getStore().getSnapshot();
        return run(script, snapshot, container, persistingBlock, testMode, extraGAS);
    }

    public static ApplicationEngine run(byte[] script) {
        IScriptContainer container = null;
        Block persistingBlock = null;
        boolean testMode = false;
        Fixed8 extraGAS = null;
        Snapshot snapshot = Blockchain.singleton().getStore().getSnapshot();
        return run(script, snapshot, container, persistingBlock, testMode, extraGAS);
    }
}