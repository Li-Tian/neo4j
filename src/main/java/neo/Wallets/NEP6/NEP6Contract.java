package neo.Wallets.NEP6;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.sun.xml.internal.messaging.saaj.packaging.mime.internet.ParameterList;

import java.util.ArrayList;
import java.util.List;

import neo.csharp.BitConverter;
import neo.smartcontract.Contract;
import neo.smartcontract.ContractParameterType;
import neo.smartcontract.Helper;

import static org.bouncycastle.asn1.x500.style.RFC4519Style.name;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: NEP6Contract
 * @Package neo.Wallets.NEP6
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 14:08 2019/3/14
 */
public class NEP6Contract extends Contract {
    public String[] parameterNames;

    public boolean deployed;

    public NEP6Contract() {
    }

    public static NEP6Contract fromJson(JsonObject json) {
        if (json == null) return null;
        //LINQ START
        NEP6Contract tempContract = new NEP6Contract();
        tempContract.script = BitConverter.hexToBytes(json.get("script").getAsString());
        List<ContractParameterType> templist = new ArrayList<>();
        List<String> templist2 = new ArrayList<>();
        json.getAsJsonArray("parameters").forEach
                (p -> templist.add(ContractParameterType.parse(p.getAsJsonObject
                        ().get("type").getAsByte())));
        json.getAsJsonArray("parameters").forEach
                (p -> templist2.add(p.getAsJsonObject().get("name").getAsString()));
        tempContract.parameterList = templist.toArray(new ContractParameterType[0]);
        tempContract.parameterNames = templist2.toArray(new String[0]);
        tempContract.deployed = json.get("deployed").getAsBoolean();
        return tempContract;
/*        {
            script = json["script"].AsString().HexToBytes(),
                    ParameterList = ((JArray)json["parameters"]).Select(p => p["type"].AsEnum<ContractParameterType>()).ToArray(),
                ParameterNames = ((JArray)json["parameters"]).Select(p => p["name"].AsString()).ToArray(),
                Deployed = json["deployed"].AsBoolean()
        };*/
        //LINQ END


    }

    public JsonObject toJson() {
        JsonObject contract = new JsonObject();
        contract.addProperty("script", BitConverter.toHexString(script));

        //LINQ START
        JsonArray tempArray = new JsonArray();
        for (int i = 0; i < parameterList.length; i++) {
            JsonObject tempObject = new JsonObject();
            tempObject.addProperty("name", parameterNames[i]);
            tempObject.addProperty("type", parameterList[i].value());
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
        return contract;
    }
}