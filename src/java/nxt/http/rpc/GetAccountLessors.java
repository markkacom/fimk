package nxt.http.rpc;

import nxt.Account;
import nxt.Nxt;
import nxt.db.DbIterator;
import nxt.http.ParameterException;
import nxt.http.websocket.JSONData;
import nxt.http.websocket.RPCCall;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class GetAccountLessors extends RPCCall {
    
    public static RPCCall instance = new GetAccountLessors("getAccountLessors");
    
    public GetAccountLessors(String identifier) {
        super(identifier);
    }
  
    @SuppressWarnings("unchecked")
    @Override
    public JSONStreamAware call(JSONObject arguments) throws ParameterException {
      Account account = ParameterParser.getAccount(arguments);
      int height = ParameterParser.getInt(arguments, "height", 0, Integer.MAX_VALUE, false);
      if (height == 0) {
          height = Nxt.getBlockchain().getHeight();
      }

      JSONObject response = new JSONObject();
      response.put("height", height);
      JSONArray lessorsJSON = new JSONArray();

      try (DbIterator<Account> lessors = account.getLessors(height)) {
          if (lessors.hasNext()) {
              while (lessors.hasNext()) {
                  Account lessor = lessors.next();
                  JSONObject lessorJSON = new JSONObject();
                  JSONData.putAccount(lessorJSON, "lessor", lessor.getId());
                  lessorJSON.put("guaranteedBalanceNQT", String.valueOf(lessor.getGuaranteedBalanceNQT(1440, height)));
                  lessorJSON.put("currentLeasingHeightFrom", lessor.getCurrentLeasingHeightFrom());
                  lessorJSON.put("currentLeasingHeightTo", lessor.getCurrentLeasingHeightTo());
                  lessorsJSON.add(lessorJSON);
              }
          }
      }
      response.put("lessors", lessorsJSON);
      return response;
    }  

}
