package nxt.http.rpc;

import nxt.Asset;
import nxt.Order;
import nxt.http.ParameterException;
import nxt.http.websocket.RPCCall;
import nxt.util.Convert;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import static nxt.http.JSONResponses.UNKNOWN_ORDER;

public class GetBidOrder extends RPCCall {
    
    public static RPCCall instance = new GetBidOrder("getBidOrder");

    public GetBidOrder(String identifier) {
        super(identifier);
    }
  
    @SuppressWarnings("unchecked")
    @Override
    public JSONStreamAware call(JSONObject arguments) throws ParameterException {
        JSONObject response = new JSONObject();
        long orderId = ParameterParser.getOrderId(arguments);
        Order.Bid order = Order.Bid.getBidOrder(orderId);
        if (order == null) {
            return UNKNOWN_ORDER;
        }
        Asset asset = Asset.getAsset(order.getAssetId());
        if (asset != null) {
            response.put("name", asset.getName());
            response.put("decimals", asset.getDecimals());
            response.put("asset", Convert.toUnsignedLong(asset.getId()));
        }
        
        response.put("quantityQNT", String.valueOf(order.getQuantityQNT()));
        response.put("priceNQT", String.valueOf(order.getPriceNQT()));
        response.put("height", order.getHeight());
        return response;      
    }  

}
