package nxt.http;

import nxt.Account;
import nxt.Asset;
import nxt.Attachment;
import nxt.NxtException;
import nxt.txn.AssetRewardingAttachment;
import nxt.txn.AssetRewardingTxnType;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class AssignAssetRewarding extends CreateTransaction {

    static final AssignAssetRewarding instance = new AssignAssetRewarding();

    private AssignAssetRewarding() {
        super(new APITag[] {APITag.CREATE_TRANSACTION},
                "asset", "target", "lotteryType", "frequency", "baseAmount", "balanceDivider", "balanceAssetId", "targetAccount");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Asset asset = ParameterParser.getAsset(req);
        AssetRewardingTxnType.Target target = AssetRewardingTxnType.Target.get(
                (byte) ParameterParser.getInt(req, "target", 0, 2,  true));
        int frequency = ParameterParser.getInt(req, "frequency", 0, Integer.MAX_VALUE,  true);
        long baseAmount = ParameterParser.getLong(req, "baseAmount", 1, Long.MAX_VALUE,  true);
        long balanceDivider = ParameterParser.getLong(req, "balanceDivider", 1, Long.MAX_VALUE,  false);

        long targetInfo = 0;
        AssetRewardingTxnType.LotteryType lotteryType = null;
        if (target == AssetRewardingTxnType.Target.REGISTERED_POP_REWARD_RECEIVER) {
            targetInfo = ParameterParser.getUnsignedLong(req, "balanceAssetId", false);
            lotteryType = AssetRewardingTxnType.LotteryType.get(
                    (byte) ParameterParser.getInt(req, "lotteryType", 0, 1,  false));
        }
        if (target == AssetRewardingTxnType.Target.CONSTANT_ACCOUNT) {
            targetInfo = ParameterParser.getAccountId(req, "targetAccount", true);
        }

        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = new AssetRewardingAttachment(
                asset.getId(), frequency, (byte) target.code,
                lotteryType == null ? 0 : (byte) lotteryType.code,
                baseAmount, balanceDivider, targetInfo);
        return createTransaction(req, account, attachment);
    }

}
