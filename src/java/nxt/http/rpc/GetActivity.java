package nxt.http.rpc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import nxt.Block;
import nxt.MofoQueries;
import nxt.Nxt;
import nxt.Trade;
import nxt.Transaction;
import nxt.MofoQueries.TransactionFilter;
import nxt.UnconfirmedTransaction;
import nxt.db.DbIterator;
import nxt.http.ParameterException;
import nxt.http.websocket.JSONData;
import nxt.http.websocket.RPCCall;
import nxt.util.Convert;

public class GetActivity extends RPCCall {

    public static RPCCall instance = new GetActivity("getActivity");
    static int COUNT = 15;

    public GetActivity(String identifier) {
        super(identifier);
    }

    @SuppressWarnings("unchecked")
    @Override
    public JSONStreamAware call(JSONObject arguments) throws ParameterException {
        long accountId = 0;
        String accountIdVal = Convert.emptyToNull((String) arguments.get("account"));
        if (accountIdVal != null) {
            accountId = Convert.parseAccountId(accountIdVal);
        }
        int timestamp = ParameterParser.getTimestamp(arguments);
        if (timestamp == 0) {
            timestamp = Nxt.getEpochTime();
        }
        boolean includeAssetInfo    = "true".equals(arguments.get("includeAssetInfo"));
        boolean includeBlocks       = "true".equals(arguments.get("includeBlocks"));
        boolean includeTrades       = ! "false".equals(arguments.get("includeTrades"));
        boolean includeTransactions = ! "false".equals(arguments.get("includeTransactions"));

        List<TransactionFilter> filters = ParameterParser.getTransactionFilter(arguments);

        List<Trade> trades = null;
        List<Block> blocks = null;
        List<Transaction> transactions = null;
        List<Transaction> unconfirmedTransactions = getUnconfirmedTransactions(accountId, timestamp, filters, includeTransactions);
        Iterator<Transaction> unconfirmedTransactionIterator = null;
        if (includeTransactions && unconfirmedTransactions != null) {
            unconfirmedTransactionIterator = unconfirmedTransactions.iterator();
        }

        try (
            DbIterator<? extends Trade> tradeIterator = getTrades(accountId, timestamp, includeTrades);
            DbIterator<? extends Block> blockIterator = getBlocks(accountId, timestamp, includeBlocks);
            DbIterator<? extends Transaction> transactionIterator = getTransactions(accountId, timestamp, filters, includeTransactions);
        ) {
            int i = 0;
            Trade trade = null;
            Block block = null;
            Transaction transaction = null;

            while (i++ < COUNT) {
                if (includeTrades && trade == null && tradeIterator.hasNext()) {
                    trade = tradeIterator.next();
                }
                if (includeBlocks && block == null && blockIterator.hasNext()) {
                    block = blockIterator.next();
                }
                if (includeTransactions && transaction == null) {
                    if (unconfirmedTransactionIterator != null && unconfirmedTransactionIterator.hasNext()) {
                        transaction = unconfirmedTransactionIterator.next();
                    }
                    else if (transactionIterator.hasNext()) {
                        transaction = transactionIterator.next();
                    }
                }

                switch (getHighestIndex(
                          block == null ? 0 : block.getTimestamp(),
                          transaction == null ? 0 : transaction.getTimestamp(),
                          trade == null ? 0 : trade.getTimestamp())) {
                case 0:
                    if (block != null) {
                        if (blocks == null) {
                            blocks = new ArrayList<Block>();
                        }
                        blocks.add(block);
                        block = null;
                    }
                    break;
                case 1:
                    if (transaction != null) {
                        if (transactions == null) {
                            transactions = new ArrayList<Transaction>();
                        }
                        transactions.add(transaction);
                        transaction = null;
                    }
                    break;
                case 2:
                    if (trade != null) {
                        if (trades == null) {
                            trades = new ArrayList<Trade>();
                        }
                        trades.add(trade);
                        trade = null;
                    }
                    break;
                }
            }
        }

        JSONObject response = new JSONObject();

        if (trades != null) {
            JSONArray array = new JSONArray();
            for (Trade trade : trades) {
                array.add(JSONData.trade(trade, includeAssetInfo));
            }
            response.put("trades", array);
        }

        if (blocks != null) {
            JSONArray array = new JSONArray();
            for (Block block : blocks) {
                array.add(JSONData.minimalBlock(block));
            }
            response.put("blocks", array);
        }

        if (transactions != null) {
            JSONArray array = new JSONArray();
            for (Transaction transaction : transactions) {
                if (transaction instanceof UnconfirmedTransaction) {
                    array.add(JSONData.transaction(transaction, true));
                }
                else {
                    array.add(JSONData.transaction(transaction, false));
                }
            }
            response.put("transactions", array);
        }

        return response;
    }


    private DbIterator<? extends Trade> getTrades(long accountId, int timestamp, boolean includeTrades) {
        if (includeTrades) {
            if (accountId != 0) {
                return MofoQueries.getAccountAssetTradesBefore(accountId, timestamp, COUNT);
            }
            return MofoQueries.getAssetTradesBefore(timestamp, COUNT);
        }
        return null;
    }

    private DbIterator<? extends Block> getBlocks(long accountId, int timestamp, boolean includeBlocks) {
        if (includeBlocks) {
            if (accountId != 0) {
                return MofoQueries.getBlocks(accountId, timestamp, COUNT);
            }
            return MofoQueries.getBlocks(timestamp, COUNT);
        }
        return null;
    }

    private DbIterator<? extends Transaction> getTransactions(long accountId, int timestamp, List<TransactionFilter> filters, boolean includeTransactions) {
        if (includeTransactions) {
            if (accountId != 0) {
                return MofoQueries.getTransactions(accountId, timestamp, COUNT, filters);
            }
            return MofoQueries.getTransactions(timestamp, COUNT, filters);
        }
        return null;
    }

    private List<Transaction> getUnconfirmedTransactions(long accountId, int timestamp, List<TransactionFilter> filters, boolean includeTransactions) {
        if (includeTransactions && Nxt.getBlockchain().getLastBlock().getTimestamp() < timestamp) {
            if (accountId != 0) {
                return MofoQueries.getUnconfirmedTransactions(accountId, timestamp, COUNT, filters);
            }
            return MofoQueries.getUnconfirmedTransactions(timestamp, COUNT, filters);
        }
        return null;
    }

    private int getHighestIndex(int x, int y, int z) {
        if (x >= y && x >= z) { return 0; }
        if (y >= x && y >= z) { return 1; }
        return 2;
    }
}
