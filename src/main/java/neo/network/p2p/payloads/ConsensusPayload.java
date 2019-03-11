package neo.network.p2p.payloads;

import neo.UInt160;
import neo.UInt256;
import neo.cryptography.Crypto;
import neo.cryptography.ecc.ECPoint;
import neo.csharp.BitConverter;
import neo.csharp.Uint;
import neo.csharp.Ushort;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.FormatException;
import neo.exception.InvalidOperationException;
import neo.persistence.Snapshot;
import neo.smartcontract.Contract;

/**
 * P2p consensus message payload consensus message data（Wrap specific consensus messages. When p2p
 * broadcasts, it will be stored in the payload of the Inventory message.）
 */
public class ConsensusPayload implements IInventory {

    /**
     * Consensus message version
     */
    public Uint version;

    /**
     * The previous block hash
     */
    public UInt256 prevHash;

    /**
     * The proposal block index
     */
    public Uint blockIndex;

    /**
     * The sender(the Speaker or Delegates) index in the validators array
     */
    public Ushort validatorIndex;

    /**
     * Block timestamp
     */
    public Uint timestamp;

    /**
     * Consensus message data
     */
    public byte[] data;

    /**
     * Witness, the executable verification script
     */
    public Witness witness;

    private UInt256 hash = null;

    /**
     * Hash data of the GetHashData( = unsigned data) by using hash256 algorithm
     */
    @Override
    public UInt256 hash() {
        if (hash == null) {
            hash = new UInt256(Crypto.Default.hash256(this.getHashData()));
        }
        return hash;
    }

    /**
     * P2p inventory message type, equals to InventoryType.Consensus
     */
    @Override
    public InventoryType inventoryType() {
        return InventoryType.Consensus;
    }

    /**
     * Verify this payload
     * <note>
     * 1) Check if BlockIndex is more than the snapshot.Height 2) Verify the witness script
     * </note>
     *
     * @param snapshot database snapshot
     * @return Returns true if the validation is passed,  otherwise return false
     */
    @Override
    public boolean verify(Snapshot snapshot) {
        if (blockIndex.compareTo(snapshot.getHeight()) < 0) {
            return false;
        }
        return IVerifiable.verifyWitnesses(this, snapshot);
    }

    /**
     * get witnesses
     *
     * @return witness array
     */
    @Override
    public Witness[] getWitnesses() {
        return new Witness[]{witness};
    }

    @Override
    public byte[] getHashData() {
        return new byte[0];
    }

    /**
     * set witnesses
     */
    @Override
    public void setWitnesses(Witness[] witnesses) {
        if (witnesses.length != 1) {
            throw new IllegalArgumentException();
        }
        witness = witnesses[0];
    }

    /**
     * Deserialize from the reader of the unsigned binary data without the witness field
     *
     * @param reader BinaryReader
     */
    @Override
    public void deserializeUnsigned(BinaryReader reader) {
        version = reader.readUint();
        prevHash = reader.readSerializable(UInt256::new);
        blockIndex = reader.readUint();
        validatorIndex = reader.readUshort();
        timestamp = reader.readUint();
        data = reader.readVarBytes();
    }

    /**
     * Get the verification scripts' hashes
     *
     * @param snapshot Database Snapshot
     * @return The script hash of the sender's signing contract
     */
    @Override
    public UInt160[] getScriptHashesForVerifying(Snapshot snapshot) {
        ECPoint[] validators = snapshot.getValidatorPubkeys();
        if (validators.length < validatorIndex.intValue()) {
            throw new InvalidOperationException();
        }
        byte[] scriptBytes = Contract.createSignatureRedeemScript(validators[validatorIndex.intValue()]);
        UInt160 scriptHash = UInt160.parseToScriptHash(scriptBytes);
        return new UInt160[]{scriptHash};
    }

    /**
     * Serialize the unsigned message. It includes the fields as follows:
     * <ul>
     * <li>Version: consensus message version, current is zero</li>
     * <li>PrevHash: the previous block hash</li>
     * <li>BlockIndex: the proposal block index</li>
     * <li>ValidatorIndex: the sender(the Speaker or Delegates) index in the validators array</li>
     * <li>Timestamp: block time stamp</li>
     * <li>Data: consensus message data</li>
     * </ul>
     *
     * @param writer BinaryWriter
     */
    @Override
    public void serializeUnsigned(BinaryWriter writer) {
        writer.writeUint(version);
        writer.writeSerializable(prevHash);
        writer.writeUint(blockIndex);
        writer.writeUshort(validatorIndex);
        writer.writeUint(timestamp);
        writer.writeVarBytes(data);
    }

    /**
     * ConsensusPayload size
     */
    @Override
    public int size() {
        // C# Size => sizeof(uint) + PrevHash.Size + sizeof(uint) + sizeof(ushort) + sizeof(uint)
        // + Data.GetVarSize() + 1 + Witness.Size;
        return Uint.BYTES + prevHash.size() + Uint.BYTES + Ushort.BYTES + Uint.BYTES
                + BitConverter.getVarSize(data) + 1 + witness.size();
    }

    /**
     * Serialize the message. It includes the fields as follows:
     * <ul>
     * <li>Version: consensus message version, current is zero</li>
     * <li>PrevHash: the previous block hash</li>
     * <li>BlockIndex: the proposal block index</li>
     * <li>ValidatorIndex: the sender(the Speaker or Delegates) index in the validators array</li>
     * <li>Timestamp: block time stamp</li>
     * <li>Data: consensus message data</li>
     * <li>1: Witness, the executable verification script</li>
     * </ul>
     *
     * @param writer BinaryWriter
     */
    @Override
    public void serialize(BinaryWriter writer) {
        this.serializeUnsigned(writer);
        writer.writeByte((byte) 1);
        writer.writeSerializable(witness);
    }

    /**
     * Deserialize from the reader
     *
     * @param reader BinaryReader
     */
    @Override
    public void deserialize(BinaryReader reader) {
        this.deserializeUnsigned(reader);
        if (reader.readByte() != 1) {
            throw new FormatException();
        }
        witness = reader.readSerializable(Witness::new);
    }

    /**
     * Get hash data
     *
     * @return hash data
     */
    @Override
    public byte[] getMessage() {
        return this.getHashData();
    }
}
