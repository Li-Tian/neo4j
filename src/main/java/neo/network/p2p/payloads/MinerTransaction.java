package neo.network.p2p.payloads;

import com.google.gson.JsonObject;

import neo.Fixed8;
import neo.csharp.Uint;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.FormatException;
import neo.ledger.Blockchain;
import neo.log.notr.TR;

/**
 * Miner Transaction，It is a reward for giving speaker consensus node. At the same time, as the
 * first transaction for each block.
 *
 * @note MinerTransaction's inputs must be empty, and outputs can only have GAS asset.
 */
public class MinerTransaction extends Transaction {

    /**
     * TODO 由于 Nonce 的值是 2的32次方，因此在300万个区块中有很高的概率会使得生成的nonce相同，
     * 从而使得两个交易的哈希值完全相同。最好的解决方法是在MinerTransaction中添加区块高度。
     */

    /**
     * transaction nonce
     */
    public Uint nonce = Uint.ZERO;

    /**
     * make a miner transaction
     */
    public MinerTransaction() {
        super(TransactionType.MinerTransaction);
    }

    /**
     * storage size
     */
    @Override
    public int size() {
        TR.enter();
        return super.size() + Uint.BYTES;
    }

    /**
     * NetworkFee，default value is 0
     */
    @Override
    public Fixed8 getNetworkFee() {
        TR.enter();
        return Fixed8.ZERO;
    }

    /**
     * Deserialize exclusive data，read nonce
     *
     * @param reader BinaryReader
     */
    @Override
    protected void deserializeExclusiveData(BinaryReader reader) {
        TR.enter();
        if (version != 0) throw new FormatException();
        this.nonce = reader.readUint();
    }

    /**
     * Handling deserialized transactions
     *
     * @throws FormatException If input of miner transaction is not null or asset is not gass.
     */
    @Override
    protected void onDeserialized() {
        TR.enter();
        super.onDeserialized();
        if (inputs.length != 0)
            throw new FormatException();
        for (TransactionOutput output : outputs) {
            if (!output.assetId.equals(Blockchain.UtilityToken.hash())) {
                throw new FormatException("MinerTransaction's output can only have gas asset");
            }
        }
    }

    /**
     * Serialize exclusive data, the following is the fields:
     * <ul>
     * <li>Nonce: transaction nonce</li>
     * </ul>
     *
     * @param writer BinaryWriter
     */
    @Override
    protected void serializeExclusiveData(BinaryWriter writer) {
        TR.enter();
        writer.writeUint(nonce);
    }

    /**
     * Convert to JObject object
     *
     * @return a JObject object
     */
    @Override
    public JsonObject toJson() {
        TR.enter();
        JsonObject json = super.toJson();
        json.addProperty("nonce", nonce);
        return json;
    }
}
