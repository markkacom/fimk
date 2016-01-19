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

import nxt.NxtException;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.Logger;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class SignMessage extends APIServlet.APIRequestHandler {

    static final SignMessage instance = new SignMessage();

    private SignMessage() {
        super(new APITag[] {APITag.MOFO}, "message", "secretPhrase");
    }

    @SuppressWarnings("unchecked")
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        String message = Convert.emptyToNull(req.getParameter("message"));
        String secretPhrase = Convert.emptyToNull(req.getParameter("secretPhrase"));

        if (message == null) {
            return JSONResponses.missing("message");
        }
        if (secretPhrase == null) {
            return JSONResponses.missing("secretPhrase");
        }

        JSONObject response = new JSONObject();
        try {
            byte[] messageBytes = Convert.toBytes(message);
            if (messageBytes == null) {
                return JSONResponses.incorrect("message");
            }

            byte[] signature = Crypto.sign(messageBytes, secretPhrase);
            response.put("signature", Convert.toHexString(signature));

        } catch (NumberFormatException e) {
            Logger.logDebugMessage(e.getMessage(), e);
            JSONData.putException(response, e, "Incorrect unsigned message");
        }
        return response;
    }
}
