package nxt.http;

import java.util.List;

import nxt.MofoQueries;
import nxt.MofoQueries.TransactionFilter;
import nxt.Nxt;
import nxt.Transaction;
import nxt.NxtException;
import nxt.db.DbIterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class MofoGetRecentTransactions extends APIServlet.APIRequestHandler {

    static final MofoGetRecentTransactions instance = new MofoGetRecentTransactions();
    static final int COUNT = 20;
    
    private MofoGetRecentTransactions() {
        super(new APITag[] {APITag.MOFO}, "account", "timestamp", "transactionFilter" );
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
      
        List<Long> accountIds = ParameterParser.getAccountIds(req);
        int timestamp = ParameterParser.getTimestamp(req);        
        List<TransactionFilter> filters = ParameterParser.getTransactionFilter(req);
        
        JSONArray transactions = new JSONArray();
       
        /* Unconfirmed transactions are always included */
        try (
            DbIterator<? extends Transaction> iterator = Nxt.getTransactionProcessor().getAllUnconfirmedTransactions()
        ) {
            while (iterator.hasNext()) {
                Transaction transaction = iterator.next();
                for (Long id : accountIds) {
                    if (id.equals(Long.valueOf(transaction.getSenderId())) || id.equals(Long.valueOf(transaction.getRecipientId()))) {
                        transactions.add(JSONData.unconfirmedTransaction(transaction));
                        break;
                    }
                }
            }
        }        
        
        try (
            DbIterator<? extends Transaction> iterator = MofoQueries.getRecentTransactions(accountIds, timestamp, filters, COUNT);
        ) {          
            while (iterator.hasNext()) {
                transactions.add(JSONData.transaction(iterator.next()));
            }
        }
        
        JSONObject response = new JSONObject();
        response.put("transactions", transactions);
        return response;
    }  

}
