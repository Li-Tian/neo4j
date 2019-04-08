package neo.smartcontract;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import neo.Fixed8;
import neo.UInt160;
import neo.cryptography.Crypto;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.exception.InvalidOperationException;
import neo.log.notr.TR;
import neo.network.p2p.payloads.IVerifiable;
import neo.persistence.Snapshot;
import neo.vm.OpCode;
import neo.vm.ScriptBuilder;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: Helper
 * @Package neo.smartcontract
 * @Description: 智能合约操作相关辅助类
 * @date Created in 13:42 2019/3/12
 */
public class Helper {
    //方法名和方法名哈希映射器
    private static ConcurrentMap<String, Uint> methodHashes = new ConcurrentHashMap<>();

    /**
     * @param script 脚本字节
     * @Author:doubi.liu
     * @description:判断是否是多方签名
     * @date:2019/4/1
     */
    public static boolean isMultiSigContract(byte[] script) {
        TR.enter();
        int m = 0;
        int n = 0;
        int i = 0;
        if (script.length < 37) {
            return TR.exit(false);
        }
        if (script[i] > OpCode.PUSH16.getCode()) {
            return TR.exit(false);
        }
        if (script[i] < OpCode.PUSH1.getCode() && script[i] != 1 && script[i] != 2) {
            return TR.exit(false);
        }
        switch (script[i]) {
            case 1:
                m = script[++i];
                ++i;
                break;
            case 2:
                byte[] temparray = new byte[2];
                temparray[0] = script[i + 1];
                temparray[1] = script[i + 2];
                m = BitConverter.toUshort(temparray).intValue();
                i += 2;
                break;
            default:
                m = script[i++] - 80;
                break;
        }
        if (m < 1 || m > 1024) {
            return TR.exit(false);
        }
        while (script[i] == 33) {
            i += 34;
            if (script.length <= i) {
                return TR.exit(false);
            }
            ++n;
        }
        if (n < m || n > 1024) {
            return TR.exit(false);
        }
        switch (script[i]) {
            case 1:
                if (n != script[++i]) {
                    return TR.exit(false);
                }
                ++i;
                break;
            case 2:
                byte[] temparray = new byte[2];
                temparray[0] = script[i + 1];
                temparray[1] = script[i + 2];
                if (script.length < i + 3 || n != BitConverter.toUshort(temparray).intValue()) {
                    return TR.exit(false);
                }
                i += 2;
                break;
            default:
                if (n != script[i++] - 80) {
                    return TR.exit(false);
                }
                break;
        }
        if (script[i++] != OpCode.CHECKMULTISIG.getCode()) {
            return TR.exit(false);
        }
        if (script.length != i) {
            return TR.exit(false);
        }
        return TR.exit(true);
    }

    /**
     * @param script 脚本字节
     * @Author:doubi.liu
     * @description:判断是否是鉴权合约
     * @date:2019/4/1
     */
    public static boolean isSignatureContract(byte[] script) {
        TR.enter();
        if (script.length != 35) {
            return TR.exit(false);
        }
        if (script[0] != 33 || script[34] != OpCode.CHECKSIG.getCode()) {
            return TR.exit(false);
        }
        return TR.exit(true);
    }

    /**
     * @param script 脚本字节
     * @Author:doubi.liu
     * @description:判断是否是标准合约
     * @date:2019/4/1
     */
    public static boolean isStandardContract(byte[] script) {
        TR.enter();
        return TR.exit(isSignatureContract(script) || isMultiSigContract(script));
    }

    /**
     * @param method 方法名
     * @Author:doubi.liu
     * @description:方法名和方法名哈希互操作方法
     * @date:2019/4/1
     */
    public static Uint toInteropMethodHash(String method) {
        TR.enter();
        if (methodHashes.get(method) == null) {
            try {
                byte[] temp = neo.cryptography.Helper.sha256(method.getBytes("ASCII"));
                methodHashes.put(method, BitConverter.toUint(BitConverter.subBytes(temp, 0,
                        4)));
                return TR.exit(methodHashes.get(method));
            } catch (UnsupportedEncodingException e) {
                TR.fixMe("字符串转码异常");
                throw TR.exit(new RuntimeException(e));
            }
        } else {
            return TR.exit(methodHashes.get(method));
        }
    }

    /**
     * @param script 脚本字节
     * @Author:doubi.liu
     * @description:求解脚本哈希
     * @date:2019/4/1
     */
    public static UInt160 toScriptHash(byte[] script) {
        TR.enter();
        return TR.exit(new UInt160(Crypto.Default.hash160(script)));
    }

    /**
     * @Author:doubi.liu
     * @description:验证见证人脚本
     * @date:2019/4/1
     */
    static boolean verifyWitnesses(IVerifiable verifiable, Snapshot snapshot) {
        UInt160[] hashes;
        try {
            hashes = verifiable.getScriptHashesForVerifying(snapshot);
        } catch (InvalidOperationException e) {
            return false;
        }
        if (hashes.length != verifiable.getWitnesses().length) {
            return false;
        }
        for (int i = 0; i < hashes.length; i++) {
            byte[] verification = verifiable.getWitnesses()[i].verificationScript;
            if (verification.length == 0) {
                ScriptBuilder sb = new ScriptBuilder();
                sb.emitAppCall(hashes[i].toArray());
                verification = sb.toArray();
            } else {
                if (hashes[i] != verifiable.getWitnesses()[i].scriptHash()) return false;
            }
            ApplicationEngine engine = new ApplicationEngine(TriggerType.Verification,
                    verifiable, snapshot, Fixed8.ZERO);

            engine.loadScript(verification);
            engine.loadScript(verifiable.getWitnesses()[i].invocationScript);
            if (!engine.execute2()) return false;
            if (engine.resultStack.getCount() != 1 || !engine.resultStack.pop().getBoolean())
                return false;

        }
        return true;
    }
}