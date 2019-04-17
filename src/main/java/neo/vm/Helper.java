package neo.vm;

import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import neo.UInt160;
import neo.UInt256;
import neo.cryptography.ecc.ECPoint;
import neo.csharp.Uint;
import neo.csharp.Ulong;
import neo.csharp.Ushort;
import neo.csharp.common.ByteEnum;
import neo.csharp.common.ByteFlag;
import neo.csharp.io.ISerializable;
import neo.io.SerializeHelper;
import neo.log.notr.TR;
import neo.smartcontract.ContractParameter;
import neo.smartcontract.ContractParameterType;
import neo.vm.Types.Array;
import neo.vm.Types.Boolean;
import neo.vm.Types.ByteArray;
import neo.vm.Types.Integer;
import neo.vm.Types.InteropInterface;
import neo.vm.Types.Map;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: Helper
 * @Package neo.vm
 * @Description: vm 扩展类
 * @date Created in 10:23 2019/3/14
 */
public class Helper {

    /**
     * @param ops OpCode集合
     * @Author:doubi.liu
     * @description:一次构建多个OpCode
     * @date:2019/4/4
     */
    public static ScriptBuilder emit(ScriptBuilder sb, OpCode... ops) {
        for (OpCode op : ops) {
            sb.emit(op);
        }
        return sb;
    }

    public static ScriptBuilder emitAppCall(ScriptBuilder sb, UInt160 scriptHash) {
        boolean useTailCall = false;
        return sb.emitAppCall(scriptHash.toArray(), useTailCall);
    }

    public static ScriptBuilder emitAppCall(ScriptBuilder sb, UInt160 scriptHash, boolean
            useTailCall) {
        return sb.emitAppCall(scriptHash.toArray(), useTailCall);
    }

    public static ScriptBuilder emitAppCall(ScriptBuilder sb, UInt160 scriptHash,
                                            ContractParameter... parameters) {
        for (int i = parameters.length - 1; i >= 0; i--)
            Helper.emitPush(sb, parameters[i]);
        return Helper.emitAppCall(sb, scriptHash);
    }

    public static ScriptBuilder emitAppCall(ScriptBuilder sb, UInt160 scriptHash, String
            operation) {
        sb.emitPush(false);
        sb.emitPush(operation);
        Helper.emitAppCall(sb, scriptHash);
        return sb;
    }

    public static ScriptBuilder emitAppCall(ScriptBuilder sb, UInt160 scriptHash, String
            operation, ContractParameter... args) {
        for (int i = args.length - 1; i >= 0; i--) {
            Helper.emitPush(sb, args[i]);
        }
        sb.emitPush(new BigInteger(String.valueOf(args.length)));
        sb.emit(OpCode.PACK);
        sb.emitPush(operation);
        Helper.emitAppCall(sb, scriptHash);
        return sb;
    }

    public static ScriptBuilder emitAppCall(ScriptBuilder sb, UInt160 scriptHash, String
            operation, Object... args) {
        for (int i = args.length - 1; i >= 0; i--) {
            Helper.emitPush(sb, args[i]);
        }
        sb.emitPush(new BigInteger(String.valueOf(args.length)));
        sb.emit(OpCode.PACK);
        sb.emitPush(operation);
        Helper.emitAppCall(sb, scriptHash);
        return sb;
    }

    public static ScriptBuilder emitPush(ScriptBuilder sb, ISerializable data) {
        return sb.emitPush(SerializeHelper.toBytes(data));
    }

