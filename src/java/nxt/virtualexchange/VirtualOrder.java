package nxt.virtualexchange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;

import nxt.Attachment;
import nxt.Nxt;
import nxt.Order;
import nxt.Order.Ask;
import nxt.Order.Bid;
import nxt.Transaction;
import nxt.db.DbIterator;
import nxt.http.websocket.MofoSocketServer;
import nxt.util.Convert;


public abstract class VirtualOrder {
  
    private static final Map<Long, VirtualAsk> virtualAskMap = new HashMap<Long, VirtualAsk>();
    private static final Map<Long, VirtualBid> virtualBidMap = new HashMap<Long, VirtualBid>();
    
    static class PeekableIterator<T> {
        
        static abstract class PeekableIteratorFilter<T> {
            public abstract boolean accept(T entity);
        }
      
        private Iterator<T> iterator;
        private T next;
        private PeekableIteratorFilter<T> filter;
  
        public PeekableIterator(Iterator<T> iterator) {
            this(iterator, null);
        }        
        public PeekableIterator(Iterator<T> iterator, PeekableIteratorFilter<T> filter) {
            this.iterator = iterator;
            this.filter = filter;
            next = iterator.hasNext() ? nextInternal() : null;
        }
        public boolean hasNext() {
            return (next != null);
        }
        public T next() {
            T current = next;
            next = (iterator.hasNext() ? nextInternal() : null);
            return current;
        }
        public T peek() {
            return next;
        }
        private T nextInternal() {
            while (iterator.hasNext()) {
                T entity = iterator.next();
                if (filter != null && !filter.accept(entity)) {
                    continue;
                }
                return entity;
            }
            return null;
        }
    }
    
    private final long id;
    private final long accountId;
    private final long assetId;
    private final long priceNQT;
    private final int height;
    private short transactionIndex;
    private long quantityQNT;
    
    private VirtualOrder(Transaction transaction, Attachment.ColoredCoinsOrderPlacement attachment) {
        this.id = transaction.getId();
        this.accountId = transaction.getSenderId();
        this.assetId = attachment.getAssetId();
        this.quantityQNT = attachment.getQuantityQNT();
        this.priceNQT = attachment.getPriceNQT();
        this.height = transaction.getHeight() == Integer.MAX_VALUE ? Nxt.getBlockchain().getHeight() : transaction.getHeight();
        this.transactionIndex = 0;
    }    
    
    public VirtualOrder(Order order) {
        this.id = order.getId();
        this.accountId = order.getAccountId();
        this.assetId = order.getAssetId();
        this.quantityQNT = order.getQuantityQNT();
        this.priceNQT = order.getPriceNQT();
        this.height = order.getHeight();
        this.transactionIndex = 0;
    }    
    
    private static void matchOrders(long assetId, int timestamp) {
    
        VirtualAsk askOrder;
        VirtualBid bidOrder;
    
        while ((askOrder = VirtualAsk.getNextOrder(assetId)) != null
                && (bidOrder = VirtualBid.getNextOrder(assetId)) != null) {
    
            if (askOrder.getPriceNQT() > bidOrder.getPriceNQT()) {
                break;
            }
    
            VirtualTrade trade = VirtualTrade.addTrade(assetId, timestamp, Nxt.getBlockchain().getHeight()+1, askOrder, bidOrder);
    
            askOrder.updateQuantityQNT(Math.subtractExact(askOrder.getQuantityQNT(), trade.getQuantityQNT()));
            bidOrder.updateQuantityQNT(Math.subtractExact(bidOrder.getQuantityQNT(), trade.getQuantityQNT()));
        }
    }  

    protected abstract String getType();
    protected abstract void save();
    
    public static Map<Long, VirtualAsk> getVirtualAsks() {
        return virtualAskMap;
    }
    
    public static Map<Long, VirtualBid> getVirtualBids() {
        return virtualBidMap;
    }
    
    public long getQuantityQNT() {
        return quantityQNT;
    }
    
    public void setQuantityQNT(long quantityQNT) {
        this.quantityQNT = quantityQNT;
    }
    
    public long getId() {
        return id;
    }
    
    public long getAccountId() {
        return accountId;
    }
    
    public long getAssetId() {
        return assetId;
    }
    
    public long getPriceNQT() {
        return priceNQT;
    }
    
    public int getHeight() {
        return height;
    }
    
