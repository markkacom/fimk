package nxt.txn.extension;

import nxt.Account;
import nxt.MofoTransactions;
import nxt.Transaction;
import nxt.TransactionType;

import java.util.HashMap;
import java.util.Map;

public abstract class TransactionTypeExtension {

    static Map<Integer, TransactionTypeExtension> transactionTypeExtensionMap = new HashMap<>(2);
    static Map<String, TransactionTypeExtension> markExtensionMap = new HashMap<>(2);

    public static void init() {
        register(
                MofoTransactions.NamespacedAliasAssignmentTransaction.NAMESPACED_ALIAS_ASSIGNMENT,
                new ExpiryExtension()
        );
        register(
                MofoTransactions.NamespacedAliasAssignmentTransaction.NAMESPACED_ALIAS_ASSIGNMENT,
                new RegisterRewardCandidateExtension()
        );
    }

    public static void register(TransactionType transactionType, TransactionTypeExtension extension) {
        //check is extension has unique mark (used to distinguish extensions when they are carried in same type transactions)
        if (markExtensionMap.containsKey(extension.getMark())) {
            throw new RuntimeException(String.format("Duplicate extension's mark %s", extension.getMark()));
        }
        markExtensionMap.put(extension.getMark(), extension);
        transactionTypeExtensionMap.put(twoBytesToInt(transactionType.getType(), transactionType.getSubtype()), extension);
    }

    /**
     * Used to used to distinguish extensions when they are carried in same type transactions extensions when they are carried in same type transactions
     */
    protected abstract String getMark();

    public abstract String getName();

    public static TransactionTypeExtension get(TransactionType transactionType) {
        return transactionTypeExtensionMap.get(twoBytesToInt(transactionType.getType(), transactionType.getSubtype()));
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

