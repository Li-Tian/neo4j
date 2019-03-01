package neo.ledger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.bouncycastle.math.ec.ECPoint;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import neo.UInt256;
import neo.UInt160;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.io.ICloneable;
import neo.Fixed8;

import neo.log.tr.TR;

public class AccountState extends StateBase implements ICloneable<AccountState> {
    public UInt160 scriptHash;
    public boolean isFrozen;
    public ECPoint[] votes;
    public ConcurrentHashMap<UInt256, Fixed8> balances;


    @Override
    public int size() {
        TR.enter();
        return super.size();
        // C# TODO waiting for ECPoint
//        return TR.exit(super.size() + scriptHash.size() + Byte.BYTES + Votes.GetVarSize() + Integer.SIZE + balances.size() * (32 + 8));
    }

    public AccountState() {
        TR.enter();
        TR.exit();
    }

    public AccountState(UInt160 hash) {
        TR.enter();
        this.scriptHash = hash;
        this.isFrozen = false;
        this.votes = new ECPoint[0];
        this.balances = new ConcurrentHashMap<>();
        TR.exit();
    }

    @Override
    public AccountState copy() {
        TR.enter();
        AccountState result = new AccountState();
        result.scriptHash = this.scriptHash;
        result.isFrozen = this.isFrozen;
        result.votes = this.votes;
        result.balances = new ConcurrentHashMap<>(this.balances);
        return TR.exit(result);
    }

    @Override
    public void deserialize(BinaryReader reader) {
        TR.enter();
        super.deserialize(reader);
        scriptHash = reader.readSerializable(UInt160::new);
        isFrozen = reader.readBoolean();
        votes = new ECPoint[reader.readVarInt().intValue()];
        for (int i = 0; i < votes.length; i++) {
//            votes[i] = ECPoint.DeserializeFrom(reader, ECCurve.Secp256r1);
//            votes[i] = reader.readSerializable(ECPoint::new);
        }
        int count = (int) reader.readVarInt().intValue();
        balances = new ConcurrentHashMap<UInt256, Fixed8>(count);
        for (int i = 0; i < count; i++) {
            UInt256 assetId = reader.readSerializable(UInt256::new);
            Fixed8 value = reader.readSerializable(Fixed8::new);
            balances.put(assetId, value);
        }
        TR.exit();
    }

    @Override
    public void fromReplica(AccountState replica) {
        TR.enter();
        scriptHash = replica.scriptHash;
        isFrozen = replica.isFrozen;
        votes = replica.votes;
        balances = replica.balances;
        TR.exit();
    }

    public Fixed8 getBalance(UInt256 asset_id) {
        TR.enter();
        Fixed8 value = balances.get(asset_id);
        if (value == null) {
            value = Fixed8.ZERO;
        }
        return TR.exit(value);
    }

    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        super.serialize(writer);
        writer.writeSerializable(scriptHash);
        writer.writeBoolean(isFrozen);
        //writer.write(votes); // TODO waiting for ECPoint
        //var balances = Balances.Where(p => p.Value > Fixed8.Zero).ToArray();

        List<Map.Entry<UInt256, Fixed8>> validBalances = balances.entrySet().stream().
                filter(x -> x.getValue().compareTo(Fixed8.ZERO) > 0).collect(Collectors.toList());
        writer.writeVarInt(validBalances.size());
        validBalances.forEach(entry -> {
            writer.writeSerializable(entry.getKey());
            writer.writeSerializable(entry.getValue());
        });
        TR.exit();
    }

    public JsonObject toJson() {
        TR.enter();
        JsonObject json = super.toJson();
        json.addProperty("script_hash", scriptHash.toString());
        json.addProperty("frozen", isFrozen);
        //LINQ
        //json["votes"] = new JArray(votes.Select(p = > (JObject) p.ToString()));
        JsonArray votesArray = new JsonArray(votes.length);
        for (ECPoint vote : votes) {
            // TODO waiting ECPoint /..
            votesArray.add(vote.toString());
        }
        json.add("votes", votesArray);

        JsonArray balanceArray = new JsonArray(votes.length);
        for (Map.Entry<UInt256, Fixed8> entry : balances.entrySet()) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("asset", entry.getKey().toString());
            jsonObject.addProperty("value", entry.getValue().toString());
        }
        json.add("balances", balanceArray);
        return TR.exit(json);
    }
}