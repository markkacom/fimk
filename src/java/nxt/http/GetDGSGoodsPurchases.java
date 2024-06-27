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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

@Path("/fimk?requestType=getDGSGoodsPurchases")
public final class GetDGSGoodsPurchases extends APIServlet.APIRequestHandler {

    static final GetDGSGoodsPurchases instance = new GetDGSGoodsPurchases();

    private GetDGSGoodsPurchases() {
        super(new APITag[] {APITag.DGS}, "goods", "buyer", "firstIndex", "lastIndex", "withPublicFeedbacksOnly", "completed");
    }

    @Override
    @Operation(summary = "Get goods purchase",
            tags = {APITag2.DGS})
    @Parameter(name = "goods", in = ParameterIn.QUERY, required = true, description = "goods id")
    @Parameter(name = "buyer", in = ParameterIn.QUERY, description = "buyer account id")
    @Parameter(name = "firstIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "first index")
    @Parameter(name = "lastIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "last index")
    @Parameter(name = "withPublicFeedbacksOnly", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"),
            description = "with public feedbacks only")
    @Parameter(name = "completed", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"),
            description = "completed")
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        DigitalGoodsStore.Goods goods = ParameterParser.getGoods(req);
        long buyerId = ParameterParser.getAccountId(req, "buyer", false);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        final boolean withPublicFeedbacksOnly = "true".equalsIgnoreCase(req.getParameter("withPublicFeedbacksOnly"));
        final boolean completed = "true".equalsIgnoreCase(req.getParameter("completed"));


        JSONObject response = new JSONObject();
        JSONArray purchasesJSON = new JSONArray();
        response.put("purchases", purchasesJSON);

        try (DbIterator<DigitalGoodsStore.Purchase> iterator = DigitalGoodsStore.Purchase.getGoodsPurchases(goods.getId(),
                buyerId, withPublicFeedbacksOnly, completed, firstIndex, lastIndex)) {
            while(iterator.hasNext()) {
                purchasesJSON.add(JSONData.purchase(iterator.next()));
            }
        }
        return response;
    }

}
