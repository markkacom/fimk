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
import nxt.Constants;
import nxt.Nxt;
import nxt.NxtException;
import nxt.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

@Path("/fimk?requestType=getAccountLessors")
public final class GetAccountLessors extends APIServlet.APIRequestHandler {

    static final GetAccountLessors instance = new GetAccountLessors();

    private GetAccountLessors() {
        super(new APITag[] {APITag.ACCOUNTS}, "account", "height");
    }

    @Override
    @Operation(summary = "Get account lessors",
            tags = {APITag2.ACCOUNT})
    @Parameter(name = "account", in = ParameterIn.QUERY, required = true, description = "account id")
    @Parameter(name = "height", in = ParameterIn.QUERY, schema = @Schema(type = "integer"))
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Account account = ParameterParser.getAccount(req);
        int height = ParameterParser.getHeight(req);
        if (height < 0) {
            height = Nxt.getBlockchain().getHeight();
        }

        JSONObject response = new JSONObject();
        JSONData.putAccount(response, "account", account.getId());
        response.put("height", height);
        JSONArray lessorsJSON = new JSONArray();

        try (DbIterator<Account> lessors = account.getLessors(height)) {
            if (lessors.hasNext()) {
                while (lessors.hasNext()) {
                    Account lessor = lessors.next();
                    JSONObject lessorJSON = new JSONObject();
                    JSONData.putAccount(lessorJSON, "lessor", lessor.getId());
                    lessorJSON.put("guaranteedBalanceNQT", String.valueOf(lessor.getGuaranteedBalanceNQT(Constants.GUARANTEED_BALANCE_CONFIRMATIONS, height)));
                    lessorsJSON.add(lessorJSON);
                }
            }
        }
        response.put("lessors", lessorsJSON);
        return response;

    }

}
