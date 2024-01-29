/******************************************************************************
 * Copyright © 2014-2016 Krypto Fin ry and FIMK Developers.                   *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * FIMK software, including this file, may be copied, modified, propagated,   *
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
import nxt.AccountColor;
import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/fimk?requestType=accountColorGet")
public final class AccountColorGet extends APIServlet.APIRequestHandler {

    static final AccountColorGet instance = new AccountColorGet();

    private AccountColorGet() {
        super(new APITag[] {APITag.MOFO}, "accountColorId", "includeAccountInfo", "includeDescription");
    }

    @Override
    @GET
    @Operation(summary = "Get Account Color",
            tags = {APITag2.ACCOUNT})
    @Parameter(name = "accountColorId", in = ParameterIn.QUERY, required = true)
    @Parameter(name = "includeAccountInfo", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"))
    @Parameter(name = "includeDescription", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"))
    public JSONStreamAware processRequest(@Parameter(hidden = true) HttpServletRequest req) throws NxtException {

        boolean includeAccountInfo = "true".equals(req.getParameter("includeAccountInfo"));
        boolean includeDescription = "true".equals(req.getParameter("includeDescription"));
        long accountColorId = ParameterParser.getUnsignedLong(req, "accountColorId", true);
        AccountColor accountColor = AccountColor.getAccountColor(accountColorId);
        if (accountColor == null) {
            return nxt.http.JSONResponses.unknown("accountColor");
        }

        return JSONData.accountColor(accountColor, includeAccountInfo, includeDescription);
    }
}