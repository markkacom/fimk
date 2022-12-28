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
import nxt.reward.AssetRewarding;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAssetRewardings extends APIServlet.APIRequestHandler {

    static final GetAssetRewardings instance = new GetAssetRewardings();

    private GetAssetRewardings() {
        super(new APITag[] {APITag.REWARDS}, "asset", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        // parameter asset is allowed 0 that means no filter by asset
        long assetId = ParameterParser.getUnsignedLong(req, "asset", true, true);

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONArray result = new JSONArray();
        try (DbIterator<AssetRewarding> iterator = AssetRewarding.getList(assetId, firstIndex, lastIndex)) {
            while(iterator.hasNext()) {
                result.add(JSONData.assetRewarding(iterator.next()));
            }
        }

        JSONObject response = new JSONObject();
        response.put("assetRewardings", result);
        return response;
    }

}
