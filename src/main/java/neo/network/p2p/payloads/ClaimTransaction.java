package neo.network.p2p.payloads;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.Collection;

import neo.Fixed8;
import neo.UInt160;
import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.FormatException;
import neo.persistence.Snapshot;

/**
 * Claim交易，用于发起提取GAS交易
 */
public class ClaimTransaction extends Transaction {

    /**
     * 已经花费的GAS outputs
     */
    public CoinReference[] claims;

    /**
     * 构造函数
     */
    public ClaimTransaction() {
        super(TransactionType.ClaimTransaction);
    }

    /**
     * 存储大小
     */
    @Override
    public int size() {
        return super.size() + BitConverter.getVarSize(claims);
    }

    /**
     * 网络费用，默认0
     */
    @Override
    public Fixed8 getNetworkFee() {
        return Fixed8.ZERO;
    }

    /**
     * 反序列化，读取claims数据，其他数据未提取
     *
     * @param reader 二进制输入流
     * @throws FormatException 当交易版本号不为0，或者Claims长度为0时，抛出异常
     */
    @Override
    protected void deserializeExclusiveData(BinaryReader reader) {
        if (version != 0) throw new FormatException();
        claims = reader.readArray(CoinReference[]::new, CoinReference::new);
        if (claims.length == 0) throw new FormatException();
    }

    /**
     * 获取待验证脚本hash
     *
     * @param snapshot 数据库快照
     * @return 验证脚本hash列表，包括output指向的收款人地址。按照哈希值排序。
     * @throws InvalidOperationException 若引用的output不存在时，抛出该异常
     */
    @Override
    public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
        UInt160[] hashes = super.getScriptHashesForVerifying(snapshot);

        for (CoinReference claim : claims) {
//                Transaction tx = snapshot.GetTransaction(group.Key);
//                if (tx == null) throw new InvalidOperationException();
//                foreach(CoinReference claim in group)
//                {
//                    if (tx.Outputs.Length <= claim.PrevIndex) throw new InvalidOperationException();
//                    hashes.Add(tx.Outputs[claim.PrevIndex].ScriptHash);
//                }
//            }
//            return hashes.OrderBy(p = > p).ToArray();
        }
        // TODO waiting for db
        return hashes;
    }

    /**
     * 序列化，写出claims数据，其他数据未提取
     * <p>序列化字段</p>
     * <ul>
     * <li>Claims: 已经花费的GAS outputs</li>
     * </ul>
     *
     * @param writer 二进制输出流
     */
    @Override
    protected void serializeExclusiveData(BinaryWriter writer) {
        writer.writeArray(claims);
    }

    /**
     * 转成json对象
     *
     * @return json对象
     */
    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();

        JsonArray array = new JsonArray(claims.length);
        for (CoinReference claim : claims) {
            array.add(claim.toJson());
        }
        json.add("claims", array);
        return json;
    }

    /**
     * 验证交易
     *
     * @param snapshot 数据库快照
     * @param mempool  内存池交易
     * @return 返回情况如下：<br/>
     * <ul>
     * <li>1. 进行交易的基本验证，若验证失败，则返回false</li>
     * <li>2. 若Claims包含重复交易时，返回false</li>
     * <li>3. 若Claims与内存池交易存在重复时，返回false</li>
     * <li>4. 若此Claim交易引用一笔不存在的Output则返回false</li>
     * <li>5. 若此Claim交易的输入GAS之和大于等于输出的GAS之和，返回false </li>
     * <li>6. 若Claim交易所引用的交易所计算出来的GAS量不等于Claim交易所声明的GAS量时，返回false </li>
     * <li>7. 若处理过程异常时，返回false </li>
     * </ul>
     */
    @Override
    public boolean verify(Snapshot snapshot, Collection<Transaction> mempool) {
        if (!super.verify(snapshot, mempool)) return false;
        if (claims.length != Arrays.stream(claims).distinct().count())
            return false;

        return true; // TODO waiting
//        if (mempool.OfType < ClaimTransaction > ().Where(p = > p != this).
//        SelectMany(p = > p.Claims).
//        Intersect(Claims).Count() > 0)
//        return false;
//        TransactionResult result = GetTransactionResults().FirstOrDefault(p = > p.AssetId == Blockchain.UtilityToken.Hash)
//        ;
//        if (result == null || result.Amount > Fixed8.Zero) return false;
//        try {
//            return snapshot.CalculateBonus(Claims, false) == -result.Amount;
//        } catch (ArgumentException) {
//            return false;
//        } catch (NotSupportedException) {
//            return false;
//        }
    }
}
