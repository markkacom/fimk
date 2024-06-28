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
import nxt.Account;
import nxt.db.DbIterator;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

@Path("/fimk?requestType=searchAccountIdentifiers")
public final class SearchAccountIdentifiers extends APIServlet.APIRequestHandler {

    static final SearchAccountIdentifiers instance = new SearchAccountIdentifiers();

    private SearchAccountIdentifiers() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.SEARCH}, "query", "accountColorId", "firstIndex", "lastIndex");
    }

    @SuppressWarnings("unchecked")
    @Override
    @Operation(summary = "Search account identifiers",
            tags = {APITag2.ACCOUNT, APITag2.SEARCH})
    @Parameter(name = "query", in = ParameterIn.QUERY)
    @Parameter(name = "accountColorId", in = ParameterIn.QUERY, description = "account color id")
    @Parameter(name = "firstIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "first index")
    @Parameter(name = "lastIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "last index")
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        String query = Convert.nullToEmpty(req.getParameter("query"));
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        long accountColorId = ParameterParser.getUnsignedLong(req, "accountColorId", false);

        JSONObject response = new JSONObject();
        JSONArray accountsJSONArray = new JSONArray();
        try (DbIterator<Account.AccountIdentifier> identifiers = Account.searchAccountIdentifiers(query, accountColorId, firstIndex, lastIndex)) {
            for (Account.AccountIdentifier identifier : identifiers) {
                JSONObject json = new JSONObject();
                JSONData.putAccount(json, "account", identifier.getAccountId());
                accountsJSONArray.add(json);
            }
        }
        response.put("accounts", accountsJSONArray);
        return response;
    }

}
