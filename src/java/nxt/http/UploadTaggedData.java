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
import nxt.Attachment;
import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

@Path("/fimk?requestType=uploadTaggedData")
public final class UploadTaggedData extends CreateTransaction {

    static final UploadTaggedData instance = new UploadTaggedData();

    private UploadTaggedData() {
        super("file", new APITag[] {APITag.DATA, APITag.CREATE_TRANSACTION},
                "name", "description", "tags", "type", "channel", "isText", "filename", "data");
    }

    @Override
    @Operation(summary = "Upload tagged data",
            tags = {APITag2.DATA, APITag2.CREATE_TRANSACTION})
    @Parameter(name = "name", in = ParameterIn.QUERY)
    @Parameter(name = "description", in = ParameterIn.QUERY)
    @Parameter(name = "tags", in = ParameterIn.QUERY)
    @Parameter(name = "type", in = ParameterIn.QUERY)
    @Parameter(name = "channel", in = ParameterIn.QUERY)
    @Parameter(name = "isText", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"), description = "is text")
    @Parameter(name = "filename", in = ParameterIn.QUERY, description = "file name")
    @Parameter(name = "data", in = ParameterIn.QUERY, description = "data in text format or in HEX")
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Account account = ParameterParser.getSenderAccount(req);
        Attachment.TaggedDataUpload taggedDataUpload = ParameterParser.getTaggedData(req);
        return createTransaction(req, account, taggedDataUpload);

    }

}