    public static ScriptBuilder emitPush(ScriptBuilder sb, ContractParameter parameter) {
        switch (parameter.type) {
            case Signature:
            case ByteArray:
                sb.emitPush((byte[]) parameter.value);
                break;
            case Boolean:
                sb.emitPush((boolean) parameter.value);
                break;
            case Integer:
/*                if (parameter.value instanceof BigInteger) {
                    sb.emitPush((BigInteger) parameter.value);
                } else
                    sb.emitPush((BigInteger) typeof(BigInteger).GetConstructor(new[]{
                parameter.value.GetType()
            }).Invoke(new[]{
                parameter.Value
            }));*/
                if (parameter.value instanceof BigInteger) {
                    sb.emitPush((BigInteger) parameter.value);
                } else if (parameter.value instanceof Integer) {
                    java.lang.Integer v = (java.lang.Integer) parameter.value;
                    sb.emitPush(BigInteger.valueOf(v.intValue()));
                } else {
                    TR.fixMe("Integer转换不支持操作");
                    throw new RuntimeException("不支持操作");
                }
                break;
            case Hash160:
                Helper.emitPush(sb, (UInt160) parameter.value);
                break;
            case Hash256:
                Helper.emitPush(sb, (UInt256) parameter.value);
                break;
            case PublicKey:
                Helper.emitPush(sb, (ECPoint) parameter.value);
                break;
            case String:
                sb.emitPush((String) parameter.value);
                break;
            case Array: {
                List<ContractParameter> parameters = (ArrayList<ContractParameter>) parameter.value;
                for (int i = parameters.size() - 1; i >= 0; i--)
                    Helper.emitPush(sb, parameters.get(i));
                sb.emitPush(new BigInteger(String.valueOf(parameters.size())));
                sb.emit(OpCode.PACK);
            }
            break;
            default:
                throw new IllegalArgumentException();
        }
        return sb;
    }

    public static ScriptBuilder emitPush(ScriptBuilder sb, Object obj) {
        if (obj instanceof java.lang.Boolean) {
            sb.emitPush((java.lang.Boolean) obj);
        } else if (obj instanceof byte[]) {
            sb.emitPush((byte[]) obj);
        } else if (obj instanceof String) {
            sb.emitPush((String) obj);
        } else if (obj instanceof BigInteger) {
            sb.emitPush((BigInteger) obj);
        } else if (obj instanceof ISerializable) {
            Helper.emitPush(sb, (ISerializable) obj);
        } else if (obj instanceof Byte) {
            sb.emitPush(new BigInteger(String.valueOf(obj)));
            TR.fixMe("此处处理的是有符号byte,无符号未处理");
        } else if (obj instanceof Short) {
            sb.emitPush(new BigInteger(String.valueOf(obj)));
        } else if (obj instanceof Ushort) {
            sb.emitPush(new BigInteger(String.valueOf(((Ushort) obj).intValue())));
        } else if (obj instanceof java.lang.Integer) {
            sb.emitPush(new BigInteger(String.valueOf(obj)));
        } else if (obj instanceof Uint) {
            sb.emitPush(new BigInteger(String.valueOf(((Uint) obj).intValue())));
        } else if (obj instanceof Long) {
            sb.emitPush(new BigInteger(String.valueOf((Long) obj)));
        } else if (obj instanceof Ulong) {
            sb.emitPush(new BigInteger(String.valueOf(((Ulong) obj).intValue())));
        } else if (obj instanceof ByteEnum) { // TODO add by luc 2019.4.15 -------------------------
            ByteEnum byteEnum = (ByteEnum) obj;
            int value = byteEnum.value();
            sb.emitPush(BigInteger.valueOf(value));
        } else if (obj instanceof ByteFlag) {
            ByteFlag flag = (ByteFlag) obj;
            int value = flag.value();
            sb.emitPush(BigInteger.valueOf(value));
        } else if (obj instanceof Enum) {  // end --------------------------------------------------
            sb.emitPush(new BigInteger(String.valueOf(((Enum) obj).ordinal())));
            TR.fixMe("Enum类型数据是否能转换正常未经验证");
        } else {
            throw new IllegalArgumentException();
        }
        return sb;
    }

//  @modify by luchuan 2019.4.15 -------------------------------------------------------------------
//    public static ScriptBuilder emitSysCall(ScriptBuilder sb, String api, Object[] args) {
//        for (int i = args.length - 1; i >= 0; i--)
//            emitPush(sb, args[i]);
//        return sb.emitSysCall(api);
//    }

    // add by luchuan

