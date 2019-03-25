package neo.plugins;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletRequest;

public interface IRpcPlugin {
    JsonObject onProcess(HttpServletRequest req, String method, JsonArray _params);
}