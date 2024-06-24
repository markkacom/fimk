package nxt.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import nxt.NxtException;
import nxt.reward.RewardItem;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import java.util.List;

@Path("/fimk?requestType=accountColorList")
public final class GetAccountRewardTotals extends APIServlet.APIRequestHandler {

    static final GetAccountRewardTotals instance = new GetAccountRewardTotals();

    private GetAccountRewardTotals() {
        super(new APITag[]{APITag.REWARDS}, "account");
    }

    @Override
    @Operation(summary = "Get account phased transactions",
            tags = {APITag2.REWARDS})
    @Parameter(name = "account", in = ParameterIn.QUERY, required = true, description = "account id")
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
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
