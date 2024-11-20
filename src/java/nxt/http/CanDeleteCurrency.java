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
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import nxt.Account;
import nxt.Currency;
import nxt.NxtException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/fimk?requestType=canDeleteCurrency")
public final class CanDeleteCurrency extends APIServlet.APIRequestHandler {

    static final CanDeleteCurrency instance = new CanDeleteCurrency();

    private CanDeleteCurrency() {
        super(new APITag[] {APITag.MS}, "account", "currency");
    }

    @Override
    @GET
    @Operation(summary = "Is allowed to delete currency",
            tags = {APITag2.MS})
    @Parameter(name = "account", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", minimum = "0"),
            description = "account id")
    @Parameter(name = "currency", in = ParameterIn.QUERY, required = true, description = "currency id")
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Currency currency = ParameterParser.getCurrency(req);
        Account account = ParameterParser.getAccount(req);
        JSONObject response = new JSONObject();
        response.put("canDelete", currency.canBeDeletedBy(account.getId()));
        return response;
    }

}
