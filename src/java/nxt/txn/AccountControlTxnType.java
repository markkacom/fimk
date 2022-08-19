package nxt.txn;

import nxt.*;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

public abstract class AccountControlTxnType extends TransactionType {

    private AccountControlTxnType() {
    }

    @Override
    public final byte getType() {
        return TransactionType.TYPE_ACCOUNT_CONTROL;
    }

    @Override
    protected final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    protected final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
    }

    public static final TransactionType EFFECTIVE_BALANCE_LEASING = new AccountControlTxnType() {

        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING;
        }

        @Override
        public String getName() {
            return "EffectiveBalanceLeasing";
        }

        @Override
        protected EffectiveBalanceLeasingAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) throws NxtException.NotValidException {
            return new EffectiveBalanceLeasingAttachment(buffer, transactionVersion);
        }

        @Override
        protected EffectiveBalanceLeasingAttachment parseAttachment(JSONObject attachmentData, int timestamp) throws NxtException.NotValidException {
            return new EffectiveBalanceLeasingAttachment(attachmentData);
        }

        @Override
        protected void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            EffectiveBalanceLeasingAttachment attachment = (EffectiveBalanceLeasingAttachment) transaction.getAttachment();
            Account.getAccount(transaction.getSenderId()).leaseEffectiveBalance(transaction.getRecipientId(), attachment.getPeriod());
        }

        @Override
        protected void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            EffectiveBalanceLeasingAttachment attachment = (EffectiveBalanceLeasingAttachment) transaction.getAttachment();
            Account recipientAccount = Account.getAccount(transaction.getRecipientId());
            if (transaction.getSenderId() == transaction.getRecipientId()
                    || transaction.getAmountNQT() != 0
                    || attachment.getPeriod() < Constants.LEASING_DELAY) {
                throw new NxtException.NotValidException("Invalid effective balance leasing: "
                        + transaction.getJSONObject() + " transaction " + transaction.getStringId());
            }
            if (recipientAccount == null || recipientAccount.getKeyHeight() <= 0) {
                throw new NxtException.NotCurrentlyValidException("Invalid effective balance leasing: "
                        + " recipient account " + transaction.getRecipientId() + " not found or no public key published");
            }
            if (transaction.getRecipientId() == Genesis.CREATOR_ID) {
                throw new NxtException.NotCurrentlyValidException("Leasing to Genesis account not allowed");
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

    public static final TransactionType POP_REWARD_CHALLENGE = new AccountControlTxnType() {

        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING;
        }

        @Override
        public String getName() {
            return "EffectiveBalanceLeasing";
        }

        @Override
        protected EffectiveBalanceLeasingAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) throws NxtException.NotValidException {
            return new EffectiveBalanceLeasingAttachment(buffer, transactionVersion);
        }

        @Override
        protected EffectiveBalanceLeasingAttachment parseAttachment(JSONObject attachmentData, int timestamp) throws NxtException.NotValidException {
            return new EffectiveBalanceLeasingAttachment(attachmentData);
        }

        @Override
        protected void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            EffectiveBalanceLeasingAttachment attachment = (EffectiveBalanceLeasingAttachment) transaction.getAttachment();
            Account.getAccount(transaction.getSenderId()).leaseEffectiveBalance(transaction.getRecipientId(), attachment.getPeriod());
        }

        @Override
        protected void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            EffectiveBalanceLeasingAttachment attachment = (EffectiveBalanceLeasingAttachment) transaction.getAttachment();
            Account recipientAccount = Account.getAccount(transaction.getRecipientId());
            if (transaction.getSenderId() == transaction.getRecipientId()
                    || transaction.getAmountNQT() != 0
                    || attachment.getPeriod() < Constants.LEASING_DELAY) {
                throw new NxtException.NotValidException("Invalid effective balance leasing: "
                        + transaction.getJSONObject() + " transaction " + transaction.getStringId());
            }
            if (recipientAccount == null || recipientAccount.getKeyHeight() <= 0) {
                throw new NxtException.NotCurrentlyValidException("Invalid effective balance leasing: "
                        + " recipient account " + transaction.getRecipientId() + " not found or no public key published");
            }
            if (transaction.getRecipientId() == Genesis.CREATOR_ID) {
                throw new NxtException.NotCurrentlyValidException("Leasing to Genesis account not allowed");
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

}
