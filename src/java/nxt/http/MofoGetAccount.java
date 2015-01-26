package nxt.http;

import nxt.Account;
import nxt.NxtException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class MofoGetAccount extends APIServlet.APIRequestHandler {

    static final MofoGetAccount instance = new MofoGetAccount();
    
    private MofoGetAccount() {
        super(new APITag[] {APITag.MOFO}, "account" );
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
      
        Account account = ParameterParser.getAccount(req);        
        JSONObject response = JSONData.accountBalance(account);
        JSONData.putAccount(response, "account", account.getId());
        return response;
    }  

}
