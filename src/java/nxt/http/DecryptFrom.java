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
import nxt.crypto.EncryptedData;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static nxt.http.JSONResponses.DECRYPTION_FAILED;
import static nxt.http.JSONResponses.INCORRECT_ACCOUNT;

@Path("/fimk?requestType=decryptFrom")
public final class DecryptFrom extends APIServlet.APIRequestHandler {

    static final DecryptFrom instance = new DecryptFrom();

    private DecryptFrom() {
        super(new APITag[]{APITag.MESSAGES}, "account", "data", "nonce", "decryptedMessageIsText", "uncompressDecryptedMessage", "secretPhrase");
    }

    @Override
    @GET
    @Operation(summary = "Decrypt from",
            tags = {APITag2.MESSAGES})
    @Parameter(name = "account", in = ParameterIn.QUERY, required = true)
    @Parameter(name = "data", in = ParameterIn.QUERY, description = "data in HEX format")
    @Parameter(name = "nonce", in = ParameterIn.QUERY, description = "nonce in HEX format")
    @Parameter(name = "decryptedMessageIsText", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"))
    @Parameter(name = "uncompressDecryptedMessage", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"))
    @Parameter(name = "secretPhrase", in = ParameterIn.QUERY)
    public JSONStreamAware processRequest(@Parameter(hidden = true) HttpServletRequest req) throws NxtException {

        Account account = ParameterParser.getAccount(req);
        if (account.getPublicKey() == null) {
            return INCORRECT_ACCOUNT;
        }
        String secretPhrase = ParameterParser.getSecretPhrase(req);
        byte[] data = Convert.parseHexString(Convert.nullToEmpty(req.getParameter("data")));
        byte[] nonce = Convert.parseHexString(Convert.nullToEmpty(req.getParameter("nonce")));
        EncryptedData encryptedData = new EncryptedData(data, nonce);
        boolean isText = !"false".equalsIgnoreCase(req.getParameter("decryptedMessageIsText"));
        boolean uncompress = !"false".equalsIgnoreCase(req.getParameter("uncompressDecryptedMessage"));
        try {
            byte[] decrypted = account.decryptFrom(encryptedData, secretPhrase, uncompress);
            JSONObject response = new JSONObject();
            response.put("decryptedMessage", isText ? Convert.toString(decrypted) : Convert.toHexString(decrypted));
            return response;
        } catch (RuntimeException e) {
            Logger.logDebugMessage(e.toString());
            return DECRYPTION_FAILED;
        }
    }

}
