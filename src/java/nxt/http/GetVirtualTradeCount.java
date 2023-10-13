package nxt.http;

import nxt.NxtException;
import nxt.util.Convert;
import nxt.virtualexchange.VirtualTrade;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetVirtualTradeCount extends APIServlet.APIRequestHandler {

    static final GetVirtualTradeCount instance = new GetVirtualTradeCount();

    private GetVirtualTradeCount() {
        super(new APITag[] {APITag.AE}, "asset", "account");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        long assetId = ParameterParser.getAsset(req).getId();

        String accountValue = Convert.emptyToNull(req.getParameter("account"));
        long accountId = 0;
        if (accountValue != null) {
            accountId = ParameterParser.getAccountId(req);
        }

        JSONObject response = new JSONObject();
        int tradeCount = VirtualTrade.getTradeCount(assetId, accountId);
        response.put("tradeCount", tradeCount);
        return response;
    }

}
