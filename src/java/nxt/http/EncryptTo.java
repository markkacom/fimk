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
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static nxt.http.JSONResponses.INCORRECT_RECIPIENT;

@Path("/fimk?requestType=encryptTo")
public final class EncryptTo extends APIServlet.APIRequestHandler {

    static final EncryptTo instance = new EncryptTo();

    private EncryptTo() {
        super(new APITag[] {APITag.MESSAGES}, "recipient", "messageToEncrypt", "messageToEncryptIsText", "compressMessageToEncrypt", "secretPhrase");
    }

    @Override
    @GET
    @Operation(summary = "Encrypt data",
            tags = {APITag2.MESSAGES})
    @Parameter(name = "recipient", in = ParameterIn.QUERY, required = true)
    @Parameter(name = "messageToEncrypt", in = ParameterIn.QUERY, description = "message to encrypt")
    @Parameter(name = "messageToEncryptIsText", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"), description = "is text message")
    @Parameter(name = "compressMessageToEncrypt", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"), description = "compress message")
    @Parameter(name = "secretPhrase", in = ParameterIn.QUERY, required = true, description = "secret phrase")
    public JSONStreamAware processRequest(@Parameter(hidden = true) HttpServletRequest req) throws NxtException {

        long recipientId = ParameterParser.getAccountId(req, "recipient", true);
        Account recipientAccount = Account.getAccount(recipientId);
        if (recipientAccount == null || recipientAccount.getPublicKey() == null) {
            return INCORRECT_RECIPIENT;
        }

        EncryptedData encryptedData = ParameterParser.getEncryptedData(req, recipientAccount);
        return JSONData.encryptedData(encryptedData);

    }

}
