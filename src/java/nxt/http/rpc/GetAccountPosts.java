package nxt.http.rpc;

import java.io.UnsupportedEncodingException;
import java.util.List;

import nxt.Account;
import nxt.MofoQueries;
import nxt.Transaction;
import nxt.http.ParameterException;
import nxt.http.websocket.JSONData;
import nxt.http.websocket.RPCCall;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class GetAccountPosts extends RPCCall {
    
    public static RPCCall instance = new GetAccountPosts("getAccountPosts");
    static int COUNT = 10;

    public GetAccountPosts(String identifier) {
        super(identifier);
    }
  
    @SuppressWarnings("unchecked")
    @Override
    public JSONStreamAware call(JSONObject arguments) throws ParameterException, UnsupportedEncodingException {
        Account acount = ParameterParser.getAccount(arguments);      
        int firstIndex = ParameterParser.getFirstIndex(arguments);
        int lastIndex = ParameterParser.getLastIndex(arguments);
        
        JSONArray response = new JSONArray();
        
        if (firstIndex == 0) {
            List<? extends Transaction> unconfirmed_transactions = MofoQueries.getUnconfirmedAccountPosts(acount.getId(), COUNT);
            for (Transaction transaction : unconfirmed_transactions) {
                response.add(JSONData.transaction(transaction, true));
            }
        }
        
        response.addAll(MofoQueries.getAccountPosts(acount.getId(), firstIndex, lastIndex));        
        return response;
    }
}
