package neo.network.p2p.payloads;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.Collection;

import neo.Fixed8;
import neo.UInt160;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.FormatException;
import neo.persistence.Snapshot;

public class ClaimTransaction extends Transaction {

    public CoinReference[] claims;

    public ClaimTransaction() {
        super(TransactionType.ClaimTransaction);
    }

    @Override
    public int size() {
//        TODO Size => base.Size + Claims.GetVarSize();
        return super.size();
    }

    @Override
    public Fixed8 getNetworkFee() {
        return Fixed8.ZERO;
    }

    @Override
    protected void deserializeExclusiveData(BinaryReader reader) {
        if (version != 0) throw new FormatException();
        claims = reader.readArray(CoinReference[]::new, CoinReference::new);
        if (claims.length == 0) throw new FormatException();
    }

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

    @Override
    protected void serializeExclusiveData(BinaryWriter writer) {
        writer.writeArray(claims);
    }

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
