package nxt.http.rpc;

import nxt.Asset;
import nxt.MofoQueries;
import nxt.http.ParameterException;
import nxt.http.websocket.RPCCall;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class GetAssetChartData extends RPCCall {
    
    public static RPCCall instance = new GetAssetChartData("getAssetChartData");
    static final int COUNT = 200;

    public GetAssetChartData(String identifier) {
        super(identifier);
    }
    
    static final byte TEN_MINUTES = 0;
    static final byte HOUR = 1;
    static final byte DAY  = 2;
    static final byte WEEK = 3;    
  
    private int windowToSeconds(byte window) {
        switch (window) {
        case TEN_MINUTES: return 600;
        case HOUR: return 3600;
        case DAY: return 86400;
        case WEEK: return 604800;
        }
        throw new RuntimeException("Invalid window");
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public JSONStreamAware call(JSONObject arguments) throws ParameterException {
      
        Asset asset = ParameterParser.getAsset(arguments);
        byte window = (byte) ParameterParser.getInt(arguments, "window", TEN_MINUTES, WEEK, true);

        JSONObject response = new JSONObject();
        JSONObject json = new JSONObject();
        json.put("name", asset.getName());
        json.put("decimals", asset.getDecimals());
        
        response.put("asset", json);
        JSONArray chart_data = MofoQueries.getAssetChartData(asset.getId(), windowToSeconds(window));
        response.put("data", chart_data);
        
        return response;      
    }  

}
