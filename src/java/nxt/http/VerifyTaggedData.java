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
import nxt.Attachment;
import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import java.util.Arrays;

import static nxt.http.JSONResponses.*;

@Path("/fimk?requestType=verifyTaggedData")
public final class VerifyTaggedData extends APIServlet.APIRequestHandler {

    static final VerifyTaggedData instance = new VerifyTaggedData();

    private VerifyTaggedData() {
        super("file", new APITag[]{APITag.DATA}, "transaction",
                "name", "description", "tags", "type", "channel", "isText", "filename", "data");
    }

    @Override
    @Operation(summary = "Verify tagged data",
            tags = {APITag2.DATA})
    @Parameter(name = "transaction", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", minimum = "0"),
            description = "transaction id")
    @Parameter(name = "name", in = ParameterIn.QUERY)
    @Parameter(name = "description", in = ParameterIn.QUERY)
    @Parameter(name = "tags", in = ParameterIn.QUERY)
    @Parameter(name = "type", in = ParameterIn.QUERY)
    @Parameter(name = "channel", in = ParameterIn.QUERY)
    @Parameter(name = "isText", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"), description = "is text")
    @Parameter(name = "filename", in = ParameterIn.QUERY, description = "file name")
    @Parameter(name = "data", in = ParameterIn.QUERY, description = "data in text format or in HEX")
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        long transactionId = ParameterParser.getUnsignedLong(req, "transaction", true);
        Transaction transaction = Nxt.getBlockchain().getTransaction(transactionId);
        if (transaction == null) {
            return UNKNOWN_TRANSACTION;
        }

        Attachment.TaggedDataUpload taggedData = ParameterParser.getTaggedData(req);
        Attachment attachment = transaction.getAttachment();

        if (! (attachment instanceof Attachment.TaggedDataUpload)) {
            return INCORRECT_TRANSACTION;
        }

        Attachment.TaggedDataUpload myTaggedData = (Attachment.TaggedDataUpload)attachment;
        if (!Arrays.equals(myTaggedData.getHash(), taggedData.getHash())) {
            return HASHES_MISMATCH;
        }

        JSONObject response = myTaggedData.getJSONObject();
        response.put("verify", true);
        return response;
    }

}
