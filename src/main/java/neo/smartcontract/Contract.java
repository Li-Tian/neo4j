package neo.smartcontract;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import neo.UInt160;
import neo.log.notr.TR;
import neo.vm.OpCode;
import neo.vm.ScriptBuilder;
import neo.cryptography.ecc.ECPoint;

/**
 * 合约类，提供了合约的构造方法，以及创建多方签名和单签合约的方法
 */
public class Contract {
    public byte[] script;
    public ContractParameterType[] parameterList;
    private String _address;

    public String address() {
        TR.enter();
        if (_address == null) {
            _address = scriptHash().toAddress();
        }
        return TR.exit(_address);
    }

    private UInt160 _scriptHash;

    public UInt160 scriptHash() {
        if (_scriptHash == null) {
            _scriptHash = UInt160.parseToScriptHash(script);
        }
        return _scriptHash;
    }

    public Contract() {
    }

    public Contract(ContractParameterType[] parameterList, byte[] redeemScript) {
        this.script = redeemScript;
        this.parameterList = parameterList;
    }

    public static Contract create(ContractParameterType[] parameterList, byte[] redeemScript) {
        TR.enter();
        return TR.exit(new Contract(parameterList, redeemScript));
    }

    public static Contract createMultiSigContract(int m, ECPoint[] publicKeys) {
        TR.enter();
        ContractParameterType[] output = new ContractParameterType[m];
        Arrays.fill(output, ContractParameterType.Signature);
        return TR.exit(new Contract(output, createMultiSigRedeemScript(m, publicKeys)));
    }

    public static byte[] createMultiSigRedeemScript(int m, ECPoint[] publicKeys) {
        TR.enter();
        if (!(1 <= m && m <= publicKeys.length && publicKeys.length <= 1024)) {
            TR.exit();
            throw new IllegalArgumentException();
        }
        ScriptBuilder sb = new ScriptBuilder();
        sb.emitPush(BigInteger.valueOf(m));
        for (ECPoint publicKey : Arrays.stream(publicKeys).sorted(ECPoint::compareTo).toArray(ECPoint[]::new)) {
            sb.emitPush(publicKey.getEncoded(true));
        }
        sb.emitPush(BigInteger.valueOf(publicKeys.length));
        sb.emit(OpCode.CHECKMULTISIG);
        return TR.exit(sb.toArray());
    }

    public static byte[] createMultiSigRedeemScript(int m, Collection<ECPoint> publicKeys) {
        TR.enter();
        if (!(1 <= m && m <= publicKeys.size() && publicKeys.size() <= 1024)) {
            TR.exit();
            throw new IllegalArgumentException();
        }
        ScriptBuilder sb = new ScriptBuilder();
        sb.emitPush(BigInteger.valueOf(m));
        publicKeys.stream().sorted().forEach(publicKey -> sb.emitPush(publicKey.getEncoded(true)));
        sb.emitPush(BigInteger.valueOf(publicKeys.size()));
        sb.emit(OpCode.CHECKMULTISIG);
        return TR.exit(sb.toArray());
    }

    public static Contract CreateSignatureContract(ECPoint publicKey) {
        TR.enter();
        return TR.exit(new Contract(new ContractParameterType[]{ContractParameterType.Signature}, createSignatureRedeemScript(publicKey)));
    }

    /**
     * 获取公钥对应的脚本字节码
     *
     * @param publicKey 公钥
     * @return script bytes
     */
    public static byte[] createSignatureRedeemScript(ECPoint publicKey) {
        TR.enter();
        ScriptBuilder sb = new ScriptBuilder();
        sb.emitPush(publicKey.getEncoded(true));
        sb.emit(OpCode.CHECKSIG);
        return TR.exit(sb.toArray());
    }
}
