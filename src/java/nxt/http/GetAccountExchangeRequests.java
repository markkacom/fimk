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
import nxt.*;
import nxt.db.FilteringIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

@Path("/fimk?requestType=getAccountExchangeRequests")
public final class GetAccountExchangeRequests extends APIServlet.APIRequestHandler {

    static final GetAccountExchangeRequests instance = new GetAccountExchangeRequests();

    private GetAccountExchangeRequests() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.MS}, "account", "currency", "firstIndex", "lastIndex");
    }

    @Override
    @Operation(summary = "Get account exchange requests",
            tags = {APITag2.ACCOUNT, APITag2.MS})
    @Parameter(name = "account", in = ParameterIn.QUERY, required = true, description = "account id")
    @Parameter(name = "currency", in = ParameterIn.QUERY, required = true, description = "currency id")
    @Parameter(name = "firstIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "first index")
    @Parameter(name = "lastIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "last index")
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Account account = ParameterParser.getAccount(req);
        Currency currency = ParameterParser.getCurrency(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONArray exchangeRequests = new JSONArray();
        try (FilteringIterator<? extends Transaction> transactions = Exchange.getAccountCurrencyExchangeRequests(account.getId(), currency.getId(),
                firstIndex, lastIndex)) {
            for (Transaction transaction : transactions) {
                exchangeRequests.add(JSONData.exchangeRequest(transaction, true));
            }
        }

        JSONObject response = new JSONObject();
        response.put("exchangeRequests", exchangeRequests);
        return response;


    }

}
