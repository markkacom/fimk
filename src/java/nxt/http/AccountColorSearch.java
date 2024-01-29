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
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/fimk?requestType=accountColorSearch")
public final class AccountColorSearch extends APIServlet.APIRequestHandler {

    static final AccountColorSearch instance = new AccountColorSearch();

    private AccountColorSearch() {
        super(new APITag[] {APITag.MOFO}, "account", "name", "firstIndex", "lastIndex", "includeAccountInfo", "includeDescription");
    }

    @SuppressWarnings("unchecked")
    @Override
    @GET
    @Operation(summary = "Search Account Colors",
            tags = {APITag2.ACCOUNT})
    @Parameter(name = "account", in = ParameterIn.QUERY)
    @Parameter(name = "name", in = ParameterIn.QUERY)
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
        String name = Convert.nullToEmpty(req.getParameter("name"));

        JSONObject response = new JSONObject();
        JSONArray colors = new JSONArray();
        response.put("accountColors", colors);

        try (DbIterator<AccountColor> iterator = AccountColor.searchAccountColorsByName(name, accountId, firstIndex, lastIndex)) {
            while (iterator.hasNext()) {
                colors.add(JSONData.accountColor(iterator.next(), includeAccountInfo, includeDescription));
            }
        }

        return response;
    }
}