    public short getTransactionIndex() {
        return transactionIndex;
    }
    
    public void setTransactionIndex(short transactionIndex) {
        this.transactionIndex = transactionIndex;
    }

    @SuppressWarnings("unchecked")
    public JSONObject toJSONObject() {
        JSONObject json = new JSONObject();
        json.put("order", Long.toUnsignedString(getId()));
        json.put("quantityQNT", String.valueOf(getQuantityQNT()));
        json.put("priceNQT", String.valueOf(getPriceNQT()));
        json.put("height", getHeight());
        json.put("confirmations", Nxt.getBlockchain().getHeight() - getHeight());
        json.put("accountRS", Convert.rsAccount(getAccountId()));
        json.put("type", getType());
        json.put("transactionIndex", transactionIndex);
        return json; 
    }
    
    public static void notifyOrderAdded(VirtualOrder order) {
        if (!Nxt.getBlockchainProcessor().isScanning()) {
            String asset = Long.toUnsignedString(order.getAssetId());
            String topic = order.getType().equals(VirtualBid.TYPE) ? "BID_ORDER_ADD*"+asset : "ASK_ORDER_ADD*"+asset;
            MofoSocketServer.notifyJSON(topic, order.toJSONObject());
        }
    }
    
    @SuppressWarnings("unchecked")
    public static void notifyOrderUpdate(VirtualOrder order) {
        if (!Nxt.getBlockchainProcessor().isScanning()) {
            String asset = Long.toUnsignedString(order.getAssetId());
            String topic = order.getType().equals(VirtualBid.TYPE) ? "BID_ORDER_UPDATE*"+asset : "ASK_ORDER_UPDATE*"+asset;
          
            JSONObject json = new JSONObject();
            json.put("quantityQNT", String.valueOf(order.getQuantityQNT()));
            json.put("order", Long.toUnsignedString(order.getId()));
            MofoSocketServer.notifyJSON(topic, json);
        }
    }    

    @SuppressWarnings("unchecked")
    public static void notifyOrderRemoved(VirtualOrder order) {
        if (!Nxt.getBlockchainProcessor().isScanning()) {
            String asset = Long.toUnsignedString(order.getAssetId());
            String topic = order.getType().equals(VirtualBid.TYPE) ? "BID_ORDER_REMOVE*"+asset : "ASK_ORDER_REMOVE*"+asset;
          
            JSONObject json = new JSONObject();
            json.put("order", Long.toUnsignedString(order.getId()));
            MofoSocketServer.notifyJSON(topic, json);
        }
    }
    
    protected void updateQuantityQNT(long quantityQNT) {
        setQuantityQNT(quantityQNT);
        save();
        if (quantityQNT > 0) {
            notifyOrderUpdate(this);
        } 
        else if (quantityQNT == 0) {
            notifyOrderRemoved(this);
        } 
        else {
            throw new IllegalArgumentException("Negative quantity: " + quantityQNT
                    + " for order: " + Long.toUnsignedString(getId()));
        }
    }
    
    public static class VirtualAsk extends VirtualOrder implements Comparable<VirtualAsk> {
        static final String TYPE = "ask";
      
        public VirtualAsk(Order order) {
            super(order);
        }

        public VirtualAsk(Transaction transaction, Attachment.ColoredCoinsOrderPlacement attachment) {
            super(transaction, attachment);
        }

        @Override
        protected String getType() {
            return TYPE;
        }
        
        @Override
        protected void save() {
            virtualAskMap.put(Long.valueOf(getId()), this);
        }
        
        @Override
        public int compareTo(VirtualAsk other) {
            if (getPriceNQT() < other.getPriceNQT()) {        
                return 1; // lowest ask wins
            }
            else if (getPriceNQT() == other.getPriceNQT()) {  
                if (getHeight() < other.getHeight()) {        
                    return 1; // lowest height wins
                }
                else if (getHeight() == other.getHeight()) {
                    if (getTransactionIndex() < other.getTransactionIndex()) {
                        return 1; // lowest transaction index wins
                    }
                    return (getTransactionIndex() > other.getTransactionIndex()) ? -1 : 0;
                }
                else {
                  return -1;
                }
            }
            return -1;
        }

