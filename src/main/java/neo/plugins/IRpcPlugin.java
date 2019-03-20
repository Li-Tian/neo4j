package neo.plugins;

import com.google.gson.JsonArray;

import javax.xml.ws.spi.http.HttpContext;

public interface IRpcPlugin {
    Object onProcess(HttpContext context, String method, JsonArray _params);
}