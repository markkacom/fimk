package nxt.txn;

import nxt.Attachment;
import nxt.TransactionType;
import nxt.util.Convert;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

public final class AssetRewardingAttachment extends Attachment.AbstractAttachment {

    private final long asset;
    private final int frequency; // every N blocks. 0 means disabled rewarding
    private final byte target;  // 0)REGISTERED_POP_REWARD_RECEIVER or 1)FORGER or 2)CONSTANT_ACCOUNT (account id)
    /**
     * a) random candidate with reward amount proportional candidate balance,
     * b) constant amount with random weighted (by balance) candidate;
     */
    private final byte lotteryType;  // case REGISTERED_POP_REWARD_RECEIVER
    private final long baseAmount;  // case REGISTERED_POP_REWARD_RECEIVER
    private final long balanceDivider;  // case REGISTERED_POP_REWARD_RECEIVER. reward = accountBalance * baseAmount / balanceDivider   Reward is equal baseAmount when account balance is equal baseBalance.
    private final long targetInfo;  // field plays roles: balanceAssetId (case REGISTERED_POP_REWARD_RECEIVER) or targetAccount (case CONSTANT_ACCOUNT) or nothing (case FORGER)
//todo add field expiry

    AssetRewardingAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) {
        super(buffer, transactionVersion);
        this.asset = buffer.getLong();
        this.frequency = buffer.getInt();
        this.target = buffer.get();
        this.lotteryType = buffer.get();
        this.baseAmount = buffer.getLong();
        this.balanceDivider = buffer.getLong();
        this.targetInfo = buffer.getLong();
    }

    AssetRewardingAttachment(JSONObject attachmentData, int timestamp) {
        super(attachmentData);
        this.asset = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
        this.frequency = ((Long) attachmentData.get("frequency")).intValue();
        this.target = ((Long) attachmentData.get("target")).byteValue();
        this.lotteryType = ((Long) attachmentData.get("privateLotteryType")).byteValue();
        this.baseAmount = Convert.parseLong(attachmentData.get("baseAmount"));
        this.balanceDivider = Convert.parseLong(attachmentData.get("baseBalance"));
        this.targetInfo = Convert.parseLong(attachmentData.get("a"));
    }

    public AssetRewardingAttachment(long asset, int frequency, byte target, byte lotteryType, long baseAmount, long balanceDivider, long targetInfo) {
        this.asset = asset;
        this.frequency = frequency;
        this.target = target;
        this.lotteryType = lotteryType;
        this.baseAmount = baseAmount;
        this.balanceDivider = balanceDivider;
        this.targetInfo = targetInfo;
    }

    @Override
    protected int getMySize() {
        return 8 + 4 + 1 + 1 + 8 + 8 + 8;
    }

    @Override
    protected void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(asset);
        buffer.putInt(frequency);
        buffer.put(target);
        buffer.put(lotteryType);
        buffer.putLong(baseAmount);
        buffer.putLong(balanceDivider);
        buffer.putLong(targetInfo);
    }

    @Override
    protected void putMyJSON(JSONObject attachment) {
        attachment.put("asset", asset);
        attachment.put("frequency", frequency);
        attachment.put("target", target);
        attachment.put("privateLotteryType", lotteryType);
        attachment.put("baseAmount", baseAmount);
        attachment.put("baseBalance", balanceDivider);
        attachment.put(fieldName(), targetInfo);
    }

    @Override
    public TransactionType getTransactionType() {
        return ColoredCoinsTxnTypes.ASSET_REWARDING;
    }

    public long getAsset() {
        return asset;
    }

    public int getFrequency() {
        return frequency;
    }

    public byte getTarget() {
        return target;
    }

    public byte getLotteryType() {
        return lotteryType;
    }

    public long getBaseAmount() {
        return baseAmount;
    }

    public long getBalanceDivider() {
        return balanceDivider;
    }

    public long getTargetInfo() {
        return targetInfo;
    }

    private String fieldName() {
        if (target == 0) return "balanceAssetId";
        else if (target == 1) return "forger";
        else if (target == 2) return "account";
        return null;
    }

}
