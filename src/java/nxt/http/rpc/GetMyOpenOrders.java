package nxt.http.rpc;

import java.util.List;

import nxt.Asset;
import nxt.MofoQueries;
import nxt.http.ParameterException;
import nxt.http.websocket.RPCCall;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetMyOpenOrders extends RPCCall {

    public static RPCCall instance = new GetMyOpenOrders("getMyOpenOrders");
    static int COUNT = 15;

    public GetMyOpenOrders(String identifier) {
        super(identifier);
    }

    @SuppressWarnings("unchecked")
    @Override
    public JSONStreamAware call(JSONObject arguments) throws ParameterException {      
        Asset asset = ParameterParser.getAsset(arguments);
        List<Long> accountIds = ParameterParser.getAccountIds(arguments);
        int firstIndex = ParameterParser.getFirstIndex(arguments);
        int lastIndex = ParameterParser.getLastIndex(arguments);
        
        JSONObject response = new JSONObject();
        response.put("decimals", asset.getDecimals());
        response.put("orders", MofoQueries.getAssetOpenOrders(accountIds, asset.getId(), firstIndex, lastIndex));
        
        return response;
    }
}
