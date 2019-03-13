package neo.network.p2p.payloads;

import com.google.gson.JsonObject;

import java.awt.*;
import java.math.BigDecimal;
import java.util.Collection;

import neo.Fixed8;
import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.FormatException;
import neo.log.notr.TR;
import neo.persistence.Snapshot;

/**
 * Invocation of the transaction, which execute the scripts and contracts, including deploying and
 * executing smart contract
 */
public class InvocationTransaction extends Transaction {

    /**
     * The hash script
     */
    public byte[] script;

    /**
     * Gas consumption
     */
    public Fixed8 gas;

    /**
     * The construct function: creating the InvocationTransaction
     */
    public InvocationTransaction() {
        super(TransactionType.InvocationTransaction, InvocationTransaction::new);
    }

    /**
     * The size of storage
     */
    @Override
    public int size() {
        TR.enter();
        return TR.exit(super.size() + BitConverter.getVarSize(script));
    }

    /**
     * Serialization
     * <p>fields:</p>
     * <ul>
     * <li>Script: The script waiting to be executed</li>
     * <li>Gas: If the version of transaction is larger than 1, then serialize this data</li>
     * </ul>
     *
     * @param writer The binary output writer
     */
    @Override
    protected void serializeExclusiveData(BinaryWriter writer) {
        TR.enter();
        writer.writeVarBytes(script);
        if (version >= 1) {
            writer.writeSerializable(gas);
        }
        TR.exit();
    }

    /**
     * Deserialization function which exclude the data
     *
     * @param reader The binary input reader
     * @throws FormatException 1. If the transaction version is larger than 1 then throw this
     *                         exception<br/> 2. The transction script's length is equal to 0<br/>
     *                         3. The gas consumption of smart contract invocation is smaller than
     *                         0.
     * @note When the version is 0， do not need gas. The default value is 0<br/> When the version is
     * 1, need set the gas <br/>
     */
    @Override
    protected void deserializeExclusiveData(BinaryReader reader) {
        TR.enter();
        if (version > 1) throw new FormatException();
        script = reader.readVarBytes(65536);
        if (script.length == 0) {
            throw new FormatException();
        }
        if (version >= 1) {
            gas = reader.readSerializable(Fixed8::new);
            if (gas.compareTo(Fixed8.ZERO) < 0) {
                throw new FormatException();
            }
        } else {
            gas = Fixed8.ZERO;
        }
        TR.exit();
    }

    /**
     * get system fee
     *
     * @return system fee = gas
     */
    @Override
    public Fixed8 getSystemFee() {
        TR.enter();
        return TR.exit(gas);
    }

    /**
     * Get the gas consumption
     *
     * @param consumed The real consumpt gas
     * @return GThe Gas consumption is equal to the comsumed gas minus the free 10 Gas. If the gas
     * consumption is small than 0, then return 0 Finally use the ceil of the gas.
     */
    public static Fixed8 getGas(Fixed8 consumed) {
        TR.enter();
        // c3 code Fixed8 gas = consumed - Fixed8.FromDecimal(10); // TODO 10 is hard code
        Fixed8 gas = Fixed8.subtract(consumed, Fixed8.fromDecimal(BigDecimal.valueOf(10)));
        if (gas.compareTo(Fixed8.ZERO) <= 0) return TR.exit(Fixed8.ZERO);
        return TR.exit(gas.ceiling());
    }


    /**
     * Verify the transaction
     *
     * @param snapshot Snapshot of database
     * @param mempool  Memory pool of transactions
     * @return 1. If the consuming gas can not be divide by 10^8 ,return false.(e.g. The gas must be
     * the integer format of Fixed8, which means there is not decimal Gas.)<br/>  2. The basic
     * verification of the transaction invocation. If not verified, return false.
     */
    @Override
    public boolean verify(Snapshot snapshot, Collection<Transaction> mempool) {
        TR.enter();
        if (gas.getData() % 100000000 != 0) {
            return TR.exit(false);
        }
        return TR.exit(super.verify(snapshot, mempool));
    }

    /**
     * Transfer to json object
     *
     * @return Json object
     */
    @Override
    public JsonObject toJson() {
        TR.enter();
        JsonObject jsonObject = super.toJson();
        jsonObject.addProperty("script", BitConverter.toHexString(script));
        jsonObject.addProperty("gas", gas.toString());
        return TR.exit(jsonObject);
    }
}
