package neo.network.p2p.payloads;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;

import neo.Fixed8;
import neo.UInt160;
import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.FormatException;
import neo.exception.InvalidOperationException;
import neo.ledger.Blockchain;
import neo.log.tr.TR;
import neo.persistence.Snapshot;

/**
 * Claim transaction, used to claim GAS
 */
public class ClaimTransaction extends Transaction {

    /**
     * the outputs collection of spent GAS
     */
    public CoinReference[] claims;

    /**
     * NetworkFee，the default is 0
     */
    public ClaimTransaction() {
        super(TransactionType.ClaimTransaction);
    }

    /**
     * size for storage
     */
    @Override
    public int size() {
        TR.enter();
        return TR.exit(super.size() + BitConverter.getVarSize(claims));
    }

    /**
     * NetworkFee，the default is 0
     */
    @Override
    public Fixed8 getNetworkFee() {
        TR.enter();
        return TR.exit(Fixed8.ZERO);
    }

    /**
     * Deserialize method. Read the claims data in binary reader, other data is not extracted
     *
     * @param reader BinaryReader
     * @throws FormatException Throws an exception when the transaction version number is not 0, or
     *                         the length of claim data is 0.
     */
    @Override
    protected void deserializeExclusiveData(BinaryReader reader) {
        TR.enter();
        if (version != 0) throw new FormatException();
        claims = reader.readArray(CoinReference[]::new, CoinReference::new);
        if (claims.length == 0) throw new FormatException();
        TR.exit();
    }

    /**
     * get verify script hashes
     *
     * @param snapshot database snapshot
     * @return verify script hashes list.Includes the payee address pointed to by output.Sort by
     * hash value.
     * @throws InvalidOperationException If the referenced output doe not exist, throw this
     *                                   exception
     */
    @Override
    public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
        TR.enter();
        UInt160[] hashes = super.getScriptHashesForVerifying(snapshot);
        HashSet<UInt160> hashSet = new HashSet(Arrays.asList(hashes));

        Arrays.stream(claims).collect(Collectors.groupingBy(p -> p.prevHash)).forEach((key, group) -> {
            Transaction tx = snapshot.getTransaction(key);
            if (tx == null) throw new InvalidOperationException();
            for (CoinReference claim : group) {
                if (tx.outputs.length <= claim.prevIndex.intValue()) {
                    throw new InvalidOperationException();
                }
                hashSet.add(tx.outputs[claim.prevIndex.intValue()].scriptHash);
            }
        });
        hashes = new UInt160[hashSet.size()];
        hashSet.toArray(hashes);
        Arrays.sort(hashes);
        return TR.exit(hashes);
    }

    /**
     * Serialize
     * <p>fields:</p>
     * <ul>
     * <li>Claims: the outputs of spent GAS</li>
     * </ul>
     *
     * @param writer BinaryWriter
     */
    @Override
    protected void serializeExclusiveData(BinaryWriter writer) {
        TR.enter();
        writer.writeArray(claims);
    }

    /**
     * Convert to a JObject object
     *
     * @return JObject object
     */
    @Override
    public JsonObject toJson() {
        TR.enter();
        JsonObject json = super.toJson();

        JsonArray array = new JsonArray(claims.length);
        for (CoinReference claim : claims) {
            array.add(claim.toJson());
        }
        json.add("claims", array);
        return TR.exit(json);
    }

    /**
     * Verify transcation
     *
     * @param snapshot database snapshot
     * @param mempool  transaction mempool
     * @return return following：<br/>
     * <ul>
     * <li>1. Perform basic verification of the transaction, return false if the verification
     * fails.</li>
     * <li>2. If the claims data contains duplicate transactions, return false.</li>
     * <li>3. If the claims data overlaps with transactions in mempool , return false.</li>
     * <li>4. If this claim transaction references a non-existent Output then return false</li>
     * <li>5. If the sum of the input GAS of this claim transaction is greater than or equal to the
     * sum of the output GAS, return false.</li>
     * <li>6. If the amount of GAS calculated by the claim transcation reference is not equal to
     * the amount of GAS declared by the claim transcation, return false. </li>
     * <li> 7. If the processing is abnormal, return false. </li>
     * </ul>
     */
    @Override
    public boolean verify(Snapshot snapshot, Collection<Transaction> mempool) {
        TR.enter();
        if (!super.verify(snapshot, mempool)) {
            return TR.exit(false);
        }
        if (claims.length != Arrays.stream(claims).distinct().count()) {
            return TR.exit(false);
        }
        // check whether the claimSet already contain the claims
        HashSet<CoinReference> claimSet = new HashSet<>();
        for (Transaction tx : mempool) {
            if (tx instanceof ClaimTransaction) {
                ClaimTransaction claimtx = (ClaimTransaction) tx;
                for (int i = 0; i < claimtx.claims.length; i++) {
                    claimSet.add(claimtx.claims[i]);
                }
            }
        }
        for (CoinReference claim : claims) {
            if (claimSet.contains(claim)) {
                return TR.exit(false);
            }
        }

        Optional<TransactionResult> optional = getTransactionResults()
                .stream()
                .filter(p -> p.assetId.equals(Blockchain.UtilityToken.hash()))
                .findAny();
        if (!optional.isPresent() || optional.get().amount.compareTo(Fixed8.ZERO) > 0) {
            // gas amount must more than zero.
            return TR.exit(false);
        }
        TransactionResult result = optional.get();
        return TR.exit(snapshot.calculateBonus(Arrays.asList(claims), false).equals(Fixed8.negate(result.amount)));

        // C# code:
        //        if (mempool.OfType < ClaimTransaction > ().Where(p = > p != this).
        //        SelectMany(p = > p.Claims).
        //        Intersect(Claims).Count() > 0)
        //        return false;
        //        TransactionResult result = GetTransactionResults().FirstOrDefault(p = > p.AssetId == Blockchain.UtilityToken.Hash);
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
