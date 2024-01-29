/******************************************************************************
 * Copyright © 2014-2016 Krypto Fin ry and FIMK Developers.                   *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * FIMK software, including this file, may be copied, modified, propagated,   *
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
import nxt.*;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/fimk?requestType=accountColorAssign")
public final class AccountColorAssign extends CreateTransaction {

    static final AccountColorAssign instance = new AccountColorAssign();

    private AccountColorAssign() {
        super(new APITag[] {APITag.MOFO, APITag.CREATE_TRANSACTION}, "recipient", "accountColorId");
    }

    @Override
    @POST
    @Operation(summary = "Assign color to account",
            tags = {APITag2.ACCOUNT, APITag2.CREATE_TRANSACTION})
    @Parameter(name = "recipient", in = ParameterIn.QUERY, required = true)
    @Parameter(name = "accountColorId", in = ParameterIn.QUERY, required = true)
    public JSONStreamAware processRequest(@Parameter(hidden = true) HttpServletRequest req) throws NxtException {

        long recipientId = ParameterParser.getAccountId(req, "recipient", true);
        if (Account.getAccount(recipientId) != null) {
            return nxt.http.JSONResponses.incorrect("recipient", "Recipient account already exists");
        }

        long accountColorId = ParameterParser.getUnsignedLong(req, "accountColorId", true);
        AccountColor accountColor = AccountColor.getAccountColor(accountColorId);
        if (accountColor == null) {
            return nxt.http.JSONResponses.incorrect("accountColorId", "Account Color does not exist");
        }

        Account senderAccount = ParameterParser.getSenderAccount(req);
        Attachment attachment = new MofoAttachment.AccountColorAssignAttachment(accountColorId);
        return createTransaction(req, senderAccount, recipientId, 0L, attachment);
    }
}