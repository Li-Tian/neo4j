package neo.Wallets.NEP6;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: NEP6ContractTest
 * @Package neo.Wallets.NEP6
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 15:13 2019/4/2
 */
public class NEP6ContractTest {
    @Test
    public void fromJson() throws Exception {
       String json="{\"script\":\"52c56b6c766b00527ac461516c766b51527ac46203006c766b51c3616c7566" +
               "\",\"parameters\":[{\"name\":\"parameter0\",\"type\":\"Signature\"}],\"deployed\":false}";
        JsonObject object=new JsonParser().parse(json).getAsJsonObject();
        NEP6Contract contract=NEP6Contract.fromJson(object);
        Assert.assertEquals(json,contract.toJson().toString());

    }

    @Test
    public void toJson() throws Exception {
        String json="{\"script\":\"52c56b6c766b00527ac461516c766b51527ac46203006c766b51c3616c7566" +
                "\",\"parameters\":[{\"name\":\"parameter0\",\"type\":\"Signature\"}],\"deployed\":false}";
        JsonObject object=new JsonParser().parse(json).getAsJsonObject();
        NEP6Contract contract=NEP6Contract.fromJson(object);
        Assert.assertEquals(json,contract.toJson().toString());
    }

}