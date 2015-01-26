package nxt.http;

import nxt.Account;
import nxt.MofoQueries;
import nxt.NxtException;
import nxt.Transaction;
import nxt.db.DbIterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class MofoGetAccountComments extends APIServlet.APIRequestHandler {

    static final MofoGetAccountComments instance = new MofoGetAccountComments();
    static final int COUNT = 20;
    
    private MofoGetAccountComments() {
        super(new APITag[] {APITag.MOFO}, "timestamp", "account" );
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {      
        Account account = ParameterParser.getAccount(req);
        int timestamp = ParameterParser.getTimestamp(req);
        
        JSONArray transactions = new JSONArray();
        try (
            DbIterator<? extends Transaction> iterator = MofoQueries.getAccountComments(account.getId(), timestamp, COUNT);
        ) {          
            while (iterator.hasNext()) {
                transactions.add(JSONData.transaction(iterator.next()));
            }
        }        
        
        JSONObject response = new JSONObject();
        response.put("comments", transactions);
        return response;
    }  

}
