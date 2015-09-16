package nxt.http;


import nxt.Account;
import nxt.Attachment;
import nxt.MofoAttachment;
import nxt.MofoIdentifier;
import nxt.MofoVerificationAuthority;
import nxt.NxtException;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.JSON;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_SIGNATORY;

/* @api-name setAccountIdentifier */
public final class MofoAccountIdAssignment extends CreateTransaction {

    static final MofoAccountIdAssignment instance = new MofoAccountIdAssignment();

    private MofoAccountIdAssignment() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.CREATE_TRANSACTION}, "identifier", "signature", "signatory");
    }

    @SuppressWarnings("unchecked")
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        if (!Account.getAccountIDsEnabled()) {
            return JSONResponses.FEATURE_NOT_AVAILABLE;
        }
      
        long recipient = ParameterParser.getAccountId(req, "recipient", true);
        Account account = ParameterParser.getSenderAccount(req);
        String identifier = Convert.emptyToNull(req.getParameter("identifier"));
        long signatory;
        try {
            signatory = Convert.parseAccountId(Convert.emptyToNull(req.getParameter("signatory")));
        } catch (RuntimeException ex) {
            return INCORRECT_SIGNATORY;
        }
        byte[] signature = Convert.parseHexString(Convert.emptyToNull(req.getParameter("signature")));

        if (Account.getAccountByIdentifier(identifier) != null) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 4);
            response.put("errorDescription", "Duplicate account identifier");
            return JSON.prepare(response);            
        }
        
        MofoIdentifier wrapper;
        try {
            wrapper = new MofoIdentifier(identifier);
        } catch (Exception ex) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 4);
            response.put("errorDescription", "Invalid identifier");
            return JSON.prepare(response);          
        }

        long identifierId = Account.getAccountIdByIdentifier(wrapper.getNormalizedId());
        if (identifierId != 0) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 4);
            response.put("errorDescription", "Duplicate identifier");
            return JSON.prepare(response);          
        }

        if (wrapper.getIsDefaultServer() && recipient == account.getId()) {

            /* no validation required, account is assigning default id to itself */

        }
        else {

            /* validation might be required - try and get the public key first */

            byte[] publicKey = null;
            Account recipientAccount = Account.getAccount(recipient);
            if (recipientAccount != null) {
                publicKey = recipientAccount.getPublicKey();
            }
            if (publicKey == null) {
                String recipientPublicKey = Convert.emptyToNull(req.getParameter("recipientPublicKey"));
                if (recipientPublicKey != null) {
                    publicKey = Convert.toBytes(recipientPublicKey);
                }
            }
            
            /* assigning non default identifiers always require signatory to be a verification authority */
    
            boolean signatorIsVerificationAuthority;
            byte[] message = Convert.toBytes(identifier);
            
            if (signatory == 0) {
                signatorIsVerificationAuthority = false;
            }
            else {
                signatorIsVerificationAuthority = MofoVerificationAuthority.getIsVerificationAuthority(signatory);
                publicKey = Account.getAccount(signatory).getPublicKey();
            }
    
            if (!wrapper.getIsDefaultServer() && !signatorIsVerificationAuthority) {
                JSONObject response = new JSONObject();
                response.put("errorCode", 4);
                response.put("errorDescription", "Operation requires verified authorizer signature");
                return JSON.prepare(response);
            }
    
            if (publicKey == null) {
                JSONObject response = new JSONObject();
                response.put("errorCode", 4);
                response.put("errorDescription", "Operation requires publicKey of signatory");
                return JSON.prepare(response);              
            }
            
            if (!Crypto.verify(signature, message, publicKey, false)) {
                JSONObject response = new JSONObject();
                response.put("errorCode", 4);
                response.put("errorDescription", "Could not verify signature");
                return JSON.prepare(response);
            }
        }

        Attachment attachment = new MofoAttachment.AccountIdAssignmentAttachment(identifier, signatory, signature);
        return createTransaction(req, account, recipient, 0, attachment);
    }

}
