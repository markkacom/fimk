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

public final class GetRewards extends APIServlet.APIRequestHandler {

    static final GetRewards instance = new GetRewards();

    private GetRewards() {
        super(new APITag[] {APITag.REWARDS}, "account", "fromHeight", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        final long accountId = ParameterParser.getAccount(req).getId();
        final int fromHeight = ParameterParser.getInt(req, "fromHeight", 0, Integer.MAX_VALUE, true);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONArray rewards = new JSONArray();
        try (DbIterator<RewardItem> iterator = RewardItem.getRewardItems(accountId, fromHeight, firstIndex, lastIndex)) {
            while(iterator.hasNext()) {
                rewards.add(JSONData.rewardItem(iterator.next()));
            }
        }

        JSONObject response = new JSONObject();
        response.put("rewards", rewards);
        return response;
    }

}
