package neo.wallets.NEP6;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: ScryptParametersTest
 * @Package neo.wallets.NEP6
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 17:46 2019/4/3
 */
public class ScryptParametersTest {
    @Test
    public void getDefault() throws Exception {
       Assert.assertEquals(16384,ScryptParameters.getDefault().N);
       Assert.assertEquals(8,ScryptParameters.getDefault().R);
       Assert.assertEquals(8,ScryptParameters.getDefault().P);
    }

    @Test
    public void fromJson() throws Exception {
        String json="{\"n\":16384,\"r\":8,\"p\":8}";
        JsonObject object=new JsonParser().parse(json).getAsJsonObject();
        ScryptParameters parameters=ScryptParameters.FromJson(object);
        Assert.assertEquals(16384,parameters.N);
        Assert.assertEquals(8,parameters.R);
        Assert.assertEquals(8,parameters.P);
    }

    @Test
    public void toJson() throws Exception {
        String json="{\"n\":16384,\"r\":8,\"p\":8}";
        JsonObject object=new JsonParser().parse(json).getAsJsonObject();
        ScryptParameters parameters=ScryptParameters.FromJson(object);
        Assert.assertEquals(json,parameters.toJson().toString());
    }

}