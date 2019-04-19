package neo;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.MemoryStream;
import neo.smartcontract.Helper;
import neo.vm.OpCode;
import neo.vm.StackItem;
import scenario.InvocationTxWithContractTest;

/**
 * AVM Parser, avm -> opcode
 */
public class AVMParser {

    private static final String testFile = "nep5contract.avm";

    @Test
    public void testParser() {
        byte[] script = loadAvm();
        System.out.println(BitConverter.toHexString(script));
        Collection<OpCodeItem> opCodeItems = parseAVMScript(script);
        printCodeList(opCodeItems);
    }

    public static void printCodeList(Collection<OpCodeItem> opCodeItems) {
        int i = 1;
        for (OpCodeItem item : opCodeItems) {
            StringBuilder builder = new StringBuilder();
            for (Object param : item.params) {
                if (param instanceof byte[]) {
                    byte[] bytes = (byte[]) param;
                    builder.append(" 0x").append(BitConverter.toHexString(bytes));
                } else if (param instanceof StackItem) {
                    StackItem stackItem = (StackItem) param;
                    builder.append(" 0x").append(BitConverter.toHexString(stackItem.getByteArray()));
                } else if (param instanceof Integer) {
                    Integer integer = (Integer) param;
                    builder.append(" 0x").append(BitConverter.toHexString(BitConverter.getBytes(integer)));
                } else if (param instanceof Short) {
                    Short aShort = (Short) param;
                    builder.append(" 0x").append(BitConverter.toHexString(BitConverter.getBytes(aShort)));
                } else if (param instanceof Byte) {
                    Byte aByte = (Byte) param;
                    builder.append(" 0x").append(BitConverter.toHexString(new byte[]{aByte}));
                } else {
                    builder.append(" ").append(param);
                }
            }
            System.out.println((i++) + ": " + BitConverter.toHexString(new byte[]{item.code.getCode()}) + " " + item.code.name() + builder.toString() + " " + item.comment);
        }
    }

