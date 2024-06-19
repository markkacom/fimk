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
import nxt.NxtException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

@Path("/fimk?requestType=getAccountCurrencyCount")
public final class GetAccountCurrencyCount extends APIServlet.APIRequestHandler {

    static final GetAccountCurrencyCount instance = new GetAccountCurrencyCount();

    private GetAccountCurrencyCount() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.MS}, "account", "height");
    }

    @Override
    @Operation(summary = "Get account currency count",
            tags = {APITag2.ACCOUNT, APITag2.MS})
    @Parameter(name = "account", in = ParameterIn.QUERY, required = true)
    @Parameter(name = "height", in = ParameterIn.QUERY, schema = @Schema(type = "integer"))
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Account account = ParameterParser.getAccount(req);
        int height = ParameterParser.getHeight(req);

        JSONObject response = new JSONObject();
        response.put("numberOfCurrencies", Account.getAccountCurrencyCount(account.getId(), height));
        return response;
    }

}
