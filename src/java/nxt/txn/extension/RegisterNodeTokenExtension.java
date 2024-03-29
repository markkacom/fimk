package nxt.txn.extension;

import nxt.Account;
import nxt.MofoAttachment;
import nxt.Transaction;
import nxt.reward.AccountNode;
import nxt.util.Convert;
import nxt.util.Logger;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Rewarding node sends this transaction as response on registration transaction from account.
 */
class RegisterNodeTokenExtension extends NamespacedAliasBasedExtension {

    // "(FTR.n.v)"  first number n to distinguish features (extensions),
    // second number v to distinguish versions of the feature
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

        String payload = a.getAliasURI().trim();

        // payload format: registerAccount|address
        // examples "45987878967767|11.22.333.44", "45987878967767|macrohard.net"
        String[] ss = payload.split("\\|");
        if (ss.length != 2) return "Payload format is wrong";

        long registerAccountId;
        try {
            registerAccountId = Long.parseUnsignedLong(ss[0]);
            Account account = Account.getAccount(registerAccountId);
            if (account == null) return "Payload parameter register account id is wrong, account is not found";
        } catch (NumberFormatException e) {
            String resultMessage = "Payload parameter register account id is wrong, account is not found";
            Logger.logErrorMessage(resultMessage, e);
            return resultMessage;
        }

        long accountId = transaction.getSenderId();
        String address = ss[1];
        String addressCheckResult = checkAddressFormat(address);
        if (addressCheckResult != null) return addressCheckResult;

        String token = Convert.toHexString(transaction.getSenderPublicKey());

        if (validateOnly) return null;

        //apply

        AccountNode.save(transaction, registerAccountId, accountId, address, token);

        return null;  //successful outcome
    }

    private String checkAddressFormat(String address) {
        try {
            // WORKAROUND: add any scheme to make the resulting URI valid.
            URI uri = new URI("my://" + address);
            if (uri.getHost() == null) {
                return "Address must have host and port parts";
            }
            return null; // result is OK
        } catch (URISyntaxException ex) {
            return "Address format is wrong";
        }
    }
}
