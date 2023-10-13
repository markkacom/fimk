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

import nxt.Account;
import nxt.Appendix;
import nxt.Nxt;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.crypto.EncryptedData;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_TRANSACTION;
import static nxt.http.JSONResponses.MISSING_TRANSACTION;
import static nxt.http.JSONResponses.NO_MESSAGE;
import static nxt.http.JSONResponses.UNKNOWN_TRANSACTION;

public final class ReadMessage extends APIServlet.APIRequestHandler {

    static final ReadMessage instance = new ReadMessage();

    private ReadMessage() {
        super(new APITag[] {APITag.MESSAGES}, "transaction", "secretPhrase");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        String transactionIdString = Convert.emptyToNull(req.getParameter("transaction"));
        if (transactionIdString == null) {
            return MISSING_TRANSACTION;
        }

        Transaction transaction;
        try {
            transaction = Nxt.getBlockchain().getTransaction(Convert.parseUnsignedLong(transactionIdString));
            if (transaction == null) {
                return UNKNOWN_TRANSACTION;
            }
        } catch (RuntimeException e) {
            return INCORRECT_TRANSACTION;
        }

        JSONObject response = new JSONObject();
        Account senderAccount = Account.getAccount(transaction.getSenderId());
        Appendix.Message message = transaction.getMessage();
        Appendix.EncryptedMessage encryptedMessage = transaction.getEncryptedMessage();
        Appendix.EncryptToSelfMessage encryptToSelfMessage = transaction.getEncryptToSelfMessage();
        Appendix.PrunablePlainMessage prunableMessage = transaction.getPrunablePlainMessage();
        Appendix.PrunableEncryptedMessage prunableEncryptedMessage = transaction.getPrunableEncryptedMessage();
        if (message == null && encryptedMessage == null && encryptToSelfMessage == null && prunableMessage == null && prunableEncryptedMessage == null) {
            return NO_MESSAGE;
        }
        if (message != null) {
            response.put("message", message.toString());
            response.put("messageIsPrunable", false);
        } else if (prunableMessage != null) {
            response.put("message", prunableMessage.toString());
            response.put("messageIsPrunable", true);
        }
        String secretPhrase = Convert.emptyToNull(req.getParameter("secretPhrase"));
        if (secretPhrase != null) {
            EncryptedData encryptedData = null;
            boolean isText = false;
            boolean uncompress = true;
            if (encryptedMessage != null) {
                encryptedData = encryptedMessage.getEncryptedData();
                isText = encryptedMessage.isText();
                uncompress = encryptedMessage.isCompressed();
                response.put("encryptedMessageIsPrunable", false);
            } else if (prunableEncryptedMessage != null) {
                encryptedData = prunableEncryptedMessage.getEncryptedData();
                isText = prunableEncryptedMessage.isText();
                uncompress = prunableEncryptedMessage.isCompressed();
                response.put("encryptedMessageIsPrunable", true);
            }
            if (encryptedData != null) {
                long readerAccountId = Account.getId(Crypto.getPublicKey(secretPhrase));
                Account account = senderAccount.getId() == readerAccountId ? Account.getAccount(transaction.getRecipientId()) : senderAccount;
                if (account != null) {
                    try {
                        byte[] decrypted = account.decryptFrom(encryptedData, secretPhrase, uncompress);
                        response.put("decryptedMessage", isText ? Convert.toString(decrypted) : Convert.toHexString(decrypted));
                    } catch (RuntimeException e) {
                        Logger.logDebugMessage("Decryption of message to recipient failed: " + e.toString());
                        JSONData.putException(response, e, "Wrong secretPhrase");
                    }
                }
            }
            if (encryptToSelfMessage != null) {
                Account account = Account.getAccount(Crypto.getPublicKey(secretPhrase));
                if (account != null) {
                    try {
                        byte[] decrypted = account.decryptFrom(encryptToSelfMessage.getEncryptedData(), secretPhrase, encryptToSelfMessage.isCompressed());
                        response.put("decryptedMessageToSelf", encryptToSelfMessage.isText() ? Convert.toString(decrypted) : Convert.toHexString(decrypted));
                    } catch (RuntimeException e) {
                        Logger.logDebugMessage("Decryption of message to self failed: " + e.toString());
                    }
                }
            }
        }
        return response;
    }

}
