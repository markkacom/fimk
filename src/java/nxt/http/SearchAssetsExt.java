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

import nxt.Asset;
import nxt.MofoAsset;
import nxt.db.DbIterator;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

/**
 * Search assets excluding private assets not allowed for account.
 */
public final class SearchAssetsExt extends APIServlet.APIRequestHandler {

    static final SearchAssetsExt instance = new SearchAssetsExt();

    private SearchAssetsExt() {
        super(new APITag[] {APITag.AE, APITag.SEARCH}, "query", "firstIndex", "lastIndex", "includeCounts");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        String query = Convert.nullToEmpty(req.getParameter("query"));
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        boolean includeCounts = !"false".equalsIgnoreCase(req.getParameter("includeCounts"));
        String accountPrivateAllowed = Convert.emptyToNull(req.getParameter("accountPrivateAllowed"));

        JSONObject response = new JSONObject();
        JSONArray assetsJSONArray = new JSONArray();
        try (DbIterator<Asset> assets = Asset.searchAssetsExt(query, firstIndex, lastIndex)) {
            while (assets.hasNext()) {
                Asset asset = assets.next();
                if (accountPrivateAllowed != null) {
                    long accountId = ParameterParser.getAccountId(req, "accountPrivateAllowed", false);
                    if (MofoAsset.isPrivateAsset(asset)) {
                        if (!MofoAsset.getAccountAllowed(asset, accountId)) {
                            continue;
                        }
                    }
                }
                assetsJSONArray.add(JSONData.asset(asset, includeCounts));
            }
        }
        response.put("assets", assetsJSONArray);
        return response;
    }

}
