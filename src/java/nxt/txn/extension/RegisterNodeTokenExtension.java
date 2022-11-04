package nxt.txn.extension;

import nxt.Account;
import nxt.MofoAttachment;
import nxt.Transaction;
import nxt.reward.AccountNode;
import nxt.util.Logger;

/**
 * Rewarding node sends this transaction as response on registration transaction from account.
 */
class RegisterNodeTokenExtension extends NamespacedAliasBasedExtension {

    // "(FTR.n.v)"  first number n to distinguish features (extensions), second number v to distinguish versions of the feature
    private static final String MARK = "(FTR.2.0)";

    @Override
    protected String getMark() {
        return MARK;
    }

    @Override
    public String getName() {
        return "Register Node Token";
    }

    public String process(boolean validateOnly, Transaction transaction, Account sender, Account recipient) {
        String validateResult = validate(transaction);
        if (validateResult != null) return validateResult;

        MofoAttachment.NamespacedAliasAssignmentAttachment a =
                (MofoAttachment.NamespacedAliasAssignmentAttachment) transaction.getAttachment();

        int stage = -1;

        String payload = a.getAliasURI();
        String[] ss = payload.split("\\|");

        long accountId;
        String address;
        String token = null;

        if (ss.length == 1) {
            // stage 1
            // payload format: address
            accountId = transaction.getSenderId();
            address = ss[0];
            stage = 1;
        } else if (ss.length == 2) {
            // stage 2
            // payload format: address|token
            // examples "11.22.333.44|394e8567c365a34c9856", "macrohard.net|394e8567c365a34c9856"
            accountId = transaction.getRecipientId();
            address = ss[0];
            token = ss[1];
            stage = 2;
        } else {
            return "Payload format is wrong";
        }

        // prevent registration of token sending for same pair account+address.
        // Hacker can send such transaction to overwrite the previous rightful registration with wrong token.
        if (stage == 2 && transaction.getSenderId() != AccountNode.TOKEN_SENDER_ID) {
            String resultMessage = "The token sender does not match setting 'tokenSenderAccount'";
            Logger.logWarningMessage(resultMessage);
            return resultMessage;
        }

        if (validateOnly) return null;

        //apply

        if (stage == 1) {
            AccountNode.saveApplicantStage(transaction, accountId, address);
        }
        if (stage == 2) {
            AccountNode.save(transaction, accountId, address, token);
        }
        return null;  //successful outcome
    }
}
