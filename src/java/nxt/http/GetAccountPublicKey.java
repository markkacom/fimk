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
import nxt.Account;
import nxt.NxtException;
import nxt.util.Convert;
import nxt.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

@Path("/fimk?requestType=getAccountPublicKey")
public final class GetAccountPublicKey extends APIServlet.APIRequestHandler {

    static final GetAccountPublicKey instance = new GetAccountPublicKey();

    private GetAccountPublicKey() {
        super(new APITag[] {APITag.ACCOUNTS}, "account");
    }

    @Override
    @Operation(summary = "Get account public key",
            tags = {APITag2.ACCOUNT})
    @Parameter(name = "account", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", minimum = "0"),
            description = "account id")
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Account account = ParameterParser.getAccount(req);

        if (account.getPublicKey() != null) {
            JSONObject response = new JSONObject();
            response.put("publicKey", Convert.toHexString(account.getPublicKey()));
            return response;
        } else {
            return JSON.emptyJSON;
        }
    }

}
