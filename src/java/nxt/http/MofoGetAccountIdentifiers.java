package nxt.http;

import nxt.Account;
import nxt.Account.AccountIdentifier;
import nxt.NxtException;
import nxt.db.DbIterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

/* @api-name getAccountIdentifiers */
public final class MofoGetAccountIdentifiers extends APIServlet.APIRequestHandler {

    static final MofoGetAccountIdentifiers instance = new MofoGetAccountIdentifiers();

    private MofoGetAccountIdentifiers() {
        super(new APITag[] {APITag.ACCOUNTS}, "account", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        if (!Account.getAccountIDsEnabled()) {
            return JSONResponses.FEATURE_NOT_AVAILABLE;
        }
      
        long accountId = ParameterParser.getAccountId(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);        
        
        
        JSONArray result = new JSONArray();
        try (DbIterator<AccountIdentifier> identifiers = Account.getAccountIdentifiers(accountId, firstIndex, lastIndex)) {
            while (identifiers.hasNext()) {
                result.add(identifiers.next().getEmail());
            }
        }
        JSONObject response = new JSONObject();
        response.put("identifiers", result);
        return response;        
    }

}
