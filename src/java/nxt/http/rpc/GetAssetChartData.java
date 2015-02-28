package nxt.http.rpc;

import java.util.List;

import nxt.Asset;
import nxt.MofoChart;
import nxt.MofoChartWindow;
import nxt.Nxt;
import nxt.http.ParameterException;
import nxt.http.websocket.JSONData;
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
  
    @SuppressWarnings("unchecked")
    @Override
    public JSONStreamAware call(JSONObject arguments) throws ParameterException {
      
        Asset asset = ParameterParser.getAsset(arguments);
        byte window = (byte) ParameterParser.getInt(arguments, "window", MofoChartWindow.HOUR, MofoChartWindow.WEEK, true);
        int timestamp = ParameterParser.getTimestamp(arguments);        
        if (timestamp == 0) {
            timestamp = Nxt.getEpochTime();
        }       
        
        List<MofoChartWindow> data = MofoChart.getChartData(asset.getId(), window, timestamp, COUNT);
        
        JSONObject response = new JSONObject();
        JSONObject json = new JSONObject();
        json.put("name", asset.getName());
        json.put("decimals", asset.getDecimals());
        
        response.put("asset", json);
        JSONArray chart_data = new JSONArray();
        response.put("data", chart_data);        
        
        for (MofoChartWindow d : data) {
            json = new JSONObject();
            json.put("timestamp", d.getTimestamp());
            json.put("open", String.valueOf(d.getOpenNQT()));
            json.put("close", String.valueOf(d.getCloseNQT()));
            json.put("high", String.valueOf(d.getHighNQT()));
            json.put("low", String.valueOf(d.getLowNQT()));
            json.put("avg", String.valueOf(d.getAverageNQT()));
            json.put("vol", String.valueOf(d.getVolumeQNT()));
            chart_data.add(json);
        }
        
        return response;      
    }  

}
