package neo.wallets.NEP6;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: NEP6AccountTest
 * @Package neo.wallets.NEP6
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 16:10 2019/4/1
 */
public class NEP6AccountTest {
    @Test
    public void decrypted() throws Exception {
          NEP6Account account=new NEP6Account(null,null,null);
        Assert.assertEquals(true,account.decrypted());
    }

    @Test
    public void hasKey() throws Exception {
        NEP6Account account=new NEP6Account(null,null,"");
        Assert.assertEquals(true,account.hasKey());
    }

    @Test
    public void fromJson() throws Exception {
      String json="{\"address\":\"AZnqQgpVvFKJF6gxComwDTUqZzWg8vyv12\",\"label\":null," +
              "\"isDefault\":false,\"lock\":false,\"key\":\"6PYNaeA3WyZJJNnFqq3PbXszry5hL7bbageAmN3sc4tziXmPzpcQSEqHjr\",\"contract\":{\"script\":\"210276caff42decbebf4337be45c0a517e5b6575ec43aacbd32d95db199182b5b1e4ac\",\"parameters\":[{\"name\":\"signature\",\"type\":\"Signature\"}],\"deployed\":false},\"extra\":null}";
        JsonObject object=new JsonParser().parse(json).getAsJsonObject();
        NEP6Account account=NEP6Account.fromJson(object,null);
        Assert.assertEquals(json,account.toJson().toString());
    }

    @Test
    public void getKey() throws Exception {
        NEP6Account account=new NEP6Account(null,null,null);
        Assert.assertEquals(null,account.getKey());
    }

    @Test
    public void getKey1() throws Exception {
        NEP6Account account=new NEP6Account(null,null,null);
        Assert.assertEquals(null,account.getKey("aaaaa"));
    }

    @Test
    public void toJson() throws Exception {
        String json="{\"address\":\"AZnqQgpVvFKJF6gxComwDTUqZzWg8vyv12\",\"label\":null," +
                "\"isDefault\":false,\"lock\":false,\"key\":\"6PYNaeA3WyZJJNnFqq3PbXszry5hL7bbageAmN3sc4tziXmPzpcQSEqHjr\",\"contract\":{\"script\":\"210276caff42decbebf4337be45c0a517e5b6575ec43aacbd32d95db199182b5b1e4ac\",\"parameters\":[{\"name\":\"signature\",\"type\":\"Signature\"}],\"deployed\":false},\"extra\":null}";
        JsonObject object=new JsonParser().parse(json).getAsJsonObject();
        NEP6Account account=NEP6Account.fromJson(object,null);
        Assert.assertEquals(json,account.toJson().toString());
    }

    @Test
    public void verifyPassword() throws Exception {
        NEP6Wallet wallet=new NEP6Wallet();
        wallet.scrypt=new ScryptParameters(16384,8,8);
       NEP6Account account=new NEP6Account(wallet,null,
               "6PYNaeA3WyZJJNnFqq3PbXszry5hL7bbageAmN3sc4tziXmPzpcQSEqHjr");
        Assert.assertEquals(true,account.verifyPassword("1234567890"));
    }



}