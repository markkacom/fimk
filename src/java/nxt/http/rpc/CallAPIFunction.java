package nxt.http.rpc;

import static nxt.http.JSONResponses.INCORRECT_JSON_ARGS;
import nxt.NxtException;
import nxt.http.FakeHttpServletRequest;
import nxt.http.MofoCallAPIFunction;
import nxt.http.ParameterException;
import nxt.http.websocket.RPCCall;
import nxt.util.Convert;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class CallAPIFunction extends RPCCall {
    
    public static RPCCall instance = new CallAPIFunction("callAPIFunction");

    public CallAPIFunction(String identifier) {
        super(identifier);
    }
  
    @Override
    public JSONStreamAware call(JSONObject arguments) throws NxtException {
      
        String requestType = Convert.emptyToNull((String) arguments.get("requestType"));
        if (requestType == null) {
            throw new ParameterException(INCORRECT_JSON_ARGS);
        }
        
        FakeHttpServletRequest fakeReq = new FakeHttpServletRequest();

        for (Object paramKey : arguments.keySet()) {
          
            if (!(paramKey instanceof String)) {
                throw new ParameterException(INCORRECT_JSON_ARGS);
            }
            
            Object paramValue = arguments.get(paramKey);
            if (!(paramValue instanceof String)) {
                paramValue = String.valueOf(paramValue);
            }
            
            fakeReq.addParameter((String)paramKey, (String)paramValue);
        }
        
        return MofoCallAPIFunction.instance.processRequest(fakeReq);
    }  

}
