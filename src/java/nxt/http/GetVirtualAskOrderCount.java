package nxt.http;

import java.util.List;

import nxt.NxtException;
import nxt.util.Convert;
import nxt.virtualexchange.VirtualOrder.VirtualAsk;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetVirtualAskOrderCount extends APIServlet.APIRequestHandler {

    static final GetVirtualAskOrderCount instance = new GetVirtualAskOrderCount();

    private GetVirtualAskOrderCount() {
        super(new APITag[] {APITag.AE}, "asset", "account");
    }

    @SuppressWarnings("unchecked")
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        long assetId = ParameterParser.getAsset(req).getId();

        String accountValue = Convert.emptyToNull(req.getParameter("account"));
        long accountId = 0;
        if (accountValue != null) {
            accountId = ParameterParser.getAccountId(req);
        }

        JSONObject response = new JSONObject();
        int askOrderCount = VirtualAsk.getAskCount(assetId, accountId);
        response.put("askOrderCount", askOrderCount);
        return response;
    }

}
