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
import nxt.TaggedData;
import nxt.util.JSON;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.missing;

public final class GetAssetGoodsImage extends APIServlet.APIRequestHandler {

    static final GetAssetGoodsImage instance = new GetAssetGoodsImage();

    private GetAssetGoodsImage() {
        super(new APITag[] {APITag.DATA}, "asset", "goods");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        boolean emptyAsset = req.getParameter("asset") == null || req.getParameter("asset").trim().isEmpty();
        long assetId = ParameterParser.getUnsignedLong(req, "asset", false, true);
        long goodsId = ParameterParser.getUnsignedLong(req, "goods", false);

        if (emptyAsset && goodsId == 0) {
            throw new ParameterException(missing("asset", "goods"));
        }

        TaggedData taggedData = TaggedData.getAssetGoodsImage(assetId, goodsId);
        if (taggedData != null) {
            return JSONData.taggedData(taggedData, true);
        }
        return JSON.emptyJSON;
    }

}
