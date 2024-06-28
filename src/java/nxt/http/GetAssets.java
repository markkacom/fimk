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
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import nxt.Asset;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

import static nxt.http.JSONResponses.INCORRECT_ASSET;
import static nxt.http.JSONResponses.UNKNOWN_ASSET;

@Path("/fimk?requestType=getAssets")
public final class GetAssets extends APIServlet.APIRequestHandler {

    static final GetAssets instance = new GetAssets();

    private GetAssets() {
        super(new APITag[] {APITag.AE}, "assets", "assets", "assets", "includeCounts"); // limit to 3 for testing
    }

    @Override
    @Operation(summary = "Get assets",
            tags = {APITag2.AE})
    @Parameter(name = "assets", array = @ArraySchema(schema = @Schema(implementation = String.class)),
            in = ParameterIn.QUERY, description = "asset identifiers")
    @Parameter(name = "includeCounts", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"),
            description = "include counts")
    public JSONStreamAware processRequest(HttpServletRequest req) {

        String[] assets = req.getParameterValues("assets");
        boolean includeCounts = !"false".equalsIgnoreCase(req.getParameter("includeCounts"));

        JSONObject response = new JSONObject();
        JSONArray assetsJSONArray = new JSONArray();
        response.put("assets", assetsJSONArray);
        for (String assetIdString : assets) {
            if (assetIdString == null || assetIdString.equals("")) {
                continue;
            }
            try {
                Asset asset = Asset.getAsset(Convert.parseUnsignedLong(assetIdString));
                if (asset == null) {
                    return UNKNOWN_ASSET;
                }
                assetsJSONArray.add(JSONData.asset(asset, includeCounts));
            } catch (RuntimeException e) {
                return INCORRECT_ASSET;
            }
        }
        return response;
    }

}
