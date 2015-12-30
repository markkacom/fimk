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

import nxt.Account;
import nxt.AccountColor;
import nxt.Attachment;
import nxt.MofoAttachment;
import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class AccountColorAssign extends CreateTransaction {

    static final AccountColorAssign instance = new AccountColorAssign();

    private AccountColorAssign() {
        super(new APITag[] {APITag.MOFO, APITag.CREATE_TRANSACTION}, "recipient", "accountColorId");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        if (!AccountColor.getAccountColorEnabled()) {
            return nxt.http.JSONResponses.FEATURE_NOT_AVAILABLE;
        }

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