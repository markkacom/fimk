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
import nxt.*;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import static nxt.http.JSONResponses.UNKNOWN_TRANSACTION;

@Path("/fimk?requestType=extendTaggedData")
public final class ExtendTaggedData extends CreateTransaction {

    static final ExtendTaggedData instance = new ExtendTaggedData();

    private ExtendTaggedData() {
        super("file", new APITag[] {APITag.DATA, APITag.CREATE_TRANSACTION}, "transaction",
                "name", "description", "tags", "type", "channel", "isText", "filename", "data");
    }

    @Override
    @Operation(summary = "Extend tagged data",
            tags = {APITag2.DEBUG})
    @Parameter(name = "transaction", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", minimum = "0"),
            description = "transaction id")
    @Parameter(name = "name", in = ParameterIn.QUERY)
    @Parameter(name = "description", in = ParameterIn.QUERY)
    @Parameter(name = "tags", in = ParameterIn.QUERY)
    @Parameter(name = "type", in = ParameterIn.QUERY)
    @Parameter(name = "channel", in = ParameterIn.QUERY)
    @Parameter(name = "isText", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"), description = "is text data")
    @Parameter(name = "filename", in = ParameterIn.QUERY, description = "file name")
    @Parameter(name = "data", in = ParameterIn.QUERY)
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Account account = ParameterParser.getSenderAccount(req);
        long transactionId = ParameterParser.getUnsignedLong(req, "transaction", true);
        TaggedData taggedData = TaggedData.getData(transactionId);
        if (taggedData == null) {
            Transaction transaction = Nxt.getBlockchain().getTransaction(transactionId);
            if (transaction == null || transaction.getType() != TransactionType.Data.TAGGED_DATA_UPLOAD) {
                return UNKNOWN_TRANSACTION;
            }
            Attachment.TaggedDataUpload taggedDataUpload = ParameterParser.getTaggedData(req);
            taggedData = new TaggedData(transaction, taggedDataUpload);
        }
        Attachment.TaggedDataExtend taggedDataExtend = new Attachment.TaggedDataExtend(taggedData);
        return createTransaction(req, account, taggedDataExtend);

    }

}
