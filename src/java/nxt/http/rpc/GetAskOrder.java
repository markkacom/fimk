package nxt.http.rpc;

import static nxt.http.JSONResponses.UNKNOWN_ORDER;
import nxt.Asset;
import nxt.Order;
import nxt.http.ParameterException;
import nxt.http.websocket.RPCCall;
import nxt.util.Convert;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class GetAskOrder extends RPCCall {
    
    public static RPCCall instance = new GetAskOrder("getAskOrder");

    public GetAskOrder(String identifier) {
        super(identifier);
    }
  
    @SuppressWarnings("unchecked")
    @Override
    public JSONStreamAware call(JSONObject arguments) throws ParameterException {
      JSONObject response = new JSONObject();
      long orderId = ParameterParser.getOrderId(arguments);
      Order.Ask order = Order.Ask.getAskOrder(orderId);
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
