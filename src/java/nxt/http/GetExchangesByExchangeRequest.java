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
import nxt.Exchange;
import nxt.db.DbIterator;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

import static nxt.http.JSONResponses.MISSING_TRANSACTION;

@Path("/fimk?requestType=getExchangesByExchangeRequest")
public final class GetExchangesByExchangeRequest extends APIServlet.APIRequestHandler {

    static final GetExchangesByExchangeRequest instance = new GetExchangesByExchangeRequest();

    private GetExchangesByExchangeRequest() {
        super(new APITag[] {APITag.MS}, "transaction", "includeCurrencyInfo");
    }

    @Override
    @Operation(summary = "Get exchanges by exchange request",
            tags = {APITag2.MS})
    @Parameter(name = "transaction", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", minimum = "0"),
            description = "transaction id")
    @Parameter(name = "includeCurrencyInfo", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"),
            description = "include currency info")
    public JSONStreamAware processRequest(HttpServletRequest req) {
        String transactionIdString = Convert.emptyToNull(req.getParameter("transaction"));
        if (transactionIdString == null) {
            return MISSING_TRANSACTION;
        }
        long transactionId = Convert.parseUnsignedLong(transactionIdString);
        boolean includeCurrencyInfo = !"false".equalsIgnoreCase(req.getParameter("includeCurrencyInfo"));
        JSONObject response = new JSONObject();
        JSONArray exchangesData = new JSONArray();
        try (DbIterator<Exchange> exchanges = Exchange.getExchanges(transactionId)) {
            while (exchanges.hasNext()) {
                exchangesData.add(JSONData.exchange(exchanges.next(), includeCurrencyInfo));
            }
        }
        response.put("exchanges", exchangesData);
        return response;
    }

}
