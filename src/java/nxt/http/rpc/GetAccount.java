package nxt.http.rpc;

import nxt.Account;
import nxt.Generator;
import nxt.Nxt;
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
        boolean includeForging = "true".equalsIgnoreCase((String) arguments.get("includeForging"));
        
        JSONObject response = JSONData.accountBalance(account);
        JSONData.putAccount(response, "account", account.getId());
        if (account != null) {
            if (account.getPublicKey() != null) {
                response.put("publicKey", Convert.toHexString(account.getPublicKey()));              
            }          
            response.put("description", account.getDescription());
            
            if (account.getCurrentLeasingHeightFrom() != Integer.MAX_VALUE) {
                response.put("leasingHeightFrom", account.getCurrentLeasingHeightFrom());
                response.put("leasingHeightTo", account.getCurrentLeasingHeightTo());
                response.put("height", Nxt.getBlockchain().getHeight());
                response.put("lesseeIdRS", Convert.rsAccount(account.getCurrentLesseeId()));
            }
            
            if (includeForging) {
                Generator generator = Generator.getGenerator(account.getId());
                if (generator == null) {
                    response.put("isForging", false);
                }
                else {
                    response.put("isForging", true);
                    response.put("deadline", generator.getDeadline());
                    response.put("hitTime", generator.getHitTime());
                }
            }
        }
        return response;      
    }  

}
