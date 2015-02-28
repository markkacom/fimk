package nxt.http.rpc;

import nxt.Account;
import nxt.Generator;
import nxt.http.ParameterException;
import nxt.http.websocket.JSONData;
import nxt.http.websocket.RPCCall;
import nxt.util.Convert;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class GetAccount extends RPCCall {
    
    public static RPCCall instance = new GetAccount("getAccount");

    public GetAccount(String identifier) {
        super(identifier);
    }
  
    @SuppressWarnings("unchecked")
    @Override
    public JSONStreamAware call(JSONObject arguments) throws ParameterException {
      
        Account account = ParameterParser.getAccount(arguments);        
        JSONObject response = JSONData.accountBalance(account);
        JSONData.putAccount(response, "account", account.getId());
        if (account != null) {
            if (account.getPublicKey() != null) {
                response.put("publicKey", Convert.toHexString(account.getPublicKey()));              
            }          
            response.put("description", account.getDescription());          
            response.put("isForging", Generator.isAccountForging(account.getId()));
        }
        return response;      
    }  

}
