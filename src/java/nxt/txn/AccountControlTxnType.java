package nxt.txn;

import nxt.*;
import nxt.reward.RewardCandidate;
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

    public static Attachment.EmptyAttachment REWARD_APPLICANT_REGISTRATION_ATTACHMENT = new Attachment.EmptyAttachment() {
        @Override
        public TransactionType getTransactionType() {
            return REWARD_APPLICANT_REGISTRATION;
        }
    };

    public static final TransactionType REWARD_APPLICANT_REGISTRATION = new AccountControlTxnType() {

        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_REWARD_APPLICANT_REGISTRATION;
        }

        @Override
        public String getName() {
            return "RewardApplicantRegistration";
        }

        @Override
        public Fee getBaselineFee(Transaction transaction) {
            return Fee.NONE;
        }

        @Override
        protected Attachment.EmptyAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) {
            return REWARD_APPLICANT_REGISTRATION_ATTACHMENT;
        }

        @Override
        protected Attachment.EmptyAttachment parseAttachment(JSONObject attachmentData, int timestamp) {
            return REWARD_APPLICANT_REGISTRATION_ATTACHMENT;
        }

        @Override
        protected void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
            if (! HardFork.POS_POP_REWARD_BLOCK(-1)) {
                throw new NxtException.NotYetEnabledException("Reward applicant registration not yet enabled at height " + Nxt.getBlockchain().getHeight());
            }

            if (transaction.getAmountNQT() != 0) {
                throw new NxtException.NotValidException("Invalid amount (must be zero)");
            }
            Account a = Account.getAccount(transaction.getSenderId());
            if (a == null || a.getGuaranteedBalanceNQT() < Constants.REWARD_APPLICANT_MIN_BALANCE) {
                throw new NxtException.NotValidException("Sender balance is less than the min "
                        + Constants.REWARD_APPLICANT_MIN_BALANCE + " FIMK");
            }

            // do not accept transaction if there is the same type previous txn (same sender) less than N blocks ago
            int currentHeight = Nxt.getBlockchain().getHeight();
            int preHeight = TransactionDb.hasTransaction(
                    TransactionType.TYPE_ACCOUNT_CONTROL,
                    TransactionType.SUBTYPE_REWARD_APPLICANT_REGISTRATION,
                    currentHeight - Constants.REWARD_APPLICANT_REGISTRATION_ACCEPT_HEIGHT_LIMIT,
                    currentHeight,
                    transaction.getSenderId()
            );
            if (preHeight > 0) {
                throw new NxtException.NotValidException(String.format(
                        "Current height is %d, previous transaction at height %d, the difference %d is less required %d",
                        currentHeight, preHeight, currentHeight - preHeight,
                        Constants.REWARD_APPLICANT_REGISTRATION_ACCEPT_HEIGHT_LIMIT)
                );
            }
        }

        @Override
        protected void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            RewardCandidate.save(transaction, -1);
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
