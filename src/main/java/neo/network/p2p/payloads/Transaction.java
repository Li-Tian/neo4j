package neo.network.p2p.payloads;


import java.io.ByteArrayOutputStream;
import java.util.HashMap;

import neo.Fixed8;
import neo.ProtocolSettings;
import neo.UInt256;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.io.ICloneable;

public abstract class Transaction implements IInventory, ICloneable<Transaction> {

    public static final int MaxTransactionSize = 102400;

    /**
     * Maximum number of attributes that can be contained within a transaction
     */
    private static final int MaxTransactionAttributes = 16;


    public final TransactionType type;
    public byte version;
    public TransactionAttribute[] attributes;
    public CoinReference inputs;
    public TransactionOutput outputs;
    public Witness[] witnesses;

    private Fixed8 feePerByte = Fixed8.negate(Fixed8.SATOSHI);
    private Fixed8 networkFee = Fixed8.negate(Fixed8.SATOSHI);
    private HashMap<CoinReference, TransactionOutput> references;
    private UInt256 hash = null;

    public Transaction(TransactionType type) {
        this.type = type;
    }


    @Override
    public UInt256 hash() {
        return hash;
    }

    @Override
    public InventoryType inventoryType() {
        return InventoryType.Tr;
    }

    @Override
    public void fromReplica(Transaction replica) {
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public void serialize(BinaryWriter writer) {

    }

    @Override
    public void deserialize(BinaryReader reader) {

    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null) return false;
        if (!(obj instanceof Transaction)) return false;

        Transaction other = (Transaction) obj;
        return this.hash().equals(other.hash());
    }

    @Override
    public int hashCode() {
        return hash().hashCode();
    }

    @Override
    public byte[] GetMessage() {
        return this.getHashData();
    }

    public boolean isLowPriority() {
        //
        return false;
    }

    public Fixed8 systemFee() {
        Fixed8 fee = ProtocolSettings.Default.systemFee.get(type);
        return fee == null ? Fixed8.ZERO : fee;
    }


    private boolean verifyReceivingScripts() {
        //TODO: run ApplicationEngine
        //foreach (UInt160 hash in Outputs.Select(p => p.ScriptHash).Distinct())
        //{
        //    ContractState contract = Blockchain.Default.GetContract(hash);
        //    if (contract == null) continue;
        //    if (!contract.Payable) return false;
        //    using (StateReader service = new StateReader())
        //    {
        //        ApplicationEngine engine = new ApplicationEngine(TriggerType.VerificationR, this, Blockchain.Default, service, Fixed8.Zero);
        //        engine.LoadScript(contract.Script, false);
        //        using (ScriptBuilder sb = new ScriptBuilder())
        //        {
        //            sb.EmitPush(0);
        //            sb.Emit(OpCode.PACK);
        //            sb.EmitPush("receiving");
        //            engine.LoadScript(sb.ToArray(), false);
        //        }
        //        if (!engine.Execute()) return false;
        //        if (engine.EvaluationStack.Count != 1 || !engine.EvaluationStack.Pop().GetBoolean()) return false;
        //    }
        //}
        return true;
    }


    @Override
    public byte[] getHashData() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BinaryWriter writer = new BinaryWriter(outputStream);
        serializeUnsigned(writer);
        writer.flush();
        return outputStream.toByteArray();
    }
}
