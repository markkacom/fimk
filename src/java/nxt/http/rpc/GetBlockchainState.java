package nxt.http.rpc;

import nxt.Nxt;
import nxt.http.ParameterException;
import nxt.http.websocket.RPCCall;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class GetBlockchainState extends RPCCall {
  
    public static RPCCall instance = new GetBlockchainState("getBlockchainState");
  
    public GetBlockchainState(String identifier) {
        super(identifier);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public JSONStreamAware call(JSONObject arguments) throws ParameterException {
        JSONObject response = new JSONObject();
        response.put("height", Nxt.getBlockchain().getHeight());
        response.put("timestamp", Nxt.getBlockchain().getLastBlock().getTimestamp());
        return response;
    }  

}
