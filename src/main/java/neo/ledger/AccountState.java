package neo.ledger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;


import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import neo.UInt256;
import neo.UInt160;
import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.io.ICloneable;
import neo.Fixed8;
import neo.cryptography.ecc.ECPoint;

import neo.log.tr.TR;

/**
 * 用户状态
 */
public class AccountState extends StateBase implements ICloneable<AccountState> {

    /**
     * 脚本合约hash
     */
    public UInt160 scriptHash;

    /**
     * 账户是否冻结
     */
    public boolean isFrozen;

    /**
     * 投票列表
     */
    public ECPoint[] votes;

    /**
     * 全局资产余额
     */
    public ConcurrentHashMap<UInt256, Fixed8> balances;


    /**
     * 构造函数
     */
    public AccountState() {
        TR.enter();
        this.isFrozen = false;
        this.votes = new ECPoint[0];
        this.balances = new ConcurrentHashMap<>();
        TR.exit();
    }

    /**
     * 构造函数
     *
     * @param hash 脚本hash
     */
    public AccountState(UInt160 hash) {
        TR.enter();
        this.scriptHash = hash;
        this.isFrozen = false;
        this.votes = new ECPoint[0];
        this.balances = new ConcurrentHashMap<>();
        TR.exit();
    }

    @Override
    public int size() {
        TR.enter();
        // 1 + 20 + 1 + ? + 1 + 2 * 40 => 103 + ?  ？ = 1 + 1
        return TR.exit(super.size() + scriptHash.size() + Byte.BYTES
                + BitConverter.getVarSize(votes) + BitConverter.getVarSize(balances.size()) + balances.size() * (32 + 8));
    }

    /**
     * 克隆
     *
     * @return 克隆对象
     */
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

    /**
     * 反序列化
     *
     * @param reader 二进制输入流
     */
    @Override
    public void deserialize(BinaryReader reader) {
        TR.enter();
        super.deserialize(reader);
        scriptHash = reader.readSerializable(UInt160::new);
        isFrozen = reader.readBoolean();
        votes = new ECPoint[reader.readVarInt().intValue()];
        for (int i = 0; i < votes.length; i++) {
            votes[i] = reader.readSerializable(ECPoint::new);
        }
        int count = reader.readVarInt().intValue();
        balances = new ConcurrentHashMap<>(count);
        for (int i = 0; i < count; i++) {
            UInt256 assetId = reader.readSerializable(UInt256::new);
            Fixed8 value = reader.readSerializable(Fixed8::new);
            balances.put(assetId, value);
        }
        TR.exit();
    }

    /**
     * 从副本拷贝值
     *
     * @param replica 副本
     */
    @Override
    public void fromReplica(AccountState replica) {
        TR.enter();
        scriptHash = replica.scriptHash;
        isFrozen = replica.isFrozen;
        votes = replica.votes;
        balances = replica.balances;
        TR.exit();
    }

    /**
     * 查询资产剩余
     *
     * @param asset_id 资产ID
     * @return 资产余额，若没有查询到时，返回0
     */
    public Fixed8 getBalance(UInt256 asset_id) {
        TR.enter();
        Fixed8 value = balances.get(asset_id);
        if (value == null) {
            value = Fixed8.ZERO;
        }
        return TR.exit(value);
    }

    public synchronized void increaseBalance(UInt256 asset_id, Fixed8 amount) {
        Fixed8 total = amount;
        if (balances.containsKey(asset_id)) {
            Fixed8 rest = balances.get(asset_id);
            total = Fixed8.add(amount, rest);
        }
        balances.put(asset_id, total);
    }

    /**
     * 序列化
     * <p>序列化字段</p>
     * <ul>
     * <li>scriptHash: 脚本合约hash</li>
     * <li>isFrozen: 账户是否冻结</li>
     * <li>votes: 投票列表</li>
     * <li>balances: 全局资产余额</li>
     * </ul>
     *
     * @param writer 二进制输出流
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        super.serialize(writer);
        writer.writeSerializable(scriptHash);
        writer.writeBoolean(isFrozen);
        writer.writeArray(votes);
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

    /**
     * 转成json对象
     *
     * @return 格式： { 'script_hash': 'xxxx', 'frozen': false, 'votes':['xxxx'], 'balances':{
     * 'asset_xxx': 'value...' } }
     */
    public JsonObject toJson() {
        TR.enter();
        JsonObject json = super.toJson();
        json.addProperty("script_hash", scriptHash.toString());
        json.addProperty("frozen", isFrozen);
        //LINQ
        //json["votes"] = new JArray(votes.Select(p = > (JObject) p.ToString()));
        JsonArray votesArray = new JsonArray(votes.length);
        Arrays.stream(votes).forEach(p -> votesArray.add(p.toString()));
        json.add("votes", votesArray);

        JsonArray balanceArray = new JsonArray(votes.length);
        for (Map.Entry<UInt256, Fixed8> entry : balances.entrySet()) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("asset", entry.getKey().toString());
            jsonObject.addProperty("value", entry.getValue().toString());
            balanceArray.add(jsonObject);
        }
        json.add("balances", balanceArray);
        return TR.exit(json);
    }
}