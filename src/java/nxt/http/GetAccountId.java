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
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

import static nxt.http.JSONResponses.MISSING_SECRET_PHRASE_OR_PUBLIC_KEY;

@Path("/fimk?requestType=getAccountId")
public final class GetAccountId extends APIServlet.APIRequestHandler {

    static final GetAccountId instance = new GetAccountId();

    private GetAccountId() {
        super(new APITag[] {APITag.ACCOUNTS}, "secretPhrase", "publicKey");
    }

    @Override
    @Operation(summary = "Get account id", description = "Must be specified secret phrase or public key",
            tags = {APITag2.ACCOUNT})
    @Parameter(name = "secretPhrase", in = ParameterIn.QUERY, description = "secret phrase in HEX")
    @Parameter(name = "publicKey", in = ParameterIn.QUERY, description = "public key in HEX")
    public JSONStreamAware processRequest(HttpServletRequest req) {

        long accountId;
        String secretPhrase = Convert.emptyToNull(req.getParameter("secretPhrase"));
        String publicKeyString = Convert.emptyToNull(req.getParameter("publicKey"));
        if (secretPhrase != null) {
            byte[] publicKey = Crypto.getPublicKey(secretPhrase);
            accountId = Account.getId(publicKey);
            publicKeyString = Convert.toHexString(publicKey);
        } else if (publicKeyString != null) {
            accountId = Account.getId(Convert.parseHexString(publicKeyString));
        } else {
            return MISSING_SECRET_PHRASE_OR_PUBLIC_KEY;
        }

        JSONObject response = new JSONObject();
        JSONData.putAccount(response, "account", accountId);
        response.put("publicKey", publicKeyString);

        return response;
    }

    @Override
    boolean requirePost() {
        return true;
    }

}
