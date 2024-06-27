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
import nxt.DigitalGoodsStore;
import nxt.NxtException;
import nxt.db.DbIterator;
import nxt.db.DbUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

@Path("/fimk?requestType=getDGSPurchases")
public final class GetDGSPurchases extends APIServlet.APIRequestHandler {

    static final GetDGSPurchases instance = new GetDGSPurchases();

    private GetDGSPurchases() {
        super(new APITag[] {APITag.DGS}, "seller", "buyer", "firstIndex", "lastIndex", "withPublicFeedbacksOnly", "completed");
    }

    @Override
    @Operation(summary = "Get purchases",
            tags = {APITag2.DGS})
    @Parameter(name = "seller", in = ParameterIn.QUERY, description = "seller account id")
    @Parameter(name = "buyer", in = ParameterIn.QUERY, description = "buyer account id")
    @Parameter(name = "firstIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "first index")
    @Parameter(name = "lastIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "last index")
    @Parameter(name = "withPublicFeedbacksOnly", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"),
            description = "with public feedbacks only")
    @Parameter(name = "completed", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"), description = "completed")
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        long sellerId = ParameterParser.getAccountId(req, "seller", false);
        long buyerId = ParameterParser.getAccountId(req, "buyer", false);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        final boolean completed = "true".equalsIgnoreCase(req.getParameter("completed"));
        final boolean withPublicFeedbacksOnly = "true".equalsIgnoreCase(req.getParameter("withPublicFeedbacksOnly"));


        JSONObject response = new JSONObject();
        JSONArray purchasesJSON = new JSONArray();
        response.put("purchases", purchasesJSON);

        DbIterator<DigitalGoodsStore.Purchase> purchases;
        if (sellerId == 0 && buyerId == 0) {
            purchases = DigitalGoodsStore.Purchase.getPurchases(withPublicFeedbacksOnly, completed, firstIndex, lastIndex);
        } else if (sellerId != 0 && buyerId == 0) {
            purchases = DigitalGoodsStore.Purchase.getSellerPurchases(sellerId, withPublicFeedbacksOnly, completed, firstIndex, lastIndex);
        } else if (sellerId == 0) {
            purchases = DigitalGoodsStore.Purchase.getBuyerPurchases(buyerId, withPublicFeedbacksOnly, completed, firstIndex, lastIndex);
        } else {
            purchases = DigitalGoodsStore.Purchase.getSellerBuyerPurchases(sellerId, buyerId, withPublicFeedbacksOnly, completed, firstIndex, lastIndex);
        }
        try {
            while (purchases.hasNext()) {
                purchasesJSON.add(JSONData.purchase(purchases.next()));
            }
        } finally {
            DbUtils.close(purchases);
        }
        return response;
    }

}
