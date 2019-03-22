package neo.plugins;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.xml.ws.spi.http.HttpContext;

public interface IRpcPlugin {
    JsonObject onProcess(HttpContext context, String method, JsonArray _params);
}