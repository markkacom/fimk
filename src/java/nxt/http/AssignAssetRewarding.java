package nxt.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import nxt.Account;
import nxt.Asset;
import nxt.Attachment;
import nxt.NxtException;
import nxt.txn.AssetRewardingAttachment;
import nxt.txn.AssetRewardingTxnType;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/fimk?requestType=assignAssetRewarding")
public final class AssignAssetRewarding extends CreateTransaction {

    static final AssignAssetRewarding instance = new AssignAssetRewarding();

    private AssignAssetRewarding() {
        super(new APITag[] {APITag.CREATE_TRANSACTION},
                "asset", "target", "lotteryType", "frequency", "halvingBlocks", "baseAmount", "balanceDivider", "balanceAssetId", "targetAccount");
    }

    @Override
    @POST
    @Operation(summary = "Assign rewarding rules to asset",
            tags = {APITag2.ASSET, APITag2.CREATE_TRANSACTION},
            description = "")
    @Parameter(name = "asset", in = ParameterIn.QUERY, required = true)
    @Parameter(name = "target", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer"),
            description = "method of resolving rewardee account: 0 - registered POP reward receiver, 1 - forger, 2 - constant account")
    @Parameter(name = "frequency", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer"),
            description = "apply reward every N blocks (0 means disabled)")
    @Parameter(name = "baseAmount", in = ParameterIn.QUERY, required = true,
            description = "base amount of reward")
    @Parameter(name = "lotteryType", in = ParameterIn.QUERY, schema = @Schema(type = "integer"),
            description = "lottery type (in case target is 0): 0 - random candidate, reward amount proportional candidate balance, 1 - random weighted (by balance) candidate, constant reward amount")
    @Parameter(name = "halvingBlocks", in = ParameterIn.QUERY, schema = @Schema(type = "integer"),
            description = "halving reward amount every N blocks")
    @Parameter(name = "balanceDivider", in = ParameterIn.QUERY,
            description = "balance divider (to calculate reward amount) (for case lottery type is 0)")
    @Parameter(name = "balanceAssetId", in = ParameterIn.QUERY,
            description = "asset id which balance is used (for cases lottery type is 0 or 1)")
    @Parameter(name = "targetAccount", in = ParameterIn.QUERY,
            description = "account id used when target is 2 (constant account)")
    public JSONStreamAware processRequest(@Parameter(hidden = true) HttpServletRequest req) throws NxtException {
        Asset asset = ParameterParser.getAsset(req);
        AssetRewardingTxnType.Target target = AssetRewardingTxnType.Target.get(
                (byte) ParameterParser.getInt(req, "target", 0, 2,  true));
        int frequency = ParameterParser.getInt(req, "frequency", 0, Integer.MAX_VALUE,  true);
        int halvingBlocks = ParameterParser.getInt(req, "halvingBlocks", 0, Integer.MAX_VALUE,  false);
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
                baseAmount, balanceDivider, targetInfo, halvingBlocks);
        return createTransaction(req, account, attachment);
    }

}
