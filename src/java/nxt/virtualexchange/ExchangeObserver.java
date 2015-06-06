package nxt.virtualexchange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nxt.Block;
import nxt.BlockchainProcessor;
import nxt.Nxt;
import nxt.Order;
import nxt.Order.Ask;
import nxt.Order.Bid;
import nxt.Trade;
import nxt.Transaction;
import nxt.TransactionProcessor;
import nxt.TransactionType;
import nxt.Attachment.ColoredCoinsAskOrderCancellation;
import nxt.Attachment.ColoredCoinsAskOrderPlacement;
import nxt.Attachment.ColoredCoinsBidOrderCancellation;
import nxt.Attachment.ColoredCoinsBidOrderPlacement;
import nxt.http.websocket.JSONData;
import nxt.util.Listener;
import nxt.virtualexchange.VirtualOrder.VirtualAsk;
import nxt.virtualexchange.VirtualOrder.VirtualBid;

public class ExchangeObserver {
  
    static Map<Long, Ask> askOrders = new HashMap<Long, Ask>();
    static Map<Long, Bid> bidOrders = new HashMap<Long, Bid>();
    static List<Trade> trades = new ArrayList<Trade>();
    static short transactionIndex = 0;
    
    static Listener<Order> askOrderListener = new Listener<Order>() {
  
        @Override
        public void notify(Order order) {
            askOrders.put(Long.valueOf(order.getId()), (Ask) order);
        }
    };
    
    static Listener<Order> bidOrderListener = new Listener<Order>() {
  
        @Override
        public void notify(Order order) {
            bidOrders.put(Long.valueOf(order.getId()), (Bid) order);
        }
    };
    
    static Listener<Trade> tradeListener = new Listener<Trade>() {
  
        @Override
        public void notify(Trade trade) {
            trades.add(trade);
        }
    };  

    public static void init() {
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            
            @Override
            public void notify(Block block) {
                if (!Nxt.getBlockchainProcessor().isScanning()) {
                    processAsks();
                    processBids();
                    processTrades();
                }
                
                VirtualOrder.getVirtualAsks().clear();
                VirtualOrder.getVirtualBids().clear();
                VirtualTrade.getVirtualTrades().clear();
                
                askOrders.clear();
                bidOrders.clear();
                trades.clear();            }
        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
        
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {

            @Override
            public void notify(Block t) {
                transactionIndex = 0;
            }          
        }, BlockchainProcessor.Event.BLOCK_PUSHED);
      
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
  
            @Override
            public void notify(Block t) {
                transactionIndex = 0;
            }          
        }, BlockchainProcessor.Event.BLOCK_POPPED);

        Nxt.getTransactionProcessor().addListener(new Listener<List<? extends Transaction>>() {
          
            @Override
            public void notify(List<? extends Transaction> transactions) {
                for (Transaction transaction : transactions) {
                    processTransaction(transaction);
                }
            }
        }, TransactionProcessor.Event.ADDED_UNCONFIRMED_TRANSACTIONS);
          
        Order.Ask.addListener(askOrderListener, Order.Event.CREATE);
        Order.Ask.addListener(askOrderListener, Order.Event.UPDATE);
        Order.Ask.addListener(askOrderListener, Order.Event.REMOVE);
        
        Order.Bid.addListener(bidOrderListener, Order.Event.CREATE);
        Order.Bid.addListener(bidOrderListener, Order.Event.UPDATE);
        Order.Bid.addListener(bidOrderListener, Order.Event.REMOVE);
        
        Trade.addListener(tradeListener, Trade.Event.TRADE);      
    }
    
    protected static String formatTransactions(List<? extends Transaction> transactions) {
        StringBuilder builder = new StringBuilder();
        for (Transaction transaction : transactions) {
            builder.append(JSONData.transaction(transaction, true).toJSONString());
            builder.append("\n");
        }
        return builder.toString();
    }

    private static void processTransaction(Transaction transaction) {
        TransactionType type = transaction.getType();
        if (type.equals(TransactionType.ColoredCoins.ASK_ORDER_PLACEMENT)) {
            ColoredCoinsAskOrderPlacement attachment = (ColoredCoinsAskOrderPlacement) transaction.getAttachment();
            VirtualAsk ask = VirtualAsk.addOrder(transaction, attachment, transactionIndex++);
        }
        else if (type.equals(TransactionType.ColoredCoins.ASK_ORDER_CANCELLATION)) {
            ColoredCoinsAskOrderCancellation attachment = (ColoredCoinsAskOrderCancellation) transaction.getAttachment();
            VirtualAsk ask = VirtualAsk.removeOrder(attachment.getOrderId());
            //ask.setTransactionIndex(transactionIndex++);
        }
        else if (type.equals(TransactionType.ColoredCoins.BID_ORDER_PLACEMENT)) {
            ColoredCoinsBidOrderPlacement attachment = (ColoredCoinsBidOrderPlacement) transaction.getAttachment();
            VirtualBid bid = VirtualBid.addOrder(transaction, attachment, transactionIndex++);
        }
        else if (type.equals(TransactionType.ColoredCoins.BID_ORDER_CANCELLATION)) {
            ColoredCoinsBidOrderCancellation attachment = (ColoredCoinsBidOrderCancellation) transaction.getAttachment();
            VirtualBid bid = VirtualBid.removeOrder(attachment.getOrderId());
            //bid.setTransactionIndex(transactionIndex++);
        }
    }
    
    private static void processAsks() {
        Map<Long, VirtualAsk> virtualAsks = VirtualOrder.getVirtualAsks();
        for (Ask ask : askOrders.values()) {
            Long key = Long.valueOf(ask.getId());
            VirtualAsk virtualAsk = virtualAsks.get(key);
            if (virtualAsk != null) {
                if (ask.getQuantityQNT() != virtualAsk.getQuantityQNT()) {
                    virtualAsk.updateQuantityQNT(ask.getQuantityQNT());
                }
                virtualAsks.remove(key);
            }
            else {
                VirtualOrder.notifyOrderAdded(new VirtualAsk(ask));
            }
        }
        for (VirtualAsk ask : virtualAsks.values()) {
            VirtualOrder.notifyOrderRemoved(ask);
        }
    }
    
    private static void processBids() {
        Map<Long, VirtualBid> virtualBids = VirtualOrder.getVirtualBids();
        for (Bid bid : bidOrders.values()) {
            Long key = Long.valueOf(bid.getId());
            VirtualBid virtualBid = virtualBids.get(key);
            if (virtualBid != null) {
                if (bid.getQuantityQNT() != virtualBid.getQuantityQNT()) {
                    virtualBid.updateQuantityQNT(bid.getQuantityQNT());
                }
                virtualBids.remove(key);
            }
            else {
                VirtualOrder.notifyOrderAdded(new VirtualBid(bid));
            }
        }
        for (VirtualBid bid : virtualBids.values()) {
            VirtualOrder.notifyOrderRemoved(bid);
        }
    }
    
    private static void processTrades() {
        List<VirtualTrade> virtualTrades = VirtualTrade.getVirtualTrades();
        for (Trade trade : trades) {
            VirtualTrade virtualTrade = VirtualTrade.find(trade);
            if (virtualTrade != null) {
                if (trade.getQuantityQNT() != virtualTrade.getQuantityQNT()) {
                    VirtualTrade.notifyTradeUpdated(trade);
                }
                virtualTrades.remove(virtualTrade);
            }
            else {
                VirtualTrade.notifyTradeAdded(new VirtualTrade(trade));
            }
        }
        for (VirtualTrade trade : virtualTrades) {
            VirtualTrade.notifyTradeRemoved(trade);
        }
    }
}