package neo.network.p2p.payloads;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import neo.Fixed8;
import neo.UInt160;
import neo.csharp.io.BinaryReader;
import neo.exception.FormatException;
import neo.exception.InvalidOperationException;
import neo.ledger.AssetState;
import neo.ledger.Blockchain;
import neo.log.notr.TR;
import neo.persistence.Snapshot;

/**
 * Issue asset transactions
 */
public class IssueTransaction extends Transaction {

    /**
     * Constructor: create the transaction for issuing asset
     */
    public IssueTransaction() {
        super(TransactionType.IssueTransaction, IssueTransaction::new);
    }

    /**
     * get system fee
     *
     * @return 1) if the transaction VERSION equal or more than 1, then the system fee is 0<br/> 2)
     * If the issued asset is NEO or Gas, then the system fee is 0 <br/> 3) Otherwise, use the basic
     * transaction fee calculation.
     */
    @Override
    public Fixed8 getSystemFee() {
        TR.enter();
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
            return TR.exit(Fixed8.ZERO);
        }
        return TR.exit(super.getSystemFee());
    }

    /**
     * Deserilization exclusive data
     *
     * @param reader The binary input reader
     * @throws FormatException If the VERSION of transactions is larger than 1, then throw
     *                         exceptions.
     */
    @Override
    protected void deserializeExclusiveData(BinaryReader reader) {
        TR.enter();
        if (version > 1) {
            throw new FormatException();
        }
        TR.exit();
    }

    /**
     * Get the script hash of the signature which is waiting for verify
     *
     * @param snapshot database snapshot
     * @return The script hash of the transaction, and the address hash of the issuer.
     * @throws InvalidOperationException Thrown if the issued asset does not exist
     */
    @Override
    public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
        TR.enter();
        UInt160[] hashArray = super.getScriptHashesForVerifying(snapshot);
        HashSet<UInt160> hashSet = new HashSet<>();
        for (int i = 0; i < hashArray.length; i++) {
            hashSet.add(hashArray[i]);
        }
        getTransactionResults().stream().filter(p -> p.amount.compareTo(Fixed8.ZERO) < 0).forEach(result -> {
            AssetState assetState = snapshot.getAssets().tryGet(result.assetId);
            if (assetState == null) {
                throw new InvalidOperationException("assetId %s is not exist", result.assetId.toString());
            }
            hashSet.add(assetState.issuer);
        });
        hashArray = new UInt160[hashSet.size()];
        return TR.exit(hashSet.toArray(hashArray));

        // C# code
        //        foreach(TransactionResult result in GetTransactionResults().Where(p = > p.Amount < Fixed8.Zero))
        //        {
        //            AssetState asset = snapshot.Assets.TryGet(result.AssetId);
        //            if (asset == null) throw new InvalidOperationException();
        //            hashes.Add(asset.Issuer);
        //        }
        //       return hashes.OrderBy(p = > p).ToArray();
    }

    /**
     * The transaction verification
     *
     * @param snapshot database snapshot
     * @param mempool  transactions in mempool
     * @return <ul>
     * <li>1. verify the basic transactions, if verify failed, return false</li>
     * <li>2. If the input of transactions references not exist, return false</li>
     * <li>3. If the asset issued not exist return false</li>
     * <li>4. If the asset issued is negative return false</li>
     * <li>5 If the sum of the amount of this transaction  and other transactions in  mempool
     * exceeds the total issue amount , then return false</li>
     * </ul>
     */
    @Override
    public boolean verify(Snapshot snapshot, Collection<Transaction> mempool) {
        TR.enter();
        if (!super.verify(snapshot, mempool)) {
            return TR.exit(false);
        }
        Collection<TransactionResult> results = getTransactionResults();
        if (results == null) {
            return TR.exit(false);
        }
        for (TransactionResult result : results) {
            if (result.amount.compareTo(Fixed8.ZERO) >= 0) {
                continue;
            }

            AssetState asset = snapshot.getAssets().tryGet(result.assetId);
            if (asset == null) {
                return TR.exit(false);
            }
            if (asset.amount.compareTo(Fixed8.ZERO) < 0) {
                continue;
            }
            Fixed8 quantity_issued = asset.available;
            for (Transaction tx : mempool) {
                if (tx instanceof IssueTransaction && tx != this) {
                    for (TransactionOutput output : tx.outputs) {
                        if (output.assetId.equals(asset.assetId)) {
                            quantity_issued = Fixed8.add(quantity_issued, output.value);
                        }
                    }
                }
            }
            Fixed8 rest = Fixed8.subtract(asset.amount, quantity_issued);
            if (rest.compareTo(Fixed8.negate(result.amount)) < 0) {
                // the available amount of this asset is not enough
                return TR.exit(false);
            }
        }
        return TR.exit(true);

        // C# code
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
    }
}
