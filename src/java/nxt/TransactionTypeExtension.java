package nxt;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public abstract class TransactionTypeExtension {

    static Map<TransactionType, TransactionTypeExtension> map = new HashMap<>(2);

    public static void init() {
        register(
                MofoTransactions.NamespacedAliasAssignmentTransaction.NAMESPACED_ALIAS_ASSIGNMENT,
                new ExpiryExtension()
        );
    }

    public static void register(TransactionType transactionType, TransactionTypeExtension extension) {
        map.put(transactionType, extension);
    }

    public static TransactionTypeExtension get(TransactionType transactionType) {
        return map.get(transactionType);
    }


    abstract void apply(TransactionImpl transaction, Account sender, Account recipient, Attachment a);

}

class ExpiryExtension extends TransactionTypeExtension {

    void apply(TransactionImpl transaction, Account sender, Account recipient, Attachment a) {
        //todo parsing to extract asset id and expiry values
        Asset asset = Asset.getAsset(111);
        if (asset == null) return;
        try {
            asset.updateExpiry(2222);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
