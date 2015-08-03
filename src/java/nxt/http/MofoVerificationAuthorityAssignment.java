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
        if (!Account.getAccountIDsEnabled()) {
            return JSONResponses.FEATURE_NOT_AVAILABLE;
        }

        Account account = ParameterParser.getSenderAccount(req);
        int period = ParameterParser.getInt(req, "period", Constants.MIN_VERIFICATION_AUTHORITY_PERIOD, Constants.MAX_VERIFICATION_AUTHORITY_PERIOD, true);
        
        if (account.getId() != Constants.MASTER_VERIFICATION_AUTHORITY_ACCOUNT) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 4);
            response.put("errorDescription", "Account not allowed to set verified authority");
            return JSON.prepare(response);            
        }

        Attachment attachment = new MofoAttachment.VerificationAuthorityAssignmentAttachment(period);
        return createTransaction(req, account, attachment);
    }

}
