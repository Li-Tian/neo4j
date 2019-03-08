package neo.smartcontract;


import neo.log.tr.TR;
import neo.vm.OpCode;
import neo.vm.ScriptBuilder;
import neo.cryptography.ecc.ECPoint;

/**
 * 合约类，提供了合约的构造方法，以及创建多方签名和单签合约的方法
 */
public class Contract {

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
