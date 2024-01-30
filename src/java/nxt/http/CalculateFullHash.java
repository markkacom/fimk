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
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.security.MessageDigest;

import static nxt.http.JSONResponses.MISSING_SIGNATURE_HASH;
import static nxt.http.JSONResponses.MISSING_UNSIGNED_BYTES;

@Path("/fimk?requestType=сalculateFullHash")
public final class CalculateFullHash extends APIServlet.APIRequestHandler {

    static final CalculateFullHash instance = new CalculateFullHash();

    private CalculateFullHash() {
        super(new APITag[] {APITag.TRANSACTIONS}, "unsignedTransactionBytes", "signatureHash");
    }

    @Override
    @GET
    @Operation(summary = "Calculate full hash",
            tags = {APITag2.TRANSACTIONS})
    @Parameter(name = "unsignedTransactionBytes", in = ParameterIn.QUERY, required = true,
            description = "unsigned transaction bytes in HEX format")
    @Parameter(name = "signatureHash", in = ParameterIn.QUERY, required = true,
            description = "signature hash in HEX format")
    public JSONStreamAware processRequest(@Parameter(hidden = true) HttpServletRequest req) {

        String unsignedBytesString = Convert.emptyToNull(req.getParameter("unsignedTransactionBytes"));
        String signatureHashString = Convert.emptyToNull(req.getParameter("signatureHash"));

        if (unsignedBytesString == null) return MISSING_UNSIGNED_BYTES;

        if (signatureHashString == null) return MISSING_SIGNATURE_HASH;

        MessageDigest digest = Crypto.sha256();
        digest.update(Convert.parseHexString(unsignedBytesString));
        byte[] fullHash = digest.digest(Convert.parseHexString(signatureHashString));
        JSONObject response = new JSONObject();
        response.put("fullHash", Convert.toHexString(fullHash));

        return response;

    }

}
