package nxt.http.websocket;

import java.io.IOException;

import nxt.http.API;
import nxt.util.Logger;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class MofoWebSocketAdapter extends WebSocketAdapter {

    /**
     * MofoWallet websocket dialect:
     * 
     * All operations expect an array as argument where the first element is the
     * operation. Arguments to the operation are determined per operation and
     * are explained below:
     * 
     * op: call
     * 
     * The call operation calls a remote procedure and has a mechanism to 
     * asynchronously send a response to the caller.
     * 
     * ['call', call-id, method, arguments] 
     *  
     * op: subscribe
     * 
     * This subscribes the socket to a certain topic, meaning the socket will
     * be notified about said topic events. The topic argument is an array of 
     * topics.
     * 
     * ['subscribe', [topic,topic]]
     * 
     * op: unsubscribe
     * 
     * Reverse of subscribe, this unsubscribes the socket from receiving 
     * notifications about said topic.
     * 
     * ['unsubscribe', topic]
     *   
     */

    @Override
    public void onWebSocketConnect(Session sess) {
        super.onWebSocketConnect(sess);
        Logger.logDebugMessage("Socket Connected: " + sess);
        if (API.allowedBotHosts != null && ! API.allowedBotHosts.contains(sess.getRemoteAddress().getHostName())) {
            try {
                Logger.logDebugMessage("Disconnecting because of not-allowed bot host");
                sess.disconnect();
            }
            catch (IOException e) {
                Logger.logDebugMessage("Could not disconnect socket", e);
            }
            return;
        }       
    }

    @Override
    public void onWebSocketText(String message) {
        super.onWebSocketText(message);
        //Logger.logDebugMessage("Received TEXT message: " + message);
        
        Object json = JSONValue.parse(message);
        
        if (json instanceof String && "ping".equals(json)) {
          sendAsync("pong");
          return;
        }
        
        if (!(json instanceof JSONArray)) {
            Logger.logDebugMessage("WebSocket - expected an array - "+message);
            return;
        }
        
        JSONArray argv = (JSONArray) json;
        if (argv.size() < 1 || !(argv.get(0) instanceof String)) {
            Logger.logDebugMessage("WebSocket - invalid operation - "+message);
            return;
        }
        
        String op = (String) argv.get(0); 
        if ("call".equals(op)) {
          
            if (argv.size() < 2 || !(argv.get(1) instanceof String)) {
                Logger.logDebugMessage("WebSocket - invalid call id - "+message);
                return;
            }
            if (argv.size() < 3 || !(argv.get(2) instanceof String)) {
                Logger.logDebugMessage("WebSocket - invalid method - "+message);
                return;
            }
            if (argv.size() < 4 || !(argv.get(3) instanceof JSONObject)) {
                Logger.logDebugMessage("WebSocket - invalid method - "+message);
                return;
            }            
            
            final String call_id        = (String) argv.get(1);
            final String method         = (String) argv.get(2);
            final JSONObject arguments  = (JSONObject) argv.get(3);

            MofoSocketServer.rpcCall(this, call_id, method, arguments);
        }
        else if ("subscribe".equals(op)) {
            if (argv.size() < 2) {
                Logger.logDebugMessage("WebSocket - invalid topic - "+message);
            }
            else {
                if (argv.get(1) instanceof String) {
                    MofoSocketServer.subscribe(this, (String) argv.get(1));
                }
                else if (argv.get(1) instanceof JSONArray) {
                    for (Object topic : (JSONArray) argv.get(1)) {
                        if (!(topic instanceof String)) {
                            Logger.logDebugMessage("WebSocket - invalid topic - "+message);
                            break;
                        }
                        MofoSocketServer.subscribe(this, (String) topic);
                    }
                }
                else {
                    Logger.logDebugMessage("WebSocket - invalid topic - "+message);
                }
            }
        }
        else if ("unsubscribe".equals(op)) {
            if (argv.size() < 2) {
                Logger.logDebugMessage("WebSocket - invalid topic - "+message);
            }
            else {
                if (argv.get(1) instanceof String) {
                    MofoSocketServer.unsubscribe(this, (String) argv.get(1));
                }
                else if (argv.get(1) instanceof JSONArray) {
                    for (Object topic : (JSONArray) argv.get(1)) {
                        if (!(topic instanceof String)) {
                            Logger.logDebugMessage("WebSocket - invalid topic - "+message);
                            break;
                        }
                        MofoSocketServer.unsubscribe(this, (String) topic);
                    }
                }
                else {
                    Logger.logDebugMessage("WebSocket - invalid topic - "+message);
                }
            }          
        }
        else {
            Logger.logDebugMessage("WebSocket - operation not supported '"+op+"' - "+message);
        }
    }    
    
    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        super.onWebSocketClose(statusCode,reason);
        Logger.logDebugMessage("Socket Closed: [" + statusCode + "] " + reason);
        MofoSocketServer.socketClosed(this);
    }
    
    @Override
    public void onWebSocketError(Throwable cause) {
        super.onWebSocketError(cause);
        cause.printStackTrace(System.err);
        Logger.logErrorMessage(cause.toString());
    }
    
    public void sendAsync(String str) {
        if (getSession() != null && getSession().isOpen()) {
            //Logger.logDebugMessage("Send TEXT message: " + str);
            getSession().getRemote().sendStringByFuture(str);
        }
    }
}
