package neo.network.p2p.payloads;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import neo.Fixed8;
import neo.UInt256;
import neo.cryptography.MerkleTree;
import neo.csharp.BitConverter;
import neo.csharp.Ulong;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.FormatException;
import neo.ledger.Blockchain;
import neo.ledger.TrimmedBlock;

import neo.log.notr.TR;

/**
 * Block data class, a subclass of BlockBase
 */
public class Block extends BlockBase implements IInventory {

    /**
     * Array of transactions
     */
    public Transaction[] transactions;

    private Header header = null;

    /**
     * message inventory type
     */
    @Override
    public InventoryType inventoryType() {
        return InventoryType.Block;
    }

    /**
     * block header
     */
    public Header getHeader() {
        TR.enter();
        if (header == null) {
            header = new Header();
            header.prevHash = this.prevHash;
            header.merkleRoot = this.prevHash;
            header.timestamp = this.timestamp;
            header.index = this.index;
            header.consensusData = this.consensusData;
            header.nextConsensus = this.nextConsensus;
            header.witness = this.witness;
        }
        return TR.exit(header);
    }

    /**
     * the size of block
     */
    @Override
    public int size() {
        TR.enter();
        // 111 + 1 + txs =>
        return TR.exit(super.size() + BitConverter.getVarSize(transactions));
    }

    /**
     * Calculate the network fee for a batch of transactions. network_fee = input.GAS - output.GAS -
     * input.systemfee
     *
     * @param transactions transaction list
     * @return network fee
     */
    public static Fixed8 calculateNetFee(Collection<Transaction> transactions) {
        TR.enter();
        //        Transaction[] ts = transactions.Where(p = > p.Type != TransactionType.MinerTransaction && p.Type != TransactionType.ClaimTransaction).
        //        ToArray();
        //        Fixed8 amount_in = ts.SelectMany(p = > p.References.Values.Where(o = > o.AssetId == Blockchain.UtilityToken.Hash)).
        //        Sum(p = > p.Value);
        //        Fixed8 amount_out = ts.SelectMany(p = > p.Outputs.Where(o = > o.AssetId == Blockchain.UtilityToken.Hash)).
        //        Sum(p = > p.Value);
        //        Fixed8 amount_sysfee = ts.Sum(p = > p.SystemFee);
        Fixed8 amountIn = Fixed8.ZERO;
        Fixed8 amountOut = Fixed8.ZERO;
        Fixed8 amountSysfee = Fixed8.ZERO;
        for (Transaction tx : transactions) {
            if (tx.type != TransactionType.MinerTransaction && tx.type != TransactionType.ClaimTransaction) {
                for (TransactionOutput input : tx.getReferences().values()) {
                    if (input.assetId.equals(Blockchain.UtilityToken.hash())) {
                        amountIn = Fixed8.add(amountIn, input.value);
                    }
                }
                for (TransactionOutput output : tx.outputs) {
                    if (output.assetId.equals(Blockchain.UtilityToken.hash())) {
                        amountOut = Fixed8.add(amountOut, output.value);
                    }
                }
                amountSysfee = Fixed8.add(amountSysfee, tx.getSystemFee());
            }
        }
        return TR.exit(Fixed8.subtract(Fixed8.subtract(amountIn, amountOut), amountSysfee));
    }


    /**
     * Deserialize method
     *
     * @param reader BinaryReader
     * @throws FormatException An exception will be thrown if one of the following conditions
     *                         occurs:<br/> 1) When the number of transactions is 0,<br/> 2) The
     *                         first transaction is not a miner transaction;<br/>3) Except for the
     *                         first transaction, other transactions are miner transactions;<br/> 4)
     *                         The added transaction hash already exists;<br/>5) Merkel root and the
     *                         calculated values ​​are not equal.
     */
    @Override
    public void deserialize(BinaryReader reader) {
        TR.enter();
        super.deserialize(reader);
        Ulong size = reader.readVarInt(new Ulong(0x10000));
        if (Ulong.ZERO.equals(size)) {
            throw new FormatException();
        }
        transactions = new Transaction[size.intValue()];
        HashSet<UInt256> hashes = new HashSet<UInt256>();
        UInt256[] hashArray = new UInt256[transactions.length];

        for (int i = 0; i < transactions.length; i++) {
            transactions[i] = Transaction.deserializeFrom(reader);
            if (i == 0) {
                if (transactions[0].type != TransactionType.MinerTransaction) {
                    throw new FormatException();
                }
            } else {
                if (transactions[i].type == TransactionType.MinerTransaction)
                    throw new FormatException();
            }
            if (!hashes.add(transactions[i].hash())) {// 不包含
                throw new FormatException();
            }
            hashArray[i] = transactions[i].hash();
        }

        if (!MerkleTree.computeRoot(hashArray).equals(merkleRoot)) {
            throw new FormatException();
        }
        TR.exit();
    }

    /**
     * Rebuild Merkle root
     */
    public void rebuildMerkleRoot() {
        TR.enter();
        UInt256[] hashArray = new UInt256[transactions.length];
        for (int i = 0; i < transactions.length; i++) {
            hashArray[i] = transactions[i].hash();
        }
        merkleRoot = MerkleTree.computeRoot(hashArray);
        TR.exit();
    }

    /**
     * Serialize
     *
     * <p>fields</p>
     * <ul>
     * <li>Version: Version</li>
     * <li>PrevHash: Previous block hash</li>
     * <li>MerkleRoot: Merkle root</li>
     * <li>Timestamp: Timestamps</li>
     * <li>Index: Block height</li>
     * <li>ConsensusData: Consensus data，default block NONCE</li>
     * <li>NextConsensus: Next block consensus address</li>
     * <li>Transactions: Transaction colection</li>
     * </ul>
     *
     * @param writer BinaryWriter
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        super.serialize(writer);
        writer.writeArray(transactions);
        TR.exit();
    }

    /**
     * Convert to JObject object
     */
    @Override
    public JsonObject toJson() {
        TR.enter();
        JsonObject jsonObject = super.toJson();
        JsonArray array = new JsonArray();
        Arrays.stream(transactions).forEach(p -> array.add(p.toJson()));
        jsonObject.add("tr", array);
        return TR.exit(super.toJson());
    }

    /**
     * Determine if the block is equal to another object
     *
     * @param obj another object
     * @return Return true if it is equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        TR.enter();
        if (obj == this) {
            return TR.exit(true);
        }
        if (obj == null || !(obj instanceof Block)) {
            return TR.exit(false);
        }
        Block other = (Block) obj;
        return TR.exit(hash().equals(other.hash()));
    }

    /**
     * Get block hash code
     *
     * @return block hash code
     */
    @Override
    public int hashCode() {
        TR.enter();
        return TR.exit(hash().hashCode());
    }

    /**
     * Convert to trimmed block. Discard the transaction, leaving only the hash of the transaction
     *
     * @return TrimmedBlock object
     */
    public TrimmedBlock trim() {
        TR.enter();
        TrimmedBlock trimmedBlock = new TrimmedBlock();
        trimmedBlock.version = version;
        trimmedBlock.prevHash = prevHash;
        trimmedBlock.merkleRoot = merkleRoot;
        trimmedBlock.timestamp = timestamp;
        trimmedBlock.index = index;
        trimmedBlock.consensusData = consensusData;
        trimmedBlock.nextConsensus = nextConsensus;
        trimmedBlock.witness = witness;
        trimmedBlock.hashes = new UInt256[transactions.length];
        for (int i = 0; i < transactions.length; i++) {
            trimmedBlock.hashes[i] = transactions[i].hash();
        }
        return TR.exit(trimmedBlock);
    }
}