        private static VirtualAsk getVirtualAsk(Ask ask) {
            Long key = Long.valueOf(ask.getId());
            VirtualAsk virtualAsk = virtualAskMap.get(key);
            if (virtualAsk == null) {
                virtualAsk = new VirtualAsk(ask);
            }
            return virtualAsk;
        }
        
        public static VirtualAsk getNextOrder(long assetId) {
            
            /* Look in the 'real' orders to see if there is an order that has not been completely eaten */
            VirtualAsk bestOrder = null;
            DbIterator<nxt.Order.Ask> iterator = Order.Ask.getSortedOrders(assetId, 0, Integer.MAX_VALUE);
            while (iterator.hasNext()) {
                VirtualAsk virtualAsk = getVirtualAsk(iterator.next());
                if (virtualAsk.getQuantityQNT() > 0) {
                    bestOrder = virtualAsk;
                }
            }
          
            /* Look through the 'virtual' orders to see if there is a better order */
            for (VirtualAsk ask : virtualAskMap.values()) {
                if (ask.getAssetId() == assetId) {
                    if (ask.getQuantityQNT() > 0) {
                        if (bestOrder == null || ask.compareTo(bestOrder) > 0) {
                            bestOrder = ask;
                        }
                    }
                }
            }
            return bestOrder;
        }
        
        /* called from ADDED_UNCONFIRMED_TRANSACTION for PLACE_ASK, PLACE_BID transactions */
        static VirtualAsk addOrder(Transaction transaction, Attachment.ColoredCoinsAskOrderPlacement attachment, short transactionIndex) {
            VirtualAsk order = new VirtualAsk(transaction, attachment);
            order.setTransactionIndex(transactionIndex);
            order.save();
            notifyOrderAdded(order);
            matchOrders(attachment.getAssetId(), transaction.getTimestamp());
            return order;
        }
  
        /* called from ADDED_UNCONFIRMED_TRANSACTION for CANCEL_ASK, CANCEL_BID transactions */
        static VirtualAsk removeOrder(long orderId) {
            VirtualAsk order = virtualAskMap.get(Long.valueOf(orderId));
            if (order == null) {
                Ask ask = Ask.getAskOrder(orderId);
                if (ask == null) return null; /* this should never happen */
                order = new VirtualAsk(ask);
            }
            if (order.getQuantityQNT() > 0) {
                order.setQuantityQNT(0);
                notifyOrderRemoved(order);
            }
            order.save();
            return order;
        }
        
        public static List<VirtualAsk> getAsks(long assetId, int firstIndex, int lastIndex, long accountId) {
            if (firstIndex < 0 || lastIndex < firstIndex) {
                throw new IndexOutOfBoundsException();
            }
            lastIndex = Math.min(firstIndex+100, lastIndex);
            if (virtualAskMap.isEmpty()) {
                List<VirtualAsk> result = new ArrayList<VirtualAsk>();
                try (
                    DbIterator<Ask> asks = accountId == 0 ? Order.Ask.getSortedOrders(assetId, firstIndex, lastIndex) :
                                                            Order.Ask.getAskOrdersByAccountAsset(accountId, assetId, firstIndex, lastIndex)
                ) {
                    while (asks.hasNext()) {
                        result.add(new VirtualAsk(asks.next()));
                    }
                }
                return result;
            }
            return getMergedAsks(assetId, firstIndex, lastIndex, accountId);
        }

