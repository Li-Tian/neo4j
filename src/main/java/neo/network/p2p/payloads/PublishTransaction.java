package neo.network.p2p.payloads;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.Collection;

import neo.UInt160;
import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.exception.FormatException;
import neo.log.notr.TR;
import neo.persistence.Snapshot;
import neo.smartcontract.ContractParameterType;

/**
 * A transaction for publishing smart contract(given up, please use InvocationTransaction)
 */
@Deprecated
public class PublishTransaction extends Transaction {


    /**
     * The transaction script
     */
    public byte[] script;

    /**
     * The parameter list of contract
     */
    public ContractParameterType[] parameterList = {};

    /**
     * The return value type of contract
     */
    public ContractParameterType returnType;

    /**
     * If the contract need the storage
     */
    public boolean needStorage;

    /**
     * The name of contract
     */
    public String name;

    /**
     * The version of code
     */
    public String codeVersion;

    /**
     * The author
     */
    public String author;

    /**
     * The email of authir
     */
    public String email;

    /**
     * The description of the contract
     */
    public String description;


    private UInt160 scriptHash;


    /**
     * Get script hash
     *
     * @return script hash
     */
    public UInt160 getScriptHash() {
        TR.enter();
        if (scriptHash == null) {
            scriptHash = UInt160.parseToScriptHash(script);
        }
        return TR.exit(scriptHash);
    }

    /**
     * The size for storage
     */
    @Override
    public int size() {
        TR.enter();
        // C# code  Size => base.Size + Script.GetVarSize() + ParameterList.GetVarSize()
        // + sizeof(ContractParameterType) + Name.GetVarSize() + CodeVersion.GetVarSize()
        // + Author.GetVarSize() + Email.GetVarSize() + Description.GetVarSize();
        // 6 + script(1+) + param(1+) + 1 + name(1+) + code(1+) + author(1+) + emai(1+) + desc(1+)
        return TR.exit(super.size() + BitConverter.getVarSize(script) + BitConverter.getVarSize(parameterList)
                + ContractParameterType.BYTES + BitConverter.getVarSize(name)
                + BitConverter.getVarSize(codeVersion) + BitConverter.getVarSize(author)
                + BitConverter.getVarSize(email) + BitConverter.getVarSize(description));
    }

    /**
     * Construct the contract publication transaction
     */
    public PublishTransaction() {
        super(TransactionType.PublishTransaction, PublishTransaction::new);
    }


    /**
     * Deserialization of this transaction exclude the data
     *
     * @param reader The binary input reader
     */
    @Override
    protected void deserializeExclusiveData(BinaryReader reader) {
        TR.enter();
        if (version > 1) throw new FormatException();
        script = reader.readVarBytes();
        parameterList = ContractParameterType.parse(reader.readVarBytes());
        returnType = ContractParameterType.parse((byte) reader.readByte());
        if (version >= 1) {
            needStorage = reader.readBoolean();
        } else {
            needStorage = false;
        }
        name = reader.readVarString(252);
        codeVersion = reader.readVarString(252);
        author = reader.readVarString(252);
        email = reader.readVarString(252);
        description = reader.readVarString(65536);
        TR.exit();
    }


    /**
     * Serialization
     * <p>fields:</p>
     * <ul>
     * <li>Script: The smart contract script</li>
     * <li>parameterList: The list of parameters</li>
     * <li>returnType: THe return value type</li>
     * <li>needStorage: If it need a storage（valid from 1.0 version）</li>
     * <li>name: The name of contract</li>
     * <li>codeVersion: The code of version</li>
     * <li>author: author</li>
     * <li>email: Email</li>
     * <li>description: Description</li>
     * </ul>
     *
     * @param writer The binary output writer
     */
    @Override
    protected void serializeExclusiveData(BinaryWriter writer) {
        TR.enter();
        writer.writeVarBytes(script);
        writer.writeVarBytes(ContractParameterType.toBytes(parameterList));
        writer.writeByte(returnType.value());
        if (version >= 1) {
            writer.writeBoolean(needStorage);
        }
        writer.writeVarString(name);
        writer.writeVarString(codeVersion);
        writer.writeVarString(author);
        writer.writeVarString(email);
        writer.writeVarString(description);
        TR.exit();
    }


    /**
     * Transfer to json object
     *
     * @return json object
     */
    @Override
    public JsonObject toJson() {
        TR.enter();
        JsonObject json = super.toJson();
        JsonObject contract = new JsonObject();
        json.add("contract", contract);

        JsonObject code = new JsonObject();
        contract.add("code", code);

        code.addProperty("hash", getScriptHash().toString());
        code.addProperty("script", BitConverter.toHexString(script));

        JsonArray array = new JsonArray(parameterList.length);
        Arrays.stream(parameterList).forEach(p -> array.add(p.value()));
        code.add("parameters", array);
        code.addProperty("returntype", returnType.value());

        contract.addProperty("needstorage", needStorage);
        contract.addProperty("name", name);
        contract.addProperty("version", version);
        contract.addProperty("author", author);
        contract.addProperty("email", email);
        contract.addProperty("description", description);
        return TR.exit(json);
    }


    /**
     * Verify the transaction script, which is deprecated. Not accept any new publishTransaction
     *
     * @param snapshot database snapshot
     * @param mempool  transactions in mempool
     * @return return the fixed value false, which is deprecated
     */
    @Override
    public boolean verify(Snapshot snapshot, Collection<Transaction> mempool) {
        TR.enter();
        return TR.exit(false);
    }

}
