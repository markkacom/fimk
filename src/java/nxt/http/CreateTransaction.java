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

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import nxt.*;
import nxt.crypto.Crypto;
import nxt.crypto.EncryptedData;
import nxt.txn.extension.TransactionTypeExtension;
import nxt.util.Convert;
import nxt.util.JSON;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import java.util.Arrays;

import static nxt.http.JSONResponses.*;

abstract class CreateTransaction extends APIServlet.APIRequestHandler {

    private static final String[] commonParameters = new String[]{"secretPhrase", "publicKey", "feeNQT",
            "deadline", "referencedTransactionFullHash", "broadcast",
            "message", "messageIsText", "messageIsPrunable",
            "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce", "encryptedMessageIsPrunable", "compressMessageToEncrypt",
            "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce", "compressMessageToEncryptToSelf",
            "phased", "phasingFinishHeight", "phasingVotingModel", "phasingQuorum", "phasingMinBalance", "phasingHolding", "phasingMinBalanceModel",
            "phasingWhitelisted", "phasingWhitelisted", "phasingWhitelisted",
            "phasingLinkedFullHash", "phasingLinkedFullHash", "phasingLinkedFullHash",
            "phasingHashedSecret", "phasingHashedSecretAlgorithm",
            "recipientPublicKey"};

    private static String[] addCommonParameters(String[] parameters) {
        String[] result = Arrays.copyOf(parameters, parameters.length + commonParameters.length);
        System.arraycopy(commonParameters, 0, result, parameters.length, commonParameters.length);
        return result;
    }

    CreateTransaction(APITag[] apiTags, String... parameters) {
        super(apiTags, addCommonParameters(parameters));
    }

    CreateTransaction(String fileParameter, APITag[] apiTags, String... parameters) {
        super(fileParameter, apiTags, addCommonParameters(parameters));
    }

