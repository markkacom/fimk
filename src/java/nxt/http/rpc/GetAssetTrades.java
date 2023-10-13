package nxt.http.rpc;

import nxt.Asset;
import nxt.Nxt;
import nxt.Trade;
import nxt.db.DbIterator;
import nxt.http.ParameterException;
import nxt.http.websocket.JSONData;
import nxt.http.websocket.RPCCall;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class GetAssetTrades extends RPCCall {
    
    public static RPCCall instance = new GetAssetTrades("getAssetTrades");

    public GetAssetTrades(String identifier) {
        super(identifier);
    }
  
    @SuppressWarnings("unchecked")
    @Override
    public JSONStreamAware call(JSONObject arguments) throws ParameterException {
        Asset asset = ParameterParser.getAsset(arguments);
        int firstIndex = ParameterParser.getFirstIndex(arguments);
        int lastIndex = ParameterParser.getLastIndex(arguments);        
        
        JSONObject response = new JSONObject();
        response.put("name", asset.getName());
        response.put("decimals", asset.getDecimals());
        
        JSONArray jsonTrades = new JSONArray();
        response.put("trades", jsonTrades);
        
        try ( DbIterator<Trade> trades = asset.getTrades(firstIndex, lastIndex)) {
            while (trades.hasNext()) {
                Trade trade = trades.next();
                JSONObject json = new JSONObject();
                json.put("id", JSONData.generateTradeHash(trade));
                json.put("timestamp", trade.getTimestamp());
                json.put("quantityQNT", String.valueOf(trade.getQuantityQNT()));
                json.put("priceNQT", String.valueOf(trade.getPriceNQT()));
                JSONData.putAccount(json, "seller", trade.getSellerId());
                JSONData.putAccount(json, "buyer", trade.getBuyerId());
                json.put("height", trade.getHeight());
                json.put("tradeType", trade.isBuy() ? "buy" : "sell");
                json.put("confirmations", Nxt.getBlockchain().getHeight() - trade.getHeight());
                jsonTrades.add(json);              
            }
        }
      
        return response;      
    }  

}
