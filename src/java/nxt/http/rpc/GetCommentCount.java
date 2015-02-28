package nxt.http.rpc;

import nxt.MofoQueries;
import nxt.Transaction;
import nxt.db.DbIterator;
import nxt.http.ParameterException;
import nxt.http.websocket.JSONData;
import nxt.http.websocket.RPCCall;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class GetCommentCount extends RPCCall {
    
    public static RPCCall instance = new GetCommentCount("getCommentCount");

    public GetCommentCount(String identifier) {
        super(identifier);
    }
  
    @SuppressWarnings("unchecked")
    @Override
    public JSONStreamAware call(JSONObject arguments) throws ParameterException {
        long post_transaction_id = ParameterParser.getUnsignedLong(arguments, "post", true);
        
        JSONObject response = new JSONObject();
        response.put("count", MofoQueries.getCommentCount(post_transaction_id));
        
        return response;
    }
}
