/******************************************************************************
 * Copyright © 2014-2016 Krypto Fin ry and FIMK Developers.                   *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * FIMK software, including this file, may be copied, modified, propagated,   *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package nxt.http;

import nxt.Asset;
import nxt.MofoAsset;
import nxt.NxtException;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.FEATURE_NOT_AVAILABLE;
import static nxt.http.JSONResponses.INCORRECT_ASSET;

public final class GetPrivateAssetAccount extends CreateTransaction {

    static final GetPrivateAssetAccount instance = new GetPrivateAssetAccount();

    private GetPrivateAssetAccount() {
        super(new APITag[] {APITag.AE, APITag.MOFO}, "asset", "account");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        if ( ! Asset.privateEnabled()) {
            return FEATURE_NOT_AVAILABLE;
        }
        Asset asset = ParameterParser.getAsset(req);
        if ( ! MofoAsset.isPrivateAsset(asset)) {
            return INCORRECT_ASSET;
        }
        JSONObject response = new JSONObject();
        String allowed = MofoAsset.getAccountAllowed(asset.getId(), ParameterParser.getAccountId(req)) ? "true" : "false";
        response.put("allowed", allowed);
        response.put("name", asset.getName());
        response.put("decimals", asset.getDecimals());
        response.put("quantityQNT", String.valueOf(asset.getQuantityQNT()));
        response.put("asset", Long.toUnsignedString(asset.getId()));
        return response;
    }
}