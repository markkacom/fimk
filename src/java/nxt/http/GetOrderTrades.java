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
import nxt.db.DbUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

@Path("/fimk?requestType=getOrderTrades")
public final class GetOrderTrades extends APIServlet.APIRequestHandler {

    static final GetOrderTrades instance = new GetOrderTrades();

    private GetOrderTrades() {
        super(new APITag[] {APITag.AE}, "askOrder", "bidOrder", "includeAssetInfo", "firstIndex", "lastIndex");
    }

    @Override
    @Operation(summary = "Get order trades",
            tags = {APITag2.AE})
    @Parameter(name = "askOrder", in = ParameterIn.QUERY, description = "ask order id")
    @Parameter(name = "bidOrder", in = ParameterIn.QUERY, description = "bid order id")
    @Parameter(name = "includeAssetInfo", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"), description = "include asset info")
    @Parameter(name = "firstIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "first index")
    @Parameter(name = "lastIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "last index")
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        long askOrderId = ParameterParser.getUnsignedLong(req, "askOrder", false);
        long bidOrderId = ParameterParser.getUnsignedLong(req, "bidOrder", false);
        boolean includeAssetInfo = "true".equalsIgnoreCase(req.getParameter("includeAssetInfo"));
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        if (askOrderId == 0 && bidOrderId == 0) {
            return JSONResponses.missing("askOrder", "bidOrder");
        }

        JSONObject response = new JSONObject();
        JSONArray tradesData = new JSONArray();
        if (askOrderId != 0 && bidOrderId != 0) {
            Trade trade = Trade.getTrade(askOrderId, bidOrderId);
            if (trade != null) {
                tradesData.add(JSONData.trade(trade, includeAssetInfo));
            }
        } else {
            DbIterator<Trade> trades = null;
            try {
                if (askOrderId != 0) {
                    trades = Trade.getAskOrderTrades(askOrderId, firstIndex, lastIndex);
                } else {
                    trades = Trade.getBidOrderTrades(bidOrderId, firstIndex, lastIndex);
                }
                while (trades.hasNext()) {
                    Trade trade = trades.next();
                    tradesData.add(JSONData.trade(trade, includeAssetInfo));
                }
            } finally {
                DbUtils.close(trades);
            }
        }
        response.put("trades", tradesData);

        return response;
    }

}
