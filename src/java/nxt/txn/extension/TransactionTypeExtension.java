package nxt.txn.extension;

import nxt.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Transaction Extension should not change any data (blockchain) created during transaction core processing.
 * Needed data can be added to extra structures (fields, records).
 */
public abstract class TransactionTypeExtension {

    static Map<String, TransactionTypeExtension> transactionTypeExtensionMap = new HashMap<>(2);
    static Map<String, TransactionTypeExtension> markExtensionMap = new HashMap<>(2);

    public static void init() {
        register(
                MofoTransactions.NamespacedAliasAssignmentTransaction.NAMESPACED_ALIAS_ASSIGNMENT,
                new ExpiryExtension()
        );
        /* obsolete. The login registration candidate is used
        register(
                MofoTransactions.NamespacedAliasAssignmentTransaction.NAMESPACED_ALIAS_ASSIGNMENT,
                new RegisterRewardCandidateExtension()
        );*/
        register(
                MofoTransactions.NamespacedAliasAssignmentTransaction.NAMESPACED_ALIAS_ASSIGNMENT,
                new RegisterNodeTokenExtension()
        );
        register(
                nxt.TransactionType.Data.TAGGED_DATA_UPLOAD,
                new ItemImageExtension()
        );
    }

    public static void register(TransactionType transactionType, TransactionTypeExtension extension) {
        //check is extension has unique mark (used to distinguish extensions when they are carried in same type transactions)
        if (markExtensionMap.containsKey(extension.getMark())) {
            throw new RuntimeException(String.format("Duplicate extension's mark %s", extension.getMark()));
        }
        markExtensionMap.put(extension.getMark(), extension);
        transactionTypeExtensionMap.put(extensionKey(transactionType, extension.getMark()), extension);
    }

    /**
     * Transaction can be used of any type. Transaction can contain some data that is mark that transaction carries the extension data.
     * For example message transaction contains marker "X7()RooNyeudmbchydj" (start of message) and there is implemented
     * extension for this marker. So all message transaction where message text is started with this marker are processed
     * using the extension.
     * @param transaction
     * @return
     */
    public static TransactionTypeExtension get(Transaction transaction) {
        Attachment att = transaction.getAttachment();

        if (att instanceof MofoAttachment.NamespacedAliasAssignmentAttachment) {
            String mark = ((MofoAttachment.NamespacedAliasAssignmentAttachment) att).getAliasName();
            return transactionTypeExtensionMap.get(extensionKey(transaction.getType(), mark));
        }

        /* Add the logic to marked transaction of type TaggedDataUpload: save URL or picture for asset */
        if (att instanceof Attachment.TaggedDataUpload) {
            String mark = ((Attachment.TaggedDataUpload) att).getChannel();
            return transactionTypeExtensionMap.get(extensionKey(transaction.getType(), mark));
        }

        return null;
    }

    /**
     * Used to distinguish extensions when they are carried in same type transactions extensions when they are carried in same type transactions
     */
    protected abstract String getMark();

    public abstract String getName();

    private static String extensionKey(TransactionType t, String mark) {
        return t.getType() + "-" + t.getSubtype() + "-" + mark;
    }

    private static int twoBytesToInt(byte b1, byte b2) {
        return ((b1 << 8) | (b2 & 0xFF));
    }

    /**
     * Validate and apply transaction extension.
     * @return error message or null if no error
     */
    public abstract String process(boolean validateOnly, Transaction transaction, Account sender, Account recipient);

}

