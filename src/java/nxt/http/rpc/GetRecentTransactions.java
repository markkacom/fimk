package nxt.http.rpc;

import java.util.ArrayList;
import java.util.List;

import nxt.MofoQueries;
import nxt.MofoQueries.TransactionFilter;
import nxt.Nxt;
import nxt.Transaction;
import nxt.db.DbIterator;
import nxt.http.ParameterException;
import nxt.http.websocket.JSONData;
import nxt.http.websocket.RPCCall;
import nxt.util.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetRecentTransactions extends RPCCall {

  public static RPCCall instance = new GetRecentTransactions("getRecentTransactions");
  static int COUNT = 15;

  public GetRecentTransactions(String identifier) {
      super(identifier);
  }

  @SuppressWarnings({ "unchecked" })
  @Override
  public JSONStreamAware call(JSONObject arguments) throws ParameterException {
      
        List<Long> accountIds = ParameterParser.getAccountIds(arguments);
        int timestamp = ParameterParser.getTimestamp(arguments);        
        List<TransactionFilter> filters = ParameterParser.getTransactionFilter(arguments);
        List<Transaction> unconfirmed_transactions = new ArrayList<Transaction>();
        List<Transaction> confirmed_transactions = new ArrayList<Transaction>();
        
        JSONArray transactions = new JSONArray();
        JSONObject response = new JSONObject();        
        
        try {
       
            /* Unconfirmed transactions are always included */
           
            try (
                DbIterator<? extends Transaction> iterator_unconfirmed = Nxt.getTransactionProcessor().getAllUnconfirmedTransactions();
                DbIterator<? extends Transaction> iterator = MofoQueries.getRecentTransactions(accountIds, timestamp, filters, COUNT);
            ) {
                try {
                    while (iterator_unconfirmed != null && iterator_unconfirmed.hasNext()) {
                        Transaction transaction = iterator_unconfirmed.next();
                        for (Long id : accountIds) {
                            if (id.equals(Long.valueOf(transaction.getSenderId())) || id.equals(Long.valueOf(transaction.getRecipientId()))) {
                                unconfirmed_transactions.add(transaction);
                                break;
                            }
                        }
                    }
                }
                catch (Exception e) {
                    Logger.logErrorMessage("Error iterating unconfirmed transactions", e);
                }                
                finally {                   
                    while (iterator != null && iterator.hasNext()) {
                      confirmed_transactions.add(iterator.next());
                    }
                }
            }
            
            for (Transaction transaction : unconfirmed_transactions) {
                transactions.add(JSONData.transaction(transaction, true));
            }
            for (Transaction transaction : confirmed_transactions) {
                transactions.add(JSONData.transaction(transaction, false));
            }
        }
        catch (Exception e) {
            Logger.logErrorMessage("Error in GetRecentTransactions", e);
        }
        finally {        
            response.put("transactions", transactions);
        }
        return response;        
    }  

}
