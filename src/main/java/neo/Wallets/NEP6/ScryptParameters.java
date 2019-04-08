package neo.Wallets.NEP6;

import com.google.gson.JsonObject;

import neo.log.notr.TR;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: ScryptParameters
 * @Package neo.Wallets.NEP6
 * @Description: Scrypt算法参数类
 * @date Created in 14:48 2019/3/14
 */
public class ScryptParameters {

    //默认值
    private static ScryptParameters defaultInstance = new ScryptParameters(16384, 8, 8);

    /**
      * @Author:doubi.liu
      * @description:获取默认值
      * @date:2019/4/3
    */
    public static ScryptParameters getDefault() {
        TR.enter();
        return TR.exit(defaultInstance);
    }

    //三个参数N、P、R
    public final int N, R, P;

    /**
      * @Author:doubi.liu
      * @description:构造函数
      * @date:2019/4/3
    */
    public ScryptParameters(int n, int r, int p) {
        TR.enter();
        this.N = n;
        this.R = r;
        this.P = p;
        TR.exit();
    }

    /**
      * @Author:doubi.liu
      * @description:从json对象中转换JsonObject
      * @param json JsonObject 对象
      * @date:2019/4/3
    */
    public static ScryptParameters FromJson(JsonObject json) {
        TR.enter();
        return TR.exit(new ScryptParameters(json.get("n").getAsInt(), json.get("r").getAsInt(), json
                .get("p").getAsInt()));
    }

    /**
      * @Author:doubi.liu
      * @description:转Json 方法
      * @date:2019/4/3
    */
    public JsonObject toJson() {
        TR.enter();
        JsonObject json = new JsonObject();
        json.addProperty("n", N);
        json.addProperty("r", R);
        json.addProperty("p", P);
        return TR.exit(json);
    }
}