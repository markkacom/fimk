/******************************************************************************
 * Copyright © 2013-2015 The Nxt Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * Nxt software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package nxt.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import nxt.NxtException;
import nxt.reward.RewardItem;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import java.util.List;

@Path("/fimk?requestType=getRewardTotals")
public final class GetRewardTotals extends APIServlet.APIRequestHandler {

    static final GetRewardTotals instance = new GetRewardTotals();

    private GetRewardTotals() {
        super(new APITag[]{APITag.REWARDS}, "fromHeight", "toHeight");
    }

    @Override
    @Operation(summary = "Get reward totals",
            tags = {APITag2.REWARDS})
    @Parameter(name = "fromHeight", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer"),
            description = "from height")
    @Parameter(name = "toHeight", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer"),
            description = "to height")
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        final int fromHeight = ParameterParser.getInt(req, "fromHeight", 0, Integer.MAX_VALUE, true);
        final int toHeight = ParameterParser.getInt(req, "toHeight", fromHeight, Integer.MAX_VALUE, true);

        JSONArray result = new JSONArray();
        List<RewardItem.TotalItem> totals = RewardItem.getTotals(fromHeight, toHeight);
        for (RewardItem.TotalItem total : totals) {
            result.add(JSONData.rewardTotalItem(total));
        }

        JSONObject response = new JSONObject();
        response.put("rewardTotals", result);
        return response;
    }

}
