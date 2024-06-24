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
import nxt.NxtException;
import nxt.TaggedData;
import nxt.util.JSON;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

import static nxt.http.JSONResponses.missing;

@Path("/fimk?requestType=getAssetGoodsImage")
public final class GetAssetGoodsImage extends APIServlet.APIRequestHandler {

    static final GetAssetGoodsImage instance = new GetAssetGoodsImage();

    private GetAssetGoodsImage() {
        super(new APITag[] {APITag.DATA}, "asset", "goods");
    }

    @Override
    @Operation(summary = "Get asset goods image",
            tags = {APITag2.DATA})
    @Parameter(name = "asset", in = ParameterIn.QUERY,  description = "asset id")
    @Parameter(name = "goods", in = ParameterIn.QUERY, description = "goods id")
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
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
