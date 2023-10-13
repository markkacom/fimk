package nxt.http.websocket;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public abstract class RPCCall {
  
    private String identifier;

    public RPCCall(String identifier) {
        this.identifier = identifier;
    }
    
    public String getIdentifier() {
        return identifier;
    }
    
    public abstract JSONStreamAware call(JSONObject arguments) throws Exception;
    
}
