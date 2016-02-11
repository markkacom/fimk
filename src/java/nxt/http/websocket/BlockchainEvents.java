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

import nxt.http.JSONData;

public class BlockchainEvents {

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

    static private BlockchainEvents instance = new BlockchainEvents();
    static public BlockchainEvents getInstance() {
        return instance;
    }

    public BlockchainEvents() {
        transactionListeners = new ArrayList<TransactionTopicBuilder>();

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
    }

    private void removeListeners() {
        Nxt.getTransactionProcessor().addListener(addedTransactionsListener, TransactionProcessor.Event.ADDED_TRANSACTIONS);
        Nxt.getTransactionProcessor().addListener(removedTransactionsListener, TransactionProcessor.Event.REMOVED_TRANSACTIONS);
        Nxt.getTransactionProcessor().addListener(addedConfirmedTransactionsListener, TransactionProcessor.Event.ADDED_CONFIRMED_TRANSACTIONS);
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

    public void socketClosed(MofoWebSocketAdapter adapter) {
        synchronized (transactionListeners) {
            transactionListeners.removeIf(t -> t.getAdapter().equals(adapter));
        }
    }

    public void subscribe(MofoWebSocketAdapter adapter, String topicValue) {
        transactionListeners.add(new TransactionTopicBuilder(adapter, topicValue));
    }

    public void unsubscribe(MofoWebSocketAdapter adapter, String topicValue) {
        synchronized (transactionListeners) {
            transactionListeners.removeIf(t -> t.getAdapter().equals(adapter) && t.getTopic().equals(topicValue));
        }
    }

    @SuppressWarnings("unchecked")
    private void notifyAddedTransactions(TransactionTopicBuilder topic, List<Transaction> transactions) {
        JSONObject data = new JSONObject();
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
        data.put("type", "remove");
        data.put("transactions", ids);
        notify(topic.getAdapter(), topic.getTopic(), data);
    }

    @SuppressWarnings("unchecked")
    private void notifyConfirmedTransactions(TransactionTopicBuilder topic, List<String> ids) {
        JSONObject data = new JSONObject();
        data.put("type", "confirm");
        data.put("height", Nxt.getBlockchain().getHeight());
        data.put("transactions", ids);
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
