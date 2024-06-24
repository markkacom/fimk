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
import nxt.NxtException;
import nxt.Trade;
import nxt.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

@Path("/fimk?requestType=getAllTrades")
public final class GetAllTrades extends APIServlet.APIRequestHandler {

    static final GetAllTrades instance = new GetAllTrades();

    private GetAllTrades() {
        super(new APITag[] {APITag.AE}, "timestamp", "firstIndex", "lastIndex", "includeAssetInfo");
    }
    
    @Override
    @Operation(summary = "Get all trades",
            tags = {APITag2.AE})
    @Parameter(name = "timestamp", in = ParameterIn.QUERY, schema = @Schema(type = "integer", minimum = "0"), description = "timestamp")
    @Parameter(name = "firstIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "first index")
    @Parameter(name = "lastIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "last index")
    @Parameter(name = "includeAssetInfo", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"),
            description = "include asset info")
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        final int timestamp = ParameterParser.getTimestamp(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        boolean includeAssetInfo = !"false".equalsIgnoreCase(req.getParameter("includeAssetInfo"));

        JSONObject response = new JSONObject();
        JSONArray trades = new JSONArray();
        try (DbIterator<Trade> tradeIterator = Trade.getAllTrades(firstIndex, lastIndex)) {
            while (tradeIterator.hasNext()) {
                Trade trade = tradeIterator.next();
                if (trade.getTimestamp() < timestamp) {
                    break;
                }
                trades.add(JSONData.trade(trade, includeAssetInfo));
            }
        }
        response.put("trades", trades);
        return response;
    }

}
