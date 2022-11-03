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

        // payload format: accountId|address|token
        // examples "12661257429910784930|11.22.333.44|394e8567c365a34c9856"
        String payload = a.getAliasURI();
        String[] ss = payload.split("\\|");
        if (ss.length != 3) return "Payload format is wrong";
        long accountId;
        try {
            accountId = Long.parseUnsignedLong(ss[0]);
            Account account = Account.getAccount(accountId);
            if (account == null) return "Payload parameter account id is wrong, account is not found";
        } catch (NumberFormatException e) {
            String resultMessage = "Payload parameter account id is wrong";
            Logger.logErrorMessage(resultMessage, e);
            return resultMessage;
        }
        String address = ss[1];
        String token = ss[2];

        // prevent registration of token sending for same pair account+address.
        // Hacker can send such transaction to overwrite the previous rightful registration with wrong token.
        if (!token.isEmpty() && transaction.getSenderId() != AccountNode.TOKEN_SENDER_ID) {
            String resultMessage = "The token sender does not match setting 'tokenSenderAccount'";
            Logger.logWarningMessage(resultMessage);
            return resultMessage;
        }

        if (validateOnly) return null;

        //apply

        AccountNode.save(transaction, accountId, address, token);
        return null;  //successful outcome
    }
}
