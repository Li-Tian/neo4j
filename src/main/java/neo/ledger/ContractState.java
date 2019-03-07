package neo.ledger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import neo.UInt160;
import neo.csharp.BitConverter;
import neo.csharp.io.BinaryReader;
import neo.csharp.io.BinaryWriter;
import neo.io.ICloneable;
import neo.smartcontract.ContractParameterType;

/**
 * 合约状态
 */
public class ContractState extends StateBase implements ICloneable<ContractState> {

    /**
     * 合约脚本
     */
    public byte[] script;

    /**
     * 合约参数列表
     */
    public ContractParameterType[] parameterList;

    /**
     * 合约返回值类型
     */
    public ContractParameterType returnType;

    /**
     * 合约属性状态
     */
    public ContractPropertyState contractProperties;

    /**
     * 合约名字
     */
    public String name;

    /**
     * 代码版本号
     */
    public String codeVersion;

    /**
     * 作者
     */
    public String author;

    /**
     * 邮箱
     */
    public String email;

    /**
     * 合约描述
     */
    public String description;

    private UInt160 scriptHash;

    /**
     * 是否包含存储空间
     */
    public boolean hasStorage() {
        return contractProperties.hasFlag(ContractPropertyState.HasStorage);
    }

    /**
     * 是否动态调用
     */
    public boolean hasDynamicInvoke() {
        return contractProperties.hasFlag(ContractPropertyState.HasDynamicInvoke);
    }

    /**
     * 是否可支付（保留功能）
     */
    public boolean payable() {
        return contractProperties.hasFlag(ContractPropertyState.Payable);
    }

    /**
     * 存储大小
     */
    @Override
    public int size() {
//        C# code base.Size + Script.GetVarSize() + ParameterList.GetVarSize() + sizeof(ContractParameterType) + sizeof(bool)
//                + Name.GetVarSize() + CodeVersion.GetVarSize() + Author.GetVarSize() + Email.GetVarSize() + Description.GetVarSize();
        return super.size() + BitConverter.getVarSize(script) + BitConverter.getVarSize(parameterList) + ContractParameterType.BYTES + Byte.BYTES +
                BitConverter.getVarSize(name) + BitConverter.getVarSize(codeVersion) + BitConverter.getVarSize(author)
                + BitConverter.getVarSize(email) + BitConverter.getVarSize(description);
    }

    /**
     * 克隆
     *
     * @return 克隆对象
     */
    @Override
    public ContractState copy() {
        ContractState copy = new ContractState();
        copy.script = script;
        copy.parameterList = parameterList;
        copy.returnType = returnType;
        copy.contractProperties = contractProperties;
        copy.name = name;
        copy.codeVersion = codeVersion;
        copy.author = author;
        copy.email = email;
        copy.description = description;
        return copy;
    }

    /**
     * 从参数副本复制数据到此对象
     *
     * @param replica 参数副本
     */
    @Override
    public void fromReplica(ContractState replica) {
        script = replica.script;
        parameterList = replica.parameterList;
        returnType = replica.returnType;
        contractProperties = replica.contractProperties;
        name = replica.name;
        codeVersion = replica.codeVersion;
        author = replica.author;
        email = replica.email;
        description = replica.description;
    }

    /**
     * 合约脚本hash
     */
    public UInt160 getScriptHash() {
        if (scriptHash == null) {
            scriptHash = UInt160.parseToScriptHash(script);
        }
        return scriptHash;
    }

    /**
     * 反序列化
     *
     * @param reader 二进制输入流
     */
    @Override
    public void deserialize(BinaryReader reader) {
        super.deserialize(reader);
        script = reader.readVarBytes();
        parameterList = ContractParameterType.parse(reader.readVarBytes());
        returnType = ContractParameterType.parse((byte) reader.readByte());
        contractProperties = new ContractPropertyState((byte) reader.readByte());
        name = reader.readVarString();
        codeVersion = reader.readVarString();
        author = reader.readVarString();
        email = reader.readVarString();
        description = reader.readVarString();
    }

    /**
     * 序列化
     * <p>序列化字段</p>
     * <ul>
     * <li>StateVersion: 状态版本号</li>
     * <li>Script: 脚本</li>
     * <li>ParameterList: 参数列表</li>
     * <li>ReturnType: 合约脚本返回值类型</li>
     * <li>ContractProperties: 合约属性状态</li>
     * <li>Name: 合约名字</li>
     * <li>Author: 作者</li>
     * <li>Email: 邮箱</li>
     * <li>Description: 描述</li>
     * </ul>
     *
     * @param writer 二进制输出流
     */
    @Override
    public void serialize(BinaryWriter writer) {
        super.serialize(writer);
        writer.writeVarBytes(script);
        writer.writeVarBytes(ContractParameterType.toBytes(parameterList));
        writer.writeByte(returnType.value());
        writer.writeByte(contractProperties.value());
        writer.writeVarString(name);
        writer.writeVarString(codeVersion);
        writer.writeVarString(author);
        writer.writeVarString(email);
        writer.writeVarString(description);
    }

    /**
     * 转成json对象
     *
     * @return 将这个ContractState转化成Json对象返回
     */
    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("hash", getScriptHash().toString());
        json.addProperty("script", BitConverter.toHexString(script));

        JsonArray paramArray = new JsonArray();
        for (int i = 0; i < parameterList.length; i++) {
            paramArray.add(parameterList[i].value());
        }
        json.add("parameters", paramArray);

        json.addProperty("returntype", returnType.value());
        json.addProperty("name", name);
        json.addProperty("code_version", name);
        json.addProperty("author", author);
        json.addProperty("email", email);
        json.addProperty("description", description);

        JsonObject propertyObj = new JsonObject();
        propertyObj.addProperty("storage", hasStorage());
        propertyObj.addProperty("dynamic_invoke", hasDynamicInvoke());
        json.add("properties", propertyObj);
        return json;
    }


}
