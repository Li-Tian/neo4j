package neo.Wallets.NEP6;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import neo.csharp.BitConverter;
import neo.log.notr.TR;
import neo.smartcontract.Contract;
import neo.smartcontract.ContractParameterType;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: NEP6Contract
 * @Package neo.Wallets.NEP6
 * @Description: NEP6合约类
 * @date Created in 14:08 2019/3/14
 */
public class NEP6Contract extends Contract {
    //参数名称
    public String[] parameterNames;

    //是否部署
    public boolean deployed;

    /**
     * @Author:doubi.liu
     * @description:默认构造函数
     * @date:2019/4/2
     */
    public NEP6Contract() {
    }



    /**
     * @param json 对象
     * @Author:doubi.liu
     * @description:从json对象中生成NEP6Contract对象
     * @date:2019/4/2
     */
    public static NEP6Contract fromJson(JsonObject json) {
        TR.enter();
        if (json == null) {
            return TR.exit(null);
        }
        //LINQ START
        NEP6Contract tempContract = new NEP6Contract();
        tempContract.script = BitConverter.hexToBytes(json.get("script").getAsString());
        List<ContractParameterType> templist = new ArrayList<>();
        List<String> templist2 = new ArrayList<>();
        json.getAsJsonArray("parameters").forEach
                (p -> templist.add(ContractParameterType.valueOf(p.getAsJsonObject
                        ().get("type").getAsString())));
        json.getAsJsonArray("parameters").forEach
                (p -> templist2.add(p.getAsJsonObject().get("name").getAsString()));
        tempContract.parameterList = templist.toArray(new ContractParameterType[0]);
        tempContract.parameterNames = templist2.toArray(new String[0]);
        tempContract.deployed = json.get("deployed").getAsBoolean();
        return TR.exit(tempContract);
/*        {
            script = json["script"].AsString().HexToBytes(),
                    ParameterList = ((JArray)json["parameters"]).Select(p => p["type"].AsEnum<ContractParameterType>()).ToArray(),
                ParameterNames = ((JArray)json["parameters"]).Select(p => p["name"].AsString()).ToArray(),
                Deployed = json["deployed"].AsBoolean()
        };*/
        //LINQ END


    }

    /**
     * @Author:doubi.liu
     * @description: 转json方法
     * @date:2019/4/2
     */
    public JsonObject toJson() {
        TR.enter();
        JsonObject contract = new JsonObject();
        contract.addProperty("script", BitConverter.toHexString(script));

        //LINQ START
        JsonArray tempArray = new JsonArray();
        for (int i = 0; i < parameterList.length; i++) {
            JsonObject tempObject = new JsonObject();
            tempObject.addProperty("name", parameterNames[i]);
            tempObject.addProperty("type", parameterList[i].name());
            tempArray.add(tempObject);
        }
        contract.add("parameters", tempArray);
/*        contract.add("parameters",new JsonArray(ParameterList.Zip(parameterNames, (type, name) =>
                {
                        JObject parameter = new JObject();
        parameter["name"] = name;
        parameter["type"] = type;
        return parameter;
            }));*/
        //LINQ END
        contract.addProperty("deployed", deployed);
        return TR.exit(contract);
    }
}