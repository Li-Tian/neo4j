package neo.network.rpc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.bouncycastle.util.encoders.Base64;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetSocketAddress;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import neo.NeoSystem;
import neo.csharp.common.IDisposable;
import neo.log.tr.TR;

public class RpcServerHandler extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        TR.enter();
        if (NeoSystem.rpcServer == null) {
            TR.exit();
            return;
        }
        JsonObject request = null;
        String jsonrpc = req.getParameter("jsonrpc");
        String id = req.getParameter("id");
        String method = req.getParameter("method");
        String _params = req.getParameter("params");
        if (!(id == null || id.isEmpty()) && !(method == null || method.isEmpty()) && !(_params == null || _params.isEmpty())) {
            //_params = new String(Base64.decode(_params), "utf-8");
            request = new JsonObject();
            if (!(jsonrpc == null || jsonrpc.isEmpty())) {
                request.addProperty("jsonrpc", jsonrpc);
            }
            request.addProperty("id", id);
            request.addProperty("method", method);
            request.add("params", new JsonParser().parse(_params).getAsJsonArray());
        }
        JsonObject response;
        if (request == null) {
            response = NeoSystem.rpcServer.createErrorResponse(0, -32700, "Parse error", null);
        } else {
            response = NeoSystem.rpcServer.processRequest(req, resp, request);
        }
        if (response == null) {
            TR.exit();
            return;
        }
        resp.setContentType("application/json-rpc");
        resp.getWriter().write(response.toString());
        TR.exit();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        TR.enter();
        JsonElement request = null;
        BufferedReader br = req.getReader();
        String str, wholeStr = "";
        while ((str = br.readLine()) != null) {
            wholeStr += str;
        }
        request = new JsonParser().parse(wholeStr);
        JsonElement response = null;
        if (NeoSystem.rpcServer == null) {
            TR.exit();
            return;
        }
        if (request == null) {
            response = NeoSystem.rpcServer.createErrorResponse(0, -32700, "Parse error", null);
        } else if (request.isJsonArray()) {
            JsonArray array = request.getAsJsonArray();
            if (array.size() == 0) {
                response = NeoSystem.rpcServer.createErrorResponse(0, -32600, "Invalid Request", null);
            } else {
                response = new JsonArray();
                JsonArray array2 = response.getAsJsonArray();
                array.forEach(p -> {
                    if (p.isJsonObject()) {
                        JsonObject q = NeoSystem.rpcServer.processRequest(req, resp, p.getAsJsonObject());
                        if (q != null) {
                            array2.add(q);
                        }
                    }
                });
            }
        } else if (request.isJsonObject()) {
            response = NeoSystem.rpcServer.processRequest(req, resp, request.getAsJsonObject());
        }
        if (response == null) {
            TR.exit();
            return;
        }
        resp.setContentType("application/json-rpc");
        resp.getWriter().write(response.toString());
        TR.exit();
    }
}