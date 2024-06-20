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
import nxt.Order;
import nxt.db.DbIterator;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

@Path("/fimk?requestType=getAccountCurrentBidOrders")
public final class GetAccountCurrentBidOrders extends APIServlet.APIRequestHandler {

    static final GetAccountCurrentBidOrders instance = new GetAccountCurrentBidOrders();

    private GetAccountCurrentBidOrders() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.AE}, "account", "asset", "firstIndex", "lastIndex");
    }

    @Override
    @Operation(summary = "Get current bid orders of account",
            tags = {APITag2.ACCOUNT, APITag2.AE})
    @Parameter(name = "account", in = ParameterIn.QUERY, required = true, description = "account id")
    @Parameter(name = "asset", in = ParameterIn.QUERY, description = "asset id")
    @Parameter(name = "firstIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"))
    @Parameter(name = "lastIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"))
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        long accountId = ParameterParser.getAccount(req).getId();
        long assetId = 0;
        try {
            assetId = Convert.parseUnsignedLong(req.getParameter("asset"));
        } catch (RuntimeException e) {
            // ignore
        }
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        DbIterator<Order.Bid> bidOrders;
        if (assetId == 0) {
            bidOrders = Order.Bid.getBidOrdersByAccount(accountId, firstIndex, lastIndex);
        } else {
            bidOrders = Order.Bid.getBidOrdersByAccountAsset(accountId, assetId, firstIndex, lastIndex);
        }
        JSONArray orders = new JSONArray();
        try {
            while (bidOrders.hasNext()) {
                orders.add(JSONData.bidOrder(bidOrders.next()));
            }
        } finally {
            bidOrders.close();
        }
        JSONObject response = new JSONObject();
        response.put("bidOrders", orders);
        return response;
    }

}