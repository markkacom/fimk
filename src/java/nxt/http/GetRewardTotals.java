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

import nxt.NxtException;
import nxt.db.DbIterator;
import nxt.reward.RewardItem;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public final class GetRewardTotals extends APIServlet.APIRequestHandler {

    static final GetRewardTotals instance = new GetRewardTotals();

    private GetRewardTotals() {
        super(new APITag[]{APITag.REWARDS}, "fromHeight", "toHeight");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
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
