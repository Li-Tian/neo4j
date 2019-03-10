package neo.network.p2p.payloads;


import com.google.gson.JsonObject;


import java.util.Arrays;
import java.util.Collection;

import neo.UInt160;
import neo.cryptography.ecc.ECC;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.FormatException;
import neo.persistence.Snapshot;
import neo.smartcontract.Contract;
import neo.cryptography.ecc.ECPoint;

/**
 * 注册验证人【已弃用，请使用StateTransaction】
 */
@Deprecated
public class EnrollmentTransaction extends Transaction {

    /**
     * 申请人公钥地址
     */
    public ECPoint publicKey;

    private UInt160 scriptHash = null;

    /**
     * 构造函数：创建注册验证人交易
     */
    public EnrollmentTransaction() {
        super(TransactionType.EnrollmentTransaction);
    }

    /**
     * 获取脚本hash
     */
    public UInt160 getScriptHash() {
        if (scriptHash == null) {
            scriptHash = UInt160.parseToScriptHash(Contract.createSignatureRedeemScript(publicKey));
        }
        return scriptHash;
    }

    /**
     * 存储大小
     */
    @Override
    public int size() {
        return super.size() + publicKey.size();
    }

    /**
     * 反序列化，读取公钥地址
     *
     * @param reader 二进制输入流
     * @throws FormatException 如果交易版本号不等于0
     */
    @Override
    protected void deserializeExclusiveData(BinaryReader reader) {
        if (version != 0) throw new FormatException();
        publicKey = ECPoint.deserializeFrom(reader, ECC.Secp256r1.getCurve());
    }

    /**
     * 获取需要签名的交易的hash。包括交易输入的地址和申请人的公钥地址。
     *
     * @param snapshot 数据库快照
     * @return 包括交易输入的地址和申请人的公钥地址。
     */
    @Override
    public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
        // C# code
        // base.GetScriptHashesForVerifying(snapshot).Union(new UInt160[] { ScriptHash }).OrderBy(p => p).ToArray();
        UInt160 ownerHash = getScriptHash();
        UInt160[] hashes = super.getScriptHashesForVerifying(snapshot);

        UInt160[] results = new UInt160[hashes.length + 1];
        results[0] = ownerHash;
        System.arraycopy(hashes, 0, results, 1, hashes.length);
        Arrays.sort(results);
        return results;
    }

    /**
     * 序列化
     * <p>序列化字段</p>
     * <ul>
     * <li>PublicKey: 申请人公钥地址</li>
     * </ul>
     *
     * @param writer 二进制输出流
     */
    @Override
    protected void serializeExclusiveData(BinaryWriter writer) {
        writer.writeSerializable(publicKey);
    }

    /**
     * 转成json对象
     *
     * @return json对象
     */
    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("pubkey", publicKey.toString());
        return json;
    }

    /**
     * 校验该交易。已弃用该交易。拒绝新的交易。所以固定返回false
     *
     * @param snapshot 数据库快照
     * @param mempool  内存池交易
     * @return 返回false，已弃用该交易。拒绝新的交易。
     */
    @Override
    public boolean verify(Snapshot snapshot, Collection<Transaction> mempool) {
        return false;
    }
}
