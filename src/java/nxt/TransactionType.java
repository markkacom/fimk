/******************************************************************************
 * Copyright © 2013-2015 The Nxt Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * Nxt software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package nxt;

import nxt.txn.AccountControlTxnType;
import nxt.txn.ColoredCoinsTxnTypes;
import nxt.txn.extension.TransactionTypeExtension;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class TransactionType {

    static final byte TYPE_PAYMENT = 0;
    static final byte TYPE_MESSAGING = 1;
    protected static final byte TYPE_COLORED_COINS = 2;
    static final byte TYPE_DIGITAL_GOODS = 3;
    public static final byte TYPE_ACCOUNT_CONTROL = 4;
    static final byte TYPE_MONETARY_SYSTEM = 5;
    static final byte TYPE_DATA = 6;

    static final byte SUBTYPE_PAYMENT_ORDINARY_PAYMENT = 0;

    static final byte SUBTYPE_MESSAGING_ARBITRARY_MESSAGE = 0;
    static final byte SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT = 1;
    static final byte SUBTYPE_MESSAGING_POLL_CREATION = 2;
    static final byte SUBTYPE_MESSAGING_VOTE_CASTING = 3;
    static final byte SUBTYPE_MESSAGING_HUB_ANNOUNCEMENT = 4;
    static final byte SUBTYPE_MESSAGING_ACCOUNT_INFO = 5;
    static final byte SUBTYPE_MESSAGING_ALIAS_SELL = 6;
    static final byte SUBTYPE_MESSAGING_ALIAS_BUY = 7;
    static final byte SUBTYPE_MESSAGING_ALIAS_DELETE = 8;
    static final byte SUBTYPE_MESSAGING_PHASING_VOTE_CASTING = 9;

    protected static final byte SUBTYPE_COLORED_COINS_ASSET_ISSUANCE = 0;
    protected static final byte SUBTYPE_COLORED_COINS_ASSET_TRANSFER = 1;
    protected static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT = 2;
    protected static final byte SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT = 3;
    protected static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION = 4;
    protected static final byte SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION = 5;
    protected static final byte SUBTYPE_COLORED_COINS_DIVIDEND_PAYMENT = 6;
    protected static final byte SUBTYPE_COLORED_COINS_ASSET_REWARDING = 7;  // assign rules to asset for rewarding

    static final byte SUBTYPE_DIGITAL_GOODS_LISTING = 0;
    static final byte SUBTYPE_DIGITAL_GOODS_DELISTING = 1;
    static final byte SUBTYPE_DIGITAL_GOODS_PRICE_CHANGE = 2;
    static final byte SUBTYPE_DIGITAL_GOODS_QUANTITY_CHANGE = 3;
    static final byte SUBTYPE_DIGITAL_GOODS_PURCHASE = 4;
    static final byte SUBTYPE_DIGITAL_GOODS_DELIVERY = 5;
    static final byte SUBTYPE_DIGITAL_GOODS_FEEDBACK = 6;
    static final byte SUBTYPE_DIGITAL_GOODS_REFUND = 7;

    public static final byte SUBTYPE_ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING = 0;
    static final byte SUBTYPE_ACCOUNT_CONTROL_POP_REWARD_CHALLENGE = 1;
    public static final byte SUBTYPE_REWARD_APPLICANT_REGISTRATION = 2;

    static final byte SUBTYPE_DATA_TAGGED_DATA_UPLOAD = 0;
    static final byte SUBTYPE_DATA_TAGGED_DATA_EXTEND = 1;

    public static TransactionType findTransactionType(byte type, byte subtype) {
        switch (type) {
            case TYPE_PAYMENT:
                switch (subtype) {
                    case SUBTYPE_PAYMENT_ORDINARY_PAYMENT:
                        return Payment.ORDINARY;
                    default:
                        return null;
                }
            case TYPE_MESSAGING:
                switch (subtype) {
                    case SUBTYPE_MESSAGING_ARBITRARY_MESSAGE:
                        return Messaging.ARBITRARY_MESSAGE;
                    case SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT:
                        return Messaging.ALIAS_ASSIGNMENT;
                    case SUBTYPE_MESSAGING_POLL_CREATION:
                        return Messaging.POLL_CREATION;
                    case SUBTYPE_MESSAGING_VOTE_CASTING:
                        return Messaging.VOTE_CASTING;
                    case SUBTYPE_MESSAGING_HUB_ANNOUNCEMENT:
                        return Messaging.HUB_ANNOUNCEMENT;
                    case SUBTYPE_MESSAGING_ACCOUNT_INFO:
                        return Messaging.ACCOUNT_INFO;
                    case SUBTYPE_MESSAGING_ALIAS_SELL:
                        return Messaging.ALIAS_SELL;
                    case SUBTYPE_MESSAGING_ALIAS_BUY:
                        return Messaging.ALIAS_BUY;
                    case SUBTYPE_MESSAGING_ALIAS_DELETE:
                        return Messaging.ALIAS_DELETE;
                    case SUBTYPE_MESSAGING_PHASING_VOTE_CASTING:
                        return Messaging.PHASING_VOTE_CASTING;
                    default:
                        return null;
                }
            case TYPE_COLORED_COINS:
                switch (subtype) {
                    case SUBTYPE_COLORED_COINS_ASSET_ISSUANCE:
                        return ColoredCoinsTxnTypes.ASSET_ISSUANCE;
                    case SUBTYPE_COLORED_COINS_ASSET_TRANSFER:
                        return ColoredCoinsTxnTypes.ASSET_TRANSFER;
                    case SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT:
                        return ColoredCoinsTxnTypes.ASK_ORDER_PLACEMENT;
                    case SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT:
                        return ColoredCoinsTxnTypes.BID_ORDER_PLACEMENT;
                    case SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION:
                        return ColoredCoinsTxnTypes.ASK_ORDER_CANCELLATION;
                    case SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION:
                        return ColoredCoinsTxnTypes.BID_ORDER_CANCELLATION;
                    case SUBTYPE_COLORED_COINS_DIVIDEND_PAYMENT:
                        return ColoredCoinsTxnTypes.DIVIDEND_PAYMENT;
                    case SUBTYPE_COLORED_COINS_ASSET_REWARDING:
                        return ColoredCoinsTxnTypes.ASSET_REWARDING;
                    default:
                        return null;
                }
            case TYPE_DIGITAL_GOODS:
                switch (subtype) {
                    case SUBTYPE_DIGITAL_GOODS_LISTING:
                        return DigitalGoods.LISTING;
                    case SUBTYPE_DIGITAL_GOODS_DELISTING:
                        return DigitalGoods.DELISTING;
                    case SUBTYPE_DIGITAL_GOODS_PRICE_CHANGE:
                        return DigitalGoods.PRICE_CHANGE;
                    case SUBTYPE_DIGITAL_GOODS_QUANTITY_CHANGE:
                        return DigitalGoods.QUANTITY_CHANGE;
                    case SUBTYPE_DIGITAL_GOODS_PURCHASE:
                        return DigitalGoods.PURCHASE;
                    case SUBTYPE_DIGITAL_GOODS_DELIVERY:
                        return DigitalGoods.DELIVERY;
                    case SUBTYPE_DIGITAL_GOODS_FEEDBACK:
                        return DigitalGoods.FEEDBACK;
                    case SUBTYPE_DIGITAL_GOODS_REFUND:
                        return DigitalGoods.REFUND;
                    default:
                        return null;
                }
            case TYPE_ACCOUNT_CONTROL:
                switch (subtype) {
                    case SUBTYPE_ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING:
                        return AccountControlTxnType.EFFECTIVE_BALANCE_LEASING;
                    case SUBTYPE_ACCOUNT_CONTROL_POP_REWARD_CHALLENGE:
                        return AccountControlTxnType.POP_REWARD_CHALLENGE;
                    case SUBTYPE_REWARD_APPLICANT_REGISTRATION:
                        return AccountControlTxnType.REWARD_APPLICANT_REGISTRATION;
                    default:
                        return null;
                }
            case TYPE_MONETARY_SYSTEM:
                return MonetarySystem.findTransactionType(subtype);

            case MofoTransactions.TYPE_FIMKRYPTO:
                return MofoTransactions.findTransactionType(subtype);

            case (TYPE_DATA):
                switch (subtype) {
                    case SUBTYPE_DATA_TAGGED_DATA_UPLOAD:
                        return Data.TAGGED_DATA_UPLOAD;
                    case SUBTYPE_DATA_TAGGED_DATA_EXTEND:
                        return Data.TAGGED_DATA_EXTEND;
                    default:
                        return null;
                }
            default:
                return null;
        }
    }


    protected TransactionType() {}

    public abstract byte getType();

    public abstract byte getSubtype();

    protected abstract Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) throws NxtException.NotValidException;

    protected abstract Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData, int timestamp) throws NxtException.NotValidException;

    protected abstract void validateAttachment(Transaction transaction) throws NxtException.ValidationException;

    void validateAttachmentAtFinish(Transaction transaction) throws NxtException.ValidationException {
        validateAttachment(transaction);
    }

    // return false iff double spending
    final boolean applyUnconfirmed(TransactionImpl transaction, Account senderAccount) {
        long totalAmountNQT = Math.addExact(transaction.getAmountNQT(), transaction.getFeeNQT());
        if (transaction.referencedTransactionFullHash() != null
                && transaction.getTimestamp() > Constants.REFERENCED_TRANSACTION_FULL_HASH_BLOCK_TIMESTAMP) {
            totalAmountNQT = Math.addExact(totalAmountNQT, Constants.UNCONFIRMED_POOL_DEPOSIT_NQT);
        }
        if (senderAccount.getUnconfirmedBalanceNQT() < totalAmountNQT
                && !(transaction.getTimestamp() == 0 && Arrays.equals(senderAccount.getPublicKey(), Genesis.CREATOR_PUBLIC_KEY))) {
            return false;
        }
        senderAccount.addToUnconfirmedBalanceNQT(-totalAmountNQT);
        if (!applyAttachmentUnconfirmed(transaction, senderAccount)) {
            senderAccount.addToUnconfirmedBalanceNQT(totalAmountNQT);
            return false;
        }
        return true;
    }

    protected abstract boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount);

    final void apply(TransactionImpl transaction, Account senderAccount, Account recipientAccount) {
        long amount = transaction.getAmountNQT();
        if (transaction.getPhasing() == null || !isPhasable()) {
            senderAccount.addToBalanceNQT(-Math.addExact(amount, transaction.getFeeNQT()));
        } else {
            senderAccount.addToBalanceNQT(-amount);
        }
        if (recipientAccount != null) {
            recipientAccount.addToBalanceAndUnconfirmedBalanceNQT(amount);
        }
        applyAttachment(transaction, senderAccount, recipientAccount);

        //todo rollback of extension on rollback transaction. Not critical because extension have to not affect the consensus
        applyExtension(transaction, senderAccount, recipientAccount);
    }

    private void applyExtension(TransactionImpl transaction, Account senderAccount, Account recipientAccount) {
        if (Nxt.getBlockchain().getHeight() < Constants.TRANSACTION_EXTENSION_HEIGHT) return;
        try {
            TransactionTypeExtension ext = TransactionTypeExtension.get(transaction);
            if (ext != null) {
                String result = ext.process(false, transaction, senderAccount, recipientAccount);
                if (result != null) {
                    Logger.logWarningMessage(String.format("Transaction extension \"%s\" is not applied. %s", ext.getName(), result));
                }
            }
        } catch (Exception e) {
            Logger.logErrorMessage("Transaction extension applying error", e);
        }
    }

    protected abstract void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount);

    final void undoUnconfirmed(TransactionImpl transaction, Account senderAccount) {
        undoAttachmentUnconfirmed(transaction, senderAccount);
        senderAccount.addToUnconfirmedBalanceNQT(Math.addExact(transaction.getAmountNQT(), transaction.getFeeNQT()));
        if (transaction.referencedTransactionFullHash() != null
                && transaction.getTimestamp() > Constants.REFERENCED_TRANSACTION_FULL_HASH_BLOCK_TIMESTAMP) {
            senderAccount.addToUnconfirmedBalanceNQT(Constants.UNCONFIRMED_POOL_DEPOSIT_NQT);
        }
    }

    protected abstract void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount);

    boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String,Boolean>> duplicates) {
        return false;
    }

    boolean isUnconfirmedDuplicate(Transaction transaction, Map<TransactionType, Map<String,Boolean>> duplicates) {
        return false;
    }

    static boolean isDuplicate(TransactionType uniqueType, String key, Map<TransactionType, Map<String, Boolean>> duplicates, boolean exclusive) {
        Map<String,Boolean> typeDuplicates = duplicates.get(uniqueType);
        if (typeDuplicates == null) {
            typeDuplicates = new HashMap<>();
            duplicates.put(uniqueType, typeDuplicates);
        }
        Boolean hasExclusive = typeDuplicates.get(key);
        if (hasExclusive == null) {
            typeDuplicates.put(key, exclusive);
            return false;
        }
        return hasExclusive || exclusive;
    }

    protected final int getFinishValidationHeight(Transaction transaction) {
        return transaction.getPhasing() == null ? Nxt.getBlockchain().getHeight() : (transaction.getPhasing().getFinishHeight() - 1);
    }

    public abstract boolean canHaveRecipient();

    public boolean mustHaveRecipient() {
        return canHaveRecipient();
    }

    public abstract boolean isPhasingSafe();

    public boolean isPhasable() {
        return true;
    }

    public Fee getBaselineFee(Transaction transaction) {
        return Fee.DEFAULT_FEE;
    }

    public Fee getNextFee(Transaction transaction) {
        return getBaselineFee(transaction);
    }

    public abstract String getName();

    @Override
    public final String toString() {
        return getName() + " type: " + getType() + ", subtype: " + getSubtype();
    }

    public static abstract class Payment extends TransactionType {

        private Payment() {
        }

        @Override
        public final byte getType() {
            return TransactionType.TYPE_PAYMENT;
        }

        @Override
        protected final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        protected final void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        }

        @Override
        protected final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        public final boolean canHaveRecipient() {
            return true;
        }

        @Override
        public final boolean isPhasingSafe() {
            return true;
        }

        public static final TransactionType ORDINARY = new Payment() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_PAYMENT_ORDINARY_PAYMENT;
            }

            @Override
            public String getName() {
                return "OrdinaryPayment";
            }

            @Override
            protected Attachment.EmptyAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) throws NxtException.NotValidException {
                return Attachment.ORDINARY_PAYMENT;
            }

            @Override
            protected Attachment.EmptyAttachment parseAttachment(JSONObject attachmentData, int timestamp) throws NxtException.NotValidException {
                return Attachment.ORDINARY_PAYMENT;
            }

            @Override
            protected void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (transaction.getAmountNQT() <= 0 || transaction.getAmountNQT() >= Constants.MAX_BALANCE_NQT) {
                    throw new NxtException.NotValidException("Invalid ordinary payment");
                }
            }

        };

    }

    public static abstract class Messaging extends TransactionType {

        private Messaging() {
        }

        @Override
        public final byte getType() {
            return TransactionType.TYPE_MESSAGING;
        }

        @Override
        protected final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        protected final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        public final static TransactionType ARBITRARY_MESSAGE = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ARBITRARY_MESSAGE;
            }

            @Override
            public String getName() {
                return "ArbitraryMessage";
            }

            @Override
            protected Attachment.EmptyAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) throws NxtException.NotValidException {
                return Attachment.ARBITRARY_MESSAGE;
            }

            @Override
            protected Attachment.EmptyAttachment parseAttachment(JSONObject attachmentData, int timestamp) throws NxtException.NotValidException {
                return Attachment.ARBITRARY_MESSAGE;
            }

            @Override
            protected void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            }

            @Override
            protected void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment attachment = transaction.getAttachment();
                if (transaction.getAmountNQT() != 0) {
                    throw new NxtException.NotValidException("Invalid arbitrary message: " + attachment.getJSONObject());
                }
                if (transaction.getRecipientId() == Genesis.CREATOR_ID && Nxt.getBlockchain().getHeight() > Constants.MONETARY_SYSTEM_BLOCK) {
                    throw new NxtException.NotCurrentlyValidException("Sending messages to Genesis not allowed.");
                }
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean mustHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public static final TransactionType ALIAS_ASSIGNMENT = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT;
            }

            @Override
            public String getName() {
                return "AliasAssignment";
            }

            @Override
            protected Attachment.MessagingAliasAssignment parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) throws NxtException.NotValidException {
                return new Attachment.MessagingAliasAssignment(buffer, transactionVersion);
            }

            @Override
            protected Attachment.MessagingAliasAssignment parseAttachment(JSONObject attachmentData, int timestamp) throws NxtException.NotValidException {
                return new Attachment.MessagingAliasAssignment(attachmentData);
            }

            @Override
            protected void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment) transaction.getAttachment();
                Alias.addOrUpdateAlias(transaction, attachment);
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String,Boolean>> duplicates) {
                Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment) transaction.getAttachment();
                return isDuplicate(Messaging.ALIAS_ASSIGNMENT, attachment.getAliasName().toLowerCase(), duplicates, true);
            }

            @Override
            protected void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment) transaction.getAttachment();
                if (attachment.getAliasName().length() == 0
                        || attachment.getAliasName().length() > Constants.MAX_ALIAS_LENGTH
                        || attachment.getAliasURI().length() > Constants.MAX_ALIAS_URI_LENGTH) {
                    throw new NxtException.NotValidException("Invalid alias assignment: " + attachment.getJSONObject());
                }
                String normalizedAlias = attachment.getAliasName().toLowerCase();
                for (int i = 0; i < normalizedAlias.length(); i++) {
                    if (Constants.ALPHABET.indexOf(normalizedAlias.charAt(i)) < 0) {
                        throw new NxtException.NotValidException("Invalid alias name: " + normalizedAlias);
                    }
                }
                Alias alias = Alias.getAlias(normalizedAlias);
                if (alias != null && alias.getAccountId() != transaction.getSenderId()) {
                    throw new NxtException.NotCurrentlyValidException("Alias already owned by another account: " + normalizedAlias);
                }
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public static final TransactionType ALIAS_SELL = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ALIAS_SELL;
            }

            @Override
            public String getName() {
                return "AliasSell";
            }

            @Override
            protected Attachment.MessagingAliasSell parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) throws NxtException.NotValidException {
                return new Attachment.MessagingAliasSell(buffer, transactionVersion);
            }

            @Override
            protected Attachment.MessagingAliasSell parseAttachment(JSONObject attachmentData, int timestamp) throws NxtException.NotValidException {
                return new Attachment.MessagingAliasSell(attachmentData);
            }

            @Override
            protected void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                final Attachment.MessagingAliasSell attachment =
                        (Attachment.MessagingAliasSell) transaction.getAttachment();
                Alias.sellAlias(transaction, attachment);
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String,Boolean>> duplicates) {
                Attachment.MessagingAliasSell attachment = (Attachment.MessagingAliasSell) transaction.getAttachment();
                // not a bug, uniqueness is based on Messaging.ALIAS_ASSIGNMENT
                return isDuplicate(Messaging.ALIAS_ASSIGNMENT, attachment.getAliasName().toLowerCase(), duplicates, true);
            }

            @Override
            protected void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (transaction.getAmountNQT() != 0) {
                    throw new NxtException.NotValidException("Invalid sell alias transaction: " +
                            transaction.getJSONObject());
                }
                final Attachment.MessagingAliasSell attachment =
                        (Attachment.MessagingAliasSell) transaction.getAttachment();
                final String aliasName = attachment.getAliasName();
                if (aliasName == null || aliasName.length() == 0) {
                    throw new NxtException.NotValidException("Missing alias name");
                }
                long priceNQT = attachment.getPriceNQT();
                if (priceNQT < 0 || priceNQT > Constants.MAX_BALANCE_NQT) {
                    throw new NxtException.NotValidException("Invalid alias sell price: " + priceNQT);
                }
                if (priceNQT == 0) {
                    if (Genesis.CREATOR_ID == transaction.getRecipientId()) {
                        throw new NxtException.NotValidException("Transferring aliases to Genesis account not allowed");
                    } else if (transaction.getRecipientId() == 0) {
                        throw new NxtException.NotValidException("Missing alias transfer recipient");
                    }
                }
                final Alias alias = Alias.getAlias(aliasName);
                if (alias == null) {
                    throw new NxtException.NotCurrentlyValidException("No such alias: " + aliasName);
                } else if (alias.getAccountId() != transaction.getSenderId()) {
                    throw new NxtException.NotCurrentlyValidException("Alias doesn't belong to sender: " + aliasName);
                }
                if (transaction.getRecipientId() == Genesis.CREATOR_ID) {
                    throw new NxtException.NotCurrentlyValidException("Selling alias to Genesis not allowed");
                }
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean mustHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public static final TransactionType ALIAS_BUY = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ALIAS_BUY;
            }

            @Override
            public String getName() {
                return "AliasBuy";
            }

            @Override
            protected Attachment.MessagingAliasBuy parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) throws NxtException.NotValidException {
                return new Attachment.MessagingAliasBuy(buffer, transactionVersion);
            }

            @Override
            protected Attachment.MessagingAliasBuy parseAttachment(JSONObject attachmentData, int timestamp) throws NxtException.NotValidException {
                return new Attachment.MessagingAliasBuy(attachmentData);
            }

            @Override
            protected void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                final Attachment.MessagingAliasBuy attachment =
                        (Attachment.MessagingAliasBuy) transaction.getAttachment();
                final String aliasName = attachment.getAliasName();
                Alias.changeOwner(transaction.getSenderId(), aliasName);
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String,Boolean>> duplicates) {
                Attachment.MessagingAliasBuy attachment = (Attachment.MessagingAliasBuy) transaction.getAttachment();
                // not a bug, uniqueness is based on Messaging.ALIAS_ASSIGNMENT
                return isDuplicate(Messaging.ALIAS_ASSIGNMENT, attachment.getAliasName().toLowerCase(), duplicates, true);
            }

            @Override
            protected void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                final Attachment.MessagingAliasBuy attachment =
                        (Attachment.MessagingAliasBuy) transaction.getAttachment();
                final String aliasName = attachment.getAliasName();
                final Alias alias = Alias.getAlias(aliasName);
                if (alias == null) {
                    throw new NxtException.NotCurrentlyValidException("No such alias: " + aliasName);
                } else if (alias.getAccountId() != transaction.getRecipientId()) {
                    throw new NxtException.NotCurrentlyValidException("Alias is owned by account other than recipient: "
                            + Long.toUnsignedString(alias.getAccountId()));
                }
                Alias.Offer offer = Alias.getOffer(alias);
                if (offer == null) {
                    throw new NxtException.NotCurrentlyValidException("Alias is not for sale: " + aliasName);
                }
                if (transaction.getAmountNQT() < offer.getPriceNQT()) {
                    String msg = "Price is too low for: " + aliasName + " ("
                            + transaction.getAmountNQT() + " < " + offer.getPriceNQT() + ")";
                    throw new NxtException.NotCurrentlyValidException(msg);
                }
                if (offer.getBuyerId() != 0 && offer.getBuyerId() != transaction.getSenderId()) {
                    throw new NxtException.NotCurrentlyValidException("Wrong buyer for " + aliasName + ": "
                            + Long.toUnsignedString(transaction.getSenderId()) + " expected: "
                            + Long.toUnsignedString(offer.getBuyerId()));
                }
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public static final TransactionType ALIAS_DELETE = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ALIAS_DELETE;
            }

            @Override
            public String getName() {
                return "AliasDelete";
            }

            @Override
            protected Attachment.MessagingAliasDelete parseAttachment(final ByteBuffer buffer, final byte transactionVersion, int timestamp) throws NxtException.NotValidException {
                return new Attachment.MessagingAliasDelete(buffer, transactionVersion);
            }

            @Override
            protected Attachment.MessagingAliasDelete parseAttachment(final JSONObject attachmentData, int timestamp) throws NxtException.NotValidException {
                return new Attachment.MessagingAliasDelete(attachmentData);
            }

            @Override
            protected void applyAttachment(final Transaction transaction, final Account senderAccount, final Account recipientAccount) {
                final Attachment.MessagingAliasDelete attachment =
                        (Attachment.MessagingAliasDelete) transaction.getAttachment();
                Alias.deleteAlias(attachment.getAliasName());
            }

            @Override
            boolean isDuplicate(final Transaction transaction, final Map<TransactionType, Map<String, Boolean>> duplicates) {
                Attachment.MessagingAliasDelete attachment = (Attachment.MessagingAliasDelete) transaction.getAttachment();
                // not a bug, uniqueness is based on Messaging.ALIAS_ASSIGNMENT
                return isDuplicate(Messaging.ALIAS_ASSIGNMENT, attachment.getAliasName().toLowerCase(), duplicates, true);
            }

            @Override
            protected void validateAttachment(final Transaction transaction) throws NxtException.ValidationException {
                if (Nxt.getBlockchain().getHeight() < Constants.MONETARY_SYSTEM_BLOCK) {
                    throw new NxtException.NotYetEnabledException("Alias delete operation not yet enabled at height " + Nxt.getBlockchain().getHeight());
                }
                final Attachment.MessagingAliasDelete attachment =
                        (Attachment.MessagingAliasDelete) transaction.getAttachment();
                final String aliasName = attachment.getAliasName();
                if (aliasName == null || aliasName.length() == 0) {
                    throw new NxtException.NotValidException("Missing alias name");
                }
                final Alias alias = Alias.getAlias(aliasName);
                if (alias == null) {
                    throw new NxtException.NotCurrentlyValidException("No such alias: " + aliasName);
                } else if (alias.getAccountId() != transaction.getSenderId()) {
                    throw new NxtException.NotCurrentlyValidException("Alias doesn't belong to sender: " + aliasName);
                }
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public final static TransactionType POLL_CREATION = new Messaging() {

            private final Fee POLL_FEE = (transaction, appendage) -> {
                int numOptions = ((Attachment.MessagingPollCreation)appendage).getPollOptions().length;
                return numOptions <= 20 ? 10 * Constants.ONE_NXT : (10 + numOptions - 20) * Constants.ONE_NXT;
            };

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_POLL_CREATION;
            }

            @Override
            public String getName() {
                return "PollCreation";
            }

            @Override
            public Fee getBaselineFee(Transaction transaction) {
                return POLL_FEE;
            }

            @Override
            protected Attachment.MessagingPollCreation parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) throws NxtException.NotValidException {
                return new Attachment.MessagingPollCreation(buffer, transactionVersion);
            }

            @Override
            protected Attachment.MessagingPollCreation parseAttachment(JSONObject attachmentData, int timestamp) throws NxtException.NotValidException {
                return new Attachment.MessagingPollCreation(attachmentData);
            }

            @Override
            protected void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingPollCreation attachment = (Attachment.MessagingPollCreation) transaction.getAttachment();
                Poll.addPoll(transaction, attachment);
            }

            @Override
            protected void validateAttachment(Transaction transaction) throws NxtException.ValidationException {

                if (Nxt.getBlockchain().getHeight() < Constants.VOTING_SYSTEM_BLOCK) {
                    throw new NxtException.NotYetEnabledException("Voting System not yet enabled at height " + Nxt.getBlockchain().getHeight());
                }

                Attachment.MessagingPollCreation attachment = (Attachment.MessagingPollCreation) transaction.getAttachment();

                int optionsCount = attachment.getPollOptions().length;

                if (attachment.getPollName().length() > Constants.MAX_POLL_NAME_LENGTH
                        || attachment.getPollName().isEmpty()
                        || attachment.getPollDescription().length() > Constants.MAX_POLL_DESCRIPTION_LENGTH
                        || optionsCount > Constants.MAX_POLL_OPTION_COUNT
                        || optionsCount == 0) {
                    throw new NxtException.NotValidException("Invalid poll attachment: " + attachment.getJSONObject());
                }

                if (attachment.getMinNumberOfOptions() < 1
                        || attachment.getMinNumberOfOptions() > optionsCount) {
                    throw new NxtException.NotValidException("Invalid min number of options: " + attachment.getJSONObject());
                }

                if (attachment.getMaxNumberOfOptions() < 1
                        || attachment.getMaxNumberOfOptions() < attachment.getMinNumberOfOptions()
                        || attachment.getMaxNumberOfOptions() > optionsCount) {
                    throw new NxtException.NotValidException("Invalid max number of options: " + attachment.getJSONObject());
                }

                for (int i = 0; i < optionsCount; i++) {
                    if (attachment.getPollOptions()[i].length() > Constants.MAX_POLL_OPTION_LENGTH
                            || attachment.getPollOptions()[i].isEmpty()) {
                        throw new NxtException.NotValidException("Invalid poll options length: " + attachment.getJSONObject());
                    }
                }

                if (attachment.getMinRangeValue() < Constants.MIN_VOTE_VALUE
                        || attachment.getMaxRangeValue() > Constants.MAX_VOTE_VALUE){
                    throw new NxtException.NotValidException("Invalid range: " + attachment.getJSONObject());
                }

                if (attachment.getFinishHeight() <= getFinishValidationHeight(transaction) + 1
                    || attachment.getFinishHeight() >= getFinishValidationHeight(transaction) + Constants.MAX_POLL_DURATION) {
                    throw new NxtException.NotCurrentlyValidException("Invalid finishing height" + attachment.getJSONObject());
                }

                if (! attachment.getVoteWeighting().acceptsVotes() || attachment.getVoteWeighting().getVotingModel() == VoteWeighting.VotingModel.HASH) {
                    throw new NxtException.NotValidException("VotingModel " + attachment.getVoteWeighting().getVotingModel() + " not valid for regular polls");
                }

                attachment.getVoteWeighting().validate();

            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public final static TransactionType VOTE_CASTING = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_VOTE_CASTING;
            }

            @Override
            public String getName() {
                return "VoteCasting";
            }

            @Override
            protected Attachment.MessagingVoteCasting parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) throws NxtException.NotValidException {
                return new Attachment.MessagingVoteCasting(buffer, transactionVersion);
            }

            @Override
            protected Attachment.MessagingVoteCasting parseAttachment(JSONObject attachmentData, int timestamp) throws NxtException.NotValidException {
                return new Attachment.MessagingVoteCasting(attachmentData);
            }

            @Override
            protected void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingVoteCasting attachment = (Attachment.MessagingVoteCasting) transaction.getAttachment();
                Vote.addVote(transaction, attachment);
            }

            @Override
            protected void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (Nxt.getBlockchain().getHeight() < Constants.VOTING_SYSTEM_BLOCK) {
                    throw new NxtException.NotYetEnabledException("Voting System not yet enabled at height " + Nxt.getBlockchain().getHeight());
                }

                Attachment.MessagingVoteCasting attachment = (Attachment.MessagingVoteCasting) transaction.getAttachment();
                if (attachment.getPollId() == 0 || attachment.getPollVote() == null
                        || attachment.getPollVote().length > Constants.MAX_POLL_OPTION_COUNT) {
                    throw new NxtException.NotValidException("Invalid vote casting attachment: " + attachment.getJSONObject());
                }

                long pollId = attachment.getPollId();

                Poll poll = Poll.getPoll(pollId);
                if (poll == null) {
                    throw new NxtException.NotCurrentlyValidException("Invalid poll: " + Long.toUnsignedString(attachment.getPollId()));
                }

                if (Vote.getVote(pollId, transaction.getSenderId()) != null) {
                    throw new NxtException.NotCurrentlyValidException("Double voting attempt");
                }

                if (poll.getFinishHeight() <= getFinishValidationHeight(transaction)) {
                    throw new NxtException.NotCurrentlyValidException("Voting for this poll finishes at " + poll.getFinishHeight());
                }

                byte[] votes = attachment.getPollVote();
                int positiveCount = 0;
                for (byte vote : votes) {
                    if (vote != Constants.NO_VOTE_VALUE && (vote < poll.getMinRangeValue() || vote > poll.getMaxRangeValue())) {
                        throw new NxtException.NotValidException(String.format("Invalid vote %d, vote must be between %d and %d",
                                vote, poll.getMinRangeValue(), poll.getMaxRangeValue()));
                    }
                    if (vote != Constants.NO_VOTE_VALUE) {
                        positiveCount++;
                    }
                }

                if (positiveCount < poll.getMinNumberOfOptions() || positiveCount > poll.getMaxNumberOfOptions()) {
                    throw new NxtException.NotValidException(String.format("Invalid num of choices %d, number of choices must be between %d and %d",
                            positiveCount, poll.getMinNumberOfOptions(), poll.getMaxNumberOfOptions()));
                }
            }

            @Override
            boolean isDuplicate(final Transaction transaction, final Map<TransactionType, Map<String, Boolean>> duplicates) {
                Attachment.MessagingVoteCasting attachment = (Attachment.MessagingVoteCasting) transaction.getAttachment();
                String key = Long.toUnsignedString(attachment.getPollId()) + ":" + Long.toUnsignedString(transaction.getSenderId());
                return isDuplicate(Messaging.VOTE_CASTING, key, duplicates, true);
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public static final TransactionType PHASING_VOTE_CASTING = new Messaging() {

            private final Fee PHASING_VOTE_FEE = (transaction, appendage) -> {
                Attachment.MessagingPhasingVoteCasting attachment = (Attachment.MessagingPhasingVoteCasting) transaction.getAttachment();
                return attachment.getTransactionFullHashes().size() * Constants.ONE_NXT;
            };

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_PHASING_VOTE_CASTING;
            }

            @Override
            public String getName() {
                return "PhasingVoteCasting";
            }

            @Override
            public Fee getBaselineFee(Transaction transaction) {
                return PHASING_VOTE_FEE;
            }

            @Override
            protected Attachment.MessagingPhasingVoteCasting parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) throws NxtException.NotValidException {
                return new Attachment.MessagingPhasingVoteCasting(buffer, transactionVersion);
            }

            @Override
            protected Attachment.MessagingPhasingVoteCasting parseAttachment(JSONObject attachmentData, int timestamp) throws NxtException.NotValidException {
                return new Attachment.MessagingPhasingVoteCasting(attachmentData);
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            protected void validateAttachment(Transaction transaction) throws NxtException.ValidationException {

                if (Nxt.getBlockchain().getHeight() < Constants.PHASING_BLOCK) {
                    throw new NxtException.NotYetEnabledException("Phasing not yet enabled at height " + Nxt.getBlockchain().getHeight());
                }

                Attachment.MessagingPhasingVoteCasting attachment = (Attachment.MessagingPhasingVoteCasting) transaction.getAttachment();
                byte[] revealedSecret = attachment.getRevealedSecret();
                if (revealedSecret.length > Constants.MAX_PHASING_REVEALED_SECRET_LENGTH) {
                    throw new NxtException.NotValidException("Invalid revealed secret length " + revealedSecret.length);
                }
                byte[] hashedSecret = null;
                byte algorithm = 0;

                List<byte[]> hashes = attachment.getTransactionFullHashes();
                if (hashes.size() > Constants.MAX_PHASING_VOTE_TRANSACTIONS) {
                    throw new NxtException.NotValidException("No more than " + Constants.MAX_PHASING_VOTE_TRANSACTIONS + " votes allowed for two-phased multi-voting");
                }

                long voterId = transaction.getSenderId();
                for (byte[] hash : hashes) {
                    long phasedTransactionId = Convert.fullHashToId(hash);
                    if (phasedTransactionId == 0) {
                        throw new NxtException.NotValidException("Invalid phased transactionFullHash " + Convert.toHexString(hash));
                    }

                    PhasingPoll poll = PhasingPoll.getPoll(phasedTransactionId);
                    if (poll == null) {
                        throw new NxtException.NotCurrentlyValidException("Invalid phased transaction " + Long.toUnsignedString(phasedTransactionId)
                                + ", or phasing is finished");
                    }
                    if (! poll.getVoteWeighting().acceptsVotes()) {
                        throw new NxtException.NotValidException("This phased transaction does not require or accept voting");
                    }
                    long[] whitelist = poll.getWhitelist();
                    if (whitelist.length > 0 && Arrays.binarySearch(whitelist, voterId) < 0) {
                        throw new NxtException.NotValidException("Voter is not in the phased transaction whitelist");
                    }
                    if (revealedSecret.length > 0) {
                        if (poll.getVoteWeighting().getVotingModel() != VoteWeighting.VotingModel.HASH) {
                            throw new NxtException.NotValidException("Phased transaction " + Long.toUnsignedString(phasedTransactionId) + " does not accept by-hash voting");
                        }
                        if (hashedSecret != null && !Arrays.equals(poll.getHashedSecret(), hashedSecret)) {
                            throw new NxtException.NotValidException("Phased transaction " + Long.toUnsignedString(phasedTransactionId) + " is using a different hashedSecret");
                        }
                        if (algorithm != 0 && algorithm != poll.getAlgorithm()) {
                            throw new NxtException.NotValidException("Phased transaction " + Long.toUnsignedString(phasedTransactionId) + " is using a different hashedSecretAlgorithm");
                        }
                        if (hashedSecret == null && ! poll.verifySecret(revealedSecret)) {
                            throw new NxtException.NotValidException("Revealed secret does not match phased transaction hashed secret");
                        }
                        hashedSecret = poll.getHashedSecret();
                        algorithm = poll.getAlgorithm();
                    } else if (poll.getVoteWeighting().getVotingModel() == VoteWeighting.VotingModel.HASH) {
                        throw new NxtException.NotValidException("Phased transaction " + Long.toUnsignedString(phasedTransactionId) + " requires revealed secret for approval");
                    }
                    if (!Arrays.equals(poll.getFullHash(), hash)) {
                        throw new NxtException.NotCurrentlyValidException("Phased transaction hash does not match hash in voting transaction");
                    }
                    if (poll.getFinishHeight() <= getFinishValidationHeight(transaction) + 1) {
                        throw new NxtException.NotCurrentlyValidException(String.format("Phased transaction finishes at height %d which is not after approval transaction height %d",
                                poll.getFinishHeight(), getFinishValidationHeight(transaction) + 1));
                    }
                }
            }

            @Override
            protected final void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingPhasingVoteCasting attachment = (Attachment.MessagingPhasingVoteCasting) transaction.getAttachment();
                List<byte[]> hashes = attachment.getTransactionFullHashes();
                for (byte[] hash : hashes) {
                    PhasingVote.addVote(transaction, senderAccount, Convert.fullHashToId(hash));
                }
            }

            @Override
            public boolean isPhasingSafe() {
                return true;
            }

        };

        public static final TransactionType HUB_ANNOUNCEMENT = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_HUB_ANNOUNCEMENT;
            }

            @Override
            public String getName() {
                return "HubAnnouncement";
            }

            @Override
            protected Attachment.MessagingHubAnnouncement parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) throws NxtException.NotValidException {
                return new Attachment.MessagingHubAnnouncement(buffer, transactionVersion);
            }

            @Override
            protected Attachment.MessagingHubAnnouncement parseAttachment(JSONObject attachmentData, int timestamp) throws NxtException.NotValidException {
                return new Attachment.MessagingHubAnnouncement(attachmentData);
            }

            @Override
            protected void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingHubAnnouncement attachment = (Attachment.MessagingHubAnnouncement) transaction.getAttachment();
                Hub.addOrUpdateHub(transaction, attachment);
            }

            @Override
            protected void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (Nxt.getBlockchain().getHeight() < Constants.TRANSPARENT_FORGING_BLOCK_7) {
                    throw new NxtException.NotYetEnabledException("Hub terminal announcement not yet enabled at height " + Nxt.getBlockchain().getHeight());
                }
                Attachment.MessagingHubAnnouncement attachment = (Attachment.MessagingHubAnnouncement) transaction.getAttachment();
                if (attachment.getMinFeePerByteNQT() < 0 || attachment.getMinFeePerByteNQT() > Constants.MAX_BALANCE_NQT
                        || attachment.getUris().length > Constants.MAX_HUB_ANNOUNCEMENT_URIS) {
                    // cfb: "0" is allowed to show that another way to determine the min fee should be used
                    throw new NxtException.NotValidException("Invalid hub terminal announcement: " + attachment.getJSONObject());
                }
                for (String uri : attachment.getUris()) {
                    if (uri.length() > Constants.MAX_HUB_ANNOUNCEMENT_URI_LENGTH) {
                        throw new NxtException.NotValidException("Invalid URI length: " + uri.length());
                    }
                    //TODO: also check URI validity here?
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

        public static final Messaging ACCOUNT_INFO = new Messaging() {

            @Override
            public byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ACCOUNT_INFO;
            }

            @Override
            public String getName() {
                return "AccountInfo";
            }

            @Override
            protected Attachment.MessagingAccountInfo parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) throws NxtException.NotValidException {
                return new Attachment.MessagingAccountInfo(buffer, transactionVersion);
            }

            @Override
            protected Attachment.MessagingAccountInfo parseAttachment(JSONObject attachmentData, int timestamp) throws NxtException.NotValidException {
                return new Attachment.MessagingAccountInfo(attachmentData);
            }

            @Override
            protected void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.MessagingAccountInfo attachment = (Attachment.MessagingAccountInfo)transaction.getAttachment();
                if (attachment.getName().length() > Constants.MAX_ACCOUNT_NAME_LENGTH
                        || attachment.getDescription().length() > Constants.MAX_ACCOUNT_DESCRIPTION_LENGTH) {
                    throw new NxtException.NotValidException("Invalid account info issuance: " + attachment.getJSONObject());
                }
            }

            @Override
            protected void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingAccountInfo attachment = (Attachment.MessagingAccountInfo) transaction.getAttachment();
                senderAccount.setAccountInfo(attachment.getName(), attachment.getDescription());
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

    }

    public static abstract class DigitalGoods extends TransactionType {

        private DigitalGoods() {
        }

        @Override
        public final byte getType() {
            return TransactionType.TYPE_DIGITAL_GOODS;
        }

        @Override
        protected boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        protected void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        protected final void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            if (transaction.getAmountNQT() != 0) {
                throw new NxtException.NotValidException("Invalid digital goods transaction");
            }
            doValidateAttachment(transaction);
        }

        abstract void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException;


        public static final TransactionType LISTING = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_LISTING;
            }

            @Override
            public String getName() {
                return "DigitalGoodsListing";
            }

            @Override
            protected Attachment.DigitalGoodsListing parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsListing(buffer, transactionVersion);
            }

            @Override
            protected Attachment.DigitalGoodsListing parseAttachment(JSONObject attachmentData, int timestamp) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsListing(attachmentData);
            }

            @Override
            protected void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsListing attachment = (Attachment.DigitalGoodsListing) transaction.getAttachment();
                DigitalGoodsStore.listGoods(transaction, attachment);
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.DigitalGoodsListing attachment = (Attachment.DigitalGoodsListing) transaction.getAttachment();

                // before fork LISTING attachment version must be 1 after must be > 1
                boolean isFork = HardFork.MARKETPLACE_PRICE_IN_ASSET_BLOCK(transaction.getHeight());
                if (isFork) {
                    if (attachment.getVersion() < 2) {
                        throw new NxtException.NotValidException(String.format(
                                "Version mismatch, actual %d, expected not less 2", attachment.getVersion()
                        ));
                    }
                } else {
                    if (attachment.getVersion() > 1) {
                        throw new NxtException.NotYetEnabledException("Marketplace pricing in asset not yet enabled");
                    }
                }

                if (attachment.getName().length() == 0
                        || attachment.getName().length() > Constants.MAX_DGS_LISTING_NAME_LENGTH
                        || attachment.getDescription().length() > Constants.MAX_DGS_LISTING_DESCRIPTION_LENGTH
                        || attachment.getTags().length() > Constants.MAX_DGS_LISTING_TAGS_LENGTH
                        || attachment.getQuantity() < 0 || attachment.getQuantity() > Constants.MAX_DGS_LISTING_QUANTITY
                        || attachment.getPriceNQT() <= 0 || attachment.getPriceNQT() > Constants.MAX_BALANCE_NQT) {
                    throw new NxtException.NotValidException("Invalid digital goods listing: " + attachment.getJSONObject());
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

        public static final TransactionType DELISTING = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_DELISTING;
            }

            @Override
            public String getName() {
                return "DigitalGoodsDelisting";
            }

            @Override
            protected Attachment.DigitalGoodsDelisting parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsDelisting(buffer, transactionVersion);
            }

            @Override
            protected Attachment.DigitalGoodsDelisting parseAttachment(JSONObject attachmentData, int timestamp) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsDelisting(attachmentData);
            }

            @Override
            protected void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsDelisting attachment = (Attachment.DigitalGoodsDelisting) transaction.getAttachment();
                DigitalGoodsStore.delistGoods(attachment.getGoodsId());
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.DigitalGoodsDelisting attachment = (Attachment.DigitalGoodsDelisting) transaction.getAttachment();
                DigitalGoodsStore.Goods goods = DigitalGoodsStore.Goods.getGoods(attachment.getGoodsId());
                if (goods != null && transaction.getSenderId() != goods.getSellerId()) {
                    throw new NxtException.NotValidException("Invalid digital goods delisting - seller is different: " + attachment.getJSONObject());
                }
                if (goods == null || goods.isDelisted()) {
                    throw new NxtException.NotCurrentlyValidException("Goods " + Long.toUnsignedString(attachment.getGoodsId()) +
                            "not yet listed or already delisted");
                }
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String,Boolean>> duplicates) {
                Attachment.DigitalGoodsDelisting attachment = (Attachment.DigitalGoodsDelisting) transaction.getAttachment();
                return isDuplicate(DigitalGoods.DELISTING, Long.toUnsignedString(attachment.getGoodsId()), duplicates, true);
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

        public static final TransactionType PRICE_CHANGE = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_PRICE_CHANGE;
            }

            @Override
            public String getName() {
                return "DigitalGoodsPriceChange";
            }

            @Override
            protected Attachment.DigitalGoodsPriceChange parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsPriceChange(buffer, transactionVersion);
            }

            @Override
            protected Attachment.DigitalGoodsPriceChange parseAttachment(JSONObject attachmentData, int timestamp) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsPriceChange(attachmentData);
            }

            @Override
            protected void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsPriceChange attachment = (Attachment.DigitalGoodsPriceChange) transaction.getAttachment();
                DigitalGoodsStore.changePrice(attachment.getGoodsId(), attachment.getPriceNQT());
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.DigitalGoodsPriceChange attachment = (Attachment.DigitalGoodsPriceChange) transaction.getAttachment();
                DigitalGoodsStore.Goods goods = DigitalGoodsStore.Goods.getGoods(attachment.getGoodsId());
                if (attachment.getPriceNQT() <= 0 || attachment.getPriceNQT() > Constants.MAX_BALANCE_NQT
                        || (goods != null && transaction.getSenderId() != goods.getSellerId())) {
                    throw new NxtException.NotValidException("Invalid digital goods price change: " + attachment.getJSONObject());
                }
                if (goods == null || goods.isDelisted()) {
                    throw new NxtException.NotCurrentlyValidException("Goods " + Long.toUnsignedString(attachment.getGoodsId()) +
                            "not yet listed or already delisted");
                }
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Boolean>> duplicates) {
                Attachment.DigitalGoodsPriceChange attachment = (Attachment.DigitalGoodsPriceChange) transaction.getAttachment();
                // not a bug, uniqueness is based on DigitalGoods.DELISTING
                return isDuplicate(DigitalGoods.DELISTING, Long.toUnsignedString(attachment.getGoodsId()), duplicates, true);
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public static final TransactionType QUANTITY_CHANGE = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_QUANTITY_CHANGE;
            }

            @Override
            public String getName() {
                return "DigitalGoodsQuantityChange";
            }

            @Override
            protected Attachment.DigitalGoodsQuantityChange parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsQuantityChange(buffer, transactionVersion);
            }

            @Override
            protected Attachment.DigitalGoodsQuantityChange parseAttachment(JSONObject attachmentData, int timestamp) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsQuantityChange(attachmentData);
            }

            @Override
            protected void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsQuantityChange attachment = (Attachment.DigitalGoodsQuantityChange) transaction.getAttachment();
                DigitalGoodsStore.changeQuantity(attachment.getGoodsId(), attachment.getDeltaQuantity());
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.DigitalGoodsQuantityChange attachment = (Attachment.DigitalGoodsQuantityChange) transaction.getAttachment();
                DigitalGoodsStore.Goods goods = DigitalGoodsStore.Goods.getGoods(attachment.getGoodsId());
                if (attachment.getDeltaQuantity() < -Constants.MAX_DGS_LISTING_QUANTITY
                        || attachment.getDeltaQuantity() > Constants.MAX_DGS_LISTING_QUANTITY
                        || (goods != null && transaction.getSenderId() != goods.getSellerId())) {
                    throw new NxtException.NotValidException("Invalid digital goods quantity change: " + attachment.getJSONObject());
                }
                if (goods == null || goods.isDelisted()) {
                    throw new NxtException.NotCurrentlyValidException("Goods " + Long.toUnsignedString(attachment.getGoodsId()) +
                            "not yet listed or already delisted");
                }
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Boolean>> duplicates) {
                Attachment.DigitalGoodsQuantityChange attachment = (Attachment.DigitalGoodsQuantityChange) transaction.getAttachment();
                // not a bug, uniqueness is based on DigitalGoods.DELISTING
                return isDuplicate(DigitalGoods.DELISTING, Long.toUnsignedString(attachment.getGoodsId()), duplicates, true);
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public static final TransactionType PURCHASE = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_PURCHASE;
            }

            @Override
            public String getName() {
                return "DigitalGoodsPurchase";
            }

            @Override
            protected Attachment.DigitalGoodsPurchase parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsPurchase(buffer, transactionVersion);
            }

            @Override
            protected Attachment.DigitalGoodsPurchase parseAttachment(JSONObject attachmentData, int timestamp) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsPurchase(attachmentData);
            }

            @Override
            protected boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                /* 1) check asset/fimk balance; 2) update balance */
                Attachment.DigitalGoodsPurchase attachment = (Attachment.DigitalGoodsPurchase) transaction.getAttachment();
                DigitalGoodsStore.Goods goods = DigitalGoodsStore.Goods.getGoods(attachment.getGoodsId());
                long assetId = goods.getAssetId();
                long sum = Math.multiplyExact(attachment.getQuantity(), attachment.getPriceNQT());
                if (assetId == 0) {
                    if (senderAccount.getUnconfirmedBalanceNQT() >= sum) {
                        senderAccount.addToUnconfirmedBalanceNQT(-sum);
                        return true;
                    }
                } else {
                    long assetBalance = Account.getUnconfirmedAssetBalanceQNT(senderAccount.getId(), assetId);
                    if (assetBalance >= sum) {
                        senderAccount.addToUnconfirmedAssetBalanceQNT(assetId, -sum);
                        return true;
                    }
                }
                return false;
            }

            @Override
            protected void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.DigitalGoodsPurchase attachment = (Attachment.DigitalGoodsPurchase) transaction.getAttachment();
                DigitalGoodsStore.Goods goods = DigitalGoodsStore.Goods.getGoods(attachment.getGoodsId());
                long assetId = goods.getAssetId();
                long sum = Math.multiplyExact(attachment.getQuantity(), attachment.getPriceNQT());
                if (assetId == 0) {
                    senderAccount.addToUnconfirmedBalanceNQT(sum);
                } else {
                    senderAccount.addToUnconfirmedAssetBalanceQNT(assetId, sum);
                }
            }

            @Override
            protected void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsPurchase attachment = (Attachment.DigitalGoodsPurchase) transaction.getAttachment();
                DigitalGoodsStore.purchase(transaction, attachment);
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.DigitalGoodsPurchase attachment = (Attachment.DigitalGoodsPurchase) transaction.getAttachment();
                DigitalGoodsStore.Goods goods = DigitalGoodsStore.Goods.getGoods(attachment.getGoodsId());
                if (goods == null || goods.isDelisted()) {
                    throw new NxtException.NotCurrentlyValidException("Goods " + Long.toUnsignedString(attachment.getGoodsId()) +
                            "not yet listed or already delisted");
                }
                if (attachment.getQuantity() <= 0 || attachment.getQuantity() > Constants.MAX_DGS_LISTING_QUANTITY
                        || attachment.getPriceNQT() <= 0
                        || (goods.getAssetId() == 0 && attachment.getPriceNQT() > Constants.MAX_BALANCE_NQT)
                        || (goods.getSellerId() != transaction.getRecipientId())) {
                    throw new NxtException.NotValidException("Invalid digital goods purchase: " + attachment.getJSONObject());
                }
                if (transaction.getEncryptedMessage() != null && ! transaction.getEncryptedMessage().isText()) {
                    throw new NxtException.NotValidException("Only text encrypted messages allowed");
                }
                if (attachment.getQuantity() > goods.getQuantity()) {
                    throw new NxtException.NotCurrentlyValidException("Goods quantity exceeds the available balance: " + attachment.getJSONObject());
                }
                if (attachment.getPriceNQT() != goods.getPriceNQT()) {
                    throw new NxtException.NotCurrentlyValidException("Goods price is wrong: " + attachment.getJSONObject());
                }
                if (attachment.getDeliveryDeadlineTimestamp() <= Nxt.getBlockchain().getLastBlockTimestamp()) {
                    throw new NxtException.NotCurrentlyValidException("Delivery deadline has already expired: " + attachment.getDeliveryDeadlineTimestamp());
                }
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Boolean>> duplicates) {
                if (Nxt.getBlockchain().getHeight() < Constants.MONETARY_SYSTEM_BLOCK) {
                    return false;
                }
                Attachment.DigitalGoodsPurchase attachment = (Attachment.DigitalGoodsPurchase) transaction.getAttachment();
                // not a bug, uniqueness is based on DigitalGoods.DELISTING
                return isDuplicate(DigitalGoods.DELISTING, Long.toUnsignedString(attachment.getGoodsId()), duplicates, false);
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public static final TransactionType DELIVERY = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_DELIVERY;
            }

            @Override
            public String getName() {
                return "DigitalGoodsDelivery";
            }

            @Override
            protected Attachment.DigitalGoodsDelivery parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsDelivery(buffer, transactionVersion);
            }

            @Override
            protected Attachment.DigitalGoodsDelivery parseAttachment(JSONObject attachmentData, int timestamp) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsDelivery(attachmentData);
            }

            @Override
            protected void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsDelivery attachment = (Attachment.DigitalGoodsDelivery)transaction.getAttachment();
                DigitalGoodsStore.deliver(transaction, attachment);
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.DigitalGoodsDelivery attachment = (Attachment.DigitalGoodsDelivery) transaction.getAttachment();
                DigitalGoodsStore.Purchase purchase = DigitalGoodsStore.Purchase.getPendingPurchase(attachment.getPurchaseId());
                long assetId = 0;
                if (purchase != null) {
                    DigitalGoodsStore.Goods goods = DigitalGoodsStore.Goods.getGoods(purchase.getGoodsId());
                    assetId = goods.getAssetId();
                }
                int maxGoodsLength = Nxt.getBlockchain().getHeight() > Constants.VOTING_SYSTEM_BLOCK
                        ? Constants.MAX_DGS_GOODS_LENGTH_2 : Constants.MAX_DGS_GOODS_LENGTH;
                if (attachment.getGoods().getData().length > maxGoodsLength
                        || attachment.getGoods().getData().length == 0
                        || attachment.getGoods().getNonce().length != 32
                        || attachment.getDiscountNQT() < 0
                        || (assetId == 0 && attachment.getDiscountNQT() > Constants.MAX_BALANCE_NQT)
                        || (purchase != null &&
                        (purchase.getBuyerId() != transaction.getRecipientId()
                                || transaction.getSenderId() != purchase.getSellerId()
                                || attachment.getDiscountNQT() > Math.multiplyExact(purchase.getPriceNQT(), purchase.getQuantity())))) {
                    throw new NxtException.NotValidException("Invalid digital goods delivery: " + attachment.getJSONObject());
                }
                if (purchase == null || purchase.getEncryptedGoods() != null) {
                    throw new NxtException.NotCurrentlyValidException("Purchase does not exist yet, or already delivered: "
                            + attachment.getJSONObject());
                }
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Boolean>> duplicates) {
                Attachment.DigitalGoodsDelivery attachment = (Attachment.DigitalGoodsDelivery) transaction.getAttachment();
                return isDuplicate(DigitalGoods.DELIVERY, Long.toUnsignedString(attachment.getPurchaseId()), duplicates, true);
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public static final TransactionType FEEDBACK = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_FEEDBACK;
            }

            @Override
            public String getName() {
                return "DigitalGoodsFeedback";
            }

            @Override
            protected Attachment.DigitalGoodsFeedback parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsFeedback(buffer, transactionVersion);
            }

            @Override
            protected Attachment.DigitalGoodsFeedback parseAttachment(JSONObject attachmentData, int timestamp) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsFeedback(attachmentData);
            }

            @Override
            protected void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsFeedback attachment = (Attachment.DigitalGoodsFeedback)transaction.getAttachment();
                DigitalGoodsStore.feedback(attachment.getPurchaseId(), transaction.getEncryptedMessage(), transaction.getMessage());
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.DigitalGoodsFeedback attachment = (Attachment.DigitalGoodsFeedback) transaction.getAttachment();
                DigitalGoodsStore.Purchase purchase = DigitalGoodsStore.Purchase.getPurchase(attachment.getPurchaseId());
                if (purchase != null &&
                        (purchase.getSellerId() != transaction.getRecipientId()
                                || transaction.getSenderId() != purchase.getBuyerId())) {
                    throw new NxtException.NotValidException("Invalid digital goods feedback: " + attachment.getJSONObject());
                }
                if (transaction.getEncryptedMessage() == null && transaction.getMessage() == null) {
                    throw new NxtException.NotValidException("Missing feedback message");
                }
                if (transaction.getEncryptedMessage() != null && ! transaction.getEncryptedMessage().isText()) {
                    throw new NxtException.NotValidException("Only text encrypted messages allowed");
                }
                if (transaction.getMessage() != null && ! transaction.getMessage().isText()) {
                    throw new NxtException.NotValidException("Only text public messages allowed");
                }
                if (purchase == null || purchase.getEncryptedGoods() == null) {
                    throw new NxtException.NotCurrentlyValidException("Purchase does not exist yet or not yet delivered");
                }
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

        public static final TransactionType REFUND = new DigitalGoods() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_DIGITAL_GOODS_REFUND;
            }

            @Override
            public String getName() {
                return "DigitalGoodsRefund";
            }

            @Override
            protected Attachment.DigitalGoodsRefund parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsRefund(buffer, transactionVersion);
            }

            @Override
            protected Attachment.DigitalGoodsRefund parseAttachment(JSONObject attachmentData, int timestamp) throws NxtException.NotValidException {
                return new Attachment.DigitalGoodsRefund(attachmentData);
            }

            private boolean applyUnconfirmedInternal(Attachment.DigitalGoodsRefund attachment, Account senderAccount, int coef) {
                DigitalGoodsStore.Purchase purchase = DigitalGoodsStore.Purchase.getPurchase(attachment.getPurchaseId());
                DigitalGoodsStore.Goods goods = DigitalGoodsStore.Goods.getGoods(purchase.getGoodsId());
                long assetId = goods.getAssetId();
                if (assetId == 0) {
                    if (senderAccount.getUnconfirmedBalanceNQT() >= attachment.getRefundNQT()) {
                        senderAccount.addToUnconfirmedBalanceNQT(coef * attachment.getRefundNQT());
                        return true;
                    }
                } else {
                    if (senderAccount.getUnconfirmedAssetBalanceQNT(assetId) >= attachment.getRefundNQT()) {
                        senderAccount.addToUnconfirmedAssetBalanceQNT(assetId, coef * attachment.getRefundNQT());
                        return true;
                    }
                }
                return false;
            }

            @Override
            protected boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.DigitalGoodsRefund attachment = (Attachment.DigitalGoodsRefund) transaction.getAttachment();
                return applyUnconfirmedInternal(attachment, senderAccount, -1);
            }

            @Override
            protected void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
                Attachment.DigitalGoodsRefund attachment = (Attachment.DigitalGoodsRefund) transaction.getAttachment();
                applyUnconfirmedInternal(attachment, senderAccount, 1);
            }

            @Override
            protected void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.DigitalGoodsRefund attachment = (Attachment.DigitalGoodsRefund) transaction.getAttachment();
                DigitalGoodsStore.refund(transaction.getSenderId(), attachment.getPurchaseId(),
                        attachment.getRefundNQT(), transaction.getEncryptedMessage());
            }

            @Override
            void doValidateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.DigitalGoodsRefund attachment = (Attachment.DigitalGoodsRefund) transaction.getAttachment();
                DigitalGoodsStore.Purchase purchase = DigitalGoodsStore.Purchase.getPurchase(attachment.getPurchaseId());
                if (attachment.getRefundNQT() < 0 || attachment.getRefundNQT() > Constants.MAX_BALANCE_NQT
                        || (purchase != null &&
                        (purchase.getBuyerId() != transaction.getRecipientId()
                                || transaction.getSenderId() != purchase.getSellerId()))) {
                    throw new NxtException.NotValidException("Invalid digital goods refund: " + attachment.getJSONObject());
                }
                if (transaction.getEncryptedMessage() != null && ! transaction.getEncryptedMessage().isText()) {
                    throw new NxtException.NotValidException("Only text encrypted messages allowed");
                }
                if (purchase == null || purchase.getEncryptedGoods() == null || purchase.getRefundNQT() != 0) {
                    throw new NxtException.NotCurrentlyValidException("Purchase does not exist or is not delivered or is already refunded");
                }
            }

            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Boolean>> duplicates) {
                Attachment.DigitalGoodsRefund attachment = (Attachment.DigitalGoodsRefund) transaction.getAttachment();
                return isDuplicate(DigitalGoods.REFUND, Long.toUnsignedString(attachment.getPurchaseId()), duplicates, true);
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };

    }

    public static abstract class Data extends TransactionType {

        private static final Fee TAGGED_DATA_FEE = new Fee.SizeBasedFee(Constants.ONE_NXT, Constants.ONE_NXT/10) {
            @Override
            public int getSize(TransactionImpl transaction, Appendix appendix) {
                return appendix.getFullSize();
            }
        };

        private Data() {
        }

        @Override
        public final byte getType() {
            return TransactionType.TYPE_DATA;
        }

        @Override
        public final Fee getBaselineFee(Transaction transaction) {
            return TAGGED_DATA_FEE;
        }

        @Override
        protected final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        protected final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        final void validateAttachmentAtFinish(Transaction transaction) {
        }

        @Override
        public final boolean canHaveRecipient() {
            return false;
        }

        @Override
        public final boolean isPhasingSafe() {
            return false;
        }

        @Override
        public final boolean isPhasable() {
            return false;
        }

        public static final TransactionType TAGGED_DATA_UPLOAD = new Data() {

            @Override
            public byte getSubtype() {
                return SUBTYPE_DATA_TAGGED_DATA_UPLOAD;
            }

            @Override
            protected Attachment.TaggedDataUpload parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) throws NxtException.NotValidException {
                return new Attachment.TaggedDataUpload(buffer, transactionVersion);
            }

            @Override
            protected Attachment.TaggedDataUpload parseAttachment(JSONObject attachmentData, int timestamp) throws NxtException.NotValidException {
                return new Attachment.TaggedDataUpload(attachmentData);
            }

            @Override
            protected void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (Nxt.getBlockchain().getHeight() < Constants.TAGGED_DATA_BLOCK) {
                    throw new NxtException.NotYetEnabledException("Prunable Tagged Data not yet enabled");
                }
                Attachment.TaggedDataUpload attachment = (Attachment.TaggedDataUpload) transaction.getAttachment();
                if (attachment.getData() == null && Nxt.getEpochTime() - transaction.getTimestamp() < Constants.MIN_PRUNABLE_LIFETIME) {
                    throw new NxtException.NotCurrentlyValidException("Data has been pruned prematurely");
                }
                if (attachment.getData() != null) {
                    if (attachment.getName().length() == 0 || attachment.getName().length() > Constants.MAX_TAGGED_DATA_NAME_LENGTH) {
                        throw new NxtException.NotValidException("Invalid name length: " + attachment.getName().length());
                    }
                    if (attachment.getDescription().length() > Constants.MAX_TAGGED_DATA_DESCRIPTION_LENGTH) {
                        throw new NxtException.NotValidException("Invalid description length: " + attachment.getDescription().length());
                    }
                    if (attachment.getTags().length() > Constants.MAX_TAGGED_DATA_TAGS_LENGTH) {
                        throw new NxtException.NotValidException("Invalid tags length: " + attachment.getTags().length());
                    }
                    if (attachment.getType().length() > Constants.MAX_TAGGED_DATA_TYPE_LENGTH) {
                        throw new NxtException.NotValidException("Invalid type length: " + attachment.getType().length());
                    }
                    if (attachment.getChannel().length() > Constants.MAX_TAGGED_DATA_CHANNEL_LENGTH) {
                        throw new NxtException.NotValidException("Invalid channel length: " + attachment.getChannel().length());
                    }
                    if (attachment.getFilename().length() > Constants.MAX_TAGGED_DATA_FILENAME_LENGTH) {
                        throw new NxtException.NotValidException("Invalid filename length: " + attachment.getFilename().length());
                    }
                    if (attachment.getData().length == 0 || attachment.getData().length > Constants.MAX_TAGGED_DATA_DATA_LENGTH) {
                        throw new NxtException.NotValidException("Invalid data length: " + attachment.getData().length);
                    }
                }
            }

            @Override
            protected void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.TaggedDataUpload attachment = (Attachment.TaggedDataUpload) transaction.getAttachment();
                TaggedData.add(transaction, attachment);
            }

            @Override
            public String getName() {
                return "TaggedDataUpload";
            }

        };

        public static final TransactionType TAGGED_DATA_EXTEND = new Data() {

            @Override
            public byte getSubtype() {
                return SUBTYPE_DATA_TAGGED_DATA_EXTEND;
            }

            @Override
            protected Attachment.TaggedDataExtend parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) throws NxtException.NotValidException {
                return new Attachment.TaggedDataExtend(buffer, transactionVersion);
            }

            @Override
            protected Attachment.TaggedDataExtend parseAttachment(JSONObject attachmentData, int timestamp) throws NxtException.NotValidException {
                return new Attachment.TaggedDataExtend(attachmentData);
            }

            @Override
            protected void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (Nxt.getBlockchain().getHeight() < Constants.TAGGED_DATA_BLOCK) {
                    throw new NxtException.NotYetEnabledException("Prunable Tagged Data not yet enabled");
                }
                Attachment.TaggedDataExtend attachment = (Attachment.TaggedDataExtend) transaction.getAttachment();
                if ((attachment.jsonIsPruned() || attachment.getData() == null) && Nxt.getEpochTime() - transaction.getTimestamp() < Constants.MIN_PRUNABLE_LIFETIME) {
                    throw new NxtException.NotCurrentlyValidException("Data has been pruned prematurely");
                }
                TransactionImpl uploadTransaction = TransactionDb.findTransaction(attachment.getTaggedDataId(), Nxt.getBlockchain().getHeight());
                if (uploadTransaction == null) {
                    throw new NxtException.NotCurrentlyValidException("No such tagged data upload " + Long.toUnsignedString(attachment.getTaggedDataId()));
                }
                if (uploadTransaction.getType() != TAGGED_DATA_UPLOAD) {
                    throw new NxtException.NotValidException("Transaction " + Long.toUnsignedString(attachment.getTaggedDataId())
                            + " is not a tagged data upload");
                }
                if (attachment.getData() != null) {
                    Attachment.TaggedDataUpload taggedDataUpload = (Attachment.TaggedDataUpload)uploadTransaction.getAttachment();
                    if (!Arrays.equals(attachment.getHash(), taggedDataUpload.getHash())) {
                        throw new NxtException.NotValidException("Hashes don't match! Extend hash: " + Convert.toHexString(attachment.getHash())
                                + " upload hash: " + Convert.toHexString(taggedDataUpload.getHash()));
                    }
                }
            }

            @Override
            protected void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.TaggedDataExtend attachment = (Attachment.TaggedDataExtend) transaction.getAttachment();
                TaggedData.extend(transaction, attachment);
            }

            @Override
            public String getName() {
                return "TaggedDataExtend";
            }

        };

    }

}
