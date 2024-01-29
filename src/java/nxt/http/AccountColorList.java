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
import nxt.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/fimk?requestType=accountColorList")
public final class AccountColorList extends APIServlet.APIRequestHandler {

    static final AccountColorList instance = new AccountColorList();

    private AccountColorList() {
        super(new APITag[] {APITag.MOFO}, "account", "firstIndex", "lastIndex", "includeAccountInfo", "includeDescription");
    }

    @SuppressWarnings("unchecked")
    @Override
    @GET
    @Operation(summary = "List Account Colors",
            tags = {APITag2.ACCOUNT})
    @Parameter(name = "account", in = ParameterIn.QUERY)
    @Parameter(name = "firstIndex", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer"))
    @Parameter(name = "lastIndex", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer"))
    @Parameter(name = "includeAccountInfo", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"))
    @Parameter(name = "includeDescription", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"))
    public JSONStreamAware processRequest(@Parameter(hidden = true) HttpServletRequest req) throws NxtException {

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        long accountId = ParameterParser.getAccountId(req, "account", false);
        boolean includeAccountInfo = "true".equals(req.getParameter("includeAccountInfo"));
        boolean includeDescription = "true".equals(req.getParameter("includeDescription"));

        JSONObject response = new JSONObject();
        JSONArray colors = new JSONArray();
        response.put("accountColors", colors);

        try (DbIterator<AccountColor> iterator = accountId == 0 ?
                AccountColor.getAllAccountColors(firstIndex, lastIndex) :
                AccountColor.getAccountColorsIssuedBy(accountId, firstIndex, lastIndex)) {
            while (iterator.hasNext()) {
                colors.add(JSONData.accountColor(iterator.next(), includeAccountInfo, includeDescription));
            }
        }

        return response;
    }
}