        private static List<VirtualAsk> getMergedAsks(long assetId, int firstIndex, int lastIndex, final long accountId) {
            List<VirtualAsk> sortedAsks = new ArrayList<VirtualAsk>();
            for (VirtualAsk a : virtualAskMap.values()) {
                if (a.getAssetId() == assetId) {
                    sortedAsks.add(a);
                }
            }
            Collections.sort(sortedAsks, Collections.reverseOrder());
          
            List<VirtualAsk> result = new ArrayList<VirtualAsk>();
            try (DbIterator<Ask> dbIterator = Order.Ask.getSortedOrders(assetId, 0, Integer.MAX_VALUE)) {
              
                PeekableIterator<VirtualAsk> virtualAsks;
                PeekableIterator<Ask> asks;
                
                if (accountId != 0) {
                    virtualAsks = new PeekableIterator<VirtualAsk>(sortedAsks.iterator(), new PeekableIterator.PeekableIteratorFilter<VirtualAsk>() {
                        public boolean accept(VirtualAsk order) {
                            return order.getAccountId() == accountId;
                        }
                    });
                    asks = new PeekableIterator<Ask>(dbIterator.iterator(), new PeekableIterator.PeekableIteratorFilter<Ask>() {
                          public boolean accept(Ask order) {
                            return order.getAccountId() == accountId;
                        }
                    });
                }
                else {
                    virtualAsks = new PeekableIterator<VirtualAsk>(sortedAsks.iterator());
                    asks = new PeekableIterator<Ask>(dbIterator.iterator());
                }                
              
                VirtualAsk virtualAsk = virtualAsks.peek();
                VirtualAsk ask = asks.peek() != null ? getVirtualAsk(asks.peek()) : null;
                
                while ((virtualAsk != null || ask != null) && result.size() < lastIndex) {
                    if (virtualAsk == null) {
                        result.add(ask);
                        asks.next();
                        ask = asks.peek() != null ? getVirtualAsk(asks.peek()) : null;
                    }
                    else if (ask == null) {
                        result.add(virtualAsk);
                        virtualAsks.next();
                        virtualAsk = virtualAsks.peek();
                    }
                    else {
                        int compared = ask.compareTo(virtualAsk);
                        if (compared < 0) {
                            result.add(virtualAsk);
                            virtualAsks.next();
                            virtualAsk = virtualAsks.peek();
                        }
                        else {
                            result.add(ask);
                            asks.next();
                            ask = asks.peek() != null ? getVirtualAsk(asks.peek()) : null;
                        }
                    }
                }
            }
            return result.subList(firstIndex, Math.min(result.size(), lastIndex));
        }
    }
    
    public static class VirtualBid extends VirtualOrder implements Comparable<VirtualBid> {
        static final String TYPE = "bid";
      
        public VirtualBid(Order order) {
            super(order);
        }

        public VirtualBid(Transaction transaction, Attachment.ColoredCoinsOrderPlacement attachment) {
            super(transaction, attachment);
        }
        
        @Override
        protected String getType() {
            return TYPE;
        }
        
        @Override
        protected void save() {
            virtualBidMap.put(Long.valueOf(getId()), this);
        }
        
        @Override
        public int compareTo(VirtualBid other) {
            if (getPriceNQT() > other.getPriceNQT()) {        
                return 1; // highest bid wins
            }
            else if (getPriceNQT() == other.getPriceNQT()) {  
                if (getHeight() < other.getHeight()) {        
                    return 1; // lowest height wins
                }
                else if (getHeight() == other.getHeight()) {
                    if (getTransactionIndex() < other.getTransactionIndex()) {
                        return 1; // lowest transaction index wins
                    }
                    return (getTransactionIndex() > other.getTransactionIndex()) ? -1 : 0;
                }
                else {
                  return -1;
                }
            }
            return -1;
        }

        private static VirtualBid getVirtualBid(Bid bid) {
            Long key = Long.valueOf(bid.getId());
            VirtualBid virtualBid = virtualBidMap.get(key);
            if (virtualBid == null) {
                virtualBid = new VirtualBid(bid);
            }
            return virtualBid;
        }      

        public static VirtualBid getNextOrder(long assetId) {
          
            /* Look in the 'real' orders to see if there is an order that has not been completely eaten */
            VirtualBid bestOrder = null;
            DbIterator<nxt.Order.Bid> iterator = Order.Bid.getSortedOrders(assetId, 0, Integer.MAX_VALUE);
            while (iterator.hasNext()) {
                VirtualBid virtualBid = getVirtualBid(iterator.next());
                if (virtualBid.getQuantityQNT() > 0) {
                    bestOrder = virtualBid;
                }
            }
          
            /* Look through the 'virtual' orders to see if there is a better order */
            for (VirtualBid bid : virtualBidMap.values()) {
                if (bid.getAssetId() == assetId) {
                    if (bid.getQuantityQNT() > 0) {
                        if (bestOrder == null || bid.compareTo(bestOrder) > 0) {
                            bestOrder = bid;
                        }
                    }
                }
            }
            return bestOrder;
        }
        
        /* called from ADDED_UNCONFIRMED_TRANSACTION for PLACE_ASK, PLACE_BID transactions */
        static VirtualBid addOrder(Transaction transaction, Attachment.ColoredCoinsBidOrderPlacement attachment, short transactionIndex) {
            VirtualBid order = new VirtualBid(transaction, attachment);
            order.setTransactionIndex(transactionIndex);
            order.save();
            notifyOrderAdded(order);
            matchOrders(attachment.getAssetId(), transaction.getTimestamp());
            return order;
        }
  
