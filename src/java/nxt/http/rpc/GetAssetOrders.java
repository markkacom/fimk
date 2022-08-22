package nxt.http.rpc;

import nxt.*;
import nxt.db.DbIterator;
import nxt.http.ParameterException;
import nxt.http.websocket.JSONData;
import nxt.http.websocket.RPCCall;
import nxt.txn.AskOrderPlacementAttachment;
import nxt.txn.BidOrderPlacementAttachment;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.util.List;

import static nxt.http.JSONResponses.INCORRECT_ORDER_TYPE;

public class GetAssetOrders extends RPCCall {
    
    public static RPCCall instance = new GetAssetOrders("getAssetOrders");

    public GetAssetOrders(String identifier) {
        super(identifier);
    }
  
    @SuppressWarnings("unchecked")
    @Override
    public JSONStreamAware call(JSONObject arguments) throws ParameterException {
        Asset asset = ParameterParser.getAsset(arguments);
        String type = Convert.emptyToNull((String) arguments.get("type"));
        if (type == null || (!"bid".equals(type) && !"ask".equals(type))) {
            throw new ParameterException(INCORRECT_ORDER_TYPE);
        }
        
        int firstIndex = ParameterParser.getFirstIndex(arguments);
        int lastIndex = ParameterParser.getLastIndex(arguments);        
        
        JSONObject response = new JSONObject();
        response.put("name", asset.getName());
        response.put("decimals", asset.getDecimals());
        
        JSONArray orders = new JSONArray();
        response.put("orders", orders);
        
        if ("ask".equals(type)) {
            try (DbIterator<Order.Ask> askOrders = Order.Ask.getSortedOrders(asset.getId(), firstIndex, lastIndex)) {
                while (askOrders.hasNext()) {
                    orders.add(JSONData.order(askOrders.next()));
                }
            }
            
            /* If index is zero include matching unconfirmed orders */
            if (firstIndex == 0) {
                List<Transaction> unconfirmed = MofoQueries.getUnconfirmedTransactions((byte)2, (byte)2);
                for (Transaction transaction : unconfirmed) {
                    AskOrderPlacementAttachment attachment = (AskOrderPlacementAttachment) transaction.getAttachment();
                  
                    JSONObject json = new JSONObject();
                    json.put("order", Long.toUnsignedString(transaction.getId()));
                    json.put("quantityQNT", String.valueOf(attachment.getQuantityQNT()));
                    json.put("priceNQT", String.valueOf(attachment.getPriceNQT()));
                    json.put("height", Nxt.getBlockchain().getHeight() + 1);
                    json.put("confirmations", 0);
                    json.put("accountRS", Convert.rsAccount(transaction.getSenderId()));
                    json.put("type", "sell");
                    
                    orders.add(json);
                }
            }            
        }
        else {
            try (DbIterator<Order.Bid> bidOrders = Order.Bid.getSortedOrders(asset.getId(), firstIndex, lastIndex)) {
                while (bidOrders.hasNext()) {
                    orders.add(JSONData.order(bidOrders.next()));
                }
            }
            
            /* If index is zero include matching unconfirmed orders */
            if (firstIndex == 0) {
                List<Transaction> unconfirmed = MofoQueries.getUnconfirmedTransactions((byte)2, (byte)3);
                for (Transaction transaction : unconfirmed) {
                    BidOrderPlacementAttachment attachment = (BidOrderPlacementAttachment) transaction.getAttachment();
                    
                    JSONObject json = new JSONObject();
                    json.put("order", Long.toUnsignedString(transaction.getId()));
                    json.put("quantityQNT", String.valueOf(attachment.getQuantityQNT()));
                    json.put("priceNQT", String.valueOf(attachment.getPriceNQT()));
                    json.put("height", Nxt.getBlockchain().getHeight() + 1);
                    json.put("confirmations", 0);
                    json.put("accountRS", Convert.rsAccount(transaction.getSenderId()));
                    json.put("type", "sell");
                    
                    orders.add(json);
                }
            }
        }
      
        return response;      
    }  

}
