package nxt;

import nxt.util.Logger;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public abstract class TransactionTypeExtension {

    static Map<TransactionType, TransactionTypeExtension> transactionTypeExtensionMap = new HashMap<>(2);
    static Map<String, TransactionTypeExtension> markExtensionMap = new HashMap<>(2);

    public static void init() {
        register(
                MofoTransactions.NamespacedAliasAssignmentTransaction.NAMESPACED_ALIAS_ASSIGNMENT,
                new ExpiryExtension()
        );
    }

    public static void register(TransactionType transactionType, TransactionTypeExtension extension) {
        //check is extension has unique mark (used to distinguish extensions when they are carried in same type transactions)
        if (markExtensionMap.containsKey(extension.getMark())) {
            throw new RuntimeException(String.format("Duplicate extension's mark %s", extension.getMark()));
        }
        markExtensionMap.put(extension.getMark(), extension);
        transactionTypeExtensionMap.put(transactionType, extension);
    }

    /**
     * Used to used to distinguish extensions when they are carried in same type transactions extensions when they are carried in same type transactions
     */
    protected abstract String getMark();

    protected abstract String getName();

    public static TransactionTypeExtension get(TransactionType transactionType) {
        return transactionTypeExtensionMap.get(transactionType);
    }


    abstract String apply(TransactionImpl transaction, Account sender, Account recipient, Attachment a);

}

class ExpiryExtension extends TransactionTypeExtension {

    // "(FTR.n.v)"  first number n to distinguish features (extensions), second number v to distinguish versions of the feature
    private static final String MARK = "(FTR.0.0)";

    @Override
    protected String getMark() {
        return MARK;
    }

    @Override
    protected String getName() {
        return "Expiry for Assets and Marketplaces";
    }

    String apply(TransactionImpl transaction, Account sender, Account recipient, Attachment attachment) {
        MofoAttachment.NamespacedAliasAssignmentAttachment a;
        Attachment.AbstractAttachment att = transaction.getAttachment();
        if (att instanceof MofoAttachment.NamespacedAliasAssignmentAttachment) {
            a = (MofoAttachment.NamespacedAliasAssignmentAttachment) att;
        } else {
            return "Attachment type is not suitable";
        }
        if (!getMark().equals(a.getAliasName())) return "Wrong mark";
        // payload format "assetId|expiryTimestamp", e.g. "834538499053643|465906798"
        String payload = a.getAliasURI();
        if (payload.isEmpty() || payload.indexOf('|') == -1) return "Wrong payload";
        String[] ss = payload.split("\\|");
        if (ss.length != 2) return "Wrong payload";

        long assetId;
        int expiryTimestamp;
        try {
            assetId = Long.parseUnsignedLong(ss[0]);
            expiryTimestamp = Integer.parseInt(ss[1]);
        } catch (NumberFormatException e) {
            String resultMessage = "Transaction payload is wrong";
            Logger.logErrorMessage(resultMessage, e);
            return resultMessage;
        }

        Asset asset = Asset.getAsset(assetId);
        if (asset == null) return "Asset is not found";
        if (asset.getAccountId() != sender.getId()) return "Sender is not issuer of the asset";
        if (expiryTimestamp < transaction.getTimestamp()) return "Timestamp should be greater than transaction time (should be in future)";
        try {
            asset.updateExpiry(expiryTimestamp);
            return null;  //successful outcome
        } catch (SQLException e) {
            Logger.logErrorMessage("Extension expiry updating error", e);
        }
        return "Error on applying extension, see log";
    }
}
