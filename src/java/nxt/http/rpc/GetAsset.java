package nxt.http.rpc;

import nxt.Account;
import nxt.Asset;
import nxt.AssetTransfer;
import nxt.Trade;
import nxt.http.ParameterException;
import nxt.http.websocket.JSONData;
import nxt.http.websocket.RPCCall;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class GetAsset extends RPCCall {
    
    public static RPCCall instance = new GetAsset("getAsset");

    public GetAsset(String identifier) {
        super(identifier);
    }
  
    @SuppressWarnings("unchecked")
    @Override
    public JSONStreamAware call(JSONObject arguments) throws ParameterException {
      
        Asset asset = ParameterParser.getAsset(arguments);
        JSONObject response = new JSONObject();
        response.put("name", asset.getName());
        response.put("decimals", asset.getDecimals());
        response.put("description", asset.getDescription());
        response.put("quantityQNT", asset.getQuantityQNT());
        JSONData.putAccount(response, "issuer", asset.getAccountId());
        
        response.put("numberOfTrades", Trade.getTradeCount(asset.getId()));
        response.put("numberOfTransfers", AssetTransfer.getTransferCount(asset.getId()));
        response.put("numberOfAccounts", Account.getAssetAccountCount(asset.getId()));

        return response;      
    }  

}
