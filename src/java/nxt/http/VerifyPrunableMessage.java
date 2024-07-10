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
import nxt.Appendix;
import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import nxt.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import java.util.Arrays;

import static nxt.http.JSONResponses.*;

@Path("/fimk?requestType=verifyPrunableMessage")
public final class VerifyPrunableMessage extends APIServlet.APIRequestHandler {

    static final VerifyPrunableMessage instance = new VerifyPrunableMessage();

    private static final JSONStreamAware NO_SUCH_PLAIN_MESSAGE;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 5);
        response.put("errorDescription", "This transaction has no plain message attachment");
        NO_SUCH_PLAIN_MESSAGE = JSON.prepare(response);
    }

    private static final JSONStreamAware NO_SUCH_ENCRYPTED_MESSAGE;
    static {
        JSONObject response = new JSONObject();
        response.put("errorCode", 5);
        response.put("errorDescription", "This transaction has no encrypted message attachment");
        NO_SUCH_ENCRYPTED_MESSAGE = JSON.prepare(response);
    }

    private VerifyPrunableMessage() {
        super(new APITag[] {APITag.MESSAGES}, "transaction",
                "message", "messageIsText",
                "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "compressMessageToEncrypt");
    }

    @Override
    @Operation(summary = "Verify prunable message",
            tags = {APITag2.MESSAGES})
    @Parameter(name = "transaction", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", minimum = "0"),
            description = "transaction id")
    @Parameter(name = "message", in = ParameterIn.QUERY)
    @Parameter(name = "messageIsText", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"), description = "message is text")
    @Parameter(name = "messageToEncryptIsText", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"), description = "encrypting message is text")
    @Parameter(name = "encryptedMessageData", in = ParameterIn.QUERY, description = "encrypted message data")
    @Parameter(name = "encryptedMessageNonce", in = ParameterIn.QUERY, description = "encrypted message nonce in HEX")
    @Parameter(name = "compressMessageToEncrypt", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"), description = "compress encrypting message")
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        long transactionId = ParameterParser.getUnsignedLong(req, "transaction", true);
        Transaction transaction = Nxt.getBlockchain().getTransaction(transactionId);
        if (transaction == null) {
            return UNKNOWN_TRANSACTION;
        }

        Appendix.PrunablePlainMessage plainMessage = (Appendix.PrunablePlainMessage) ParameterParser.getPlainMessage(req, true);
        Appendix.PrunableEncryptedMessage encryptedMessage = (Appendix.PrunableEncryptedMessage) ParameterParser.getEncryptedMessage(req, true);

        if (plainMessage == null && encryptedMessage == null) {
            return MISSING_MESSAGE_ENCRYPTED_MESSAGE;
        }
        if (plainMessage != null && encryptedMessage != null) {
            return EITHER_MESSAGE_ENCRYPTED_MESSAGE;
        }

        if (plainMessage != null) {
            Appendix.PrunablePlainMessage myPlainMessage = transaction.getPrunablePlainMessage();
            if (myPlainMessage == null) {
                return NO_SUCH_PLAIN_MESSAGE;
            }
            if (!Arrays.equals(myPlainMessage.getHash(), plainMessage.getHash())) {
                return JSONResponses.HASHES_MISMATCH;
            }
            JSONObject response = myPlainMessage.getJSONObject();
            response.put("verify", true);
            return response;
        } else if (encryptedMessage != null) {
            Appendix.PrunableEncryptedMessage myEncryptedMessage = transaction.getPrunableEncryptedMessage();
            if (myEncryptedMessage == null) {
                return NO_SUCH_ENCRYPTED_MESSAGE;
            }
            if (!Arrays.equals(myEncryptedMessage.getHash(), encryptedMessage.getHash())) {
                return JSONResponses.HASHES_MISMATCH;
            }
            JSONObject response = myEncryptedMessage.getJSONObject();
            response.put("verify", true);
            return response;
        }

        return JSON.emptyJSON;
    }

}
