package neo.consensus;

import java.util.Arrays;

import neo.UInt160;
import neo.UInt256;
import neo.csharp.BitConverter;
import neo.csharp.Ulong;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.FormatException;
import neo.log.notr.TR;
import neo.network.p2p.payloads.MinerTransaction;

/**
 * PrepareRequest Message
 */
public class PrepareRequest extends ConsensusMessage {

    /**
     * Block nonce, random value
     */
    public Ulong nonce;

    /**
     * The script hash of the next round consensus nodes' multi-sign contract
     */
    public UInt160 nextConsensus;

    /**
     * Hash list of the proposal block's transactions
     */
    public UInt256[] transactionHashes;

    /**
     * Miner transanction. It contains block reward for the `Primary` node
     */
    public MinerTransaction minerTransaction;

    /**
     * Signature of the proposal block
     */
    public byte[] signature;


    /**
     * Contractor, create a PrepareRequest message
     */
    public PrepareRequest() {
        super(ConsensusMessageType.PrepareRequest, PrepareRequest::new);
    }

    /**
     * size for storage
     */
    @Override
    public int size() {
        TR.enter();
        return TR.exit(super.size() + Ulong.BYTES + nextConsensus.size() + BitConverter.getVarSize(transactionHashes)
                + minerTransaction.size() + signature.length);
    }

    /**
     * Deserialize from reader
     *
     * @param reader binary reader
     * @throws FormatException if the newViewNumber is zero.
     */
    @Override
    public void deserialize(BinaryReader reader) {
        TR.enter();
        super.deserialize(reader);
        nonce = reader.readUlong();
        nextConsensus = reader.readSerializable(UInt160::new);
        transactionHashes = reader.readArray(UInt256[]::new, UInt256::new);
        if (transactionHashes.length != Arrays.stream(transactionHashes).distinct().count()) {
            throw new FormatException("TransactionHashes can not have repeat data.");
        }
        minerTransaction = reader.readSerializable(MinerTransaction::new);
        if (!minerTransaction.hash().equals(transactionHashes[0])) {
            throw new FormatException("The first transaction hash must be MinerTransaction");
        }
        // TODO hard code 64
        signature = reader.readFully(64);
        TR.exit();
    }

    /**
     * Serialize the message
     *
     * <p>fields:</p>
     * <ul>
     * <li>Type: consensual message type</li>
     * <li>ViewNumber: view number</li>
     * <li>Nonce: block nonce</li>
     * <li>NextConsensus: The script hash of the next round consensus nodes' multi-sign
     * contract</li>
     * <li>TransactionHashes: Hash list of the proposal block's transactions</li>
     * <li>MinerTransaction: Miner transanction</li>
     * <li>signature: block signature</li>
     * </ul>
     *
     * @param writer binary writer
     */
    @Override
    public void serialize(BinaryWriter writer) {
        TR.enter();
        super.serialize(writer);
        writer.writeUlong(nonce);
        writer.writeSerializable(nextConsensus);
        writer.writeArray(transactionHashes);
        writer.writeSerializable(minerTransaction);
        writer.write(signature);
        TR.exit();
    }

}