        /* called from ADDED_UNCONFIRMED_TRANSACTION for CANCEL_ASK, CANCEL_BID transactions */
        static VirtualBid removeOrder(long orderId) {
            VirtualBid order = virtualBidMap.get(Long.valueOf(orderId));
            if (order == null) {
                Bid bid = Bid.getBidOrder(orderId);
                if (bid == null) return null; /* this should never happen */
                order = new VirtualBid(bid);
            }
            if (order.getQuantityQNT() > 0) {
                order.setQuantityQNT(0);
                notifyOrderRemoved(order);
            }
            order.save();
            return order;
        }
        
        public static List<VirtualBid> getBids(long assetId, int firstIndex, int lastIndex, long accountId) {
            if (firstIndex < 0 || lastIndex < firstIndex) {
                throw new IndexOutOfBoundsException();
            }
            lastIndex = Math.min(firstIndex+100, lastIndex);
            if (virtualBidMap.isEmpty()) {
                List<VirtualBid> result = new ArrayList<VirtualBid>();
                try (
                    DbIterator<Bid> bids = accountId == 0 ? Order.Bid.getSortedOrders(assetId, firstIndex, lastIndex) :
                                                            Order.Bid.getBidOrdersByAccountAsset(accountId, assetId, firstIndex, lastIndex)
                ) {
                    while (bids.hasNext()) {
                        result.add(new VirtualBid(bids.next()));
                    }
                }
                return result;
            }
            return getMergedBids(assetId, firstIndex, lastIndex, accountId);
        }

        private static List<VirtualBid> getMergedBids(long assetId, int firstIndex, int lastIndex, final long accountId) {
            List<VirtualBid> sortedBids = new ArrayList<VirtualBid>();
            for (VirtualBid b : virtualBidMap.values()) {
                if (b.getAssetId() == assetId) {
                  sortedBids.add(b);
                }
            }
            Collections.sort(sortedBids, Collections.reverseOrder());
          
            List<VirtualBid> result = new ArrayList<VirtualBid>();
            try (DbIterator<Bid> dbIterator = Order.Bid.getSortedOrders(assetId, 0, Integer.MAX_VALUE)) {
              
                PeekableIterator<VirtualBid> virtualBids;
                PeekableIterator<Bid> bids;
                
                if (accountId != 0) {
                    virtualBids = new PeekableIterator<VirtualBid>(sortedBids.iterator(), new PeekableIterator.PeekableIteratorFilter<VirtualBid>() {
                        public boolean accept(VirtualBid order) {
                            return order.getAccountId() == accountId;
                        }
                    });
                    bids = new PeekableIterator<Bid>(dbIterator.iterator(), new PeekableIterator.PeekableIteratorFilter<Bid>() {
                          public boolean accept(Bid order) {
                            return order.getAccountId() == accountId;
                        }
                    });
                }
                else {
                    virtualBids = new PeekableIterator<VirtualBid>(sortedBids.iterator());
                    bids = new PeekableIterator<Bid>(dbIterator.iterator());
                }
              
                VirtualBid virtualBid = virtualBids.peek();
                VirtualBid bid = bids.peek() != null ? getVirtualBid(bids.peek()) : null;
                
                while ((virtualBid != null || bid != null) && result.size() < lastIndex) {
                    if (virtualBid == null) {
                        result.add(bid);
                        bids.next();
                        bid = bids.peek() != null ? getVirtualBid(bids.peek()) : null;
                    }
                    else if (bid == null) {
                        result.add(virtualBid);
                        virtualBids.next();
                        virtualBid = virtualBids.peek();
                    }
                    else {
                        int compared = bid.compareTo(virtualBid);
                        if (compared < 0) {
                            result.add(virtualBid);
                            virtualBids.next();
                            virtualBid = virtualBids.peek();
                        }
                        else {
                            result.add(bid);
                            bids.next();
                            bid = bids.peek() != null ? getVirtualBid(bids.peek()) : null;
                        }
                    }
                }
            }
            return result.subList(firstIndex, Math.min(result.size(), lastIndex));
        }        
    }
}
