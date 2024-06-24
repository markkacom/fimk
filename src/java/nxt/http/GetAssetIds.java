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
import nxt.Asset;
import nxt.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

@Path("/fimk?requestType=getAssetIds")
public final class GetAssetIds extends APIServlet.APIRequestHandler {

    static final GetAssetIds instance = new GetAssetIds();

    private GetAssetIds() {
        super(new APITag[] {APITag.AE}, "firstIndex", "lastIndex");
    }

    @Override
    @Operation(summary = "Get asset ids",
            tags = {APITag2.AE})
    @Parameter(name = "firstIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "first index")
    @Parameter(name = "lastIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "last index")
    public JSONStreamAware processRequest(HttpServletRequest req) {

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONArray assetIds = new JSONArray();
        try (DbIterator<Asset> assets = Asset.getAllAssets(firstIndex, lastIndex)) {
            while (assets.hasNext()) {
                assetIds.add(Long.toUnsignedString(assets.next().getId()));
            }
        }
        JSONObject response = new JSONObject();
        response.put("assetIds", assetIds);
        return response;
    }

}
