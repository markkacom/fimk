package nxt.txn.extension;

import nxt.*;
import nxt.util.Logger;

import java.sql.SQLException;

class ExpiryExtension extends NamespacedAliasBasedExtension {

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
        String validateResult = validate(transaction);
        if (validateResult != null) return validateResult;

        MofoAttachment.NamespacedAliasAssignmentAttachment a =
                (MofoAttachment.NamespacedAliasAssignmentAttachment) transaction.getAttachment();

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
        if (expiryTimestamp < transaction.getTimestamp()) {
            return "Timestamp should be greater than transaction time (should be in future)";
        }

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
            Logger.logErrorMessage("Extension \"Expiry\" updating error", e);
        }
        return "Error on applying extension, see log";
    }
}
