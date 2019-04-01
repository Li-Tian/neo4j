package neo.plugins;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface IRpcPlugin {
    JsonObject onProcess(HttpServletRequest req, HttpServletResponse res, String method, JsonArray _params);
}