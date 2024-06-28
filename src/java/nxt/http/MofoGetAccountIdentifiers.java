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
import nxt.Account.AccountIdentifier;
import nxt.NxtException;
import nxt.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

/* @api-name getAccountIdentifiers */
@Path("/fimk?requestType=getAccountIdentifiers")
public final class MofoGetAccountIdentifiers extends APIServlet.APIRequestHandler {

    static final MofoGetAccountIdentifiers instance = new MofoGetAccountIdentifiers();

    private MofoGetAccountIdentifiers() {
        super(new APITag[] {APITag.ACCOUNTS}, "account", "firstIndex", "lastIndex");
    }

    @Override
    @Operation(summary = "Get account identifiers",
            tags = {APITag2.ACCOUNT})
    @Parameter(name = "account", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", minimum = "0"),
            description = "account id")
    @Parameter(name = "firstIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "first index")
    @Parameter(name = "lastIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "last index")
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        long accountId = ParameterParser.getAccountId(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);


        JSONArray result = new JSONArray();
        try (DbIterator<AccountIdentifier> identifiers = Account.getAccountIdentifiers(accountId, firstIndex, lastIndex)) {
            while (identifiers.hasNext()) {
                result.add(identifiers.next().getEmail());
            }
        }
        JSONObject response = new JSONObject();
        response.put("identifiers", result);
        return response;
    }

}
