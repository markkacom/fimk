package nxt.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import nxt.Appendix;
import nxt.Asset;
import nxt.Block;
import nxt.BlockchainProcessor;
import nxt.Genesis;
import nxt.Nxt;
import nxt.NxtException;
import nxt.Trade;
import nxt.Transaction;
import nxt.TransactionProcessor;
import nxt.Attachment.MonetarySystemCurrencyDeletion;
import nxt.Attachment.MonetarySystemCurrencyMinting;
import nxt.Attachment.MonetarySystemCurrencyTransfer;
import nxt.Attachment.MonetarySystemExchangeBuy;
import nxt.Attachment.MonetarySystemExchangeSell;
import nxt.Attachment.MonetarySystemPublishExchangeOffer;
import nxt.Attachment.MonetarySystemReserveClaim;
import nxt.Attachment.MonetarySystemReserveIncrease;
import nxt.NxtException.NotValidException;
import nxt.util.Convert;
import nxt.util.Listener;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class MofoEventSocket extends WebSocketAdapter {
  
    static final int MAX_PENDING              = 15;
    static final int ONE_DAY_SECONDS          = 24 * 60 * 60;
    static final long BLOCK_PUSH_MIN_WAIT_MS  = 250L;
    static final long BLOCK_POP_MIN_WAIT_MS   = 250L;
    static final long ADDED_CONFIRMED_MIN_WAIT_MS = 250L;
    
    static final ExecutorService threadPool   = Executors.newFixedThreadPool(1);    
    
    static long LAST_BLOCK_PUSH_TIMESTAMP     = 0L;
    static long LAST_BLOCK_POP_TIMESTAMP      = 0L;
    static long LAST_ADDED_CONFIRMED_TIMESTAMP = 0L;
    
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                threadPool.shutdownNow();
            }
        }));      
    }
    
    public static final Set<MofoEventSocket> sockets = new CopyOnWriteArraySet<MofoEventSocket>();
    
    static class SubscribeParameters {        
        private String methodName;
        private Object payload;
  
        public SubscribeParameters(String message) throws NotValidException {
            Object json = JSONValue.parse(message);
            if (! (json instanceof List)) {
                throw new NxtException.NotValidException("Invalid JSON");
            }
            if ( ((List) json).size() != 2) {
                throw new NxtException.NotValidException("Invalid parameters");
            }          
            Object method = ((List) json).get(0);
            if ( ! (method instanceof String)) {
                throw new NxtException.NotValidException("Invalid method");
            }
            
            methodName = (String) method;
            payload = ((List) json).get(1);
        }
        
        public String getMethod() {
            return methodName;
        }
        
        Object getProperty(String name) throws NotValidException {
            if ( ! (payload instanceof JSONObject)) {
                throw new NxtException.NotValidException("Invalid payload");
            }
            return ((JSONObject) payload).get(name);
        }
        
        public boolean getPropertyAsBool(String name) throws NotValidException {
            Object prop = getProperty(name);
            if ( ! (prop instanceof Boolean)) {
                throw new NxtException.NotValidException("Expected '"+name+"' to be a Boolean");
            }
            return Boolean.TRUE.equals(prop);
        }
        
        public List<String> getPropertyAsStringList(String name) throws NotValidException {
            Object prop = getProperty(name);
            if ( !(prop instanceof List)) {
                throw new NxtException.NotValidException("Expected '"+name+"' to be an Array");
            }
            List<String> list = new ArrayList<String>();
            for (int i=0; i<((List) prop).size(); i++) {
                Object entry = ((List) prop).get(i);
                if ( ! (entry instanceof String)) {
                    throw new NxtException.NotValidException("Expected element '"+i+"' of property '"+name+"' to be a String");
                }
                list.add((String) entry);
            }          
            return list;
        }
    }
    
    /**
     * Quick solution to cache Trade object instances for a short while. This
     * prevents us from having to touch the DB during block popped and block
     * pushed events when we want to broadcast affected Trades to socket 
     * subscribers.
     */
    static List<Trade> currentBlockTradeCache = Collections.synchronizedList(new ArrayList<Trade>());
    
    static Long[] getAffectedTransactionsAccountIds(List<? extends Transaction> transactions) {
        Set<Long> account_ids = new HashSet<Long>();
        for (Transaction transaction : transactions) {
            account_ids.add(transaction.getSenderId());
            if (transaction.getRecipientId() != Genesis.CREATOR_ID) {
                account_ids.add(transaction.getRecipientId());
            }
        }
        return account_ids.toArray(new Long[account_ids.size()]);
    }
    
    static Long[] getAffectedTradesAccountIds(Trade[] trades) {
        Set<Long> account_ids = new HashSet<Long>();
        for (Trade trade : trades) {
            account_ids.add(trade.getSellerId());
            account_ids.add(trade.getBuyerId());
        }
        return account_ids.toArray(new Long[account_ids.size()]);
    }

    static JSONArray JSONTransactions(Transaction[] transactions, boolean unconfirmed) {
        JSONArray result = new JSONArray();
        for (Transaction transaction : transactions) {
            JSONObject json = new JSONObject();
            json.put("transaction", transaction.getStringId());
            json.put("type", transaction.getType().getType());
            json.put("subtype", transaction.getType().getSubtype());
            json.put("timestamp", transaction.getTimestamp());
            json.put("height", transaction.getHeight());            
            if (unconfirmed) {
              json.put("confirmations", 0);
            }
            else {
              json.put("confirmations", Nxt.getBlockchain().getHeight() - transaction.getHeight());
            }
            JSONData.putAccount(json, "sender", transaction.getSenderId());
            if (transaction.getRecipientId() != 0) {
                JSONData.putAccount(json, "recipient", transaction.getRecipientId());
            }
            JSONObject attachmentJSON = new JSONObject();
            for (Appendix appendage : transaction.getAppendages()) {
                attachmentJSON.putAll(appendage.getJSONObject());
                if (transaction.getType().getType() == 5) {
                    final long currencyId;
                    switch (transaction.getType().getSubtype()) {
                        case 1: 
                            currencyId = ((MonetarySystemReserveIncrease) appendage).getCurrencyId(); 
                            break;  
                        case 2: 
                            currencyId = ((MonetarySystemReserveClaim) appendage).getCurrencyId(); 
                            break;
                        case 3: 
                            currencyId = ((MonetarySystemCurrencyTransfer) appendage).getCurrencyId(); 
                            break;
                        case 4: 
                            currencyId = ((MonetarySystemPublishExchangeOffer) appendage).getCurrencyId(); 
                            break;
                        case 5: 
                            currencyId = ((MonetarySystemExchangeBuy) appendage).getCurrencyId(); 
                            break;
                        case 6: 
                            currencyId = ((MonetarySystemExchangeSell) appendage).getCurrencyId(); 
                            break;
                        case 7: 
                            currencyId = ((MonetarySystemCurrencyMinting) appendage).getCurrencyId(); 
                            break;
                        case 8: 
                            currencyId = ((MonetarySystemCurrencyDeletion) appendage).getCurrencyId(); 
                            break;
                        default:
                            continue;
                    }                
                    JSONData.putCurrencyInfo(attachmentJSON, currencyId);
                }
            }
            if (! attachmentJSON.isEmpty()) {
                for (Map.Entry entry : (Iterable<Map.Entry>) attachmentJSON.entrySet()) {
                    if (entry.getValue() instanceof Long) {
                        entry.setValue(String.valueOf(entry.getValue()));
                    }
                }
                json.put("attachment", attachmentJSON);
            }
            result.add(json);
        }
        return result;
    }
    
    static JSONArray JSONTrades(Trade[] trades) {
        JSONArray result = new JSONArray();
        for (Trade trade : trades) {
            result.add(tradeJSON(trade));
        }
        return result;
    }
    
    static JSONObject tradeJSON(Trade trade) {
        JSONObject json = new JSONObject();
        json.put("timestamp", trade.getTimestamp());
        json.put("quantityQNT", String.valueOf(trade.getQuantityQNT()));
        json.put("priceNQT", String.valueOf(trade.getPriceNQT()));
        Asset.putAsset(json, trade.getAssetId());
        JSONData.putAccount(json, "seller", trade.getSellerId());
        JSONData.putAccount(json, "buyer", trade.getBuyerId());
        json.put("height", trade.getHeight());
        json.put("tradeType", trade.isBuy() ? "buy" : "sell");
        Asset asset = Asset.getAsset(trade.getAssetId());
        json.put("name", asset.getName());
        json.put("decimals", asset.getDecimals());
        return json;
    }
    
    static void broadcast(final String method,final Transaction[] transactions, final MofoEventSocket[] sockets, final boolean unconfirmed) {
        threadPool.submit(new Runnable() {
  
            @Override
            public void run() {
                JSONArray list = JSONTransactions(transactions, unconfirmed);
                MofoEventSocket.broadcastTo(method, list, sockets);
            }                      
        });
    }
    
    static void broadcast(final String method,final Trade[] trades, final MofoEventSocket[] sockets) {
        threadPool.submit(new Runnable() {
  
            @Override
            public void run() {
                JSONArray list = JSONTrades(trades);
                MofoEventSocket.broadcastTo(method, list, sockets);
            }                      
        });
    }
    
    static void broadcast(final String method, final Block block, final MofoEventSocket[] sockets) {
        threadPool.submit(new Runnable() {
  
            @Override
            public void run() {
                MofoEventSocket.broadcastTo(method, JSONData.minimalBlock(block), sockets);
            }                      
        });
    }
   
    static {
        
        Nxt.getTransactionProcessor().addListener(new Listener<List<? extends Transaction>>() {
            @Override
            public void notify(List<? extends Transaction> _transactions) {
                if ( ! _transactions.isEmpty() && ! Nxt.getBlockchainProcessor().isScanning() && ! MofoEventSocket.sockets.isEmpty()) {                  
                    Long[] account_ids = getAffectedTransactionsAccountIds(_transactions);
                    if (account_ids.length > 0) {                          
                        MofoEventSocket[] sockets = getAffectedSockets(account_ids, true);
                        if (sockets.length > 0) {
                            broadcast("removeUnConfirmedTransactions", _transactions.toArray(new Transaction[_transactions.size()]),  sockets, true);
                        }
                    }
                }
            }
        }, TransactionProcessor.Event.REMOVED_UNCONFIRMED_TRANSACTIONS);
  
        Nxt.getTransactionProcessor().addListener(new Listener<List<? extends Transaction>>() {
            @Override
            public void notify(List<? extends Transaction> _transactions) {
                if ( ! _transactions.isEmpty() && ! Nxt.getBlockchainProcessor().isScanning() && ! MofoEventSocket.sockets.isEmpty()) {                    
                    Long[] account_ids            = getAffectedTransactionsAccountIds(_transactions);
                    if (account_ids.length > 0) {                          
                        MofoEventSocket[] sockets = getAffectedSockets(account_ids, true);
                        if (sockets.length > 0) {
                            broadcast("addUnConfirmedTransactions", _transactions.toArray(new Transaction[_transactions.size()]),  sockets, true);
                        }
                    }
                }              
            }
        }, TransactionProcessor.Event.ADDED_UNCONFIRMED_TRANSACTIONS);
  
        Nxt.getTransactionProcessor().addListener(new Listener<List<? extends Transaction>>() {
            @Override
            public void notify(List<? extends Transaction> _transactions) {
                if ( ! _transactions.isEmpty() && ! Nxt.getBlockchainProcessor().isScanning() && ! MofoEventSocket.sockets.isEmpty()) {
              
                    boolean supportsToAll = (Nxt.getEpochTime() - _transactions.get(0).getTimestamp()) < ONE_DAY_SECONDS;                        
                    if (supportsToAll || (System.currentTimeMillis() - LAST_ADDED_CONFIRMED_TIMESTAMP) > ADDED_CONFIRMED_MIN_WAIT_MS) {
                      
                        LAST_ADDED_CONFIRMED_TIMESTAMP  = System.currentTimeMillis();
                        Long[] account_ids              = getAffectedTransactionsAccountIds(_transactions);
                        if (account_ids.length > 0) {                          
                            MofoEventSocket[] sockets   = getAffectedSockets(account_ids, supportsToAll);
                            if (sockets.length > 0) {
                                broadcast("addConfirmedTransactions", _transactions.toArray(new Transaction[_transactions.size()]),  sockets, false);
                            }
                        }
                    }
                }
            }
        }, TransactionProcessor.Event.ADDED_CONFIRMED_TRANSACTIONS);
      
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(final Block block) {
                if ((System.currentTimeMillis() - LAST_BLOCK_POP_TIMESTAMP) > BLOCK_POP_MIN_WAIT_MS ||
                    (Nxt.getEpochTime() - block.getTimestamp()) < ONE_DAY_SECONDS) {
                  
                    LAST_BLOCK_POP_TIMESTAMP = System.currentTimeMillis();                    
                    if ( ! Nxt.getBlockchainProcessor().isScanning() && ! MofoEventSocket.sockets.isEmpty()) {
                        broadcast("blockPopped", block,  sockets.toArray(new MofoEventSocket[sockets.size()]));
                    }
                }
            }
        }, BlockchainProcessor.Event.BLOCK_POPPED);        
      
        /* Cache Trade object instances for a short while. */
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                currentBlockTradeCache.clear();
            }
        }, BlockchainProcessor.Event.BEFORE_BLOCK_APPLY);
        
        /* Cache Trade object instances for a short while. */
        Trade.addListener(new Listener<Trade>() {
            @Override
            public void notify(Trade trade) {
                currentBlockTradeCache.add(trade);
            }
        }, Trade.Event.TRADE);

        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(final Block block) {
                long currentTime = System.currentTimeMillis();
                if ((currentTime - LAST_BLOCK_PUSH_TIMESTAMP) > BLOCK_PUSH_MIN_WAIT_MS || 
                    (Nxt.getEpochTime() - block.getTimestamp()) < ONE_DAY_SECONDS) {
                    LAST_BLOCK_PUSH_TIMESTAMP = currentTime;              
              
                    if ( ! Nxt.getBlockchainProcessor().isScanning() && ! MofoEventSocket.sockets.isEmpty()) {
                        broadcast("blockPushed", block,  sockets.toArray(new MofoEventSocket[sockets.size()]));
                    }
                }
                
                final Trade[] trades;
                if ( ! currentBlockTradeCache.isEmpty() ) {
                    trades = currentBlockTradeCache.toArray(new Trade[currentBlockTradeCache.size()]);
                }
                else {
                    trades = null;
                }
                if (trades != null && trades.length > 0 && trades[0] != null) {                  
                    boolean supportsToAll = (Nxt.getEpochTime() - trades[0].getTimestamp()) < ONE_DAY_SECONDS;  
                    Long[] account_ids    = getAffectedTradesAccountIds(trades);
                    if (account_ids.length > 0) {                      
                        MofoEventSocket[] sockets = getAffectedSockets(account_ids, supportsToAll);
                        if (sockets.length > 0) {
                            broadcast("addedTrades", trades,  sockets);
                        }
                    }
                }              
            }
        }, BlockchainProcessor.Event.BLOCK_PUSHED);
    }
    
    private final List<Long> subscriptions = new ArrayList<Long>();
    private final List<Future<Void>> pending = new ArrayList<Future<Void>>();
    private boolean subscribeToAll = false;
    
    @Override
    public void onWebSocketConnect(Session sess) {
        super.onWebSocketConnect(sess);
        // System.out.println("Socket Connected: " + sess);
        if (API.allowedBotHosts != null && ! API.allowedBotHosts.contains(sess.getRemoteAddress().getHostName())) {
            try {
                System.out.println("Disconnecting because of not-allowed bot host");
                sess.disconnect();
            }
            catch (IOException e) {
                e.printStackTrace(System.err);
            }
            return;
        }
        sockets.add(this);
        
        JSONObject json = new JSONObject();
        json.put("connected", true);
        broadcast("status", json);
    }

    @Override
    public void onWebSocketText(String message) {
        super.onWebSocketText(message);
        // System.out.println("Received TEXT message: " + message);
        try {
            SubscribeParameters params = new SubscribeParameters(message);
            if ("subscribe".equalsIgnoreCase(params.getMethod())) {
                subscribe(params);
            }
        }
        catch (NotValidException e) {
            JSONObject response = new JSONObject();
            response.put("message", e.getMessage());
            broadcast("error", response);
        }        
    }    
    
    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        super.onWebSocketClose(statusCode,reason);
        // System.out.println("Socket Closed: [" + statusCode + "] " + reason);
        sockets.remove(this);
    }
    
    @Override
    public void onWebSocketError(Throwable cause) {
        super.onWebSocketError(cause);
        cause.printStackTrace(System.err);
    }
    
    private int getPendingSize() {
        List<Future> cloned = new ArrayList<Future>(pending);      
        for (Future future : cloned) {
            if (future.isDone()) {
                pending.remove(future);
            }
        }
        return pending.size();
    }
    
    private void broadcast(String method, Object data) {
        Session session = getSession();      
        if (session != null && session.isOpen()) {
            int count = getPendingSize(); 
            if (count == MAX_PENDING) {
                JSONArray response = new JSONArray();
                response.add("serverOverflow");
                response.add("must-reload");            
                pending.add(getSession().getRemote().sendStringByFuture(response.toJSONString()));
            }
            else if (count < MAX_PENDING) {
                JSONArray response = new JSONArray();
                response.add(method);
                response.add(data);            
                pending.add(getSession().getRemote().sendStringByFuture(response.toJSONString()));
            }
        }
        else {
            if (session != null) {
                try {
                    session.disconnect();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            sockets.remove(this);
        }
    }
    
    static void broadcastTo(String method, Object data, MofoEventSocket[] _sockets) {
        for (MofoEventSocket socket : _sockets) {
            socket.broadcast(method, data);
        }
    }
    
    static MofoEventSocket[] getAffectedSockets(Long[] account_ids, boolean supportsToAll) {
        List<MofoEventSocket> result = null;
        List<MofoEventSocket> remove = null;  
        
        synchronized (sockets) {
        
            for (MofoEventSocket socket : sockets) {
                if (socket.getSession() == null || ! socket.getSession().isOpen()) {
                    if (remove == null) {
                        remove = new ArrayList<MofoEventSocket>();
                    }
                    remove.add(socket);
                    continue;
                }
              
                boolean isSubscribed = (socket.subscribeToAll && supportsToAll) || account_ids == null; 
                if ( ! isSubscribed && account_ids != null) {
                    for (long id : account_ids) {
                        if (socket.subscriptions.contains(id)) {
                            isSubscribed = true;
                            break;
                        }
                    }
                }
                if (isSubscribed) {
                    if (result == null) {
                        result = new ArrayList<MofoEventSocket>();
                    }
                    result.add(socket);
                }
            }
            
            if (remove != null) {
                for (MofoEventSocket socket : remove) {
                    sockets.remove(socket);
                }
            }
        }
        
        return result != null ? result.toArray(new MofoEventSocket[result.size()]) : null;
    }
    
    private void subscribe(SubscribeParameters params) throws NotValidException {
        subscriptions.clear();
        subscribeToAll = params.getPropertyAsBool("all");
        for (String accountValue : params.getPropertyAsStringList("accounts")) {
            long account_id = Convert.parseAccountId(accountValue);
            if (! subscriptions.contains(account_id)) {
                subscriptions.add(account_id);
            }
        }
    }
}