package nxt.txn;

import nxt.*;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * Assign rewarding rules to asset
 * <p>
 * Owner of private asset assign rewarding rules (new transaction type) for private asset:
 * 1. every N blocks: number (0 means disabled);
 * 2. target account: REGISTERED_POP_REWARD_RECEIVER or FORGER or CONSTANT_ACCOUNT;
 * 3. lottery type (in case REGISTERED_POP_REWARD_RECEIVER):
 * a) random candidate, reward amount proportional candidate balance,
 * b) random weighted (by balance) candidate, constant reward amount;
 * 4. reward amount parameters: a) base reward amount, b) balance divider (to calculate reward amount) (for case 3.a), c) balance's asset id (for case 3.a and 3.b);
 * <p>
 * Rule 3 is used if the REGISTERED_POP_REWARD_RECEIVER is specified in the 2. Item 4.c means what asset's balance is used.
 * For example, the rules are assigned to private asset A, but the balance of asset B is used for calculation amount of reward in asset A.
 * Seems in most cases 4.c will be the balance in rewarding asset or balance in fimk.
 */
public class AssetRewardingTxnType extends ColoredCoinsTxnTypes {

    public enum Target {
        REGISTERED_POP_REWARD_RECEIVER(0),
        FORGER(1),
        CONSTANT_ACCOUNT(2);

        public final int code;

        Target(int code) {
            this.code = code;
        }

        public static Target get(byte code) {
            if (code == 0) return REGISTERED_POP_REWARD_RECEIVER;
            if (code == 1) return FORGER;
            if (code == 2) return CONSTANT_ACCOUNT;
            return null;
        }
    }

    public enum LotteryType {
        /**
         * random candidate, reward amount proportional candidate balance
         */
        RANDOM_ACCOUNT(0),
        /**
         * random weighted (by balance) candidate, constant reward amount
         */
        RANDOM_WEIGHTED_ACCOUNT(1);

        public final int code;

        LotteryType(int code) {
            this.code = code;
        }

        public static LotteryType get(byte code) {
            if (code == 0) return RANDOM_ACCOUNT;
            else if (code == 1) return RANDOM_WEIGHTED_ACCOUNT;
            return null;
        }
    }

    @Override
    public final byte getSubtype() {
        return TransactionType.SUBTYPE_COLORED_COINS_ASSET_REWARDING;
    }

    @Override
    public String getName() {
        return "AssetRewarding";
    }

    @Override
    protected AssetRewardingAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) {
        return new AssetRewardingAttachment(buffer, transactionVersion, timestamp);
    }

    @Override
    protected AssetRewardingAttachment parseAttachment(JSONObject attachmentData, int timestamp) {
        return new AssetRewardingAttachment(attachmentData, timestamp);
    }

    @Override
    protected boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    protected void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        AssetRewardingAttachment a = (AssetRewardingAttachment) transaction.getAttachment();

        AssetRewarding.save(transaction, a);

//        if (a.getTarget() == Target.REGISTERED_POP_REWARD_RECEIVER.code) {
//
//        }
//
//        if (a.getTarget() == Target.FORGER.code) {
//            a.getBaseAmount()
//        }
//
//        if (a.getTarget() == Target.CONSTANT_ACCOUNT.code) {
//
//        }
//
//        senderAccount.payDividends(attachment.getAssetId(), attachment.getHeight(), attachment.getAmountNQTPerQNT());
    }

    @Override
    protected void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
    }

    @Override
    protected void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
        if (Nxt.getBlockchain().getHeight() < Constants.PRIVATE_ASSETS_REWARD_BLOCK) {
            throw new NxtException.NotYetEnabledException("Private asset rewarding not yet enabled at height " + Nxt.getBlockchain().getHeight());
        }
        AssetRewardingAttachment a = (AssetRewardingAttachment) transaction.getAttachment();
        Asset asset = Asset.getAsset(a.getAsset());
        if (asset == null) {
            throw new NxtException.NotCurrentlyValidException(
                    "Asset " + Long.toUnsignedString(a.getAsset()) + " doesn't exist yet");
        }
        if (!MofoAsset.isPrivateAsset(asset)) {
            throw new NxtException.NotValidException("Asset " + Long.toUnsignedString(a.getAsset()) + " is not private");
        }
        if (asset.getAccountId() != transaction.getSenderId()) {
            throw new NxtException.NotValidException(
                    String.format("Asset issuer only can assign the asset rewarding. Issuer %s, sender %s",
                            Long.toUnsignedString(asset.getAccountId()), Long.toUnsignedString(transaction.getSenderId()))
            );
        }
        if (a.getFrequency() < 0) throw new NxtException.NotValidException("Wrong frequency");
        if (a.getTarget() < 0 || a.getTarget() > 2) throw new NxtException.NotValidException("Wrong target");
        if (a.getLotteryType() < 0 || a.getLotteryType() > 1) throw new NxtException.NotValidException("Wrong lottery type");
        if (a.getBaseAmount() < 0 || a.getBaseAmount() > Constants.MAX_ASSET_REWARDING_BASE_AMOUNT_QNT) {
            throw new NxtException.NotValidException("Wrong base amount");
        }
        if (a.getBalanceDivider() < 0) throw new NxtException.NotValidException("Wrong balance divider");
        if (a.getTarget() == Target.REGISTERED_POP_REWARD_RECEIVER.code) {
            Asset balanceAssetId = Asset.getAsset(a.getTargetInfo());
            if (balanceAssetId == null) throw new NxtException.NotValidException("Balance's asset is not known");
        }
        if (a.getTarget() == Target.CONSTANT_ACCOUNT.code) {
            Account targetAccount = Account.getAccount(a.getTargetInfo());
            if (targetAccount == null) throw new NxtException.NotValidException("Rewarding account is not known");
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

}
