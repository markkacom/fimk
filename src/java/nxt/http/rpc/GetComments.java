package nxt.http.rpc;

import java.util.List;

import nxt.MofoQueries;
import nxt.Transaction;
import nxt.db.DbIterator;
import nxt.http.ParameterException;
import nxt.http.websocket.JSONData;
import nxt.http.websocket.RPCCall;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class GetComments extends RPCCall {
    
    public static RPCCall instance = new GetComments("getComments");
    static int COUNT = 10;

    public GetComments(String identifier) {
        super(identifier);
    }
  
    @SuppressWarnings("unchecked")
    @Override
    public JSONStreamAware call(JSONObject arguments) throws ParameterException {
        long post_transaction_id = ParameterParser.getUnsignedLong(arguments, "post", true);
        int firstIndex = ParameterParser.getFirstIndex(arguments);
        int lastIndex = ParameterParser.getLastIndex(arguments);
        
        JSONArray response = new JSONArray();
        
        if (firstIndex == 0) {
            List<? extends Transaction> unconfirmed_transactions = MofoQueries.getUnconfirmedComments(post_transaction_id, COUNT);
            for (Transaction transaction : unconfirmed_transactions) {
                response.add(JSONData.transaction(transaction, true));
            }
        }
        
        try (
            DbIterator<? extends Transaction> iterator = MofoQueries.getComments(post_transaction_id, firstIndex, lastIndex);
        ) {  
            while (iterator.hasNext()) {
                response.add(JSONData.transaction(iterator.next(), false));
            }
        }
        return response;
    }
}
