package nxt.txn;

import nxt.*;
import nxt.util.Logger;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

public abstract class ColoredCoinsTxnTypes extends TransactionType {

    ColoredCoinsTxnTypes() {
    }

    @Override
    public final byte getType() {
        return TransactionType.TYPE_COLORED_COINS;
    }

    public static final TransactionType ASSET_ISSUANCE = new ColoredCoinsTxnTypes() {

        private final Fee ASSET_ISSUANCE_FEE = new Fee.ConstantFee(1000 * Constants.ONE_NXT);
        private final Fee PRIVATE_ASSET_ISSUANCE_FEE = new Fee.ConstantFee(10000 * Constants.ONE_NXT);

        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_COLORED_COINS_ASSET_ISSUANCE;
        }

        @Override
        public String getName() {
            return "AssetIssuance";
        }

        @Override
        public Fee getBaselineFee(Transaction transaction) {
            AssetIssuanceAttachment attachment = (AssetIssuanceAttachment) transaction.getAttachment();
            if (MofoAsset.isPrivateAsset(attachment.getType())) {
                if (Constants.isTestnet) {
                    if (Nxt.getBlockchain().getHeight() <= 110000) {
                        return ASSET_ISSUANCE_FEE;
                    }
                }
                return PRIVATE_ASSET_ISSUANCE_FEE;
            }
            return ASSET_ISSUANCE_FEE;
        }

