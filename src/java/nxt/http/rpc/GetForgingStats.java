package nxt.http.rpc;

import java.util.List;

import nxt.MofoQueries;
import nxt.MofoQueries.ForgingStatStruct;
import nxt.http.ParameterException;
import nxt.http.websocket.JSONData;
import nxt.http.websocket.RPCCall;
import nxt.Nxt;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class GetForgingStats extends RPCCall {
   
    public static RPCCall instance = new GetForgingStats("getForgingStats");

    public GetForgingStats(String identifier) {
        super(identifier);
    }
  
    @SuppressWarnings("unchecked")
    @Override
    public JSONStreamAware call(JSONObject arguments) throws ParameterException {
        int timestamp = ParameterParser.getTimestamp(arguments);
        if (timestamp == 0) {
            timestamp = Nxt.getBlockchain().getLastBlock().getTimestamp();
        }

        List<ForgingStatStruct> list = MofoQueries.getForgingStats24H(timestamp);
        
        JSONArray forgers = new JSONArray();
        for (ForgingStatStruct stat : list) {
            JSONObject json = new JSONObject();
            JSONData.putAccount(json, "account", stat.getAccountId());
            json.put("feeNQT", String.valueOf(stat.getTotalFeeNQT()));
            json.put("count", stat.getBlockCount());
            forgers.add(json);
        }
        
        JSONObject response = new JSONObject();
        response.put("forgers", forgers);
        return response;
    }  

}