    @POST
    @Parameter(name = "secretPhrase", in = ParameterIn.QUERY)
    @Parameter(name = "publicKey", in = ParameterIn.QUERY)
    @Parameter(name = "feeNQT", in = ParameterIn.QUERY, required = true, schema = @Schema(defaultValue = "10000000"))
    @Parameter(name = "deadline", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", defaultValue = "1440"))
    @Parameter(name = "broadcast", in = ParameterIn.QUERY, schema = @Schema(type = "boolean", defaultValue = "true"))
    abstract public JSONStreamAware processRequest(HttpServletRequest request) throws NxtException;

    final JSONStreamAware createTransaction(HttpServletRequest req, Account senderAccount, Attachment attachment)
            throws NxtException {
        return createTransaction(req, senderAccount, 0, 0, attachment);
    }

    final JSONStreamAware createTransaction(HttpServletRequest req, Account senderAccount, long recipientId, long amountNQT)
            throws NxtException {
        return createTransaction(req, senderAccount, recipientId, amountNQT, Attachment.ORDINARY_PAYMENT);
    }

    private Appendix.Phasing parsePhasing(HttpServletRequest req) throws ParameterException {
        byte votingModel = ParameterParser.getByte(req, "phasingVotingModel", (byte)-1, (byte)5, true);

        long quorum = ParameterParser.getLong(req, "phasingQuorum", 0, Long.MAX_VALUE, false);

        int finishHeight = ParameterParser.getInt(req, "phasingFinishHeight",
                Nxt.getBlockchain().getHeight() + 1,
                Nxt.getBlockchain().getHeight() + Constants.MAX_PHASING_DURATION + 1,
                true);

        long minBalance = ParameterParser.getLong(req, "phasingMinBalance", 0, Long.MAX_VALUE, false);

        byte minBalanceModel = ParameterParser.getByte(req, "phasingMinBalanceModel", (byte)0, (byte)3, false);

        long holdingId = ParameterParser.getUnsignedLong(req, "phasingHolding", false);

        long[] whitelist = null;
        String[] whitelistValues = req.getParameterValues("phasingWhitelisted");
        if (whitelistValues != null && whitelistValues.length > 0) {
            whitelist = new long[whitelistValues.length];
            for (int i = 0; i < whitelistValues.length; i++) {
                whitelist[i] = Convert.parseAccountId(whitelistValues[i]);
                if (whitelist[i] == 0) {
                    throw new ParameterException(INCORRECT_WHITELIST);
                }
            }
        }

        byte[][] linkedFullHashes = null;
        String[] linkedFullHashesValues = req.getParameterValues("phasingLinkedFullHash");
        if (linkedFullHashesValues != null && linkedFullHashesValues.length > 0) {
            linkedFullHashes = new byte[linkedFullHashesValues.length][];
            for (int i = 0; i < linkedFullHashes.length; i++) {
                linkedFullHashes[i] = Convert.parseHexString(linkedFullHashesValues[i]);
                if (Convert.emptyToNull(linkedFullHashes[i]) == null || linkedFullHashes[i].length != 32) {
                    throw new ParameterException(INCORRECT_LINKED_FULL_HASH);
                }
            }
        }

        byte[] hashedSecret = Convert.parseHexString(Convert.emptyToNull(req.getParameter("phasingHashedSecret")));
        byte algorithm = ParameterParser.getByte(req, "phasingHashedSecretAlgorithm", (byte) 0, Byte.MAX_VALUE, false);

        return new Appendix.Phasing(finishHeight, votingModel, holdingId, quorum, minBalance, minBalanceModel, whitelist,
                linkedFullHashes, hashedSecret, algorithm);
    }

    final JSONStreamAware createTransaction(HttpServletRequest req, Account senderAccount, long recipientId,
                                            long amountNQT, Attachment attachment)
            throws NxtException {
        String deadlineValue = req.getParameter("deadline");
        String referencedTransactionFullHash = Convert.emptyToNull(req.getParameter("referencedTransactionFullHash"));
        String secretPhrase = Convert.emptyToNull(req.getParameter("secretPhrase"));
        String publicKeyValue = Convert.emptyToNull(req.getParameter("publicKey"));
        boolean broadcast = !"false".equalsIgnoreCase(req.getParameter("broadcast")) && secretPhrase != null;
        Appendix.EncryptedMessage encryptedMessage = null;
        Appendix.PrunableEncryptedMessage prunableEncryptedMessage = null;
        Account recipient = null;
        if (attachment.getTransactionType().canHaveRecipient()) {
            recipient = Account.getAccount(recipientId);
            if ("true".equalsIgnoreCase(req.getParameter("encryptedMessageIsPrunable"))) {
                prunableEncryptedMessage = (Appendix.PrunableEncryptedMessage) ParameterParser.getEncryptedMessage(req, recipient, true);
            } else {
                encryptedMessage = (Appendix.EncryptedMessage) ParameterParser.getEncryptedMessage(req, recipient, false);
            }
        }
        Appendix.EncryptToSelfMessage encryptToSelfMessage = null;
        EncryptedData encryptedToSelfData = ParameterParser.getEncryptToSelfMessage(req);
        if (encryptedToSelfData != null) {
            boolean isText = !"false".equalsIgnoreCase(req.getParameter("messageToEncryptToSelfIsText"));
            boolean isCompressed = !"false".equalsIgnoreCase(req.getParameter("compressMessageToEncryptToSelf"));
            encryptToSelfMessage = new Appendix.EncryptToSelfMessage(encryptedToSelfData, isText, isCompressed);
        }
        Appendix.Message message = null;
        Appendix.PrunablePlainMessage prunablePlainMessage = null;
        if ("true".equalsIgnoreCase(req.getParameter("messageIsPrunable"))) {
            prunablePlainMessage = (Appendix.PrunablePlainMessage) ParameterParser.getPlainMessage(req, true);
        } else {
            message = (Appendix.Message) ParameterParser.getPlainMessage(req, false);
        }
        Appendix.PublicKeyAnnouncement publicKeyAnnouncement = null;
        String recipientPublicKey = Convert.emptyToNull(req.getParameter("recipientPublicKey"));
        if (recipientPublicKey != null) {
            publicKeyAnnouncement = new Appendix.PublicKeyAnnouncement(Convert.parseHexString(recipientPublicKey));
        }

        Appendix.Phasing phasing = null;
        boolean phased = "true".equalsIgnoreCase(req.getParameter("phased"));
        if (phased) {
            phasing = parsePhasing(req);
        }

        if (secretPhrase == null && publicKeyValue == null) return MISSING_SECRET_PHRASE;
        if (deadlineValue == null) return MISSING_DEADLINE;

        short deadline;
        try {
            deadline = Short.parseShort(deadlineValue);
            if (deadline < 1) {
                return INCORRECT_DEADLINE;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_DEADLINE;
        }

        long feeNQT = ParameterParser.getFeeNQT(req);

        JSONObject response = new JSONObject();

        // shouldn't try to get publicKey from senderAccount as it may have not been set yet
        byte[] publicKey = secretPhrase != null ? Crypto.getPublicKey(secretPhrase) : Convert.parseHexString(publicKeyValue);

        try {
            Transaction.Builder builder = Nxt.newTransactionBuilder(publicKey, amountNQT, feeNQT,
                    deadline, attachment).referencedTransactionFullHash(referencedTransactionFullHash);
            if (attachment.getTransactionType().canHaveRecipient()) {
                builder.recipientId(recipientId);
            }
            builder.appendix(encryptedMessage);
            builder.appendix(message);
            builder.appendix(publicKeyAnnouncement);
            builder.appendix(encryptToSelfMessage);
            builder.appendix(phasing);
            builder.appendix(prunablePlainMessage);
            builder.appendix(prunableEncryptedMessage);
            Transaction transaction = builder.build(secretPhrase);
            try {
                if (Math.addExact(amountNQT, transaction.getFeeNQT()) > senderAccount.getUnconfirmedBalanceNQT()) {
                    return NOT_ENOUGH_FUNDS;
                }
            } catch (ArithmeticException e) {
                return NOT_ENOUGH_FUNDS;
            }

            JSONObject txnExtensionValidationResult = validateExtension(transaction, senderAccount, recipient);
            if (txnExtensionValidationResult != null) return txnExtensionValidationResult;

            JSONObject transactionJSON = JSONData.unconfirmedTransaction(transaction);
            response.put("transactionJSON", transactionJSON);
            response.put("unsignedTransactionBytes", Convert.toHexString(transaction.getUnsignedBytes()));
            if (secretPhrase != null) {
                response.put("transaction", transaction.getStringId());
                response.put("fullHash", transactionJSON.get("fullHash"));
                response.put("transactionBytes", Convert.toHexString(transaction.getBytes()));
                response.put("signatureHash", transactionJSON.get("signatureHash"));
            }
            if (broadcast) {
                Nxt.getTransactionProcessor().broadcast(transaction);
                response.put("broadcasted", true);
            } else {
                transaction.validate();
                response.put("broadcasted", false);
            }
        } catch (NxtException.NotYetEnabledException e) {
            return FEATURE_NOT_AVAILABLE;
        } catch (NxtException.ValidationException e) {
            if (broadcast) {
                response.clear();
            }
            response.put("broadcasted", false);
            JSONData.putException(response, e);
        }
        return response;

    }

    @Override
    final boolean requirePost() {
        return true;
    }

    private JSONObject validateExtension(Transaction transaction, Account senderAccount, Account recipientAccount) {
        JSONObject response = null;
        if (Nxt.getBlockchain().getHeight() < Constants.TRANSACTION_EXTENSION_HEIGHT) return response;
        try {
            TransactionTypeExtension ext = TransactionTypeExtension.get(transaction);
            if (ext != null) {
                String result = ext.process(true, transaction, senderAccount, recipientAccount);
                if (result != null) {
                    String errorMessage = String.format("Transaction extension \"%s\" validation error. %s", ext.getName(), result);
                    Logger.logWarningMessage(errorMessage);
                    response = new JSONObject();
                    response.put("errorCode", 22);
                    response.put("errorDescription", errorMessage);
                    JSON.prepare(response);
                    return response;
                }
            }
        } catch (Exception e) {
            String errorMessage = String.format("Transaction extension validation error: %s", e.getMessage());
            response = new JSONObject();
            response.put("errorCode", 22);
            response.put("errorDescription", errorMessage);
            JSON.prepare(response);
            Logger.logErrorMessage("Transaction extension validation error", e);
        }
        return response;
    }

}
