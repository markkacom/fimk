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
import nxt.Attachment;
import nxt.Constants;
import nxt.MofoAttachment;
import nxt.NxtException;
import nxt.util.JSON;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class MofoVerificationAuthorityAssignment extends CreateTransaction {

    static final MofoVerificationAuthorityAssignment instance = new MofoVerificationAuthorityAssignment();

    private MofoVerificationAuthorityAssignment() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.CREATE_TRANSACTION}, "identifier", "signature", "signatory");
    }

    @SuppressWarnings("unchecked")
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Account senderAccount = ParameterParser.getSenderAccount(req);
        int period = ParameterParser.getInt(req, "period", Constants.MIN_VERIFICATION_AUTHORITY_PERIOD, Constants.MAX_VERIFICATION_AUTHORITY_PERIOD, true);

        if (senderAccount.getId() != Constants.MASTER_VERIFICATION_AUTHORITY_ACCOUNT) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 4);
            response.put("errorDescription", "Account not allowed to set verified authority");
            return JSON.prepare(response);
        }

        long recipientId = ParameterParser.getAccountId(req, "recipient", true);

        Attachment attachment = new MofoAttachment.VerificationAuthorityAssignmentAttachment(period);
        return createTransaction(req, senderAccount, recipientId, 0, attachment);
    }

}
