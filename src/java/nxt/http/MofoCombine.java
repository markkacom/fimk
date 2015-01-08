package nxt.http;

import static nxt.http.JSONResponses.INCORRECT_JSON_ARGS;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import nxt.NxtException;
import nxt.http.APIServlet.APIRequestHandler;
import nxt.util.Convert;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;

public final class MofoCombine extends APIServlet.APIRequestHandler {

    static final MofoCombine instance = new MofoCombine();
    
    private MofoCombine() {
        super(new APITag[] {APITag.MOFO}, "combinedRequest");
    }
  
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
      
        String combinedRequest = Convert.emptyToNull(req.getParameter("combinedRequest"));
        if (combinedRequest == null) {
            throw new ParameterException(INCORRECT_JSON_ARGS);
        }
      
        JSONObject json = (JSONObject) JSONValue.parse(combinedRequest);
        if (json == null) {
            throw new ParameterException(INCORRECT_JSON_ARGS);            
        }
        
        if ( ! json.containsKey("requests")) {
            throw new ParameterException(INCORRECT_JSON_ARGS);
        }
        
        Object requests = json.get("requests");
        if ( ! (requests instanceof List)) {
            throw new ParameterException(INCORRECT_JSON_ARGS);
        }

        JSONArray responseList = new JSONArray();        
        
        /* Iterate over the collection of requests */
        for (Object request : (List) requests) {
          
            /* Each request is an array of name=value String pairs */
            if ( ! (request instanceof List)) {
                throw new ParameterException(INCORRECT_JSON_ARGS);
            }
          
            /* Create the fake request, populating it with parameters */
            FakeHttpServletRequest fakeReq = new FakeHttpServletRequest();
            for (Object param : (List) request) {
              
                if ( ! (param instanceof String)) {
                    throw new ParameterException(INCORRECT_JSON_ARGS);
                }
                
                String[] pair = ((String) param).split("=", 2);
                if (pair.length != 2) {
                    throw new ParameterException(INCORRECT_JSON_ARGS);
                }
                
                fakeReq.addParameter(pair[0], pair[1]);                
            }
            
            String requestType = Convert.emptyToNull(fakeReq.getParameter("requestType"));
            if (requestType == null) {
                throw new ParameterException(INCORRECT_JSON_ARGS);
            }
            
            APIRequestHandler apiRequestHandler = APIServlet.apiRequestHandlers.get(requestType);
            if (apiRequestHandler == null) {
                throw new ParameterException(INCORRECT_JSON_ARGS);
            }
            
            JSONObject resp = new JSONObject();
            resp.put("requestType", requestType);
            resp.put("response", apiRequestHandler.processRequest(fakeReq));
            
            responseList.add(resp);            
        }        
      
        JSONObject response = new JSONObject();
        response.put("responses", responseList);      
        return response;    
    }
    
    @Override
    boolean requirePost() {
        return true;
    }
}
