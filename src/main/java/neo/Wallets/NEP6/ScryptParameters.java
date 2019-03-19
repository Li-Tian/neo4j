package neo.Wallets.NEP6;

import com.google.gson.JsonObject;

import com.sun.xml.internal.ws.api.streaming.XMLStreamReaderFactory;

/**
 * @author doubi.liu
 * @version V1.0
 * @Title: ScryptParameters
 * @Package neo.Wallets.NEP6
 * @Description: (用一句话描述该文件做什么)
 * @date Created in 14:48 2019/3/14
 */
public class ScryptParameters {

    private static ScryptParameters defaultInstance = new ScryptParameters(16384, 8, 8);

    public static ScryptParameters getDefault() {
        return defaultInstance;
    }

    public final int N, R, P;

    public ScryptParameters(int n, int r, int p) {
        this.N = n;
        this.R = r;
        this.P = p;
    }

    public static ScryptParameters FromJson(JsonObject json) {
        return new ScryptParameters(json.get("n").getAsInt(), json.get("r").getAsInt(), json.get
                ("p").getAsInt());
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("n", N);
        json.addProperty("r", R);
        json.addProperty("p", P);
        return json;
    }
}