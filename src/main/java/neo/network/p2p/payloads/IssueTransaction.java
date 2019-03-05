package neo.network.p2p.payloads;

import java.util.Arrays;
import java.util.Collection;

import neo.Fixed8;
import neo.UInt160;
import neo.csharp.io.BinaryReader;
import neo.exception.FormatException;
import neo.ledger.Blockchain;
import neo.persistence.Snapshot;

/**
 * 发布资产交易
 */
public class IssueTransaction extends Transaction {

    /**
     * 构造函数：创建发布资产交易
     */
    public IssueTransaction() {
        super(TransactionType.IssueTransaction);
    }

    /**
     * 系统手续费
     *
     * @return <ul>
     * <li>1）若交易版本号大于等于1，手续费为0</li>
     * <li>2）若发布的资产是NEO或GAS，则手续费为0</li>
     * <li>3）否则按基本交易计算系统手续费</li>
     * </ul>
     */
    @Override
    public Fixed8 getSystemFee() {
        if (version >= 1) {
            return Fixed8.ZERO;
        }
        // c# code
        // if (Outputs.All(p => p.AssetId == Blockchain.GoverningToken.Hash || p.AssetId == Blockchain.UtilityToken.Hash))
        //      return Fixed8.Zero;
        if (Arrays.stream(outputs)
                .filter(p -> p.assetId == Blockchain.GoverningToken.hash() ||
                        p.assetId == Blockchain.UtilityToken.hash())
                .count() > 0) {
            return Fixed8.ZERO;
        }
        return super.getSystemFee();
    }

    /**
     * 反序列化扩展数据
     *
     * @param reader 二进制输入流
     * @throws FormatException 若交易版本号大于1，则抛出该异常
     */
    @Override
    protected void deserializeExclusiveData(BinaryReader reader) {
        if (version > 1) {
            throw new FormatException();
        }
    }

    /**
     * 获取待验证签名的脚本hash
     *
     * @param snapshot 快照
     * @return 交易本身的验证脚本，以及发行者的地址脚本hash
     * @throws InvalidOperationException 若发行的资产不存在，则抛出该异常
     */
    @Override
    public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
//        TODO waiting for db
//        HashSet<UInt160> hashes = new HashSet<UInt160>(super.getScriptHashesForVerifying(snapshot));
//        foreach(TransactionResult result in GetTransactionResults().Where(p = > p.Amount < Fixed8.Zero))
//        {
//            AssetState asset = snapshot.Assets.TryGet(result.AssetId);
//            if (asset == null) throw new InvalidOperationException();
//            hashes.Add(asset.Issuer);
//        }
//        return hashes.OrderBy(p = > p).ToArray();
        return new UInt160[0];
    }

    /**
     * 校验交易
     *
     * @param snapshot 数据库快照
     * @param mempool  内存池交易
     * @return <ul>
     * <li>1. 进行交易的基本验证，若验证失败，则返回false</li>
     * <li>2. 交易引用的input 不存在时返回false</li>
     * <li>3. 若发行的资产不存在返回false</li>
     * <li>4. 若发行的量为负数时返回false</li>
     * <li>5. 若该交易的发行量加上内存池其他发行量，超过了发行总量，则返回false</li>
     * </ul>
     */
    @Override
    public boolean verify(Snapshot snapshot, Collection<Transaction> mempool) {
        if (!super.verify(snapshot, mempool)) {
            return false;
        }

//        TransactionResult[] results = getTransactionResults();
//         TODO C# waiting db
//        TransactionResult[] results = GetTransactionResults() ?.Where(p = > p.Amount < Fixed8.Zero).
//        ToArray();
//        if (results == null) return false;
//        foreach(TransactionResult r in results)
//        {
//            AssetState asset = snapshot.Assets.TryGet(r.AssetId);
//            if (asset == null) return false;
//            if (asset.Amount < Fixed8.Zero) continue;
//            Fixed8 quantity_issued = asset.Available + mempool.OfType < IssueTransaction > ().Where(p = > p != this).
//            SelectMany(p = > p.Outputs).Where(p = > p.AssetId == r.AssetId).Sum(p = > p.Value);
//            if (asset.Amount - quantity_issued < -r.Amount) return false;
//        }
        return true;
    }
}
