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
import nxt.Account;
import nxt.Asset;
import nxt.AssetTransfer;
import nxt.NxtException;
import nxt.db.DbIterator;
import nxt.db.DbUtils;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

@Path("/fimk?requestType=getAssetTransfers")
public final class GetAssetTransfers extends APIServlet.APIRequestHandler {

    static final GetAssetTransfers instance = new GetAssetTransfers();

    private GetAssetTransfers() {
        super(new APITag[] {APITag.AE}, "asset", "account", "firstIndex", "lastIndex", "includeAssetInfo");
    }

    @Override
    @Operation(summary = "Get asset transfers",
            tags = {APITag2.AE})
    @Parameter(name = "asset", in = ParameterIn.QUERY, description = "asset id")
    @Parameter(name = "account", in = ParameterIn.QUERY, description = "account id")
    @Parameter(name = "firstIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "first index")
    @Parameter(name = "lastIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "last index")
    @Parameter(name = "includeAssetInfo", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"), description = "include asset info")
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        String assetId = Convert.emptyToNull(req.getParameter("asset"));
        String accountId = Convert.emptyToNull(req.getParameter("account"));

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        boolean includeAssetInfo = !"false".equalsIgnoreCase(req.getParameter("includeAssetInfo"));

        JSONObject response = new JSONObject();
        JSONArray transfersData = new JSONArray();
        DbIterator<AssetTransfer> transfers = null;
        try {
            if (accountId == null) {
                Asset asset = ParameterParser.getAsset(req);
                transfers = asset.getAssetTransfers(firstIndex, lastIndex);
            } else if (assetId == null) {
                Account account = ParameterParser.getAccount(req);
                transfers = account.getAssetTransfers(firstIndex, lastIndex);
            } else {
                Asset asset = ParameterParser.getAsset(req);
                Account account = ParameterParser.getAccount(req);
                transfers = AssetTransfer.getAccountAssetTransfers(account.getId(), asset.getId(), firstIndex, lastIndex);
            }
            while (transfers.hasNext()) {
                transfersData.add(JSONData.assetTransfer(transfers.next(), includeAssetInfo));
            }
        } finally {
            DbUtils.close(transfers);
        }
        response.put("transfers", transfersData);

        return response;
    }

    @Override
    boolean startDbTransaction() {
        return true;
    }
}