    /**
     * @param sb   script builder
     * @param api  api
     * @param args arguments, be careful with the parameter's type, only support: Boolean, String,
     *             BigInteger, ISerializable, Byte, Short, Ushort, Integer,Uint, Long, ULong,
     *             ByteEnum, ByteFlag, Enum.
     */
    public static ScriptBuilder emitSysCall(ScriptBuilder sb, String api, Object... args) {
        for (int i = args.length - 1; i >= 0; i--) {
            emitPush(sb, args[i]);
        }
        return sb.emitSysCall(api);
    }
    // @end modify ---------------------------------------------------------------------------------

    public static ContractParameter toParameter(StackItem item) {
        return toParameter(item, null);
    }

    private static ContractParameter toParameter(StackItem item, List<AbstractMap.SimpleEntry<StackItem,
            ContractParameter>> context) {
        ContractParameter parameter = null;
        if (item instanceof Array) {
            if (context == null) {
                context = new ArrayList<AbstractMap.SimpleEntry<StackItem, ContractParameter>>();
            } else {
                //LINQ START
                //parameter = context.FirstOrDefault(p -> ReferenceEquals(p.Item1, item)) ?.Item2;
                List<AbstractMap.SimpleEntry<StackItem, ContractParameter>> templist =
                        context.stream().filter(p -> p.getKey() == item).collect(Collectors
                                .toList());
                if (templist.size() != 0) {
                    parameter = templist.get(0).getValue();
                } else {
                    parameter = null;
                }
                //LINQ END
            }
            if (parameter == null) {
                parameter = new ContractParameter(ContractParameterType.Array);
                context.add(new AbstractMap.SimpleEntry<StackItem, ContractParameter>(item, parameter));
                List<AbstractMap.SimpleEntry<StackItem, ContractParameter>> finalContext = context;
                parameter.value = StreamSupport.stream(((Array) item).getEnumerator().spliterator(), false).map
                        (p -> toParameter((StackItem) p, finalContext))
                        .collect(Collectors.toList());
            }
        } else if (item instanceof Map) {
            if (context == null)
                context = new ArrayList<AbstractMap.SimpleEntry<StackItem, ContractParameter>>();
            else {
                //LINQ START
                //parameter = context.FirstOrDefault(p -> ReferenceEquals(p.Item1, item)) ?.Item2;
                List<AbstractMap.SimpleEntry<StackItem, ContractParameter>> templist =
                        context.stream().filter(p -> p.getKey() == item).collect(Collectors
                                .toList());
                if (templist.size() != 0) {
                    parameter = templist.get(0).getValue();
                } else {
                    parameter = null;
                }
                //LINQ END
                if (parameter == null) {
                    parameter = new ContractParameter(ContractParameterType.Map);
                    context.add(new AbstractMap.SimpleEntry<StackItem, ContractParameter>(item, parameter));
                    //LINQ START
/*                    parameter.value = map.Select(p -> new AbstractMap.SimpleEntry<ContractParameter,
                            ContractParameter>(toParameter(p.key, context),
                            toParameter(p.Value, context))).
                            ToList();*/
                    List<AbstractMap.SimpleEntry<StackItem, ContractParameter>> finalContext = context;
                    parameter.value = StreamSupport.stream(((Map) item).getEnumerator().spliterator
                            (), false).map(p -> new AbstractMap.SimpleEntry<ContractParameter,
                            ContractParameter>(toParameter(p.getKey(), finalContext),
                            toParameter(p.getValue(), finalContext)))
                            .collect(Collectors.toList());
                    //LINQ END
                }
            }

        } else if (item instanceof Boolean) {
            parameter = new ContractParameter(ContractParameterType.Boolean);
            parameter.value = item.getBoolean();
        } else if (item instanceof ByteArray) {
            parameter = new ContractParameter(ContractParameterType.ByteArray);
            parameter.value = item.getByteArray();
        } else if (item instanceof Integer) {
            parameter = new ContractParameter(ContractParameterType.Integer);
            parameter.value = item.getBigInteger();
        } else if (item instanceof InteropInterface) {
            parameter = new ContractParameter(ContractParameterType.InteropInterface);
        } else {
            throw new IllegalArgumentException();
        }
        return parameter;
    }
}