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
import nxt.HardFork;
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

        long recipientId = ParameterParser.getAccountId(req, "recipient", true);
        Account senderAccount = ParameterParser.getSenderAccount(req);
        String identifier = Convert.emptyToNull(req.getParameter("identifier"));
        long signatory;
        try {
            signatory = Convert.parseAccountId(Convert.emptyToNull(req.getParameter("signatory")));
        } catch (RuntimeException ex) {
            return INCORRECT_SIGNATORY;
        }
        byte[] signature = Convert.parseHexString(Convert.emptyToNull(req.getParameter("signature")));

        MofoIdentifier wrapper;
        try {
            wrapper = new MofoIdentifier(identifier);
        } catch (Exception ex) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 4);
            response.put("errorDescription", "Invalid identifier");
            return JSON.prepare(response);
        }

        if (Account.getAccountIdByIdentifier(wrapper.getNormalizedId()) != 0) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 4);
            response.put("errorDescription", "Duplicate identifier");
            return JSON.prepare(response);
        }

        if ( ! HardFork.ACCOUNT_IDENTIFIER_BLOCK_2()) {
            if (Account.hasAccountIdentifier(recipientId)) {
                JSONObject response = new JSONObject();
                response.put("errorCode", 4);
                response.put("errorDescription", "Recipient already has identifier");
                return JSON.prepare(response);
            }

            if (wrapper.getIsDefaultServer()) {
                String similarId = wrapper.getSimilarId();
                if (Account.getAccountIdByIdentifier(similarId) != 0) {
                    JSONObject response = new JSONObject();
                    response.put("errorCode", 4);
                    response.put("errorDescription", "Duplicate identifier");
                    return JSON.prepare(response);
                }
            }
        }

        if (wrapper.getIsDefaultServer() && recipientId == senderAccount.getId()) {

            /* no validation required, account is assigning default id to itself */

        }
        else {

            /* validation might be required - try and get the public key first */

            byte[] publicKey = null;
            Account recipientAccount = Account.getAccount(recipientId);
            if (recipientAccount != null) {
                publicKey = recipientAccount.getPublicKey();
            }
            if (publicKey == null) {
                publicKey = Convert.parseHexString(Convert.emptyToNull(req.getParameter("recipientPublicKey")));
            }

            /* assigning non default identifiers always require signatory to be a verification authority */

            boolean signatorIsVerificationAuthority;
            byte[] message = Convert.toBytes(identifier);

            if (signatory == 0) {
                signatorIsVerificationAuthority = false;
            }
            else {
                /* don't touch the db if not necessary */
                if (!wrapper.getIsDefaultServer()) {
                    signatorIsVerificationAuthority = MofoVerificationAuthority.getIsVerificationAuthority(signatory);
                }
                else {
                    /* Does not matter, just assign it something */
                    signatorIsVerificationAuthority = false;
                }
                Account signatoryAccount = Account.getAccount(signatory);
                if (signatoryAccount != null) {
                    publicKey = signatoryAccount.getPublicKey();
                }
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

        Attachment attachment = new MofoAttachment.SetAccountIdentifierAttachment(identifier, signatory, signature);
        return createTransaction(req, senderAccount, recipientId, 0, attachment);
    }

}
