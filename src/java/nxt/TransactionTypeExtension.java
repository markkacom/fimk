package nxt;

import nxt.util.Logger;

import java.sql.SQLException;
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

class ExpiryExtension extends TransactionTypeExtension {

    // "(FTR.n.v)"  first number n to distinguish features (extensions), second number v to distinguish versions of the feature
    private static final String MARK = "(FTR.0.0)";

    @Override
    protected String getMark() {
        return MARK;
    }

    @Override
    public String getName() {
        return "Expiry for Assets and Marketplaces";
    }

    public String process(boolean validateOnly, Transaction transaction, Account sender, Account recipient) {
        MofoAttachment.NamespacedAliasAssignmentAttachment a;
        Attachment.AbstractAttachment att = ((TransactionImpl) transaction).getAttachment();

        //validate

        if (att instanceof MofoAttachment.NamespacedAliasAssignmentAttachment) {
            a = (MofoAttachment.NamespacedAliasAssignmentAttachment) att;
        } else {
            return "Attachment type is not suitable";
        }
        if (!getMark().equals(a.getAliasName())) return "Wrong mark";
        // payload format "assetId|expiryTimestamp|goodsId|expiryTimestamp",
        // e.g. "834538499053643|465906798|934725444373|465906798"  "||934725444373|465906798"
        String payload = a.getAliasURI();
        if (payload.isEmpty() || payload.indexOf('|') == -1) return "Wrong payload";
        String[] ss = payload.split("\\|");
        if (ss.length < 2 || ss.length > 4) return "Wrong payload";

        long assetId;
        long goodsId = 0;
        int expiryTimestamp;
        try {
            assetId = ss[0].trim().isEmpty() ? 0 : Long.parseUnsignedLong(ss[0]);
            expiryTimestamp = Integer.parseInt(ss[1]);
            if (ss.length > 2) {
                goodsId = ss[2].trim().isEmpty() ? 0 : Long.parseUnsignedLong(ss[2]);
                expiryTimestamp = Integer.parseInt(ss[3]);
            }
        } catch (NumberFormatException e) {
            String resultMessage = "Transaction payload is wrong";
            Logger.logErrorMessage(resultMessage, e);
            return resultMessage;
        }

        Asset asset = null;
        DigitalGoodsStore.Goods goods = null;
        if (assetId != 0) {
            asset = Asset.getAsset(assetId);
            if (asset == null) return "Asset is not found";
            if (asset.getAccountId() != sender.getId()) return "Sender is not the issuer of the asset";
        }
        if (goodsId != 0) {
            goods = DigitalGoodsStore.Goods.getGoods(goodsId);
            if (goods == null) return "Goods is not found";
            if (goods.getSellerId() != sender.getId()) return "Sender is not seller of the goods";
        }
        if (expiryTimestamp < transaction.getTimestamp()) return "Timestamp should be greater than transaction time (should be in future)";

        if (validateOnly) return null;

        //apply

        try {
            if (asset != null) {
                asset.updateExpiry(expiryTimestamp);
            }
            if (goods != null) {
                goods.updateExpiry(expiryTimestamp);
            }
            return null;  //successful outcome
        } catch (SQLException e) {
            Logger.logErrorMessage("Extension expiry updating error", e);
        }
        return "Error on applying extension, see log";
    }
}
