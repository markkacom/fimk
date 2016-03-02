package nxt.http.websocket;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import nxt.Block;
import nxt.BlockchainProcessor;
import nxt.Nxt;
import nxt.Transaction;
import nxt.TransactionProcessor;
import nxt.util.Listener;
import nxt.virtualexchange.VirtualOrder;
import nxt.virtualexchange.VirtualTrade;
import nxt.http.JSONData;

public class Events {

    static final ExecutorService threadPool = Executors.newFixedThreadPool(2);
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(
            new Runnable() {
                public void run() {
                    threadPool.shutdownNow();
                }
            }
        ));
    }

    private final ArrayList<TransactionTopicBuilder> transactionListeners;
    private final ArrayList<ExchangeTopicBuilder> exchangeListeners;

    static private Events instance = new Events();
    static public Events getInstance() {
        return instance;
    }

    public Events() {
        transactionListeners = new ArrayList<TransactionTopicBuilder>();
        exchangeListeners = new ArrayList<ExchangeTopicBuilder>();

        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            public void notify(Block t) {
                removeListeners();
            }
        }, BlockchainProcessor.Event.RESCAN_BEGIN);

        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            public void notify(Block t) {
                addListeners();
            }
        }, BlockchainProcessor.Event.RESCAN_END);

        addListeners();
    }

    private void addListeners() {
        Nxt.getTransactionProcessor().addListener(addedTransactionsListener, TransactionProcessor.Event.ADDED_TRANSACTIONS);
        Nxt.getTransactionProcessor().addListener(removedTransactionsListener, TransactionProcessor.Event.REMOVED_TRANSACTIONS);
        Nxt.getTransactionProcessor().addListener(addedConfirmedTransactionsListener, TransactionProcessor.Event.ADDED_CONFIRMED_TRANSACTIONS);

        VirtualTrade.addListener(createTradeListener, VirtualTrade.Event.CREATE);
        VirtualTrade.addListener(removeTradeListener, VirtualTrade.Event.REMOVE);
        VirtualTrade.addListener(updateTradeListener, VirtualTrade.Event.UPDATE);

        VirtualOrder.addListener(createOrderListener, VirtualOrder.Event.CREATE);
        VirtualOrder.addListener(removeOrderListener, VirtualOrder.Event.REMOVE);
        VirtualOrder.addListener(updateOrderListener, VirtualOrder.Event.UPDATE);
    }

    private void removeListeners() {
        Nxt.getTransactionProcessor().removeListener(addedTransactionsListener, TransactionProcessor.Event.ADDED_TRANSACTIONS);
        Nxt.getTransactionProcessor().removeListener(removedTransactionsListener, TransactionProcessor.Event.REMOVED_TRANSACTIONS);
        Nxt.getTransactionProcessor().removeListener(addedConfirmedTransactionsListener, TransactionProcessor.Event.ADDED_CONFIRMED_TRANSACTIONS);

        VirtualTrade.removeListener(createTradeListener, VirtualTrade.Event.CREATE);
        VirtualTrade.removeListener(removeTradeListener, VirtualTrade.Event.REMOVE);
        VirtualTrade.removeListener(updateTradeListener, VirtualTrade.Event.UPDATE);

        VirtualOrder.removeListener(createOrderListener, VirtualOrder.Event.CREATE);
        VirtualOrder.removeListener(removeOrderListener, VirtualOrder.Event.REMOVE);
        VirtualOrder.removeListener(updateOrderListener, VirtualOrder.Event.UPDATE);
    }

    private Listener<List<? extends Transaction>> addedTransactionsListener = new Listener<List<? extends Transaction>>() {
        public void notify(List<? extends Transaction> transactions) {
            for (TransactionTopicBuilder topic : transactionListeners) {
                List<Transaction> affectedTransactions = new ArrayList<Transaction>();
                for (Transaction transaction : transactions) {
                    if (topic.matchesTransaction(transaction)) {
                        affectedTransactions.add(transaction);
                    }
                }
                if (affectedTransactions.size() > 0) {
                    notifyAddedTransactions(topic, affectedTransactions);
                }
            }
        }
    };

    private Listener<List<? extends Transaction>> removedTransactionsListener = new Listener<List<? extends Transaction>>() {
        public void notify(List<? extends Transaction> transactions) {
            for (TransactionTopicBuilder topic : transactionListeners) {
                List<String> affectedTransactionIds = new ArrayList<String>();
                for (Transaction transaction : transactions) {
                    if (topic.matchesTransaction(transaction)) {
                        affectedTransactionIds.add(transaction.getStringId());
                    }
                }
                if (affectedTransactionIds.size() > 0) {
                    notifyRemovedTransactions(topic, affectedTransactionIds);
                }
            }
        }
    };

    private Listener<List<? extends Transaction>> addedConfirmedTransactionsListener = new Listener<List<? extends Transaction>>() {
        public void notify(List<? extends Transaction> transactions) {
            for (TransactionTopicBuilder topic : transactionListeners) {
                List<String> affectedTransactionIds = new ArrayList<String>();
                for (Transaction transaction : transactions) {
                    if (topic.matchesTransaction(transaction)) {
                        affectedTransactionIds.add(transaction.getStringId());
                    }
                }
                if (affectedTransactionIds.size() > 0) {
                    notifyConfirmedTransactions(topic, affectedTransactionIds);
                }
            }
        }
    };

    private Listener<VirtualTrade> createTradeListener = new Listener<VirtualTrade>() {
        public void notify(VirtualTrade trade) {
            for (ExchangeTopicBuilder topic : exchangeListeners) {
                if (topic.matchesTrade(trade)) {
                    notifyCreateTrade(topic, trade);
                }
            }
        }
    };

    private Listener<VirtualTrade> removeTradeListener = new Listener<VirtualTrade>() {
        public void notify(VirtualTrade trade) {
            for (ExchangeTopicBuilder topic : exchangeListeners) {
                if (topic.matchesTrade(trade)) {
                    notifyRemoveTrade(topic, trade);
                }
            }
        }
    };

    private Listener<VirtualTrade> updateTradeListener = new Listener<VirtualTrade>() {
        public void notify(VirtualTrade trade) {
            for (ExchangeTopicBuilder topic : exchangeListeners) {
                if (topic.matchesTrade(trade)) {
                    notifyUpdateTrade(topic, trade);
                }
            }
        }
    };

    private Listener<VirtualOrder> createOrderListener = new Listener<VirtualOrder>() {
        public void notify(VirtualOrder order) {
            for (ExchangeTopicBuilder topic : exchangeListeners) {
                if (topic.matchesOrder(order)) {
                    notifyCreateOrder(topic, order);
                }
            }
        }
    };

    private Listener<VirtualOrder> removeOrderListener = new Listener<VirtualOrder>() {
        public void notify(VirtualOrder order) {
            for (ExchangeTopicBuilder topic : exchangeListeners) {
                if (topic.matchesOrder(order)) {
                    notifyRemoveOrder(topic, order);
                }
            }
        }
    };

    private Listener<VirtualOrder> updateOrderListener = new Listener<VirtualOrder>() {
        public void notify(VirtualOrder order) {
            for (ExchangeTopicBuilder topic : exchangeListeners) {
                if (topic.matchesOrder(order)) {
                    notifyUpdateOrder(topic, order);
                }
            }
        }
    };

    public void socketClosed(MofoWebSocketAdapter adapter) {
        synchronized (transactionListeners) {
            transactionListeners.removeIf(t -> t.getAdapter().equals(adapter));
        }
    }

    public void subscribeTransaction(MofoWebSocketAdapter adapter, String topicValue) {
        transactionListeners.add(new TransactionTopicBuilder(adapter, topicValue));
    }

    public void unsubscribeTransaction(MofoWebSocketAdapter adapter, String topicValue) {
        synchronized (transactionListeners) {
            transactionListeners.removeIf(t -> t.getAdapter().equals(adapter) && t.getTopic().equals(topicValue));
        }
    }

    public void subscribeExchange(MofoWebSocketAdapter adapter, String topicValue) {
        exchangeListeners.add(new ExchangeTopicBuilder(adapter, topicValue));
    }

    public void unsubscribeExchange(MofoWebSocketAdapter adapter, String topicValue) {
        synchronized (exchangeListeners) {
            exchangeListeners.removeIf(t -> t.getAdapter().equals(adapter) && t.getTopic().equals(topicValue));
        }
    }

    @SuppressWarnings("unchecked")
    private void notifyAddedTransactions(TransactionTopicBuilder topic, List<Transaction> transactions) {
        JSONObject data = new JSONObject();
        data.put("target", "transaction");
        data.put("type", "add");
        JSONArray transactionsArray = new JSONArray();
        for (Transaction transaction : transactions) {
            transactionsArray.add(JSONData.unconfirmedTransaction(transaction));
        }
        data.put("transactions", transactionsArray);
        notify(topic.getAdapter(), topic.getTopic(), data);
    }

    @SuppressWarnings("unchecked")
    private void notifyRemovedTransactions(TransactionTopicBuilder topic, List<String> ids) {
        JSONObject data = new JSONObject();
        data.put("target", "transaction");
        data.put("type", "remove");
        data.put("transactions", ids);
        notify(topic.getAdapter(), topic.getTopic(), data);
    }

    @SuppressWarnings("unchecked")
    private void notifyConfirmedTransactions(TransactionTopicBuilder topic, List<String> ids) {
        JSONObject data = new JSONObject();
        data.put("target", "transaction");
        data.put("type", "confirm");
        data.put("height", Nxt.getBlockchain().getHeight());
        data.put("transactions", ids);
        notify(topic.getAdapter(), topic.getTopic(), data);
    }

    @SuppressWarnings("unchecked")
    private void notifyUpdateOrder(ExchangeTopicBuilder topic, VirtualOrder order) {
        JSONObject data = new JSONObject();
        data.put("target", order.getType()+"order"); /* @returns "bidorder" or "askorder" */
        data.put("type", "update");
        data.put("order", order.toJSONObject());
        data.put("asset", Long.toUnsignedString(order.getAssetId()));
        notify(topic.getAdapter(), topic.getTopic(), data);
    }

    @SuppressWarnings("unchecked")
    private void notifyRemoveOrder(ExchangeTopicBuilder topic, VirtualOrder order) {
        JSONObject data = new JSONObject();
        data.put("target", order.getType()+"order"); /* @returns "bidorder" or "askorder" */
        data.put("type", "remove");
        data.put("order", Long.toUnsignedString(order.getId()));
        data.put("asset", Long.toUnsignedString(order.getAssetId()));
        notify(topic.getAdapter(), topic.getTopic(), data);
    }

    @SuppressWarnings("unchecked")
    private void notifyCreateOrder(ExchangeTopicBuilder topic, VirtualOrder order) {
        JSONObject data = new JSONObject();
        data.put("target", order.getType()+"order"); /* @returns "bidorder" or "askorder" */
        data.put("type", "create");
        data.put("order", order.toJSONObject());
        data.put("asset", Long.toUnsignedString(order.getAssetId()));
        notify(topic.getAdapter(), topic.getTopic(), data);
    }

    @SuppressWarnings("unchecked")
    private void notifyUpdateTrade(ExchangeTopicBuilder topic, VirtualTrade trade) {
        JSONObject data = new JSONObject();
        data.put("target", "trade");
        data.put("type", "update");
        data.put("trade", trade.toJSONObject()); /* TODO - much to much info in standard JSON, should minimize for events */
        data.put("asset", Long.toUnsignedString(trade.getAssetId()));
        notify(topic.getAdapter(), topic.getTopic(), data);
    }

    @SuppressWarnings("unchecked")
    private void notifyRemoveTrade(ExchangeTopicBuilder topic, VirtualTrade trade) {
        JSONObject data = new JSONObject();
        data.put("target", "trade");
        data.put("type", "remove");
        data.put("trade", trade.toJSONObject()); /* TODO - much to much info in standard JSON, should minimize for events */
        data.put("asset", Long.toUnsignedString(trade.getAssetId()));
        notify(topic.getAdapter(), topic.getTopic(), data);
    }

    @SuppressWarnings("unchecked")
    private void notifyCreateTrade(ExchangeTopicBuilder topic, VirtualTrade trade) {
        JSONObject data = new JSONObject();
        data.put("target", "trade");
        data.put("type", "create");
        data.put("trade", trade.toJSONObject()); /* TODO - much to much info in standard JSON, should minimize for events */
        data.put("asset", Long.toUnsignedString(trade.getAssetId()));
        notify(topic.getAdapter(), topic.getTopic(), data);
    }

    @SuppressWarnings("unchecked")
    void notify(MofoWebSocketAdapter adapter, String topic, JSONObject data) {
        threadPool.submit(new Runnable() {
            public void run() {
                JSONArray array = new JSONArray();
                array.add("notify");
                array.add(topic);
                array.add(data);
                adapter.sendAsync(array.toJSONString());
            }
        });
    }
}
