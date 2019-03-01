package neo.network.p2p.payloads;

import java.util.Arrays;
import java.util.Collection;

import neo.Fixed8;
import neo.UInt160;
import neo.csharp.io.BinaryReader;
import neo.exception.FormatException;
import neo.ledger.Blockchain;
import neo.persistence.Snapshot;

public class IssueTransaction extends Transaction {

    public IssueTransaction() {
        super(TransactionType.IssueTransaction);
    }

    @Override
    public Fixed8 getSystemFee() {
        if (version >= 1) {
            return Fixed8.ZERO;
        }
        if (Arrays.stream(outputs)
                .filter(p -> p.assetId == Blockchain.GoverningToken.hash() ||
                        p.assetId == Blockchain.UtilityToken.hash())
                .count() > 0) {
            return Fixed8.ZERO;
        }
        return super.getSystemFee();
    }

    @Override
    protected void deserializeExclusiveData(BinaryReader reader) {
        if (version > 1) {
            throw new FormatException();
        }
    }

    @Override
    public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
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

    @Override
    public boolean verify(Snapshot snapshot, Collection<Transaction> mempool) {
        if (!super.verify(snapshot, mempool)) {
            return false;
        }

//        TransactionResult[] results = getTransactionResults();
//         TODO C# waiting state
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
