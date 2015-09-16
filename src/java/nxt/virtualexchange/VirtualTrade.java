package nxt.virtualexchange;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;

import nxt.Nxt;
import nxt.Trade;
import nxt.db.DbIterator;
import nxt.http.websocket.JSONData;
import nxt.http.websocket.MofoSocketServer;
import nxt.virtualexchange.VirtualOrder.VirtualAsk;
import nxt.virtualexchange.VirtualOrder.VirtualBid;

public class VirtualTrade {
  
    private int height;
    private long assetId;
    private int timestamp;
    private long askOrderId;
    private long bidOrderId;
    private int askOrderHeight;
    private int bidOrderHeight;
    private long sellerId;
    private long buyerId;
    private long quantityQNT;
    private boolean isBuy;
    private long priceNQT;
    private static final List<VirtualTrade> virtualTrades = new ArrayList<VirtualTrade>();

    public VirtualTrade(long assetId, int timestamp, int height, VirtualAsk askOrder, VirtualBid bidOrder) {       
        this.height = height;
        this.assetId = assetId;
        this.timestamp = timestamp;
        this.askOrderId = askOrder.getId();
        this.bidOrderId = bidOrder.getId();        
        this.askOrderHeight = askOrder.getHeight();
        this.bidOrderHeight = bidOrder.getHeight();
        this.sellerId = askOrder.getAccountId();
        this.buyerId = bidOrder.getAccountId();
        this.quantityQNT = Math.min(askOrder.getQuantityQNT(), bidOrder.getQuantityQNT());
        this.isBuy = false;
        if (askOrderHeight < bidOrderHeight) {
            this.isBuy = true;
        }
        else if (askOrderHeight == bidOrderHeight) {
            if (askOrder.getTransactionIndex() < bidOrder.getTransactionIndex()) {
                this.isBuy = true;
            }
            else if (askOrder.getTransactionIndex() == bidOrder.getTransactionIndex()) {
                this.isBuy = askOrderId < bidOrderId;
            }
        }
        this.priceNQT = isBuy ? askOrder.getPriceNQT() : bidOrder.getPriceNQT();        
    }
    
    public VirtualTrade(Trade trade) {       
        this.height = trade.getHeight();
        this.assetId = trade.getAssetId();
        this.timestamp = trade.getTimestamp();
        this.askOrderId = trade.getAskOrderId();
        this.bidOrderId = trade.getBidOrderId();        
        this.askOrderHeight = trade.getAskOrderHeight();
        this.bidOrderHeight = trade.getBidOrderHeight();
        this.sellerId = trade.getSellerId();
        this.buyerId = trade.getBuyerId();
        this.quantityQNT = trade.getQuantityQNT();
        this.isBuy = trade.isBuy();
        this.priceNQT = trade.getPriceNQT();        
    }

    public static void notifyTradeAdded(VirtualTrade trade) {
        if (!Nxt.getBlockchainProcessor().isScanning()) {
            String asset = Long.toUnsignedString(trade.getAssetId());
            String topic = "TRADE_ADDED*"+asset;
            MofoSocketServer.notifyJSON(topic, trade.toJSONObject());
        }
    }    

