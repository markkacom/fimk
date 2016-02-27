package nxt.http;

import java.util.List;

import nxt.NxtException;
import nxt.util.Convert;
import nxt.virtualexchange.VirtualTrade;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetVirtualTrades extends APIServlet.APIRequestHandler {

    static final GetVirtualTrades instance = new GetVirtualTrades();

    private GetVirtualTrades() {
        super(new APITag[] {APITag.AE}, "asset", "account", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        long assetId = ParameterParser.getAsset(req).getId();
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        String accountValue = Convert.emptyToNull(req.getParameter("account"));
        long accountId = 0;
        if (accountValue != null) {
            accountId = ParameterParser.getAccountId(req);
        }

        JSONArray json = new JSONArray();
        List<VirtualTrade> trades = VirtualTrade.getTrades(assetId, accountId, firstIndex, lastIndex);
        for (VirtualTrade trade : trades) {
            json.add(trade.toJSONObject());
        }

        JSONObject response = new JSONObject();
        response.put("trades", json);
        return response;

    }

}