        @Override
        protected AssetIssuanceAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) throws NxtException.NotValidException {
            return new AssetIssuanceAttachment(buffer, transactionVersion, timestamp);
        }

        @Override
        protected AssetIssuanceAttachment parseAttachment(JSONObject attachmentData, int timestamp) throws NxtException.NotValidException {
            return new AssetIssuanceAttachment(attachmentData, timestamp);
        }

        @Override
        protected boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        protected void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            AssetIssuanceAttachment attachment = (AssetIssuanceAttachment) transaction.getAttachment();
            long assetId = transaction.getId();
            Asset.addAsset(transaction, attachment);
            senderAccount.addToAssetAndUnconfirmedAssetBalanceQNT(assetId, attachment.getQuantityQNT());
        }

        @Override
        protected void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        protected void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            AssetIssuanceAttachment attachment = (AssetIssuanceAttachment) transaction.getAttachment();
            if (attachment.getName().length() < Constants.MIN_ASSET_NAME_LENGTH
                    || attachment.getName().length() > Constants.MAX_ASSET_NAME_LENGTH
                    || attachment.getDescription().length() > Constants.MAX_ASSET_DESCRIPTION_LENGTH
                    || attachment.getDecimals() < 0 || attachment.getDecimals() > 8
                    || attachment.getQuantityQNT() <= 0
                    || attachment.getQuantityQNT() > Constants.MAX_ASSET_QUANTITY_QNT
                    || attachment.getType() < 0 || attachment.getType() > 1) {
                throw new NxtException.NotValidException("Invalid asset issuance: " + attachment.getJSONObject());
            }
            String normalizedName = attachment.getName().toLowerCase();
            for (int i = 0; i < normalizedName.length(); i++) {
                if (Constants.ALPHABET.indexOf(normalizedName.charAt(i)) < 0) {
                    throw new NxtException.NotValidException("Invalid asset name: " + normalizedName);
                }
            }
        }

        @Override
        public boolean canHaveRecipient() {
            return false;
        }

        @Override
        public boolean isPhasingSafe() {
            return true;
        }

    };

    public static final TransactionType ASSET_TRANSFER = new ColoredCoinsTxnTypes() {

        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_COLORED_COINS_ASSET_TRANSFER;
        }

        @Override
        public String getName() {
            return "AssetTransfer";
        }

        @Override
        protected AssetTransferAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) throws NxtException.NotValidException {
            return new AssetTransferAttachment(buffer, transactionVersion);
        }

        @Override
        protected AssetTransferAttachment parseAttachment(JSONObject attachmentData, int timestamp) throws NxtException.NotValidException {
            return new AssetTransferAttachment(attachmentData);
        }

        @Override
        protected boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            AssetTransferAttachment attachment = (AssetTransferAttachment) transaction.getAttachment();
            long unconfirmedAssetBalance = senderAccount.getUnconfirmedAssetBalanceQNT(attachment.getAssetId());
            if (unconfirmedAssetBalance >= 0 && unconfirmedAssetBalance >= attachment.getQuantityQNT()) {
                senderAccount.addToUnconfirmedAssetBalanceQNT(attachment.getAssetId(), -attachment.getQuantityQNT());
                return true;
            }
            return false;
        }

        @Override
        protected void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            AssetTransferAttachment attachment = (AssetTransferAttachment) transaction.getAttachment();
            senderAccount.addToAssetBalanceQNT(attachment.getAssetId(), -attachment.getQuantityQNT());
            recipientAccount.addToAssetAndUnconfirmedAssetBalanceQNT(attachment.getAssetId(), attachment.getQuantityQNT());
            AssetTransfer.addAssetTransfer(transaction, attachment);
        }

        @Override
        protected void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            AssetTransferAttachment attachment = (AssetTransferAttachment) transaction.getAttachment();
            senderAccount.addToUnconfirmedAssetBalanceQNT(attachment.getAssetId(), attachment.getQuantityQNT());
        }

        @Override
        protected void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            AssetTransferAttachment attachment = (AssetTransferAttachment) transaction.getAttachment();
            if (transaction.getAmountNQT() != 0
                    || attachment.getComment() != null && attachment.getComment().length() > Constants.MAX_ASSET_TRANSFER_COMMENT_LENGTH
                    || attachment.getAssetId() == 0) {
                throw new NxtException.NotValidException("Invalid asset transfer amount or comment: " + attachment.getJSONObject());
            }
            if (transaction.getVersion() > 0 && attachment.getComment() != null) {
                throw new NxtException.NotValidException("Asset transfer comments no longer allowed, use message " +
                        "or encrypted message appendix instead");
            }
            Asset asset = Asset.getAsset(attachment.getAssetId());
            if (attachment.getQuantityQNT() <= 0 || (asset != null && attachment.getQuantityQNT() > asset.getQuantityQNT())) {
                throw new NxtException.NotValidException("Invalid asset transfer asset or quantity: " + attachment.getJSONObject());
            }
            if (asset == null) {
                throw new NxtException.NotCurrentlyValidException("Asset " + Long.toUnsignedString(attachment.getAssetId()) +
                        " does not exist yet");
            }
            if (Asset.privateEnabled() && MofoAsset.isPrivateAsset(asset)) {
                if (!MofoAsset.getAccountAllowed(attachment.getAssetId(), transaction.getSenderId())) {
                    throw new NxtException.NotValidException("Sender not allowed to transfer private asset");
                } else if (!MofoAsset.getAccountAllowed(attachment.getAssetId(), transaction.getRecipientId())) {
                    throw new NxtException.NotValidException("Recipient not allowed to receive private asset");
                }
            }
        }

        @Override
        public boolean canHaveRecipient() {
            return true;
        }

        @Override
        public boolean isPhasingSafe() {
            return true;
        }

    };

    abstract static class ColoredCoinsOrderPlacement extends ColoredCoinsTxnTypes {

        @Override
        protected void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            OrderPlacementAttachment attachment = (OrderPlacementAttachment) transaction.getAttachment();
            if (attachment.getPriceNQT() <= 0 || attachment.getPriceNQT() > Constants.MAX_BALANCE_NQT
                    || attachment.getAssetId() == 0) {
                throw new NxtException.NotValidException("Invalid asset order placement: " + attachment.getJSONObject());
            }
            Asset asset = Asset.getAsset(attachment.getAssetId());
            if (attachment.getQuantityQNT() <= 0 || (asset != null && attachment.getQuantityQNT() > asset.getQuantityQNT())) {
                throw new NxtException.NotValidException("Invalid asset order placement asset or quantity: " + attachment.getJSONObject());
            }
            if (asset == null) {
                throw new NxtException.NotCurrentlyValidException("Asset " + Long.toUnsignedString(attachment.getAssetId()) +
                        " does not exist yet");
            }
            doValidateAttachment(transaction);
        }

        abstract void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException;

        @Override
        public final boolean canHaveRecipient() {
            return false;
        }

        @Override
        public final boolean isPhasingSafe() {
            return true;
        }

    }

    public static final TransactionType ASK_ORDER_PLACEMENT = new ColoredCoinsOrderPlacement() {

        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT;
        }

        @Override
        public String getName() {
            return "AskOrderPlacement";
        }

        @Override
        protected AskOrderPlacementAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) throws NxtException.NotValidException {
            return new AskOrderPlacementAttachment(buffer, transactionVersion, timestamp);
        }

        @Override
        protected AskOrderPlacementAttachment parseAttachment(JSONObject attachmentData, int timestamp) throws NxtException.NotValidException {
            return new AskOrderPlacementAttachment(attachmentData, timestamp);
        }

        @Override
        protected boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            AskOrderPlacementAttachment attachment = (AskOrderPlacementAttachment) transaction.getAttachment();
            long unconfirmedAssetBalance = senderAccount.getUnconfirmedAssetBalanceQNT(attachment.getAssetId());
            if (unconfirmedAssetBalance >= 0 && unconfirmedAssetBalance >= attachment.getQuantityQNT()) {
                if (Asset.privateEnabled() && MofoAsset.isPrivateAsset(attachment.getAssetId())) {
                    try {
                        long totalAndOrderFeeQNT = Math.addExact(attachment.getQuantityQNT(), attachment.getOrderFeeQNT());
                        if (unconfirmedAssetBalance > totalAndOrderFeeQNT && MofoAsset.calculateOrderFee(attachment.getAssetId(), attachment.getQuantityQNT()) == attachment.getOrderFeeQNT()) {
                            senderAccount.addToUnconfirmedAssetBalanceQNT(attachment.getAssetId(), -totalAndOrderFeeQNT);
                            return true;
                        }
                    } catch (ArithmeticException e) {
                        Logger.logErrorMessage("Arithmetic exception", e);
                    }
                } else {
                    senderAccount.addToUnconfirmedAssetBalanceQNT(attachment.getAssetId(), -attachment.getQuantityQNT());
                    return true;
                }
            }
            return false;
        }

        @Override
        protected void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            AskOrderPlacementAttachment attachment = (AskOrderPlacementAttachment) transaction.getAttachment();
            Asset asset = Asset.getAsset(attachment.getAssetId());
            if (asset != null) {
                if (Asset.privateEnabled() && MofoAsset.isPrivateAsset(asset)) {
                    Account issuerAccount = Account.getAccount(asset.getAccountId());
                    if (issuerAccount != null) {
                        senderAccount.addToAssetBalanceQNT(attachment.getAssetId(), -attachment.getOrderFeeQNT());
                        issuerAccount.addToAssetAndUnconfirmedAssetBalanceQNT(attachment.getAssetId(), attachment.getOrderFeeQNT());

                        Order.Ask.addOrder(transaction, attachment);
                    }
                } else {
                    Order.Ask.addOrder(transaction, attachment);
                }
            }
        }

        @Override
        protected void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            AskOrderPlacementAttachment attachment = (AskOrderPlacementAttachment) transaction.getAttachment();
            if (Asset.privateEnabled() && MofoAsset.isPrivateAsset(attachment.getAssetId())) {
                senderAccount.addToUnconfirmedAssetBalanceQNT(attachment.getAssetId(), Math.addExact(attachment.getQuantityQNT(), attachment.getOrderFeeQNT()));
            } else {
                senderAccount.addToUnconfirmedAssetBalanceQNT(attachment.getAssetId(), attachment.getQuantityQNT());
            }
        }

        @Override
        void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
            AskOrderPlacementAttachment attachment = (AskOrderPlacementAttachment) transaction.getAttachment();
            if (Asset.privateEnabled() && MofoAsset.isPrivateAsset(attachment.getAssetId())) {
                if (!MofoAsset.getAccountAllowed(attachment.getAssetId(), transaction.getSenderId())) {
                    throw new NxtException.NotValidException("Account not allowed to place ask order");
                }
                long orderFeeQNT = MofoAsset.calculateOrderFee(attachment.getAssetId(), attachment.getQuantityQNT());
                if (orderFeeQNT != attachment.getOrderFeeQNT()) {
                    throw new NxtException.NotValidException("Incorrect \"orderFeeQNT\" should be " + String.valueOf(orderFeeQNT));
                }
            }
        }
    };

    public final static TransactionType BID_ORDER_PLACEMENT = new ColoredCoinsOrderPlacement() {

        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT;
        }

        @Override
        public String getName() {
            return "BidOrderPlacement";
        }

        @Override
        protected BidOrderPlacementAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) throws NxtException.NotValidException {
            return new BidOrderPlacementAttachment(buffer, transactionVersion, timestamp);
        }

        @Override
        protected BidOrderPlacementAttachment parseAttachment(JSONObject attachmentData, int timestamp) throws NxtException.NotValidException {
            return new BidOrderPlacementAttachment(attachmentData, timestamp);
        }

        @Override
        protected boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            BidOrderPlacementAttachment attachment = (BidOrderPlacementAttachment) transaction.getAttachment();
            if (senderAccount.getUnconfirmedBalanceNQT() >= Math.multiplyExact(attachment.getQuantityQNT(), attachment.getPriceNQT())) {
                if (Asset.privateEnabled() && MofoAsset.isPrivateAsset(attachment.getAssetId())) {
                    final long totalNQT = Math.multiplyExact(attachment.getQuantityQNT(), attachment.getPriceNQT());
                    long totalAndOrderFeeNQT = Math.addExact(totalNQT, attachment.getOrderFeeNQT());
                    if (senderAccount.getUnconfirmedBalanceNQT() > totalAndOrderFeeNQT && MofoAsset.calculateOrderFee(attachment.getAssetId(), totalNQT) == attachment.getOrderFeeNQT()) {
                        senderAccount.addToUnconfirmedBalanceNQT(-totalAndOrderFeeNQT);
                        return true;
                    }
                } else {
                    senderAccount.addToUnconfirmedBalanceNQT(-Math.multiplyExact(attachment.getQuantityQNT(), attachment.getPriceNQT()));
                    return true;
                }
            }
            return false;
        }

        @Override
        protected void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            BidOrderPlacementAttachment attachment = (BidOrderPlacementAttachment) transaction.getAttachment();
            Asset asset = Asset.getAsset(attachment.getAssetId());
            if (asset != null) {
                if (Asset.privateEnabled() && MofoAsset.isPrivateAsset(asset)) {
                    Account issuerAccount = Account.getAccount(asset.getAccountId());
                    if (issuerAccount != null) {
                        senderAccount.addToBalanceNQT(-attachment.getOrderFeeNQT());
                        issuerAccount.addToBalanceAndUnconfirmedBalanceNQT(attachment.getOrderFeeNQT());
                        Order.Bid.addOrder(transaction, attachment);
                    }
                } else {
                    Order.Bid.addOrder(transaction, attachment);
                }
            }
        }

        @Override
        protected void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            BidOrderPlacementAttachment attachment = (BidOrderPlacementAttachment) transaction.getAttachment();
            if (Asset.privateEnabled() && MofoAsset.isPrivateAsset(attachment.getAssetId())) {
                final long totalNQT = Math.multiplyExact(attachment.getQuantityQNT(), attachment.getPriceNQT());
                senderAccount.addToUnconfirmedBalanceNQT(Math.addExact(totalNQT, attachment.getOrderFeeNQT()));
            } else {
                senderAccount.addToUnconfirmedBalanceNQT(Math.multiplyExact(attachment.getQuantityQNT(), attachment.getPriceNQT()));
            }
        }

        @Override
        void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
            BidOrderPlacementAttachment attachment = (BidOrderPlacementAttachment) transaction.getAttachment();
            if (Asset.privateEnabled() && MofoAsset.isPrivateAsset(attachment.getAssetId())) {
                if (!MofoAsset.getAccountAllowed(attachment.getAssetId(), transaction.getSenderId())) {
                    throw new NxtException.NotValidException("Account not allowed to place bid order");
                }
                final long totalNQT = Math.multiplyExact(attachment.getQuantityQNT(), attachment.getPriceNQT());
                long orderFeeNQT = MofoAsset.calculateOrderFee(attachment.getAssetId(), totalNQT);
                if (orderFeeNQT != attachment.getOrderFeeNQT()) {
                    throw new NxtException.NotValidException("Incorrect \"orderFeeNQT\" should be " + String.valueOf(orderFeeNQT));
                }
            }
        }
    };

    abstract static class ColoredCoinsOrderCancellation extends ColoredCoinsTxnTypes {

        @Override
        protected final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        protected final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        public final boolean canHaveRecipient() {
            return false;
        }

        @Override
        public final boolean isPhasingSafe() {
            return true;
        }

    }

    public static final TransactionType ASK_ORDER_CANCELLATION = new ColoredCoinsOrderCancellation() {

        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION;
        }

        @Override
        public String getName() {
            return "AskOrderCancellation";
        }

        @Override
        protected AskOrderCancellationAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) throws NxtException.NotValidException {
            return new AskOrderCancellationAttachment(buffer, transactionVersion);
        }

        @Override
        protected AskOrderCancellationAttachment parseAttachment(JSONObject attachmentData, int timestamp) throws NxtException.NotValidException {
            return new AskOrderCancellationAttachment(attachmentData);
        }

        @Override
        protected void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            AskOrderCancellationAttachment attachment = (AskOrderCancellationAttachment) transaction.getAttachment();
            Order order = Order.Ask.getAskOrder(attachment.getOrderId());
            Order.Ask.removeOrder(attachment.getOrderId());
            if (order != null) {
                senderAccount.addToUnconfirmedAssetBalanceQNT(order.getAssetId(), order.getQuantityQNT());
            }
        }

        @Override
        protected void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            AskOrderCancellationAttachment attachment = (AskOrderCancellationAttachment) transaction.getAttachment();
            Order ask = Order.Ask.getAskOrder(attachment.getOrderId());
            if (ask == null) {
                throw new NxtException.NotCurrentlyValidException("Invalid ask order: " + Long.toUnsignedString(attachment.getOrderId()));
            }
            if (ask.getAccountId() != transaction.getSenderId()) {
                throw new NxtException.NotValidException("Order " + Long.toUnsignedString(attachment.getOrderId()) + " was created by account "
                        + Long.toUnsignedString(ask.getAccountId()));
            }
        }

    };

    public static final TransactionType BID_ORDER_CANCELLATION = new ColoredCoinsOrderCancellation() {

        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION;
        }

        @Override
        public String getName() {
            return "BidOrderCancellation";
        }

        @Override
        protected BidOrderCancellationAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) throws NxtException.NotValidException {
            return new BidOrderCancellationAttachment(buffer, transactionVersion);
        }

        @Override
        protected BidOrderCancellationAttachment parseAttachment(JSONObject attachmentData, int timestamp) throws NxtException.NotValidException {
            return new BidOrderCancellationAttachment(attachmentData);
        }

        @Override
        protected void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            BidOrderCancellationAttachment attachment = (BidOrderCancellationAttachment) transaction.getAttachment();
            Order order = Order.Bid.getBidOrder(attachment.getOrderId());
            Order.Bid.removeOrder(attachment.getOrderId());
            if (order != null) {
                senderAccount.addToUnconfirmedBalanceNQT(Math.multiplyExact(order.getQuantityQNT(), order.getPriceNQT()));
            }
        }

        @Override
        protected void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            BidOrderCancellationAttachment attachment = (BidOrderCancellationAttachment) transaction.getAttachment();
            Order bid = Order.Bid.getBidOrder(attachment.getOrderId());
            if (bid == null) {
                throw new NxtException.NotCurrentlyValidException("Invalid bid order: " + Long.toUnsignedString(attachment.getOrderId()));
            }
            if (bid.getAccountId() != transaction.getSenderId()) {
                throw new NxtException.NotValidException("Order " + Long.toUnsignedString(attachment.getOrderId()) + " was created by account "
                        + Long.toUnsignedString(bid.getAccountId()));
            }
        }

    };

    public static final TransactionType DIVIDEND_PAYMENT = new ColoredCoinsTxnTypes() {

        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_COLORED_COINS_DIVIDEND_PAYMENT;
        }

        @Override
        public String getName() {
            return "DividendPayment";
        }

        @Override
        protected DividendPaymentAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) {
            return new DividendPaymentAttachment(buffer, transactionVersion);
        }

        @Override
        protected DividendPaymentAttachment parseAttachment(JSONObject attachmentData, int timestamp) {
            return new DividendPaymentAttachment(attachmentData);
        }

        @Override
        protected boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            DividendPaymentAttachment attachment = (DividendPaymentAttachment) transaction.getAttachment();
            long quantityQNT = Asset.getAsset(attachment.getAssetId()).getQuantityQNT()
                    - senderAccount.getAssetBalanceQNT(attachment.getAssetId(), attachment.getHeight())
                    - Account.getAssetBalanceQNT(Genesis.CREATOR_ID, attachment.getAssetId(), attachment.getHeight());
            long totalDividendPayment = Math.multiplyExact(attachment.getAmountNQTPerQNT(), quantityQNT);
            if (senderAccount.getUnconfirmedBalanceNQT() >= totalDividendPayment) {
                senderAccount.addToUnconfirmedBalanceNQT(-totalDividendPayment);
                return true;
            }
            return false;
        }

        @Override
        protected void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            DividendPaymentAttachment attachment = (DividendPaymentAttachment) transaction.getAttachment();
            senderAccount.payDividends(attachment.getAssetId(), attachment.getHeight(), attachment.getAmountNQTPerQNT());
        }

        @Override
        protected void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            DividendPaymentAttachment attachment = (DividendPaymentAttachment) transaction.getAttachment();
            long quantityQNT = Asset.getAsset(attachment.getAssetId()).getQuantityQNT()
                    - senderAccount.getAssetBalanceQNT(attachment.getAssetId(), attachment.getHeight())
                    - Account.getAssetBalanceQNT(Genesis.CREATOR_ID, attachment.getAssetId(), attachment.getHeight());
            long totalDividendPayment = Math.multiplyExact(attachment.getAmountNQTPerQNT(), quantityQNT);
            senderAccount.addToUnconfirmedBalanceNQT(totalDividendPayment);
        }

        @Override
        protected void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            if (Nxt.getBlockchain().getHeight() < Constants.MONETARY_SYSTEM_BLOCK) {
                throw new NxtException.NotYetEnabledException("Dividend payment not yet enabled at height " + Nxt.getBlockchain().getHeight());
            }
            DividendPaymentAttachment attachment = (DividendPaymentAttachment) transaction.getAttachment();
            Asset asset = Asset.getAsset(attachment.getAssetId());
            if (asset == null) {
                throw new NxtException.NotCurrentlyValidException("Asset " + Long.toUnsignedString(attachment.getAssetId())
                        + "for dividend payment doesn't exist yet");
            }
            if (asset.getAccountId() != transaction.getSenderId() || attachment.getAmountNQTPerQNT() <= 0) {
                throw new NxtException.NotValidException("Invalid dividend payment sender or amount " + attachment.getJSONObject());
            }
            if (attachment.getHeight() > Nxt.getBlockchain().getHeight()
                    || attachment.getHeight() <= this.getFinishValidationHeight(transaction) - Constants.MAX_DIVIDEND_PAYMENT_ROLLBACK) {
                throw new NxtException.NotCurrentlyValidException("Invalid dividend payment height: " + attachment.getHeight());
            }
        }

        @Override
        public boolean canHaveRecipient() {
            return false;
        }

        @Override
        public boolean isPhasingSafe() {
            return true;
        }

    };

    public static final TransactionType ASSET_REWARDING = new AssetRewardingTxnType();

}