    @SuppressWarnings("unchecked")
    public static void notifyTradeRemoved(VirtualTrade trade) {
        if (!Nxt.getBlockchainProcessor().isScanning()) {
            String asset = Long.toUnsignedString(trade.getAssetId());
            String topic = "TRADE_REMOVED*"+asset;
            
            JSONObject json = new JSONObject();
            json.put("asset", Long.toUnsignedString(trade.getAssetId()));
            json.put("askOrder", Long.toUnsignedString(trade.getAskOrderId()));
            json.put("bidOrder", Long.toUnsignedString(trade.getBidOrderId()));
            MofoSocketServer.notifyJSON(topic, json);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static void notifyTradeUpdated(Trade trade) {
        if (!Nxt.getBlockchainProcessor().isScanning()) {
            String asset = Long.toUnsignedString(trade.getAssetId());
            String topic = "TRADE_UPDATED*"+asset;
            
            JSONObject json = new JSONObject();
            json.put("asset", Long.toUnsignedString(trade.getAssetId()));
            json.put("askOrder", Long.toUnsignedString(trade.getAskOrderId()));
            json.put("bidOrder", Long.toUnsignedString(trade.getBidOrderId()));
            json.put("quantityQNT", Long.toUnsignedString(trade.getQuantityQNT()));
            json.put("timestamp", trade.getTimestamp());
            MofoSocketServer.notifyJSON(topic, json);
        }
    }
    
    public static List<VirtualTrade> getVirtualTrades() {
        return virtualTrades;
    }
    
    public static VirtualTrade find(Trade trade) {
        return find(trade.getAskOrderId(), trade.getBidOrderId());
    }
    
    static VirtualTrade find(long askOrderId, long bidOrderId) {
        for (VirtualTrade trade : virtualTrades) {
            if (trade.getAskOrderId() == askOrderId && trade.getBidOrderId() == bidOrderId) {
                return trade;
            }
        }
        return null;
    }
    
    public static VirtualTrade addTrade(long assetId, int timestamp, int height,  VirtualAsk askOrder, VirtualBid bidOrder) {
        VirtualTrade trade = new VirtualTrade(assetId, timestamp, height,  askOrder, bidOrder);
        virtualTrades.add(trade);
        notifyTradeAdded(trade);
        return trade;
    }
    
    public static void removeTrade(VirtualTrade trade) {
        virtualTrades.remove(trade);
        notifyTradeRemoved(trade);
    }
    
    @SuppressWarnings("unchecked")
    public JSONObject toJSONObject() {
        JSONObject json = new JSONObject();
        json.put("quantityQNT", String.valueOf(getQuantityQNT()));
        json.put("priceNQT", String.valueOf(getPriceNQT()));
        json.put("asset", Long.toUnsignedString(getAssetId()));
        json.put("timestamp", timestamp);
        json.put("askOrder", Long.toUnsignedString(getAskOrderId()));
        json.put("bidOrder", Long.toUnsignedString(getBidOrderId()));
        JSONData.putAccount(json, "seller", sellerId);
        JSONData.putAccount(json, "buyer", buyerId);
        json.put("height", height);
        json.put("tradeType", isBuy() ? "buy" : "sell");
        json.put("confirmations", Nxt.getBlockchain().getHeight() - height);
        JSONData.putAssetInfo(json, getAssetId());
        return json;
    }    
    
    public int getHeight() {
        return height;
    }
    
    public long getAssetId() {
        return assetId;
    }
    
    public int getTimestamp() {
        return timestamp;
    }
    
    public long getAskOrderId() {
        return askOrderId;
    }
    
    public long getBidOrderId() {
        return bidOrderId;
    }
    
    public int getAskOrderHeight() {
        return askOrderHeight;
    }
    
    public int getBidOrderHeight() {
        return bidOrderHeight;
    }
    
    public long getSellerId() {
        return sellerId;
    }
    
    public long getBuyerId() {
        return buyerId;
    }
    
    public long getQuantityQNT() {
        return quantityQNT;
    }
    
    public boolean isBuy() {
        return isBuy;
    }
    
    public long getPriceNQT() {
        return priceNQT;
    }
    
    public static List<VirtualTrade> getTrades(long assetId, int firstIndex, int lastIndex) {
        if (firstIndex < 0 || lastIndex < firstIndex) {
            throw new IndexOutOfBoundsException();
        }
        lastIndex = Math.min(firstIndex+100, lastIndex);
        
        /* filter trades on asset */
        List<VirtualTrade> trades = new ArrayList<VirtualTrade>();
        for (int i=firstIndex; i<virtualTrades.size(); i++) {
            VirtualTrade trade = virtualTrades.get(i);
            if (trade.getAssetId() == assetId) {
                trades.add(trade);
            }
        }
        
        if (trades.isEmpty()) {
            List<VirtualTrade> result = new ArrayList<VirtualTrade>();
            try (DbIterator<Trade> realTrades = Trade.getAssetTrades(assetId, firstIndex, lastIndex);) {
                while (realTrades.hasNext()) {
                    result.add(new VirtualTrade(realTrades.next()));
                }
            }
            return result;
        }
        return getMergedTrades(assetId, trades, firstIndex, lastIndex);      
    }

    private static List<VirtualTrade> getMergedTrades(long assetId, List<VirtualTrade> trades, int firstIndex, int lastIndex) {
        List<VirtualTrade> result = new ArrayList<VirtualTrade>();
        if (firstIndex < trades.size()) {
            for (int i=firstIndex; i<trades.size(); i++) {
                result.add(trades.get(i));
            }
            firstIndex = 0;
            lastIndex -= result.size();
        }
        else {
            firstIndex -= trades.size();
            lastIndex -= trades.size();
        }
        try (DbIterator<Trade> iterator = Trade.getAssetTrades(assetId, firstIndex, lastIndex)) {
            while (iterator.hasNext()) {
                result.add(new VirtualTrade(iterator.next()));
            }
        }
        return result;
    }
}