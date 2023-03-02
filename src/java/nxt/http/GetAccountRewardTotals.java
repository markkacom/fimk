package nxt.http;

import nxt.NxtException;
import nxt.reward.RewardItem;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.util.List;

public final class GetAccountRewardTotals extends APIServlet.APIRequestHandler {

    static final GetAccountRewardTotals instance = new GetAccountRewardTotals();

    private GetAccountRewardTotals() {
        super(new APITag[]{APITag.REWARDS}, "account");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        final long accountId = ParameterParser.getUnsignedLong(req, "account", true);

        JSONArray result = new JSONArray();
        List<RewardItem.TotalItem> totals = RewardItem.getTotals(accountId);
        for (RewardItem.TotalItem total : totals) {
            result.add(JSONData.rewardTotalItem(total));
        }

        JSONObject response = new JSONObject();
        response.put("rewardTotals", result);
        response.put("accountRS", Convert.rsAccount(accountId));
        return response;
    }

}
