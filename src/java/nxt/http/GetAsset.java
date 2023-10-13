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
import nxt.MofoAsset.AssetFee;
import nxt.NxtException;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAsset extends APIServlet.APIRequestHandler {

    static final GetAsset instance = new GetAsset();

    private GetAsset() {
        super(new APITag[] {APITag.AE}, "asset", "includeCounts");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        boolean includeCounts = !"false".equalsIgnoreCase(req.getParameter("includeCounts"));
        Asset asset = ParameterParser.getAsset(req);
        JSONObject response = JSONData.asset(asset, includeCounts);
        AssetFee fee = MofoAsset.getFee(asset.getId());
        if (fee != null) {
            response.put("orderFeePercentage", fee.getOrderFeePercentage());
            response.put("tradeFeePercentage", fee.getTradeFeePercentage());
        }
        return response;
    }

}
