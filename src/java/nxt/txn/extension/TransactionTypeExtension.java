package nxt.txn.extension;

import nxt.*;

import java.util.HashMap;
import java.util.Map;

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
     * Used to distinguish extensions when they are carried in same type transactions extensions when they are carried in same type transactions
     */
    protected abstract String getMark();

    public abstract String getName();

    public static TransactionTypeExtension get(Transaction transaction) {
        MofoAttachment.NamespacedAliasAssignmentAttachment a;
        Attachment att = transaction.getAttachment();
        if (att instanceof MofoAttachment.NamespacedAliasAssignmentAttachment) {
            a = (MofoAttachment.NamespacedAliasAssignmentAttachment) att;
        } else {
            return null;
        }
        String mark = a.getAliasName();
        return transactionTypeExtensionMap.get(extensionKey(transaction.getType(), mark));
    }

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