    private static byte[] loadAvm() {
        String path = InvocationTxWithContractTest.class.getClassLoader().getResource("").getPath() + testFile;
        try {
            return Utils.readContentFromFile(path);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Collection<OpCodeItem> parseAVMScript(byte[] script) {
        ExecutionContext context = new ExecutionContext(script);
        return parseExecuteOpcode(context);
    }

    public static void printScriptOpCode(byte[] script){
        printCodeList(parseAVMScript(script));
    }

    private static Collection<OpCodeItem> parseExecuteOpcode(ExecutionContext context) {
        ArrayList<OpCodeItem> opcodeItems = new ArrayList<>();

        OpCode opcode = null;
        while (context.hasNextInstruction()) {
            opcode = context.getNextInstruction();
            OpCodeItem codeItem = new OpCodeItem();
            codeItem.code = opcode;
            opcodeItems.add(codeItem);

//            System.out.println(opcodeItems.size() + ": " + BitConverter.toHexString(new byte[]{opcode.getCode()}) + " " + opcode.name());

            if ((opcode.getCode() >= OpCode.PUSHBYTES1.getCode())
                    && (opcode.getCode() <= OpCode.PUSHBYTES75.getCode())) {
                codeItem.params.add(context.opReader.readFully(opcode.getCode()));
            } else {
                switch (opcode) {
                    // Push value
                    case PUSH0:
                        break;
                    case PUSHDATA1:
                        codeItem.params.add((byte) context.opReader.readByte());
                        break;
                    case PUSHDATA2:
                        short length = context.opReader.readShort();
                        codeItem.params.add(length);
                        codeItem.params.add(context.opReader.readFully(length));
                        break;
                    case PUSHDATA4:
                        int alength = context.opReader.readInt();
                        codeItem.params.add(alength);
                        codeItem.params.add(context.opReader.readFully(alength));
                        break;
                    case PUSHM1:
                    case PUSHT:
                    case PUSH1:
                    case PUSH2:
                    case PUSH3:
                    case PUSH4:
                    case PUSH5:
                    case PUSH6:
                    case PUSH7:
                    case PUSH8:
                    case PUSH9:
                    case PUSH10:
                    case PUSH11:
                    case PUSH12:
                    case PUSH13:
                    case PUSH14:
                    case PUSH15:
                    case PUSH16:
                        break;

                    // Control
                    case NOP:
                        break;
                    case JMP:
                    case JMPIF:
                    case JMPIFNOT: {
                        short offset = context.opReader.readShort();
                        codeItem.params.add(offset);
                    }
                    break;
                    case CALL: {
                        context.setInstructionPointer(context.getInstructionPointer() + 2);
                    }
                    break;
                    case RET:
                        break;
                    case APPCALL:
                    case TAILCALL: {
                        byte[] script_hash = context.opReader.readFully(20);
                        codeItem.params.add(script_hash);
                    }
                    break;
                    case SYSCALL:
                        byte[] methodBytes = context.opReader.readVarBytes(252);
                        byte[] lengthBytes = BitConverter.getVarIntEncodedBytes(methodBytes.length);
                        codeItem.params.add(BitConverter.merge(lengthBytes, methodBytes));
                        codeItem.comment = String.format("# %s", findMethod(methodBytes));
                        break;

                    // Stack ops
                    case DUPFROMALTSTACK:
                        break;
                    case TOALTSTACK:
                        break;
                    case FROMALTSTACK:
                        break;
                    case XDROP:
                        break;
                    case XSWAP:
                        break;
                    case XTUCK:
                        break;
                    case DEPTH:
                        break;
                    case DROP:
                        break;
                    case DUP:
                        break;
                    case NIP:
                        break;
                    case OVER:
                        break;
                    case PICK:
                        break;
                    case ROLL:
                        break;
                    case ROT:
                        break;
                    case SWAP:
                        break;
                    case TUCK:
                        break;
                    case CAT:
                        break;
                    case SUBSTR:
                        break;
                    case LEFT:
                        break;
                    case RIGHT:
                        break;
                    case SIZE:
                        break;

                    // Bitwise logic
                    case INVERT:
                        break;
                    case AND:
                        break;
                    case OR:
                        break;
                    case XOR:
                        break;
                    case EQUAL:
                        break;

                    // Numeric
                    case INC:
                        break;
                    case DEC:
                        break;
                    case SIGN:
                        break;
                    case NEGATE:
                        break;
                    case ABS:
                        break;
                    case NOT:
                        break;
                    case NZ:
                        break;
                    case ADD:
                        break;
                    case SUB:
                        break;
                    case MUL:
                        break;
                    case DIV:
                        break;
                    case MOD:
                        break;
                    case SHL:
                        break;
                    case SHR:
                        break;
                    case BOOLAND:
                        break;
                    case BOOLOR:
                        break;
                    case NUMEQUAL:
                        break;
                    case NUMNOTEQUAL:
                        break;
                    case LT:
                        break;
                    case GT:
                        break;
                    case LTE:
                        break;
                    case GTE:
                        break;
                    case MIN:
                        break;
                    case MAX:
                        break;
                    case WITHIN:
                        break;

                    // Crypto
                    case SHA1:
                        break;
                    case SHA256:
                        break;
                    case HASH160:
                        break;
                    case HASH256:
                        break;
                    case CHECKSIG:
                        break;
                    case VERIFY:
                        break;
                    case CHECKMULTISIG:
                        break;

                    // Array
                    case ARRAYSIZE:
                        break;
                    case PACK:
                        break;
                    case UNPACK:
                        break;
                    case PICKITEM:
                        break;
                    case SETITEM:
                        break;
                    case NEWARRAY:
                        break;
                    case NEWSTRUCT:
                        break;
                    case NEWMAP:
                        break;
                    case APPEND:
                        break;
                    case REVERSE:
                        break;
                    case REMOVE:
                        break;
                    case HASKEY:
                        break;
                    case KEYS:
                        break;
                    case VALUES:
                        break;

                    // Stack isolation
                    case CALL_I: {
                        int rvcount = context.opReader.readByte();
                        int pcount = context.opReader.readByte();
                        byte[] position = new byte[2];
                        context.opReader.readFully(position, 0, 2);

                        codeItem.params.add(Byte.valueOf((byte) rvcount));
                        codeItem.params.add(Byte.valueOf((byte) pcount));
                        codeItem.params.add(position);
                        codeItem.comment = "# ret {1} param {2} jump_offset {3}";
                    }
                    break;
                    case CALL_E:
                    case CALL_ED:
                    case CALL_ET:
                    case CALL_EDT: {
                        int rvcount = context.opReader.readByte();
                        int pcount = context.opReader.readByte();
                        codeItem.params.add((byte) rvcount);
                        codeItem.params.add((byte) pcount);
                        byte[] script_hash;
                        if (opcode == OpCode.CALL_ED || opcode == OpCode.CALL_EDT) {
                        } else {
                            script_hash = context.opReader.readFully(20);
                            codeItem.params.add(script_hash);
                        }
                    }
                    break;

                    // Exceptions
                    case THROW:
                        return null;
                    case THROWIFNOT:
                        break;
                    default:
                        return null;
                }
            }
        }
        OpCodeItem retItem = new OpCodeItem();
        retItem.code = OpCode.RET;
        opcodeItems.add(retItem);
        return opcodeItems;
    }

    private static class OpCodeItem {
        OpCode code;
        ArrayList<Object> params = new ArrayList<>(0);
        String comment = "";
    }


    private static class ExecutionContext {
        //脚本的BinaryReader
        BinaryReader opReader;

        //脚本的BinaryReader的流
        MemoryStream opReaderStream;

        //Script
        public byte[] script;

        ExecutionContext(byte[] script) {
            this.script = script;
            this.opReaderStream = new MemoryStream(script);
            this.opReader = new BinaryReader(this.opReaderStream);
        }

        public int getInstructionPointer() {
            return opReaderStream.getPosition();
        }

        public void setInstructionPointer(int value) {
            opReaderStream.seek(value);
        }

        public boolean hasNextInstruction() {
            return opReaderStream.getPosition() < script.length;
        }

        public OpCode getNextInstruction() {
            return OpCode.fromByte((byte) opReader.readByte());
        }
    }

    private static HashMap<String, String> methodMap = new HashMap<>();

    static {
        addMethod("System.ExecutionEngine.GetScriptContainer");
        addMethod("System.ExecutionEngine.GetExecutingScriptHash");
        addMethod("System.ExecutionEngine.GetCallingScriptHash");
        addMethod("System.ExecutionEngine.GetEntryScriptHash");
        addMethod("System.Runtime.Platform");
        addMethod("System.Runtime.GetTrigger");
        addMethod("System.Runtime.CheckWitness");
        addMethod("System.Runtime.Notify");
        addMethod("System.Runtime.Log");
        addMethod("System.Runtime.GetTime");
        addMethod("System.Runtime.Serialize");
        addMethod("System.Runtime.Deserialize");
        addMethod("System.Blockchain.GetHeight");
        addMethod("System.Blockchain.GetHeader");
        addMethod("System.Blockchain.GetBlock");
        addMethod("System.Blockchain.GetTransaction");
        addMethod("System.Blockchain.GetTransactionHeight");
        addMethod("System.Blockchain.GetContract");
        addMethod("System.Header.GetIndex");
        addMethod("System.Header.GetHash");
        addMethod("System.Header.GetPrevHash");
        addMethod("System.Header.GetTimestamp");
        addMethod("System.Block.GetTransactionCount");
        addMethod("System.Block.GetTransactions");
        addMethod("System.Block.GetTransaction");
        addMethod("System.Transaction.GetHash");
        addMethod("System.Contract.Destroy");
        addMethod("System.Contract.GetStorageContext");
        addMethod("System.Storage.GetContext");
        addMethod("System.Storage.GetReadOnlyContext");
        addMethod("System.Storage.Get");
        addMethod("System.Storage.Put");
        addMethod("System.Storage.PutEx");
        addMethod("System.Storage.Delete");
        addMethod("System.StorageContext.AsReadOnly");
        addMethod("Neo.Runtime.GetTrigger");
        addMethod("Neo.Runtime.CheckWitness");
        addMethod("Neo.Runtime.Notify");
        addMethod("Neo.Runtime.Log");
        addMethod("Neo.Runtime.GetTime");
        addMethod("Neo.Runtime.Serialize");
        addMethod("Neo.Runtime.Deserialize");
        addMethod("Neo.Blockchain.GetHeight");
        addMethod("Neo.Blockchain.GetHeader");
        addMethod("Neo.Blockchain.GetBlock");
        addMethod("Neo.Blockchain.GetTransaction");
        addMethod("Neo.Blockchain.GetTransactionHeight");
        addMethod("Neo.Blockchain.GetAccount");
        addMethod("Neo.Blockchain.GetValidators");
        addMethod("Neo.Blockchain.GetAsset");
        addMethod("Neo.Blockchain.GetContract");
        addMethod("Neo.Header.GetHash");
        addMethod("Neo.Header.GetVersion");
        addMethod("Neo.Header.GetPrevHash");
        addMethod("Neo.Header.GetMerkleRoot");
        addMethod("Neo.Header.GetTimestamp");
        addMethod("Neo.Header.GetIndex");
        addMethod("Neo.Header.GetConsensusData");
        addMethod("Neo.Header.GetNextConsensus");
        addMethod("Neo.Block.GetTransactionCount");
        addMethod("Neo.Block.GetTransactions");
        addMethod("Neo.Block.GetTransaction");
        addMethod("Neo.Transaction.GetHash");
        addMethod("Neo.Transaction.GetType");
        addMethod("Neo.Transaction.GetAttributes");
        addMethod("Neo.Transaction.GetInputs");
        addMethod("Neo.Transaction.GetOutputs");
        addMethod("Neo.Transaction.GetReferences");
        addMethod("Neo.Transaction.GetUnspentCoins");
        addMethod("Neo.Transaction.GetWitnesses");
        addMethod("Neo.InvocationTransaction.GetScript");
        addMethod("Neo.Witness.GetVerificationScript");
        addMethod("Neo.Attribute.GetUsage");
        addMethod("Neo.Attribute.GetData");
        addMethod("Neo.Input.GetHash");
        addMethod("Neo.Input.GetIndex");
        addMethod("Neo.Output.GetAssetId");
        addMethod("Neo.Output.GetValue");
        addMethod("Neo.Output.GetScriptHash");
        addMethod("Neo.Account.GetScriptHash");
        addMethod("Neo.Account.GetVotes");
        addMethod("Neo.Account.GetBalance");
        addMethod("Neo.Account.IsStandard");
        addMethod("Neo.Asset.Create");
        addMethod("Neo.Asset.Renew");
        addMethod("Neo.Asset.GetAssetId");
        addMethod("Neo.Asset.GetAssetType");
        addMethod("Neo.Asset.GetAmount");
        addMethod("Neo.Asset.GetAvailable");
        addMethod("Neo.Asset.GetPrecision");
        addMethod("Neo.Asset.GetOwner");
        addMethod("Neo.Asset.GetAdmin");
        addMethod("Neo.Asset.GetIssuer");
        addMethod("Neo.Contract.Create");
        addMethod("Neo.Contract.Migrate");
        addMethod("Neo.Contract.Destroy");
        addMethod("Neo.Contract.GetScript");
        addMethod("Neo.Contract.IsPayable");
        addMethod("Neo.Contract.GetStorageContext");
        addMethod("Neo.Storage.GetContext");
        addMethod("Neo.Storage.GetReadOnlyContext");
        addMethod("Neo.Storage.Get");
        addMethod("Neo.Storage.Put");
        addMethod("Neo.Storage.Delete");
        addMethod("Neo.Storage.Find");
        addMethod("Neo.StorageContext.AsReadOnly");
        addMethod("Neo.Enumerator.Create");
        addMethod("Neo.Enumerator.Next");
        addMethod("Neo.Enumerator.Value");
        addMethod("Neo.Enumerator.Concat");
        addMethod("Neo.Iterator.Create");
        addMethod("Neo.Iterator.Key");
        addMethod("Neo.Iterator.Keys");
        addMethod("Neo.Iterator.Values");
        addMethod("Neo.Iterator.Next");
        addMethod("Neo.Iterator.Value");
        addMethod("AntShares.Runtime.CheckWitness");
        addMethod("AntShares.Runtime.Notify");
        addMethod("AntShares.Runtime.Log");
        addMethod("AntShares.Blockchain.GetHeight");
        addMethod("AntShares.Blockchain.GetHeader");
        addMethod("AntShares.Blockchain.GetBlock");
        addMethod("AntShares.Blockchain.GetTransaction");
        addMethod("AntShares.Blockchain.GetAccount");
        addMethod("AntShares.Blockchain.GetValidators");
        addMethod("AntShares.Blockchain.GetAsset");
        addMethod("AntShares.Blockchain.GetContract");
        addMethod("AntShares.Header.GetHash");
        addMethod("AntShares.Header.GetVersion");
        addMethod("AntShares.Header.GetPrevHash");
        addMethod("AntShares.Header.GetMerkleRoot");
        addMethod("AntShares.Header.GetTimestamp");
        addMethod("AntShares.Header.GetConsensusData");
        addMethod("AntShares.Header.GetNextConsensus");
        addMethod("AntShares.Block.GetTransactionCount");
        addMethod("AntShares.Block.GetTransactions");
        addMethod("AntShares.Block.GetTransaction");
        addMethod("AntShares.Transaction.GetHash");
        addMethod("AntShares.Transaction.GetType");
        addMethod("AntShares.Transaction.GetAttributes");
        addMethod("AntShares.Transaction.GetInputs");
        addMethod("AntShares.Transaction.GetOutputs");
        addMethod("AntShares.Transaction.GetReferences");
        addMethod("AntShares.Attribute.GetUsage");
        addMethod("AntShares.Attribute.GetData");
        addMethod("AntShares.Input.GetHash");
        addMethod("AntShares.Input.GetIndex");
        addMethod("AntShares.Output.GetAssetId");
        addMethod("AntShares.Output.GetValue");
        addMethod("AntShares.Output.GetScriptHash");
        addMethod("AntShares.Account.GetScriptHash");
        addMethod("AntShares.Account.GetVotes");
        addMethod("AntShares.Account.GetBalance");
        addMethod("AntShares.Asset.Create");
        addMethod("AntShares.Asset.Renew");
        addMethod("AntShares.Asset.GetAssetId");
        addMethod("AntShares.Asset.GetAssetType");
        addMethod("AntShares.Asset.GetAmount");
        addMethod("AntShares.Asset.GetAvailable");
        addMethod("AntShares.Asset.GetPrecision");
        addMethod("AntShares.Asset.GetOwner");
        addMethod("AntShares.Asset.GetAdmin");
        addMethod("AntShares.Asset.GetIssuer");
        addMethod("AntShares.Contract.Create");
        addMethod("AntShares.Contract.Migrate");
        addMethod("AntShares.Contract.Destroy");
        addMethod("AntShares.Contract.GetScript");
        addMethod("AntShares.Contract.GetStorageContext");
        addMethod("AntShares.Storage.GetContext");
        addMethod("AntShares.Storage.Get");
        addMethod("AntShares.Storage.Put");
        addMethod("AntShares.Storage.Delete");
    }

    private static void addMethod(String method) {
        methodMap.put(new String(Helper.toInteropMethodHash(method).toBytes()), method);
    }

    private static String findMethod(byte[] methodHash) {
        String name =  methodMap.get(new String(methodHash));
        if (name == null){
            return new String(methodHash);
        }
        return name;
    }
}


