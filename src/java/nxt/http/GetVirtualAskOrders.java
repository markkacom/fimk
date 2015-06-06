package nxt.http;

import java.util.List;

import nxt.NxtException;
import nxt.util.Convert;
import nxt.virtualexchange.VirtualOrder.VirtualAsk;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetVirtualAskOrders extends APIServlet.APIRequestHandler {

    static final GetVirtualAskOrders instance = new GetVirtualAskOrders();

    private GetVirtualAskOrders() {
        super(new APITag[] {APITag.AE}, "asset", "firstIndex", "lastIndex", "account");
    }

    @SuppressWarnings("unchecked")
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

        JSONArray orders = new JSONArray();
        List<VirtualAsk> askOrders = VirtualAsk.getAsks(assetId, firstIndex, lastIndex, accountId);
        for (VirtualAsk ask : askOrders) {
            orders.add(ask.toJSONObject());
        }

        JSONObject response = new JSONObject();
        response.put("askOrders", orders);
        return response;

    }

}
